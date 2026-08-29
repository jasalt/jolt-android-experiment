# Observed 2026-08-29

The portable reducer suite passed 5 tests / 10 assertions including
`:worker/completed` state.

A clean API 35 emulator run logged the required thread handoff:

```text
I/AndroidWorker: callback on AndroidWorker
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: owner JNI session initialized
I/jolt_probe: dispatch counter=0
I/AndroidWorker: completion returned on main
```

The layout tree and inspected screenshot show Jolt-derived worker state:

```text
Jolt model: ... :lifecycle :resumed, :worker :completed ...
Worker callback: ... :lifecycle :created, :worker :completed ...
```

The worker executor has no native method and makes no JNI call; it only queues
serialized data through `JoltRuntime`. No AndroidRuntime fatal exception or
native crash occurred.
