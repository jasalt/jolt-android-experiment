# Jolt Android library lifecycle stress

## Purpose

Exercise the minimal Android ARM64 Jolt managed-library proof beyond its
one-shot `poc_answer()` call. This experiment covers lifecycle behavior that
must be established before using the runtime from an Android application.

## Scope

`jolt_probe` permits exactly one lifecycle per process and only lets its owner
thread enter Jolt. It performs initialization, repeated ABI-table export calls,
an allocating Jolt call, explicit heap compaction, a post-compaction call,
owner-thread shutdown, and a deterministic rejected second initialization.

A deliberately created foreign pthread is rejected by the **JNI wrapper before
it calls Jolt**. This is an Android integration policy that avoids unproven Chez
thread activation/deactivation behavior; it is not evidence that Chez/Jolt
would independently reject a correctly activated foreign thread.

## Reproduction

Run from the repository root in the pinned development shell:

```sh
JOLT_SOURCE=../jolt nix develop -c scripts/jolt-android-library-build
gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop net.joltlang.androidpoc.abiprobe
adb logcat -c
adb shell monkey -p net.joltlang.androidpoc.abiprobe 1
sleep 5
adb logcat -d -v brief | grep jolt_probe
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb exec-out cat /sdcard/window.xml | grep -o 'Jolt stress = 42; repeat init = -1'
```

Force-stop and repeat the last five commands to test process relaunch.

## Result

See [actual.txt](actual.txt) and
[`artifacts/logs/exp005-jolt-stress-runtime.txt`](../../artifacts/logs/exp005-jolt-stress-runtime.txt).
