# Observed 2026-08-28

A clean APK built with Kotlin 2.1.0 and installed on the deterministic API 35
x86_64 emulator. The application packages ARM64 Jolt artifacts and runs them
through Android’s ARM64 translation path.

The first request is started by the activity, while the second is requested by
the UI/main-thread completion callback. Both JNI entries are observed on the
single dedicated runtime thread:

```text
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: owner JNI thread recorded
I/jolt_probe: one=42 calls=10000 allocation=100000 compact=ok again=42 wrong-thread=rejected shutdown=ok
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: repeat init rejected
Jolt runtime stress = 42; repeat init = -1
```

No `AndroidRuntime` fatal exception or native crash was emitted. `onDestroy`
uses `quitSafely()` after the operation has returned and its native lifecycle
has already called `jolt_library_shutdown`.

The explicit native non-owner return value (`-11`) is an executable guard but
is not intentionally invoked from Java in this run: the safe Android contract
is to enqueue all work through `JoltRuntime`, not to call JNI from arbitrary
callbacks.
