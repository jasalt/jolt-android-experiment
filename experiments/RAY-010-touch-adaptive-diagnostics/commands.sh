#!/usr/bin/env bash
# Build and exercise touch-first adaptive diagnostics on the API-35 emulator.
set -euo pipefail

root=$(cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$root"
out="$root/experiments/RAY-010-touch-adaptive-diagnostics/evidence"
serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibgallery
activity=android.app.NativeActivity
apk="$root/raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk"
mkdir -p "$out"

reset_display() {
  adb -s "$serial" shell wm size reset >/dev/null 2>&1 || true
  adb -s "$serial" shell settings put system accelerometer_rotation 0 >/dev/null 2>&1 || true
  adb -s "$serial" shell settings put system user_rotation 0 >/dev/null 2>&1 || true
}
trap reset_display EXIT

: "${JOLT_SOURCE:?set to a clean checkout at the pinned Jolt revision}"
(cd raylib && jolt -M:test) | tee "$out/pure-tests.txt"
JOLT_SOURCE="$JOLT_SOURCE" ./scripts/raylib-persistent-loop-build-android \
  2>&1 | tee "$out/build.log"
./scripts/emulator-start headless | tee "$out/emulator-start.txt"
adb -s "$serial" install -r "$apk" | tee "$out/install.txt"
adb -s "$serial" shell am force-stop "$package"
reset_display
adb -s "$serial" logcat -c
adb -s "$serial" shell am start -W -n "$package/$activity" \
  >"$out/portrait-launch.txt"
sleep 5

# Tap, hold, and drag are separate interactions. Capture the hold while active
# so both visible marker and machine-readable down state are preserved.
adb -s "$serial" shell input tap 540 1200
sleep 1
adb -s "$serial" shell input swipe 400 1000 400 1000 2500 &
hold_pid=$!
sleep 1
adb -s "$serial" exec-out screencap -p >"$out/portrait-hold.png"
wait "$hold_pid"
adb -s "$serial" shell input swipe 250 1000 800 1500 1200
sleep 2
adb -s "$serial" exec-out screencap -p >"$out/portrait-after-drag.png"
adb -s "$serial" logcat -d -v threadtime >"$out/portrait-logcat.txt"
identify "$out/portrait-hold.png" "$out/portrait-after-drag.png" \
  >"$out/portrait-identify.txt"
grep -Fq ':screen [1080 2400]' "$out/portrait-logcat.txt"
grep -Fq ':orientation :portrait' "$out/portrait-logcat.txt"
grep -Fq ':phase :press' "$out/portrait-logcat.txt"
grep -Fq ':phase :down' "$out/portrait-logcat.txt"
grep -Fq ':phase :release' "$out/portrait-logcat.txt"
grep -Eq ':touches \{:count 1, :ids \[[0-9]+' "$out/portrait-logcat.txt"
grep -Eq ':point-0 \[[1-9][0-9]* [1-9][0-9]*\]' "$out/portrait-logcat.txt"
grep -Eq ':tap-count [1-9][0-9]*' "$out/portrait-logcat.txt"
grep -Eq ':drag-samples [1-9][0-9]*' "$out/portrait-logcat.txt"

# Select landscape and relaunch the NativeActivity. Raylib's public resize
# behavior during configChanges belongs to the separate lifecycle task; this
# task proves that a fresh loop derives layout from the live landscape metrics.
adb -s "$serial" shell settings put system user_rotation 1
sleep 2
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" logcat -c
adb -s "$serial" shell am start -W -n "$package/$activity" \
  >"$out/landscape-launch.txt"
sleep 5
adb -s "$serial" exec-out screencap -p >"$out/landscape.png"
adb -s "$serial" logcat -d -v threadtime >"$out/landscape-logcat.txt"
identify "$out/landscape.png" >"$out/landscape-identify.txt"
grep -Fq ':orientation :landscape' "$out/landscape-logcat.txt"
grep -Eq ':screen \[[0-9]+ [0-9]+\].*:orientation :landscape' \
  "$out/landscape-logcat.txt"

# A second physical-size override proves layout input is not fixed to the
# default Pixel dimensions. Relaunch after selecting portrait so Raylib reports
# the new scalar metrics at frame zero.
adb -s "$serial" shell input keyevent 4 || true
sleep 2
adb -s "$serial" shell wm size 720x1280
adb -s "$serial" shell settings put system user_rotation 0
adb -s "$serial" logcat -c
adb -s "$serial" shell am start -W -n "$package/$activity" \
  >"$out/small-portrait-launch.txt"
sleep 5
adb -s "$serial" exec-out screencap -p >"$out/small-portrait.png"
adb -s "$serial" logcat -d -v threadtime >"$out/small-portrait-logcat.txt"
identify "$out/small-portrait.png" >"$out/small-portrait-identify.txt"
grep -Fq ':screen [720 1280]' "$out/small-portrait-logcat.txt"
grep -Fq ':orientation :portrait' "$out/small-portrait-logcat.txt"

# Android Back must produce a retrievable close-requested state and orderly
# same-thread Jolt shutdown rather than a crash or an indefinite background loop.
adb -s "$serial" shell input keyevent 4
sleep 3
adb -s "$serial" logcat -d -v threadtime >"$out/back-logcat.txt"
grep -Fq ':close-requested? true' "$out/back-logcat.txt"
grep -Fq 'persistent-loop bootstrap complete' "$out/back-logcat.txt"
! grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibgallery' \
  "$out/back-logcat.txt"

sha256sum "$out"/*.png >"$out/SHA256SUMS"
printf 'Touch edges/state, point-0, adaptive portrait/landscape/two-size layout, and deterministic Back passed.\n' |
  tee "$out/result.txt"
