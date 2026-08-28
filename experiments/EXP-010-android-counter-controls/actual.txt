# Observed 2026-08-28

A clean API 35 x86_64 emulator APK run exposed native Android buttons with UI
hierarchy bounds:

| Control | Bounds | Tap center |
| --- | --- | --- |
| INCREMENT | `[0,360][1080,486]` | `(540,423)` |
| DECREMENT | `[0,486][1080,612]` | `(540,549)` |
| RESET | `[0,612][1080,738]` | `(540,675)` |

The hierarchy after each automated tap showed Jolt-derived model transitions:

```text
initial         counter 0
increment       counter 1
decrement       counter 0
increment again counter 1
reset           counter 0
```

All result models retained `:lifecycle :resumed`. Logcat records a queued
`JoltRuntime` JNI entry and `jolt_probe` counter result for every event. No
AndroidRuntime fatal exception or native crash occurred.

The final inspected screenshot shows the runtime/lifecycle header, Jolt model
with final counter 0, and all three unclipped controls. The emulator process ABI
is x86_64; the packaged Jolt runtime is ARM64 and runs via the previously proven
translation path.
