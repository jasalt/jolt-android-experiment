# ADR-0001: Shared Jolt core with platform host adapters

- **Status:** Accepted
- **Date:** 2026-08-26

## Context

The PoC must determine how much meaningful application code can run unchanged
on Android and Linux without pretending that their UI and platform APIs are the
same. Android uses Kotlin, Compose, and Android SDK APIs. The Linux reference
host uses Jolt with Glimmer/GTK4. Putting domain state in either platform's UI
objects would make portability superficial and complicate behavioral comparison.

Jolt supports `.cljc` for portable code and `.jolt` as a marker for host-specific
Jolt interop.

## Decision

Use a shared Jolt core for:

- domain models and reducers;
- event and effect vocabularies;
- validation and business rules;
- state serialization;
- capability decisions;
- derived view models;
- portable tests.

Use `.cljc` for intentionally portable code. Platform adapters remain
responsible for:

- native lifecycle and bootstrap;
- widget rendering and UI event capture;
- platform capabilities and effects;
- filesystem paths and persistence mechanisms;
- permissions, notifications, intents, and clipboard access;
- host thread scheduling.

The authoritative demo domain state lives in Jolt, not in Compose or GTK
objects. Each host renders view data, emits domain events, and executes effects.
Platform capabilities are represented as data; portable reducers should not be
filled with platform conditionals.

The target arrangement is:

```text
shared .cljc domain/reducer/events/effects/wire model
              │
             Jolt
       ┌──────┴────────┐
Android ARM64       Linux x86_64
Jolt/Chez .so       Jolt + Glimmer/GTK4
JNI + Kotlin
Compose + Android SDK
```

Android uses native Kotlin and Jetpack Compose. Linux uses Jolt and
Glimmer/GTK4. This PoC will not build a generic Jolt Compose DSL or generic Java
object interop layer.

## Consequences

- Domain behavior can be compared with identical tests on both hosts.
- Platform-specific code remains necessary and is measured honestly.
- UI hosts must adapt Jolt view data rather than becoming the source of truth.
- Effects require explicit platform adapters and capability handling.
- The design favors a diagnostic PoC over a general mobile framework.

## Validation

Validate this decision by running the same meaningful `.cljc` reducer,
serialization, and business-rule tests under the Android Jolt runtime and the
Linux Jolt host. Report shared and platform-specific lines of code without
optimizing for an artificial sharing percentage.
