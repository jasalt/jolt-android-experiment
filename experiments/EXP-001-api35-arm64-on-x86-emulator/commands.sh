#!/usr/bin/env bash
# Run from the repository root through the pinned shell:
#   nix develop -c ./experiments/EXP-001-api35-arm64-on-x86-emulator/commands.sh
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
experiment_dir="$repo_root/experiments/EXP-001-api35-arm64-on-x86-emulator"
output_dir="$repo_root/artifacts/abi-probe"
compiler="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android35-clang"

: "${ANDROID_NDK_ROOT:?run through nix develop}"
[[ -x "$compiler" ]] || { printf 'missing NDK compiler: %s\n' "$compiler" >&2; exit 1; }
mkdir -p "$output_dir"
"$compiler" -shared -fPIC -Wl,-soname,libabi_probe.so \
  -o "$output_dir/libabi_probe.so" "$experiment_dir/minimal/poc_answer.c"
file "$output_dir/libabi_probe.so"
readelf -h "$output_dir/libabi_probe.so"
readelf -Ws "$output_dir/libabi_probe.so" | grep -F 'poc_answer'
