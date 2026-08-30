#!/usr/bin/env bash
# Verify the shared reducer through the built Raylib Android host.
set -euo pipefail

root=$(cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$root"
serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibgallery
apk="$root/raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk"
out="$root/experiments/RAY-014-shared-reducer-raylib/evidence"
mkdir -p "$out"
: "${JOLT_SOURCE:?set JOLT_SOURCE to a clean checkout at the pinned Jolt revision}"

(cd raylib && jolt -M:test) | tee "$out/pure-tests.txt"
JOLT_SOURCE="$JOLT_SOURCE" ./scripts/raylib-persistent-loop-build-android \
  2>&1 | tee "$out/aot-build.txt"
./scripts/emulator-start headless | tee "$out/emulator-start.txt"
adb -s "$serial" install -r "$apk" | tee "$out/install.txt"
adb -s "$serial" shell wm size reset >/dev/null
adb -s "$serial" logcat -c
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" shell am start -W -n "$package/android.app.NativeActivity" \
  >"$out/launch.txt"
sleep 2
adb -s "$serial" shell input tap 283 523
sleep .5
adb -s "$serial" shell input tap 540 2277
sleep .4
adb -s "$serial" exec-out screencap -p >"$out/reducer-counter.png"
adb -s "$serial" shell input tap 198 2277
sleep .4
adb -s "$serial" shell input tap 40 40
sleep .3
adb -s "$serial" shell input keyevent 4
sleep .5
adb -s "$serial" logcat -d -s jolt_raylib_gallery_state:* >"$out/reducer.log"
adb -s "$serial" logcat -d -s jolt_raylib_gallery:* >"$out/bootstrap.log"
adb -s "$serial" logcat -d >"$out/logcat.txt"

grep -Fq ':last-event {:type :counter/inc}' "$out/reducer.log"
grep -Fq ':last-event {:type :counter/dec}' "$out/reducer.log"
grep -Fq ':shared-model {:counter 1' "$out/reducer.log"
grep -Fq ':type :storage/write' "$out/reducer.log"
if grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibgallery' "$out/logcat.txt"; then
  echo 'reducer run contains a crash or ANR marker' >&2
  exit 1
fi
printf 'Shared reducer event/effect flow and Raylib runtime smoke passed.\n' |
  tee "$out/result.txt"
