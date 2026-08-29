#!/usr/bin/env bash
# Run the bounded 15-minute Jolt-owned Raylib loop and lifecycle checks.
set -euo pipefail

root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$root"
out="$root/experiments/RAY-009-jolt-persistent-loop/evidence"
serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibloopprobe
activity=android.app.NativeActivity
apk="$root/raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk"
mkdir -p "$out"
inspect_dir="$(mktemp -d)"
trap 'rm -rf "$inspect_dir"' EXIT

: "${JOLT_SOURCE:?set to a clean checkout at the pinned Jolt revision}"
JOLT_SOURCE="$JOLT_SOURCE" ./scripts/raylib-persistent-loop-build-android \
  2>&1 | tee "$out/build.log"
unzip -l "$apk" | grep -E 'lib/arm64-v8a/(libmain|libjoltraylib-loop)\.so$' \
  | tee "$out/apk-libraries.txt"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$apk" \
  | grep -E 'package:|launchable-activity:' | tee "$out/apk-badging.txt"
unzip -p "$apk" lib/arm64-v8a/libmain.so > "$inspect_dir/libmain.so"
unzip -p "$apk" lib/arm64-v8a/libjoltraylib-loop.so > "$inspect_dir/libjoltraylib-loop.so"
file "$inspect_dir/libmain.so" | tee "$out/libmain-file.txt"
file "$inspect_dir/libjoltraylib-loop.so" | tee "$out/libjoltraylib-file.txt"
nm -D --defined-only "$inspect_dir/libmain.so" \
  | grep -E ' (ANativeActivity_onCreate|android_main)$' | tee "$out/libmain-symbols.txt"
nm -D --defined-only "$inspect_dir/libjoltraylib-loop.so" \
  | grep -E 'jolt_(library_init|lookup|library_shutdown)$' | tee "$out/jolt-symbols.txt"

./scripts/emulator-start headless
adb -s "$serial" install -r "$apk"
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" logcat -c
adb -s "$serial" shell am start -W -n "$package/$activity" \
  > "$out/first-launch.txt"

capture_state() {
  local label="$1"
  adb -s "$serial" exec-out screencap -p > "$out/$label.png"
  identify "$out/$label.png" > "$out/$label-identify.txt"
  adb -s "$serial" shell dumpsys meminfo "$package" > "$out/$label-meminfo.txt"
  adb -s "$serial" shell dumpsys gfxinfo "$package" framestats > "$out/$label-framestats.txt" || true
  adb -s "$serial" shell pidof "$package" | tr -d '\r' > "$out/$label-pid.txt"
}

sleep 35
capture_state 30s
adb -s "$serial" shell input keyevent 26
sleep 2
adb -s "$serial" shell input keyevent 26
sleep 5
capture_state post-resume
sleep 258
capture_state 5m
test -s "$out/5m-pid.txt"
# Keep the window alive for the 15-minute screenshot, then allow its 905s
# bounded run to close normally.
sleep 600
capture_state 15m
test -s "$out/15m-pid.txt"
sleep 15
adb -s "$serial" logcat -d -v threadtime > "$out/first-logcat.txt"
! grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibloopprobe' \
  "$out/first-logcat.txt"
grep -Fq 'raylib_persistent_loop result=' "$out/first-logcat.txt"
grep -Fq 'persistent-loop bootstrap complete' "$out/first-logcat.txt"
grep -Fq 'PLATFORM: ANDROID: Initialized successfully' "$out/first-logcat.txt"
grep -Fq 'Renderer: Android Emulator OpenGL ES Translator' "$out/first-logcat.txt"

# A fresh process must repeat the same initialization and close path.
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" logcat -c
adb -s "$serial" shell am start -W -n "$package/$activity" \
  > "$out/relaunch-launch.txt"
sleep 35
capture_state relaunch
sleep 5
adb -s "$serial" logcat -d -v threadtime > "$out/relaunch-logcat.txt"
! grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibloopprobe' \
  "$out/relaunch-logcat.txt"
grep -Fq 'jolt_lookup raylib_persistent_loop=ok' "$out/relaunch-logcat.txt"
grep -Fq 'PLATFORM: ANDROID: Initialized successfully' "$out/relaunch-logcat.txt"
grep -Fq 'Renderer: Android Emulator OpenGL ES Translator' "$out/relaunch-logcat.txt"
test -s "$out/relaunch-pid.txt"
owner="$(sed -n 's/.*enter main thread=\([0-9][0-9]*\).*/\1/p' \
  "$out/first-logcat.txt" | head -1)"
test -n "$owner"
grep -Eq "thread=$owner( |$)" "$out/first-logcat.txt"
grep -Eq "owner=$owner( |$)" "$out/first-logcat.txt"
relaunch_owner="$(sed -n 's/.*enter main thread=\([0-9][0-9]*\).*/\1/p' \
  "$out/relaunch-logcat.txt" | head -1)"
test -n "$relaunch_owner"
grep -Eq "thread=$relaunch_owner( |$)" "$out/relaunch-logcat.txt"
grep -Eq "owner=$relaunch_owner( |$)" "$out/relaunch-logcat.txt"

adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r' \
  > "$out/device-primary-abi.txt"
adb -s "$serial" shell getprop ro.product.cpu.abilist | tr -d '\r' \
  > "$out/device-abi-list.txt"
printf '15-minute Jolt-owned loop, background/resume, and relaunch passed without current-run crash or ANR\n' \
  | tee "$out/result.txt"
