# Jolt Android PoC

An experimental proof of concept for running [Jolt](https://jolt-lang.net/) and
Chez Scheme as a native Clojure application/runtime core in an Android 15 app,
while running meaningful shared application code in Linux GTK4 and portable
CLI/REPL hosts.

The experiment explores a different trade-off from conventional Clojure on Android. JVM Clojure can run within Android's managed runtime model and use Java interoperability to access much of the Android SDK directly, making it the more straightforward choice when Android itself is the primary target. Jolt instead compiles through Chez Scheme to native code and has no general Java interop, so Android framework access must cross an explicit Kotlin/JNI boundary. That adds integration work, but it also creates a deliberately narrow separation between portable Clojure application logic and the host platform rather than coupling that logic to Android classes and JVM semantics.

The potential benefit is a small native Clojure-oriented core that can be reused beyond Android: the same .cljc domain, state-transition, validation, serialization, and effect-handling code could run under Jolt on Linux, macOS, and potentially iOS while each platform retains its native UI and SDK integration. Compared with Clojure/JVM on Android, this sacrifices the mature JVM ecosystem, direct Android API access, and a well-understood runtime in exchange for native deployment, closer C interoperability, reduced dependence on the JVM, and a plausible shared-runtime path across Android and Apple platforms. This PoC is intended to determine whether those benefits survive the practical costs of cross-compiling Chez/Jolt, JNI data ownership, runtime-thread confinement, Android lifecycle integration, debugging, packaging, and emulator compatibility.

## Status

**Planning and environment preparation.** The architecture has been selected,
but Android execution has not yet been demonstrated. In particular, this
repository does not yet prove that Jolt/Chez can be cross-compiled for Android,
loaded safely, or executed through an Android emulator's ARM64 translation
facility.

The project reports graded outcomes, including reproducible blockers. See
[docs/PLAN.md](docs/PLAN.md) for the complete research plan and success levels. Do not
interpret proposed architecture as experimental evidence; results will be
recorded in `experiments/`, `docs/ARCHITECTURE.md`, and `REPORT.md` as work
progresses.

## Goals

The target is a reproducible feasibility result, not a production mobile
framework. The PoC investigates whether it can:

- cross-compile Chez and Jolt into an Android `arm64-v8a` shared library from
  Fedora x86_64;
- initialize and call a persistent Jolt runtime safely through Kotlin and JNI;
- remain stable across repeated calls, allocation, garbage collection, Android
  lifecycle events, and process recreation;
- send Android events into portable Jolt code and execute returned effects using
  Android APIs;
- preserve Jolt's single-runtime-thread requirement;
- provide a useful Linux nREPL workflow and an Android debug-eval path;
- run meaningful shared `.cljc` domain code in both Android and Linux GTK4
  applications;
- determine whether an API-35 x86_64 emulator can execute the ARM64 library, and
  document practical fallbacks if not.

A reduced, reproducible platform blocker is a valid outcome. The project will
not substitute another runtime or hide an integration failure merely to produce
a demo.

## Intended architecture

Portable Jolt code owns domain models, reducers, events, effects, validation,
serialization, and derived view data. Platform hosts render that data, emit
events, and execute effects; domain state does not live in Compose or GTK
objects.

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

The principal decisions are recorded in [docs/adr/](docs/adr/README.md):

1. [share a Jolt core and use platform host adapters](docs/adr/0001-shared-jolt-core-platform-adapters.md);
2. [confine every embedded Jolt call to one Android runtime thread](docs/adr/0002-single-jolt-runtime-thread.md);
3. [use a narrow, data-oriented native boundary](docs/adr/0003-data-oriented-native-boundary.md).

Accepted ADRs describe design intent, not proof that the platform supports it.
The architecture actually demonstrated will be documented separately.

## Important technical context

### Jolt is not a JVM

Jolt targets Clojure semantics, but it runs on Scheme rather than a JVM:

- there is no JVM and no general Java interop;
- `java.*` names are shallow Scheme-hosted compatibility shims;
- Jolt reaches C libraries through `jolt.ffi`; JNI is used only by the Android
  host bridge;
- Jolt strings are codepoint-indexed rather than JVM UTF-16 indexed;
- regex, threading, process-lifetime, and garbage-collection behavior must not
  be inferred from the JVM;
- `clojure.core` coverage is broad but incomplete.

See the Jolt [documentation](https://jolt-lang.net/docs/), especially
[Differences from Clojure](https://jolt-lang.net/docs/differences.html) and
[Native Interop](https://jolt-lang.net/docs/native-interop.html).

### Native and Android unknowns

Android uses Bionic rather than desktop glibc. Generated libraries must be
checked for architecture, exported symbols, PIC/linker behavior, and accidental
desktop dependencies. Pointer and string ownership across Jolt, C, JNI, and
Kotlin remains unproven until tested under allocation and GC pressure.

Current Jolt embedded-library use requires initialization, symbol lookup, and
exported calls to occur on the same operating-system thread. Android callbacks
therefore queue work onto a dedicated Jolt runtime thread rather than entering
Jolt directly.

The initial Android target is an ARM64 library. Whether an ARM64-only APK runs
through translation on an API-35 x86_64 emulator is an early experiment, not an
assumption.

## Development approach

Work proceeds by small, observable experiments:

```text
hypothesis
→ smallest isolated experiment
→ REPL/manual evaluation where possible
→ observe the running system
→ automate the successful observation as a test
→ preserve evidence
```

Platform layers are proved progressively:

```text
Chez Android
→ Jolt cross-compilation
→ Jolt .so loading
→ primitive C ABI call
→ string/data call
→ stateful repeated calls and GC
→ thread confinement
→ Android events and effects
→ debug eval
→ demo application
```

Linux Jolt execution, nREPL, and portable tests come before Android compiler
changes. Standalone Chez embedding comes before Jolt embedding. Repeated JNI/Jolt
calls under allocation and GC pressure come before substantial Compose UI.

Validation grows from pure Jolt tests through Linux integration, native C ABI
tests, JNI instrumentation, Android UI workflows, semantic layout assertions,
and screenshot inspection. Failures that identify a platform boundary are
preserved as self-contained experiments rather than hidden by workarounds.

## Development environment

The supported build boundary will be a pinned Nix flake on Fedora x86_64. It is
intended to provide Jolt/Chez sources and tools, JDK/Gradle, Android API 35 SDK
and emulator components, the NDK toolchain, CMake/Ninja, GTK4, and diagnostic
tools without relying on Android Studio's mutable SDK manager.

The repository is currently not bootstrapped, so there is no working quick start
yet. Once available, the expected clean workflow is:

```sh
nix develop
./scripts/bootstrap
./scripts/verify
```

`scripts/bootstrap` will record exact tool versions and host capabilities.
`scripts/verify` will eventually run shared and native tests, build both hosts,
exercise an emulator, collect logs, and capture UI evidence. Until those scripts
exist, follow the phased instructions in [docs/PLAN.md](docs/PLAN.md) and do not treat the
commands above as implemented.

## Repository documentation

- [docs/PLAN.md](docs/PLAN.md) — complete initial requirements, experiments, phases, and
  graded success criteria
- [docs/LIMA.md](docs/LIMA.md) — provision and operate the Lima development VM,
  including its VNC desktop
- [docs/adr/](docs/adr/README.md) — architecture decision records
- `experiments/` — minimal reproductions and observed platform results (created
  as experiments begin)
- `docs/ARCHITECTURE.md` — architecture actually demonstrated
- `docs/DEVELOPMENT.md` — reproducible developer workflows
- `docs/GOTCHAS.md` — integration traps discovered during implementation
- `docs/ANDROID-CROSS-COMPILE.md` — pinned Android cross-build procedure
- `REPORT.md` — final evidence-based feasibility assessment
- [AGENTS.md](AGENTS.md) — coding-agent workflow and repository instructions

Some listed result documents do not exist yet and will be created when their
corresponding experiments begin.

## Contributing

Read this README, [docs/PLAN.md](docs/PLAN.md), and the relevant
[ADRs](docs/adr/README.md) before changing the project. Consult current Jolt source,
tests, and documentation rather than assuming JVM Clojure behavior. Keep changes
small and reproducible, preserve exact commands and logs for platform failures,
and distinguish proposed, observed, inferred, and blocked behavior in
contributor-facing documentation.

Task state and dependency tracking use Beads; `docs/PLAN.md` remains the project
specification rather than a mutable task list.
