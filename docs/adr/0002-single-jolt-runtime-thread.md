# ADR-0002: Confine embedded Jolt access to one runtime thread

- **Status:** Accepted
- **Date:** 2026-08-26

## Context

Current Jolt native-library documentation requires `jolt_library_init`,
`jolt_lookup`, and every exported Jolt function to be entered from the same
operating-system thread. Entering the embedded runtime from another thread is
undefined behavior.

Android delivers work through several execution contexts, including its main
thread, Binder threads, lifecycle and permission callbacks, and background
workers. Allowing each callback to invoke JNI directly would violate Jolt's
runtime constraint and could turn a scheduling mistake into memory corruption.

## Decision

Create one dedicated Android `JoltRuntimeThread`, implemented initially with a
`HandlerThread`, and route every Jolt operation through its queue:

```text
Android callback → queue → Jolt HandlerThread → JNI → Jolt
```

Initialization, symbol lookup, exported calls, debug evaluation, and shutdown
must all run on this thread. Android main-thread, Binder, worker, permission,
and lifecycle callbacks may enqueue events but may not enter Jolt directly.

The native bridge records the operating-system thread used for initialization
and checks every subsequent entry. A wrong-thread call must fail explicitly and
diagnostically rather than continuing with undefined behavior.

## Consequences

- Kotlin APIs around Jolt are asynchronous or suspending even when the native
  operation itself is synchronous.
- All event ingress has a single serialization point.
- Platform effects that complete on arbitrary threads must enqueue their result
  before dispatching it to Jolt.
- Long-running Jolt calls can block later runtime work, so dispatch latency must
  be observable and bounded.
- Debug evaluation and any future reload mechanism use the same queue.
- Native initialization and shutdown have explicit lifecycle constraints.

## Validation

Automated tests must demonstrate:

1. initialization and repeated calls on the runtime thread;
2. callbacks from UI and worker threads being queued successfully;
3. deliberate direct entry from another thread failing with the bridge's
   explicit assertion;
4. allocation and GC pressure not changing thread affinity;
5. process shutdown and relaunch preserving deterministic initialization.
