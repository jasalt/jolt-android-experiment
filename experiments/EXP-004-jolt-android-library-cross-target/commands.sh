#!/usr/bin/env bash
# Run from the repository root inside `nix develop`.
set -euo pipefail

export JOLT_SOURCE="${JOLT_SOURCE:?set to pinned ../jolt checkout}"
scripts/jolt-android-library-build

readelf -h native/jolt/android-arm64/arm64-v8a/libjoltpoc.so | grep -E 'Class:|Machine:'
readelf -d native/jolt/android-arm64/arm64-v8a/libjoltpoc.so | grep NEEDED
nm -D --defined-only native/jolt/android-arm64/arm64-v8a/libjoltpoc.so \
  | grep -E 'jolt_library_(init|lookup|shutdown)'

gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop net.joltlang.androidpoc.abiprobe
adb logcat -c
adb shell monkey -p net.joltlang.androidpoc.abiprobe 1
sleep 5
adb logcat -d -v brief | grep -E 'jolt_probe|nativeloader'
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb exec-out cat /sdcard/window.xml | grep -o 'Jolt answer() = 42'
