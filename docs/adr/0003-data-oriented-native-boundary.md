# ADR-0003: Use a narrow data-oriented native boundary

- **Status:** Accepted
- **Date:** 2026-08-26

## Context

Android SDK objects belong to the Kotlin host, while portable application
semantics belong to Jolt. A generic bridge exposing Java/Kotlin objects to Jolt
would substantially enlarge the JNI surface and couple portable code to Android.
It would also obscure ownership, threading, and garbage-collection behavior at
the most uncertain part of the PoC.

Pointers returned from Jolt-managed memory may become invalid across allocation
or GC. Kotlin/JVM strings use UTF-16 while Jolt strings are codepoint-indexed, so
Unicode conversion and ownership require explicit validation.

## Decision

Use a narrow C ABI that exchanges serialized data representing domain events,
state/view data, platform capabilities, and effects. Do not pass Android,
Kotlin, or Java objects into portable Jolt code.

Begin with the smallest primitive export, then prove strings and structured
messages experimentally. EDN is the initial candidate wire representation, not
an assumption that bypasses ownership testing.

If a returned Jolt string's lifetime cannot be made explicit and safe, use a
caller-owned output buffer ABI such as:

```c
int poc_dispatch(
    const char *input,
    char *output,
    size_t output_capacity);
```

Initialization, lookup, dispatch, and shutdown remain a deliberately small
surface. A debug-only evaluation entry point may be added after embedding is
stable, but must follow the same ownership and threading rules.

## Consequences

- Kotlin executes Android effects and sends results back as data.
- Jolt cannot directly retain or invoke arbitrary Android objects.
- The wire vocabulary becomes a versioned application interface that needs
  validation and error reporting.
- Serialization incurs overhead, accepted for this feasibility PoC.
- Buffer sizing, truncation, malformed input, and error representation must be
  defined and tested.
- A generic Java-object bridge or Jolt Compose DSL is deferred beyond the PoC.

## Validation

Prove the boundary incrementally with:

1. an integer call;
2. ASCII and Unicode strings, including Finnish characters, emoji, and
   multi-codepoint graphemes;
3. stateful structured event/effect round trips;
4. approximately 1 KiB, 64 KiB, and 1 MiB messages;
5. aggressive Java/JNI reference creation, Jolt allocation, and GC;
6. repeated calls and deterministic error behavior for malformed and oversized
   messages.
