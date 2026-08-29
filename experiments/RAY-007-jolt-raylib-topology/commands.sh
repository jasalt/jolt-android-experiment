#!/usr/bin/env bash
# Prove topology A: static Raylib in libmain.so, separate Jolt library, and
# one Jolt FFI call resolved against the current process image.
set -euo pipefail

root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$root"
out="$root/experiments/RAY-007-jolt-raylib-topology/evidence"
serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibtopologyprobe
activity=android.app.NativeActivity
apk="$root/raylib/topology-android/build/outputs/apk/debug/raylib-topology-android-debug.apk"
mkdir -p "$out"
inspect_dir="$(mktemp -d)"
trap 'rm -rf "$inspect_dir"' EXIT

: "${JOLT_SOURCE:?set to a clean checkout at the pinned Jolt revision}"
JOLT_SOURCE="$JOLT_SOURCE" ./scripts/raylib-topology-build-android \
  2>&1 | tee "$out/build.log"

unzip -l "$apk" | grep -E 'lib/arm64-v8a/(libmain|libjoltraylib-topology)\.so$' \
  | tee "$out/apk-libraries.txt"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$apk" \
  | grep -E 'package:|launchable-activity:' | tee "$out/apk-badging.txt"
unzip -p "$apk" lib/arm64-v8a/libmain.so > "$inspect_dir/libmain.so"
unzip -p "$apk" lib/arm64-v8a/libjoltraylib-topology.so \
  > "$inspect_dir/libjoltraylib-topology.so"
file "$inspect_dir/libmain.so" | tee "$out/libmain-file.txt"
file "$inspect_dir/libjoltraylib-topology.so" | tee "$out/libjoltraylib-file.txt"
nm -D --defined-only "$inspect_dir/libmain.so" \
  | grep -E ' (ANativeActivity_onCreate|android_main|GetScreenWidth)$' \
  | tee "$out/libmain-raylib-symbols.txt"
readelf -d "$inspect_dir/libmain.so" | grep NEEDED | tee "$out/libmain-needed.txt"
readelf -d "$root/native/jolt/android-arm64/arm64-v8a/libjoltraylib-topology.so" \
  | grep NEEDED | tee "$out/jolt-needed.txt"
! grep -Fq 'libraylib' "$out/jolt-needed.txt"
! unzip -l "$apk" | grep -q 'libraylib'
nm -D --defined-only "$inspect_dir/libjoltraylib-topology.so" \
  | grep -E 'jolt_(library_init|lookup|library_shutdown)$' \
  | tee "$out/jolt-symbols.txt"

./scripts/emulator-start headless
run_once() {
  local label="$1"
  adb -s "$serial" shell am force-stop "$package"
  adb -s "$serial" logcat -c
  adb -s "$serial" shell am start -W -n "$package/$activity" \
    > "$out/$label-launch.txt"
  sleep 4
  adb -s "$serial" logcat -d -v threadtime > "$out/$label-logcat.txt"
  grep -E 'jolt_raylib_topology|Berberis|nativeloader' \
    "$out/$label-logcat.txt" > "$out/$label-key-lines.txt"
  ! grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibtopologyprobe' \
    "$out/$label-logcat.txt"
  grep -Fq 'direct process dlsym GetScreenWidth=ok' "$out/$label-logcat.txt"
  grep -Fq 'jolt_lookup raylib_process_screen_width=ok' "$out/$label-logcat.txt"
  grep -Fq 'GetScreenWidth via process lookup result=0' "$out/$label-logcat.txt"
  grep -Fq 'topology A complete' "$out/$label-logcat.txt"

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
printf 'Topology A passed twice with one Jolt-to-Raylib process lookup and no current-run crash or ANR\n' \
  | tee "$out/result.txt"
