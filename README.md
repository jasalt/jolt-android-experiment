# Jolt Android PoC

An experimental proof of concept for running [Jolt](https://jolt-lang.net/) and
Chez Scheme as a native Clojure application/runtime core in an Android 15 app,
while running meaningful shared application code in Linux GTK4 and portable
CLI/REPL hosts.

The experiment explores a different trade-off from conventional Clojure on Android. JVM Clojure can run within Android's managed runtime model and use Java interoperability to access much of the Android SDK directly, making it the more straightforward choice when Android itself is the primary target. Jolt instead compiles through Chez Scheme to native code and has no general Java interop, so Android framework access must cross an explicit Kotlin/JNI boundary. That adds integration work, but it also creates a deliberately narrow separation between portable Clojure application logic and the host platform rather than coupling that logic to Android classes and JVM semantics.

The potential benefit is a small native Clojure-oriented core that can be reused beyond Android: the same .cljc domain, state-transition, validation, serialization, and effect-handling code could run under Jolt on Linux, macOS, and potentially iOS while each platform retains its native UI and SDK integration. Compared with Clojure/JVM on Android, this sacrifices the mature JVM ecosystem, direct Android API access, and a well-understood runtime in exchange for native deployment, closer C interoperability, reduced dependence on the JVM, and a plausible shared-runtime path across Android and Apple platforms. This PoC is intended to determine whether those benefits survive the practical costs of cross-compiling Chez/Jolt, JNI data ownership, runtime-thread confinement, Android lifecycle integration, debugging, packaging, and emulator compatibility.

## Status

**Android feasibility demonstrated under translation; native Apple Silicon
portable-core and Android build validation pass.** The repository now reproducibly cross-builds
Chez and a reduced Jolt managed library for Android `arm64-v8a`, loads it in an
API 35 x86_64 emulator through Android's ARM64 translation path, confines calls
to one Kotlin runtime thread, and demonstrates shared state, lifecycle events,
clipboard effects, persistence restore, notification permission round trip, and
a worker callback. See [REPORT.md](REPORT.md) for evidence boundaries.

Native Apple Silicon macOS now runs the portable CLI fixtures and normal Jolt
nREPL without GTK ([EXP-017](experiments/EXP-017-native-arm64-portable-cli))
and builds the Android debug APK through the Nix-provided SDK/NDK
([EXP-019](experiments/EXP-019-native-darwin-android-nix-build)). This does not
prove native ARM64 Android execution or a successful macOS emulator boot.
The Linux x86_64 GTK/Glimmer reference host renders the shared view model,
keeps reducer state separate from adapter outcomes, validates nREPL-style
main-loop scheduling, executes persistence and clipboard adapters, invokes its
URI adapter with an explicit request-only/headless-handler limitation, and
preserves visual evidence
([EXP-021](experiments/EXP-021-x86-64-linux-gtk-host)). This does not prove a
native ARM64 Linux GTK host. The Android Compose diagnostic shell includes
notification posting, URL/vibration/info adapters, and permission flow. Its
debug-only ADB-forwarded eval/redefinition evidence is bounded and is not
Android nREPL/CIDER ([EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval)).
The independent Raylib NativeActivity host now has a separate debug-only Jolt
nREPL workflow: pure `eval`/`load-file` replacements visibly update later owner
frames without rebuild or restart, and short Raylib FFI probes use a bounded
owner queue ([RAY-017](experiments/RAY-017-android-raylib-nrepl)). Its release
variant has no debug nREPL export, network permission, or listener.

A fail-fast `scripts/verify` now preserves supported clean-room validation
outputs and explicit host-specific skips. The project reports graded outcomes,
including reproducible blockers. See
[docs/PLAN.md](docs/PLAN.md) for the complete research plan and success levels.

[Task management](https://jasalt.github.io/jolt-android-experiment/bv/)

Do not interpret design intent as experimental evidence; observed architecture,
traps, and assessment are recorded in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md),
[docs/GOTCHAS.md](docs/GOTCHAS.md), `experiments/`, and [REPORT.md](REPORT.md).

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
- provide a useful Jolt nREPL workflow for the portable core and an Android
  debug-eval path;
- run meaningful shared `.cljc` domain code in Android, Linux GTK4, and a
  non-GUI CLI/REPL reference host;
- let Apple Silicon macOS developers work on the portable core and assemble the
  Android APK locally with Nix, without requiring Linux, GTK, or Android Studio;
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
Android ARM64       Linux GTK4             CLI / nREPL
Jolt/Chez .so       Jolt + Glimmer          Jolt portable core
JNI + Kotlin         GTK4                    Linux / macOS ARM
Compose + Android SDK
```

The principal decisions are recorded in [docs/adr/](docs/adr/README.md):

1. [share a Jolt core across Android, GTK, and CLI/REPL hosts](docs/adr/0001-shared-jolt-core-platform-adapters.md);
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

### Native and Android boundaries

Android uses Bionic rather than desktop glibc. The tested generated libraries
have the required architecture, exported symbols, PIC/linker behavior, and no
accidental desktop dependencies. The bounded EDN ABI copies returned strings
through C/JNI-owned storage; its pointer and string ownership is covered by the
allocation, compaction, and lifecycle experiments. This does not establish a
general ownership contract for arbitrary native values.

Current Jolt embedded-library use requires initialization, symbol lookup, and
exported calls to occur on the same operating-system thread. Android callbacks
therefore queue work onto a dedicated Jolt runtime thread rather than entering
Jolt directly; this invariant is covered by the handler-thread and callback
experiments.

The ARM64 library runs on the API-35 x86_64 emulator through Android's ARM64
translation path, as demonstrated by the emulator and application experiments.
That is translation evidence, not native ARM64 Android-device execution.

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

Portable CLI execution, nREPL, and portable tests come before Android compiler
changes; GTK is an additional Linux UI reference host, not a prerequisite for
portable-core work. Standalone Chez embedding comes before Jolt embedding.
Repeated JNI/Jolt calls under allocation and GC pressure come before substantial
Compose UI.

Validation grows from pure Jolt tests through Linux integration, native C ABI
tests, JNI instrumentation, Android UI workflows, semantic layout assertions,
and screenshot inspection. Failures that identify a platform boundary are
preserved as self-contained experiments rather than hidden by workarounds.

## Development environment

The supported build boundary will be a pinned Nix flake. The portable-core shell
must support native `x86_64-linux`, `aarch64-linux`, and `aarch64-darwin` hosts.
It is intended to provide Jolt/Chez sources and tools, JDK/Gradle, Android API
35 SDK and emulator components, the NDK toolchain, CMake/Ninja, GTK4 where
applicable, and diagnostic tools without relying on Android Studio's mutable SDK
 manager. The Android SDK/NDK build workflow is available on `x86_64-linux` and
 `aarch64-darwin`; GTK remains Linux-only. Emulator operation is platform- and
 host-resource-specific and must be reported from an actual run. Apple Silicon
 developers can use the native shell directly or the native `aarch64` Lima VM
 described in [docs/LIMA.md](docs/LIMA.md).

The current reproducible baseline is:

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/test-portable
nix --extra-experimental-features 'nix-command flakes' develop -c env JOLT_SOURCE=../jolt scripts/jolt-android-library-build
nix --extra-experimental-features 'nix-command flakes' develop -c gradle --no-daemon :app:assembleDebug
```

The Android build is exercised through the deterministic API 35 AVD and the
command sequences stored with each experiment. `scripts/bootstrap` records the
reproducible environment, and `scripts/verify` is the fail-fast clean-room
verifier. Its supported host-specific tiers and skips are documented in
[EXP-016](experiments/EXP-016-clean-room-validation); a skipped tier is not a
failed or unimplemented script.

## Repository documentation

- [docs/PLAN.md](docs/PLAN.md) — complete initial requirements, experiments, phases, and
  graded success criteria
- [docs/LIMA.md](docs/LIMA.md) — provision and operate native Linux or Apple
  Silicon Lima development VMs, including the VNC desktop
- [docs/adr/](docs/adr/README.md) — architecture decision records
- `experiments/` — minimal reproductions and observed platform results (created
  as experiments begin)
- `docs/ARCHITECTURE.md` — architecture actually demonstrated
- `docs/DEVELOPMENT.md` — reproducible developer workflows
- `docs/GOTCHAS.md` — integration traps discovered during implementation
- `docs/ANDROID-CROSS-COMPILE.md` — pinned Android cross-build procedure
- `REPORT.md` — final evidence-based feasibility assessment
- [AGENTS.md](AGENTS.md) — coding-agent workflow and repository instructions
- [SESSIONS.md](SESSIONS.md) — coding-agent / llm assistant session history

Some listed result documents do not exist yet and will be created when their
corresponding experiments begin.

## Other related projects

- Apple UIKit support on macOS <https://github.com/jolt-lang/examples/tree/main/todomvc-uikit>

## Contributing

Read this README, [docs/PLAN.md](docs/PLAN.md), and the relevant
[ADRs](docs/adr/README.md) before changing the project. Consult current Jolt source,
tests, and documentation rather than assuming JVM Clojure behavior. Keep changes
small and reproducible, preserve exact commands and logs for platform failures,
and distinguish proposed, observed, inferred, and blocked behavior in
contributor-facing documentation.

Apple Silicon collaborators use the pinned `aarch64-darwin` Nix shell for the
portable workflow and Android APK assembly. Run the commands in
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md); do not add a host-installed Android
SDK or Android Studio path. GTK remains Linux-only. The macOS emulator image and
acceleration are available, but its boot still requires sufficient local disk
space; see [EXP-019](experiments/EXP-019-native-darwin-android-nix-build).

Task state and dependency tracking use Beads; `docs/PLAN.md` remains the project
specification rather than a mutable task list. Beads uses the existing project
Git repository and its `origin`, not a separately provisioned Dolt remote:
`.beads/` holds Git-tracked Beads configuration while the embedded Dolt data
uses separate `refs/dolt/data` refs and ignored database/runtime directories.
Use `bd vc status`, `bd vc commit`, `bd dolt pull`, and `bd dolt push` to manage
Beads state through the project repository. The optional JSONL export is for
viewing/interchange, not the source of truth or synchronization mechanism.

Consider adding LLM assistant / coding agent sessions into [SESSIONS.md](SESSIONS.md).
