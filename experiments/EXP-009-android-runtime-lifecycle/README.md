# Android runtime status and lifecycle queue

## Purpose

Add the smallest observable runtime/lifecycle surface on top of the queued Jolt
session. Android lifecycle callbacks must enqueue data through `JoltRuntime`,
not invoke JNI directly.

## Scope

The shared reducer records `:lifecycle/create`, `:lifecycle/start`, and
`:lifecycle/resume`. The Jolt library reports lifecycle through a primitive code;
the JNI bridge formats the existing caller-owned result. The Android shell shows
process ABI/PID and the dedicated runtime-thread identity alongside that Jolt
model.

## Result

See [actual.md](actual.md),
[`artifacts/logs/exp009-runtime-lifecycle.txt`](../../artifacts/logs/exp009-runtime-lifecycle.txt),
and [`artifacts/screenshots/exp009-runtime-lifecycle.png`](../../artifacts/screenshots/exp009-runtime-lifecycle.png).
