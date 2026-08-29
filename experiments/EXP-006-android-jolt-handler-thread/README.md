# Android Jolt HandlerThread confinement

## Purpose

Establish the Phase 6 boundary before adding domain dispatch, Compose, or Android
callbacks: only one `HandlerThread("JoltRuntime")` may invoke JNI/Jolt.

## Design

`JoltRuntime` owns a `HandlerThread` and posts its native stress operation to
that queue. Results are posted back to the main looper. `MainActivity` starts
the first request and, from its main-thread completion callback, enqueues the
second request; it has no direct native declaration.

The native bridge records the pthread that performs first initialization. A
later JNI entry on that same owner gets the deterministic duplicate-init result
`-1`; a non-owner entry gets `-11` before it can call Jolt. The only deliberate
foreign pthread in this experiment is rejected before ABI access, as recorded
in EXP-005.

## Result

See [actual.md](actual.md) and
[`artifacts/logs/exp006-jolt-handler-thread.txt`](../../artifacts/logs/exp006-jolt-handler-thread.txt).
