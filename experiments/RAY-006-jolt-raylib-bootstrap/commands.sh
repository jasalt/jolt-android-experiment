#!/usr/bin/env bash
# Build and run the isolated Raylib/Jolt NativeActivity bootstrap.
set -euo pipefail

root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$root"
out="$root/experiments/RAY-006-jolt-raylib-bootstrap/evidence"
serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibjoltprobe
activity=android.app.NativeActivity
apk="$root/raylib/jolt-android/build/outputs/apk/debug/raylib-jolt-android-debug.apk"
mkdir -p "$out"
inspect_dir="$(mktemp -d)"
trap 'rm -rf "$inspect_dir"' EXIT

: "${JOLT_SOURCE:?set to a clean checkout at the pinned Jolt revision}"
JOLT_SOURCE="$JOLT_SOURCE" ./scripts/raylib-jolt-bootstrap-build \
  2>&1 | tee "$out/build.log"

unzip -l "$apk" | grep -E 'lib/arm64-v8a/(libmain|libjoltraylib)\.so$' \
  | tee "$out/apk-libraries.txt"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$apk" \
  | grep -E "package:|launchable-activity:" | tee "$out/apk-badging.txt"
unzip -p "$apk" lib/arm64-v8a/libmain.so > "$inspect_dir/libmain.so"
file "$inspect_dir/libmain.so" | tee "$out/libmain-file.txt"
unzip -p "$apk" lib/arm64-v8a/libjoltraylib.so > "$inspect_dir/libjoltraylib.so"
file "$inspect_dir/libjoltraylib.so" | tee "$out/libjoltraylib-file.txt"

./scripts/emulator-start headless
run_once() {
  local label="$1"
  adb -s "$serial" shell am force-stop "$package"
  adb -s "$serial" logcat -c
  adb -s "$serial" shell am start -W -n "$package/$activity" \
    > "$out/$label-launch.txt"
  sleep 4
  adb -s "$serial" logcat -d -v threadtime > "$out/$label-logcat.txt"
  grep -E 'jolt_raylib_bootstrap|Berberis|nativeloader' \
    "$out/$label-logcat.txt" > "$out/$label-key-lines.txt"
  ! grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibjoltprobe' \
    "$out/$label-logcat.txt"

  owner="$(sed -n 's/.*enter main thread=\([0-9][0-9]*\).*/\1/p' \
    "$out/$label-logcat.txt" | head -1)"
  test -n "$owner"
  grep -Eq "thread=$owner( |$)" "$out/$label-logcat.txt"
  grep -Eq "owner=$owner( |$)" "$out/$label-logcat.txt"
}
run_once first
run_once relaunch

adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r' \
  > "$out/device-primary-abi.txt"
adb -s "$serial" shell getprop ro.product.cpu.abilist | tr -d '\r' \
  > "$out/device-abi-list.txt"
adb -s "$serial" shell pm path "$package" > "$out/package-path.txt"
printf 'Bootstrap runs completed without current-run crash or ANR\n' \
  | tee "$out/result.txt"
