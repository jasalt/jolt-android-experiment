# Android worker callback into JoltRuntime

## Purpose

Prove an off-UI-thread Android callback is marshalled as data through the
single Jolt runtime queue rather than entering JNI directly.

## Contract

A named `AndroidWorker` single-thread executor runs one bounded callback. It
only invokes `JoltRuntime.dispatch("{:type :worker/completed}", ...)`.
`JoltRuntime` executes JNI on `HandlerThread("JoltRuntime")`; the completion
returns to the main looper. Jolt records `:worker :completed` in shared state.

## Result

See [actual.md](actual.md),
[`artifacts/logs/exp013-worker-callback.txt`](../../artifacts/logs/exp013-worker-callback.txt),
and [`artifacts/screenshots/exp013-worker-callback.png`](../../artifacts/screenshots/exp013-worker-callback.png).
