# Observed 2026-08-28

## Instrumentation

The isolated Jolt cross-library patch adds `jolt_library_collect`, a direct
wrapper around Chez `Scompact_heap`. The Jolt fixture exports:

- `poc_answer` → `42`;
- `poc_allocate(n)` → constructs a vector of `n` values and returns its count.

The JNI bridge resolves both through `jolt_lookup`, performs 10,000 answer
calls, allocates 100,000 values, compacts the heap, verifies another answer,
then calls `jolt_library_shutdown` on the owner thread.

## First process launch

```text
I/jolt_probe: wrong-thread Jolt access rejected before ABI call
I/jolt_probe: one=42 calls=10000 allocation=100000 compact=ok again=42 wrong-thread=rejected shutdown=ok
I/jolt_probe: repeat init rejected
Jolt stress = 42; repeat init = -1
```

## Force-stop/relaunch

The same complete output was observed in a fresh process after `adb shell am
force-stop` and relaunch:

```text
I/jolt_probe: wrong-thread Jolt access rejected before ABI call
I/jolt_probe: one=42 calls=10000 allocation=100000 compact=ok again=42 wrong-thread=rejected shutdown=ok
I/jolt_probe: repeat init rejected
Jolt stress = 42; repeat init = -1
```

No `AndroidRuntime` fatal exception or native crash occurred in either run.

## Interpretation

- The single-process lifecycle and forced process relaunch are observed to work
  through the tested bounds.
- A second initialization is deliberately rejected by bridge state after the
  first shutdown; it does not call Chez/Jolt again.
- A foreign pthread is deliberately rejected before calling Jolt; no statement
  is made about Chez `Sactivate_thread` support or arbitrary concurrent Jolt
  calls.
