# Android Jolt queued session state

## Purpose

Prove that the Android bridge can keep Jolt state alive across queued events,
rather than rebuilding a managed runtime for every dispatch.

## Contract

`JoltRuntime` serializes dispatches on `HandlerThread("JoltRuntime")`. The
native bridge initializes the Jolt library and resolves `poc_dispatch_counter`
once, retaining that typed function pointer only while the owner session is
active. The Jolt fixture keeps `poc.reducer/initial-state` in a Jolt-owned atom.

The bridge accepts only exact increment/decrement event EDN forms, returns
caller-owned bounded result strings, and posts `jolt_library_shutdown` on the
owner queue before quitting that thread. A new process starts a fresh session.

## Result

See [actual.txt](actual.txt) and
[`artifacts/logs/exp008-jolt-session.txt`](../../artifacts/logs/exp008-jolt-session.txt).
