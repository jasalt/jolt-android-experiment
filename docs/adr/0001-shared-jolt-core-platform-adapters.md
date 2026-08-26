# ADR-0001: Shared Jolt core with platform host adapters

- **Status:** Accepted
- **Date:** 2026-08-26

## Context

The PoC must determine how much meaningful application code can run unchanged
on Android, Linux, and Apple Silicon macOS without pretending their UI and
platform APIs are the same. Android uses Kotlin, Compose, and Android SDK APIs.
Linux has a Jolt/Glimmer/GTK4 reference host. GTK is useful for native UI
comparison, but cannot be the only non-Android development path: portable-core
work must remain practical on macOS without Linux or GTK.

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

Use `.cljc` for intentionally portable code. Provide three host categories:

1. **Android:** Jolt/Chez embedded behind JNI, Kotlin, Compose, and Android SDK
   effect adapters.
2. **Linux GTK4:** Jolt with Glimmer/GTK4 as a native UI reference host.
3. **Portable CLI/REPL:** a deterministic non-GUI Jolt host for the shared core.
   It accepts serialized events, emits canonical state/effect data, uses only a
   mock/CLI effect adapter, and supports ordinary Jolt nREPL development.

The CLI/REPL host is mandatory and must run in pinned native Nix shells on
`x86_64-linux`, `aarch64-linux`, and `aarch64-darwin`, where Jolt itself is
available. It is a development and reference adapter, not a terminal UI
framework and not a replacement for Android JNI/lifecycle or GTK rendering
tests.

Platform adapters remain responsible for native lifecycle and bootstrap, widgets
and UI event capture, platform capabilities and effects, paths and persistence,
permissions, notifications, intents, clipboard access, and host scheduling.

The authoritative demo domain state lives in Jolt, not Compose or GTK objects.
Each host renders or serializes view data, emits domain events, and executes
returned effects. Platform capabilities are data; portable reducers must not be
filled with platform conditionals.

```text
shared .cljc domain/reducer/events/effects/wire model
                         │
                        Jolt
          ┌──────────────┼───────────────┐
          ▼              ▼               ▼
 Android ARM64       Linux GTK4       CLI / nREPL
 Jolt/Chez .so       Jolt/Glimmer     Jolt portable core
 JNI + Kotlin        GTK4             Linux / macOS ARM
 Compose + SDK
```

This PoC will not build a generic Jolt Compose DSL, generic Java-object interop
layer, or terminal UI framework.

## Consequences

- Domain behavior can be compared with identical tests and wire fixtures across
  hosts.
- Apple Silicon macOS developers can work locally on the portable core with Nix,
  or inside a native ARM64 Lima VM, without GTK.
- Linux GTK is an additional UI reference host rather than the sole
  non-Android development path.
- Event/effect serialization, canonical CLI output, malformed-input behavior,
  and mock effect handling become explicit test contracts.
- Platform-specific code remains necessary and is measured honestly.
- The flake must distinguish portable-core shells from Android/GTK tooling;
  unavailable host combinations must be explicit, not silently use unpinned
  tools.

## Validation

Run identical reducer, serialization, and business-rule fixtures through the
CLI on native `x86_64-linux`, `aarch64-linux`, and `aarch64-darwin`, and compare
canonical output. Demonstrate normal Jolt nREPL on native Linux and macOS.
Validate GTK and Android separately: GTK exercises Linux UI adaptation; Android
exercises JNI, lifecycle, thread confinement, and platform effects.
