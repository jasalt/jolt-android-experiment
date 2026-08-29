#!/usr/bin/env bash
# Run from the repository root inside the pinned nix develop shell.
set -euo pipefail

root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
jolt_source="${JOLT_SOURCE:?set to a clean checkout at the pinned Jolt revision}"
chez="$root/.cache/chezscheme-v10.4.1"
out="$root/experiments/RAY-005-jolt-raylib-library/evidence"
lib="$root/native/jolt/android-arm64/arm64-v8a/libjoltraylib.so"
mkdir -p "$out"

[[ "$(git -C "$jolt_source" rev-parse HEAD)" == \
  ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e ]]

JOLT_SOURCE="$jolt_source" "$root/scripts/raylib-jolt-android-library-build" \
  2>&1 | tee "$out/android-build.log"
first_sha="$(sha256sum "$lib")"
JOLT_SOURCE="$jolt_source" "$root/scripts/raylib-jolt-android-library-build" \
  2>&1 | tee -a "$out/android-build.log"
second_sha="$(sha256sum "$lib")"
printf 'first:  %s\nsecond: %s\n' "$first_sha" "$second_sha" \
  | tee "$out/android-clean-rebuild.txt"
# Jolt's generated shared object records its temporary flat-image path, so
# clean rebuilds are success-gated rather than byte-for-byte identical.
test -s "$lib"

file "$lib" | tee "$out/android-file.txt"
readelf -h "$lib" | grep -E 'Class:|OS/ABI:|Machine:' | tee "$out/android-elf-header.txt"
readelf -d "$lib" | tee "$out/android-dynamic.txt"
nm -D --defined-only "$lib" | grep -E 'jolt_(library_init|lookup|library_shutdown)$' \
  | tee "$out/android-jolt-symbols.txt"
# The managed export name is resolved through jolt_lookup, so it is not an ELF
# dynamic symbol. Keep the generated Jolt binary and source export declaration
# as two independent records of this table entry.
strings -a "$lib" | grep -E 'raylib_host_noop|poc\.raylib\.host' \
  | tee "$out/android-export-strings.txt" || true
rg -n 'raylib_host_noop|ffi/export!' "$root/raylib/android-jolt/src" \
  | tee "$out/export-source.txt"

# Resolve and call the same export in a host-built copy. This tests the Jolt
# lookup table and no-op ABI without claiming that the host can execute ARM64.
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
host_csv="$work/ta6le"
mkdir -p "$host_csv"
for file in scheme.h equates.h libkernel.a petite.boot scheme.boot; do
  ln -s "$chez/ta6le/boot/ta6le/$file" "$host_csv/$file"
done
ln -s "$chez/ta6le/lz4/lib/liblz4.a" "$host_csv/liblz4.a"
ln -s "$chez/ta6le/zlib/lib/libz.a" "$host_csv/libz.a"
JOLT_CHEZ="$chez/ta6le/bin/ta6le/scheme" \
JOLT_CHEZ_CSV="$host_csv" \
JOLT_PWD="$root/raylib/android-jolt" \
jolt build --library -m poc.raylib.host -o "$work/libjoltraylib-host.so" \
  2>&1 | tee "$out/host-build.log"
cc "$root/experiments/RAY-005-jolt-raylib-library/raylib-library-harness.c" \
  -ldl -o "$work/raylib-library-harness"
"$work/raylib-library-harness" "$work/libjoltraylib-host.so" \
  | tee "$out/host-lookup.log"

# Run the unchanged primary static ABI and build gates.
"$root/scripts/test-android-library" | tee "$out/primary-library-gate.txt"
gradle --no-daemon :app:assembleDebug 2>&1 | tee "$out/primary-apk-build.log"

(
  cd "$root"
  sha256sum "native/jolt/android-arm64/arm64-v8a/libjoltraylib.so" \
    > "experiments/RAY-005-jolt-raylib-library/evidence/SHA256SUMS"
  sha256sum -c "experiments/RAY-005-jolt-raylib-library/evidence/SHA256SUMS"
)
