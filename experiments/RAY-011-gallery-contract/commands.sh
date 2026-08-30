#!/usr/bin/env bash
# Verify the pure gallery lifecycle/input contract and its Android AOT inclusion.
set -euo pipefail

root=$(cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$root"
out="$root/experiments/RAY-011-gallery-contract/evidence"
serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibgallery
apk="$root/raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk"
mkdir -p "$out"

: "${JOLT_SOURCE:?set to a clean checkout at the pinned Jolt revision}"
(cd raylib && jolt -M:test) | tee "$out/pure-tests.txt"
JOLT_SOURCE="$JOLT_SOURCE" ./scripts/raylib-persistent-loop-build-android \
  2>&1 | tee "$out/aot-build.txt"

# Contracts must not acquire duplicate host ownership or excluded input/native
# concepts. Vector2 may appear only in the explicit unavailable-boundary text.
if grep -Eq 'InitWindow|CloseWindow|jolt_library|gamepad|native-pointer' \
    raylib/src/poc/raylib/gallery.cljc; then
  echo 'gallery contract acquired excluded host/native ownership' >&2
  exit 1
fi
if grep -Eq 'gamepad|native-pointer' raylib/src/poc/raylib/diagnostics.cljc; then
  echo 'normalized input acquired excluded gamepad/native state' >&2
  exit 1
fi
printf 'gallery contract has no window/runtime/gamepad/native-pointer ownership\n' \
  | tee "$out/source-boundary.txt"

./scripts/emulator-start headless >"$out/emulator-start.txt"
adb -s "$serial" install -r "$apk" >"$out/install.txt"
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" logcat -c
adb -s "$serial" shell am start -W -n "$package/android.app.NativeActivity" \
  >"$out/launch.txt"
sleep 5
adb -s "$serial" logcat -d -v threadtime >"$out/logcat.txt"
grep -F ':gallery-contract 1' "$out/logcat.txt" | head -1 \
  >"$out/runtime-contract-state.txt"
grep -Fq ':gesture {:code 0}' "$out/runtime-contract-state.txt"
grep -Fq ':keyboard {:activate? false' "$out/runtime-contract-state.txt"
if grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibgallery' \
    "$out/logcat.txt"; then
  echo 'current gallery invocation contains a crash or ANR marker' >&2
  exit 1
fi
adb -s "$serial" shell input keyevent 4
printf 'Pure lifecycle/input tests, AOT shared-library build, and runtime contract state passed.\n' \
  | tee "$out/result.txt"
