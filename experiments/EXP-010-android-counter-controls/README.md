# Android shared counter controls

## Purpose

Prove interactive Android controls enqueue shared reducer events through the
existing `JoltRuntime` queue and render Jolt-owned counter state after each
response.

## Scope

The native bridge allow-list adds the existing `:counter/reset` event. The
minimal native Android view has Increment, Decrement, and Reset buttons; each
listener calls `JoltRuntime.dispatch`, never JNI directly. The main-looper
completion renders the caller-owned result string.

## Result

See [actual.md](actual.md),
[`artifacts/logs/exp010-counter-controls.txt`](../../artifacts/logs/exp010-counter-controls.txt),
and [`artifacts/screenshots/exp010-counter-controls.png`](../../artifacts/screenshots/exp010-counter-controls.png).
