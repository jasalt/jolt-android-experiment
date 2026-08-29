# Observed 2026-08-29

Starting with `app/.cxx`, `app/build`, `build`, and Gradle caches removed:

```text
nix develop -c ./scripts/test-portable
Ran 6 tests. 13 assertions passed, 0 failures, 0 errors.

nix develop -c env JOLT_SOURCE=../jolt scripts/jolt-android-library-build
... wrote Android 35 AArch64 libjoltpoc.so ...

readelf -h: Class ELF64, Machine AArch64
readelf -d: libm.so, libdl.so, libc.so
nm: jolt_library_init, jolt_lookup, jolt_library_collect,
    jolt_library_shutdown

gradle --no-daemon :app:assembleDebug
BUILD SUCCESSFUL
APK: lib/arm64-v8a/libjolt_probe.so; lib/arm64-v8a/libjoltpoc.so
adb install: Success
nativeloader libjolt_probe.so: ok
jolt_probe: owner JNI session initialized
UI: Runtime: initialized; Process ABI: x86_64;
    Jolt thread: HandlerThread(JoltRuntime);
    lifecycle :resumed; worker :completed;
```

The successful run used the current x86_64 Lima guest and API-35 x86_64
emulator. The ARM64 library therefore remains translation evidence, not native
ARM64-host evidence. `JOLT_SOURCE` is an explicit clean-room prerequisite; the
repository does not claim a clone-free source archive.

## Automated rerun — 2026-08-29

`scripts/verify` now composes this clean-room sequence from the pinned Nix
shell. With `JOLT_SOURCE` at Jolt PR #778's merged revision, it passed portable
fixtures, Android cross-library rebuild/static ABI verification, APK assembly,
API-35 emulator instrumentation, install/launch, lifecycle recreation,
showcase captures, measurements, and a fresh logcat crash-marker scan. It
retained per-tier logs under `artifacts/logs/verify/` and wrote
`artifacts/reports/verify-summary.txt`. GTK was explicitly skipped because this
non-display shell had no `DISPLAY`; the known non-PIC host library boundary was
explicitly skipped per EXP-022. Neither skip is reported as a pass for that
host-specific tier.
