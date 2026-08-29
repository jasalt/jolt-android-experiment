#!/usr/bin/env bash
# Run from the repository root through the pinned shell:
#   nix develop -c ./experiments/EXP-001-api35-arm64-on-x86-emulator/commands.sh
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
experiment_dir="$repo_root/experiments/EXP-001-api35-arm64-on-x86-emulator"
output_dir="$repo_root/artifacts/abi-probe"
: "${ANDROID_NDK_ROOT:?run through nix develop}"
compiler="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android35-clang"
serial=emulator-5554

[[ -x "$compiler" ]] || {
  printf 'missing NDK compiler: %s\n' "$compiler" >&2
  exit 1
}
mkdir -p "$output_dir"
"$compiler" -shared -fPIC -Wl,-soname,libabi_probe.so \
  -o "$output_dir/libabi_probe.so" "$experiment_dir/minimal/poc_answer.c"
file "$output_dir/libabi_probe.so"
readelf -h "$output_dir/libabi_probe.so"
readelf -Ws "$output_dir/libabi_probe.so" | grep -F 'poc_answer'

gradle :app:assembleDebug
apk=app/build/outputs/apk/debug/app-debug.apk
unzip -l "$apk" | grep -F 'lib/arm64-v8a/libabi_probe.so'
./scripts/emulator-start headless
adb -s "$serial" logcat -c
adb -s "$serial" install -r "$apk"
adb -s "$serial" shell am start -W -n net.joltlang.androidpoc.abiprobe/.MainActivity
printf 'primary ABI: '
adb -s "$serial" shell getprop ro.product.cpu.abi
printf 'ABI list: '
adb -s "$serial" shell getprop ro.product.cpu.abilist
adb -s "$serial" shell uiautomator dump /sdcard/exp001-window.xml >/dev/null
adb -s "$serial" exec-out cat /sdcard/exp001-window.xml | grep -F 'poc_answer() = 42'
adb -s "$serial" logcat -d -v brief | grep -F 'libabi_probe.so' | grep -F ': ok'
