#!/usr/bin/env bash
# Build and inspect the independent plain-C Raylib Android APK.
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"

./scripts/raylib-build-android
apk=raylib/android/build/outputs/apk/debug/raylib-android-debug.apk
work_dir=${RAYLIB_PLAIN_C_WORK_DIR:-/tmp/raylib-plain-c-android}
rm -rf "$work_dir"
mkdir -p "$work_dir"

unzip -l "$apk" | tee "$work_dir/apk-listing.txt"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$apk" |
  tee "$work_dir/aapt-badging.txt"
unzip -p "$apk" lib/arm64-v8a/libmain.so >"$work_dir/libmain.so"
file "$work_dir/libmain.so" | tee "$work_dir/file.txt"
readelf -d "$work_dir/libmain.so" | tee "$work_dir/readelf-dynamic.txt"
nm -D --defined-only "$work_dir/libmain.so" | grep -E ' (android_main|ANativeActivity_onCreate)$' |
  tee "$work_dir/native-entry-symbols.txt"
