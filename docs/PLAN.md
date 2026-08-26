# Jolt Android PoC — autonomous implementation plan

## 1. Objective

Build a reproducible proof-of-concept demonstrating that **Jolt can serve as a native Clojure application/runtime core inside an Android 15 application**, while a meaningful subset of the same application/domain code runs unchanged in a **Linux GTK4 host** and a **portable CLI/REPL host**. The CLI/REPL host is the mandatory non-GUI development path for the portable core and must work on native Linux and Apple Silicon macOS without requiring GTK.

The PoC should answer, experimentally rather than theoretically:

1. Can Jolt/Chez be cross-compiled into an Android `arm64-v8a` shared library from Fedora x86_64?
2. Can an Android app initialize that library reliably?
3. Can Kotlin → JNI → Jolt calls work repeatedly under GC pressure?
4. Can callbacks/events travel Android → Jolt and actions travel Jolt → Android?
5. Can this work within Jolt's current same-thread embedded-library restriction?
6. Can a useful REPL-driven development loop be retained?
7. Can Android API objects remain on the Kotlin side while domain/application semantics live in portable Jolt?
8. Can an Android 15 x86_64 emulator execute the ARM64 Jolt library through the API-35 translation facility?
9. If not, what alternative emulator strategy is usable?
10. How much source can genuinely be shared with GTK4 and CLI/REPL Jolt hosts?
11. Can an Apple Silicon macOS developer use the portable core locally through Nix, or in a native-architecture Lima VM, without relying on Linux/GTK?

This is explicitly a **platform-feasibility PoC**, not the beginning of a production mobile framework. The portable CLI/REPL host is deliberately narrow: it validates the shared core and supports development; it is not a terminal UI framework.

Chez itself currently lists Android ARMv7/AArch64 as supported, while Jolt already provides native-library generation through `build --library`, so the critical unknown lies between those two capabilities: making Jolt's library build cross-target Android correctly. ([GitHub][1])

---

# 2. Non-negotiable development rules

The primary Android integration environment is Fedora 44 x86_64 with an X11 display. Portable-core development must also be supported on native Apple Silicon macOS through Nix or a native-architecture Lima VM; do not require Linux/GTK for CLI, tests, or nREPL.

Use this workflow for every non-trivial feature:

```text
hypothesis
   ↓
smallest experiment
   ↓
REPL evaluation where possible
   ↓
observe running system
   ↓
turn successful experiment into automated test
   ↓
commit atomically
   ↓
update Beads
```

Do **not** begin by implementing the final architecture.

For platform integration, progressively prove:

```text
Chez Android
    ↓
Jolt cross compilation
    ↓
Jolt .so loads
    ↓
integer C ABI call
    ↓
string/data call
    ↓
stateful call
    ↓
thread confinement
    ↓
Android events
    ↓
platform effects
    ↓
REPL
    ↓
demo application
```

Each failed experiment is a valid deliverable if it identifies a genuine platform boundary.

The agent must not hide or work around blocking problems merely to get a demo screenshot.

---

# 3. Important known constraints

### Jolt library threading

Current Jolt documentation states that:

* `jolt_library_init` is called once;
* `jolt_lookup`;
* and every exported Jolt function

must be entered from the **same operating-system thread**. Entering from another thread is currently undefined behavior. ([jolt-lang.net][2])

Therefore design the Android side around a dedicated:

```text
JoltRuntimeThread
```

from day one.

Do not let arbitrary Android callbacks call Jolt directly.

Use:

```text
Android callback
      │
      ▼
queue
      │
      ▼
Jolt HandlerThread
      │
      ▼
JNI
      │
      ▼
Jolt
```

This is part of the architecture, not merely a workaround.

### Android emulator ABI mismatch

Upstream Chez currently advertises Android builds for ARMv7 and ARM64, not Android x86_64. ([GitHub][1])

The preferred Linux Android Emulator configuration is nevertheless an x86_64 system image because x86_64 images can use KVM acceleration on an x86_64 Fedora host. ([Android Developers][3])

There is an interesting reason this may still work: Google's API-35 x86_64 emulator system image advertises both `x86_64` and `arm64-v8a` in its ABI list, and a 2026 report from `android-emulator-runner` demonstrates ARM-only applications executing on an API-35 x86_64 emulator. Treat this as an **experimental capability to verify**, not an architectural assumption. ([android.googlesource.com][4])

This should be one of the first experiments.

---

# 4. Repository shape

Use a single repository:

```text
jolt-android-poc/
├── flake.nix
├── flake.lock
├── deps.edn
├── AGENTS.md
│
├── src/
│   ├── shared/
│   │   └── poc/
│   │       ├── domain.cljc
│   │       ├── reducer.cljc
│   │       ├── effects.cljc
│   │       └── wire.cljc
│   │
│   ├── android/
│   │   └── poc/
│   │       └── android_entry.jolt
│   │
│   ├── linux/
│   │   └── poc/
│   │       └── gtk_app.jolt
│   └── cli/
│       └── poc/
│           └── main.jolt
│
├── android/
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── app/
│       ├── build.gradle.kts
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/.../
│           │   ├── MainActivity.kt
│           │   ├── JoltRuntime.kt
│           │   ├── JoltBridge.kt
│           │   └── PlatformEffects.kt
│           └── cpp/
│               ├── CMakeLists.txt
│               ├── jolt_jni.c
│               └── jolt_jni.h
│
├── native/
│   ├── chez/
│   ├── jolt/
│   └── android/
│
├── test/
│   ├── shared/
│   ├── cli/
│   ├── linux/
│   └── android/
│
├── scripts/
│   ├── bootstrap
│   ├── emulator-start
│   ├── emulator-reset
│   ├── android-build
│   ├── android-run
│   ├── android-log
│   ├── screenshot
│   ├── android-repl
│   ├── gtk-run
│   ├── cli
│   └── verify
│
├── experiments/
│   ├── README.md
│   └── EXP-xxxx-*/
│       ├── README.md
│       ├── commands.sh
│       └── observations.txt
│
├── artifacts/
│   ├── screenshots/
│   ├── logs/
│   └── reports/
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DEVELOPMENT.md
│   ├── GOTCHAS.md
│   ├── ANDROID-CROSS-COMPILE.md
│   └── REPRODUCING-EXPERIMENTS.md
│
└── REPORT.md
```

Use `.cljc` for code that is intended to remain portable. Jolt deliberately supports `.clj`, `.cljc`, and `.jolt`, with `.jolt` intended as the marker for host-specific Jolt interop. ([GitHub][5])

---

# 5. Nix development environment

Use Nix flakes as the default dependency boundary.

`android-nixpkgs` is a reasonable Linux Android-tooling base because it supports `x86_64-linux`, packages Google's SDK repository, and provides immutable Android SDK compositions. ([GitHub][6]) The flake must separately expose a portable-core shell on `x86_64-linux`, `aarch64-linux`, and `aarch64-darwin`; do not imply that Android emulator or GTK packages are available on all three.

Pin everything through `flake.lock`.

The shell should provide approximately:

```text
Jolt
Chez Scheme source/build dependencies
JDK
Gradle support
Android SDK platform 35
Android build-tools 35
Android platform-tools
Android emulator
Android API 35 google_apis x86_64 image (Linux Android shell)
Android NDK
CMake
Ninja
clang/lld
pkg-config
GTK4 (Linux GTK shell)
glib
git
gdb/lldb where practical
ImageMagick
jq
socat/netcat
Beads
```

Prefer the current supported stable NDK rather than arbitrary latest. As of August 2026, NDK r29 is stable; pin its exact revision in Nix rather than relying on the moving channel. ([GitHub][7])

Do not rely on Android Studio's mutable SDK manager for the build.

Android Studio itself can be installed for debugging/inspection, but the Linux Android shell must make:

```text
nix develop
./gradlew ...
adb ...
emulator ...
```

sufficient for normal operation. On macOS, the portable-core shell must make `jolt`, the CLI, tests, and nREPL available without Android Studio, Linux, or GTK.

### Required environment diagnostic

`scripts/bootstrap` must print and store:

```text
uname -a
Fedora release
Nix version
Jolt version/commit
Chez version/commit
Java version
Gradle version
Android SDK root
adb version
emulator version
NDK version
clang version
DISPLAY
/dev/kvm status
emulator acceleration status
```

Store this in:

```text
artifacts/reports/environment.txt
```

---

# 6. Emulator strategy

## Primary configuration

Create a deterministic API-35 AVD:

```text
Android 15 / API 35
google_apis
x86_64
Pixel-class phone profile
```

Android 15 corresponds to API level 35. ([Android Developers][8])

Use KVM where available.

The emulator command-line interface supports scripted startup, snapshots, wiping state, fixed ports, graphical operation and headless operation. ([Android Developers][9])

Provide two modes:

```bash
./scripts/emulator-start gui
./scripts/emulator-start headless
```

GUI mode should use the provided X11 display.

Automated tests should normally use headless mode after initial visual work.

## First mandatory ABI experiment

Build a trivial ARM64 NDK library:

```c
int poc_answer(void) {
    return 42;
}
```

package **only**:

```text
lib/arm64-v8a/libabi_probe.so
```

into an Android API-35 app.

Run it on the x86_64 API-35 emulator.

Verify:

```bash
adb shell getprop ro.product.cpu.abilist
adb shell getprop ro.product.cpu.abi
```

and whether Android loads and executes the ARM64 `.so`.

Record:

```text
EXP-001-api35-arm64-on-x86-emulator
```

If this fails, immediately test the fallback strategies below before continuing.

## Fallback A

Run an API-35 `arm64-v8a` emulator image under software CPU emulation.

Google publishes an Android 15 ARM64 emulator system image. ([android.googlesource.com][10])

Performance may be poor on x86_64 because it cannot use the normal same-architecture KVM path. That is acceptable for integration smoke tests if startup and UI latency remain usable.

## Fallback B

If an ARM64 Android emulator is operationally unusable:

* keep all native compilation and APK verification automated;
* use the API-35 x86_64 emulator for the Kotlin/Compose shell;
* run ARM64 native integration using an alternative execution harness if one is available;
* clearly classify **full emulator validation as blocked**.

Do not silently replace ARM64 Jolt with another runtime.

---

# 7. Phase 0 — establish Beads and autonomous workflow

Initialize Beads immediately:

```bash
bd init
bd prime
```

Beads uses the same project Git repository and its existing `origin`; do not
provision a separate Dolt remote or service. `.beads/` contains Git-tracked
Beads configuration, while the embedded Dolt database keeps task data in
separate `refs/dolt/data` refs and ignored database/runtime directories. Use
`bd vc status`, `bd vc commit`, `bd dolt pull`, and `bd dolt push` for task-data
history and synchronization. JSONL auto-export is for viewing/interchange, not
the source of truth. Keep Beads/Dolt commits logically aligned with source
commits without assuming either commits the other.

Current Beads is explicitly designed as a dependency-aware issue graph for coding agents, with `bd ready`, claiming, dependencies and persistent project memory. ([GitHub][11])

Add to `AGENTS.md`:

```text
Use Beads as the only task-management system.

At session start:
  bd prime
  bd ready

Before working:
  claim one ready bead.

After discovering unexpected work:
  create a new bead and add dependencies instead of
  expanding unrelated scope silently.

Before closing a bead:
  provide reproducible validation evidence.

Do not create TODO.md, TASKS.md, PLAN.md, or ad-hoc task lists.
Use bd remember for durable project discoveries.

Every completed engineering bead should normally end in one
small git commit.
```

### Initial Beads graph

Conceptually:

```text
EPIC: Environment
 ├─ Nix shell
 ├─ Android SDK
 ├─ emulator
 └─ visual automation

EPIC: Chez Android feasibility
 ├─ ABI translation probe
 ├─ Chez ARM64 cross-build
 ├─ standalone Chez native probe
 └─ Chez/JNI embedding

EPIC: Jolt Android feasibility
 ├─ inspect Jolt build pipeline
 ├─ cross-compile minimal Jolt library
 ├─ load Jolt .so
 ├─ primitive export
 ├─ string export
 ├─ repeated-call/GC test
 └─ thread-confinement test

EPIC: Shared architecture
 ├─ domain reducer
 ├─ effect model
 ├─ wire protocol
 ├─ portable CLI host
 ├─ macOS/ARM64 CLI fixture run
 └─ conformance tests

EPIC: Android demo
 ├─ Compose shell
 ├─ runtime status
 ├─ lifecycle
 ├─ persistence
 ├─ notifications
 ├─ intent
 ├─ permission
 └─ background callback

EPIC: GTK demo
 ├─ Glimmer shell
 ├─ shared reducer
 ├─ persistence adapter
 └─ feature parity

EPIC: REPL
 ├─ portable Linux/macOS nREPL
 ├─ Android debug eval
 └─ reload experiment

EPIC: Validation/documentation
 ├─ screenshots
 ├─ clean-room rebuild
 ├─ gotchas
 ├─ experiments report
 └─ final assessment
```

The actual IDs must be generated by `bd`.

Wire dependencies with `bd dep add`, rather than relying solely on hierarchy.

---

# 8. Phase 1 — prove ordinary portable Jolt first

Before Android modifications, establish a known-good portable-core baseline on
Linux. Repeat the CLI fixtures and nREPL check on Apple Silicon macOS or a native
ARM64 Lima VM before treating the CLI host as multiplatform.

Clone/pin Jolt and verify:

```bash
jolt -e '(+ 1 2)'
jolt repl
jolt nrepl-server
```

Jolt currently has native nREPL support and allows re-evaluated definitions to affect the running process, so use that as the reference development workflow. ([GitHub][5])

Create the initial portable model:

```clojure
(ns poc.reducer)

(def initial-state
  {:counter 0
   :events []
   :platform nil})

(defn step [state event]
  (case (:type event)
    :counter/inc
    [(update state :counter inc) []]

    :counter/dec
    [(update state :counter dec) []]

    :platform/info
    [(assoc state :platform (:value event)) []]

    [state []]))
```

From the Jolt REPL:

```clojure
(require '[poc.reducer :as r])

(r/step r/initial-state
        {:type :counter/inc})
```

Only after this works should the equivalent assertions become unit tests.

---

# 8A. Phase 1A — portable CLI/REPL reference host

Before building GTK, create a non-GUI host for the shared core. It is mandatory
because it gives Linux and Apple Silicon macOS developers the same fast,
REPL-driven path without a Linux desktop dependency.

Provide `src/cli/poc/main.jolt` and `scripts/cli`. The CLI must accept a
serialized event (initially EDN), invoke the shared reducer, and emit a
canonical serialized `{:model ... :effects ...}` result. Platform effects must
be represented by a mock/CLI adapter, not GTK or Android objects. Define
stdin/stdout, exit status, malformed-input, and effect-output behavior so shell
fixtures can test it deterministically.

The mandatory portable-core development loop is:

```bash
nix develop
jolt -e '(require ...)'
./scripts/cli --event '{:type :counter/inc}'
jolt nrepl-server
```

The exact namespace and script syntax may evolve, but the core must work in the
pinned `x86_64-linux`, `aarch64-linux`, and `aarch64-darwin` shells. Demonstrate
normal Jolt nREPL on native Linux and macOS. This host validates portable domain
behavior; it does not replace Android JNI/lifecycle tests or GTK rendering.

# 9. Phase 2 — Linux GTK reference host

Use Jolt's Glimmer ecosystem rather than inventing another GTK wrapper.

Glimmer's core is deliberately toolkit-independent; its GTK backend implements the native-widget boundary separately. The reconciler also already supports scheduling reactive re-renders onto a toolkit UI thread. ([GitHub][12])

Architecture:

```text
shared .cljc
    │
    ▼
Jolt
    │
    ▼
Glimmer
    │
    ▼
glimmer-gtk
    │
    ▼
GTK4
```

Build a small desktop app showing:

```text
Jolt Mobile PoC

Platform: Linux x86_64
Runtime: Jolt / Chez

Counter: 4
[-] [+] [reset]

Shared state log:
...
```

Do not put domain state into GTK objects.

GTK only:

1. renders a view model;
2. emits domain events;
3. executes platform effects.

This is the reference implementation against which Android behavior is compared.

---

# 10. Phase 3 — cross-build Chez for Android ARM64

This must be an isolated experiment.

Chez documents cross compilation through:

* host Chez;
* target boot generation;
* an `xpatch` compiler;
* target compiler/linker configuration. ([GitHub][13])

The agent should first inspect the current Chez source rather than assuming Android's exact machine-type spelling.

Determine experimentally:

```bash
grep -Rni android .
./configure --help
```

Then construct the smallest Android cross-build using the NDK's:

```text
aarch64-linux-android35-clang
aarch64-linux-android-ar
aarch64-linux-android-ranlib
...
```

Disable irrelevant terminal/X11 components.

Expected artifact:

```text
native/chez/android-arm64/
├── libkernel.a
├── scheme.h
├── equates.h
└── target boot/compiler artifacts
```

### Tests

First test symbol/format characteristics on the host:

```bash
file libkernel.a
readelf ...
nm ...
```

Then link a tiny Android `.so` that embeds only Chez.

Android should be able to:

```text
JNI
 ↓
Sscheme_init
 ↓
Sbuild_heap
 ↓
evaluate/call known Scheme procedure
 ↓
return 42
```

Do **not** involve Jolt until this passes.

---

# 11. Phase 4 — isolate Jolt cross-compilation

This is likely the highest-value research phase.

Jolt already builds host shared libraries with:

```bash
jolt build --library
```

and produces a managed-runtime `.so` exposing `jolt_library_init` and `jolt_lookup`. ([GitHub][5])

But current public documentation describes normal host compilation, not an Android cross-target.

Therefore inspect Jolt's build implementation and find where it assumes:

```text
host Chez kernel
host C compiler
host linker
host OS
```

Do not immediately refactor it.

### Experiment 4A

Create:

```clojure
(ns poc.native
  (:require [jolt.ffi :as ffi]))

(defn answer [] 42)

(ffi/export! "poc_answer" answer [] :int)
```

Try to make the smallest modifications/tool overrides necessary to emit:

```text
libjoltpoc.so
ELF 64-bit
AArch64
Android-compatible
```

### Experiment 4B

Validate it without Android packaging:

```bash
file libjoltpoc.so
readelf -h
readelf -d
readelf -Ws
```

Must expose:

```text
jolt_library_init
jolt_lookup
jolt_library_shutdown
```

and must not acquire accidental glibc dependencies.

This is important because Android uses **Bionic**, not desktop glibc.

### Experiment 4C

Load it from the Android app and invoke:

```text
poc_answer() → 42
```

Only then proceed.

---

# 12. Do not patch Jolt upstream prematurely

If Jolt needs changes, keep them in a clearly isolated layer first:

```text
native/jolt/android-cross.patch
```

or a temporary local fork/submodule.

Classify each necessary modification:

```text
A. environment/toolchain configuration only
B. generic cross-compilation capability missing
C. Android-specific runtime assumption
D. Chez limitation
E. Jolt compiler limitation
F. packaging limitation
```

For every B–E issue, create a self-contained reproduction under:

```text
experiments/
```

The final report should distinguish:

```text
"Jolt cannot currently do X"

from

"our PoC build script has not implemented X"
```

---

# 13. Phase 5 — minimal JNI bridge

Start with a separate JNI library:

```text
libjoltbridge.so
       │
       └── links → libjoltcore.so
```

Kotlin:

```text
JoltRuntime.kt
```

C:

```text
jolt_jni.c
```

Jolt:

```text
android_entry.jolt
```

Flow:

```text
Kotlin
  │
  ▼
JNI function
  │
  ▼
jolt_lookup(...)
  │
  ▼
exported Jolt fn
```

### Required incremental tests

1. initialize;
2. lookup `poc_answer`;
3. call once;
4. call 10,000 times;
5. intentionally trigger Jolt allocations;
6. intentionally trigger GC;
7. call again;
8. shutdown cleanly;
9. re-launch process;
10. repeat.

Keep `adb logcat` from every crash.

---

# 14. Phase 6 — thread-confinement architecture

Implement:

```kotlin
class JoltRuntime {
    private val thread = HandlerThread("JoltRuntime")
    ...
}
```

All JNI calls must pass through that thread.

Expose to Kotlin conceptually:

```kotlin
suspend fun dispatch(event: String): String
suspend fun eval(form: String): String   // debug only
```

Never:

```kotlin
external fun dispatch(...)
```

directly from arbitrary callbacks.

Instead:

```text
Main thread
Binder thread
WorkManager thread
permission callback
etc.
      │
      ▼
JoltRuntime.dispatchAsync()
      │
      ▼
one HandlerThread
      │
      ▼
JNI/Jolt
```

Add an assertion in the native bridge that records the thread that initialized Jolt and aborts/fails clearly if another thread tries to enter it.

This turns Jolt's current restriction into an executable invariant. ([jolt-lang.net][2])

---

# 15. Wire representation

For the PoC, prefer **data across the boundary**, never Java objects.

Conceptually:

```clojure
{:type :lifecycle/resume}

{:type :counter/inc}

{:type :permission/result
 :permission :notifications
 :granted? true}
```

Jolt returns:

```clojure
{:model
 {:counter 4
  :lifecycle :resumed}

 :effects
 [{:type :ui/render}
  {:type :notification/show
   :title "Jolt"
   :body "Counter reached 4"}]}
```

Start with EDN if interoperability proves convenient.

But make **string ownership across `export!`** a specific experiment before committing to it.

If returned-string lifetime is unclear, move to a safer ABI:

```c
int poc_dispatch(
    const char *input,
    char *output,
    size_t output_capacity);
```

rather than retaining pointers into Jolt-managed memory.

Jolt's documentation explicitly warns about pointer lifetime across the native boundary. ([jolt-lang.net][2])

---

# 16. Phase 7 — native Android UI

Use Kotlin + Jetpack Compose for the Android shell.

Do not attempt a Jolt Compose DSL in this PoC.

Architecture:

```text
              shared Jolt domain
                     │
             state + effects
                     │
                     ▼
              Kotlin adapter
                     │
                     ▼
            Jetpack Compose
```

Keep Compose stateless enough that the authoritative demo domain state remains in Jolt.

---

# 17. Demo application feature set

The Android app should be intentionally diagnostic.

## Screen 1 — Runtime

Show:

```text
Android version
SDK level
process ABI
supported ABIs
Jolt version/commit
Chez version
runtime initialized?
Jolt thread ID
JNI round-trip count
```

This immediately proves what actually executed.

## Screen 2 — Shared state

Shared Jolt reducer:

```text
counter
increment
decrement
reset
event history
```

Same behavior must run under GTK.

## Screen 3 — Lifecycle

Display events reaching Jolt:

```text
create
start
resume
pause
stop
```

State transitions should be visible.

## Screen 4 — Platform effects

Demonstrate several real Android SDK features:

```text
Vibrate / haptic
Copy text to clipboard
Show notification
Open URL / intent
Read locale
Read application package info
```

These should originate as effects produced by Jolt.

## Screen 5 — Permission round trip

Request an appropriate runtime permission.

Show:

```text
Jolt requests capability
    ↓
Kotlin requests Android permission
    ↓
Android callback
    ↓
event queued to Jolt thread
    ↓
Jolt state changes
    ↓
Compose updates
```

This proves asynchronous two-way platform interaction.

## Screen 6 — persistence

Jolt emits:

```clojure
{:type :storage/write
 :key "demo-state"
 :value "..."}
```

Android persists it.

After process kill/relaunch:

```text
Android loads value
      ↓
sends event to Jolt
      ↓
Jolt reconstructs state
```

Also implement a GTK persistence adapter using the same logical effect.

## Screen 7 — worker-thread callback

Use one Android operation that deliberately completes off the UI thread.

Its callback must enqueue into the dedicated Jolt thread.

This directly proves the hardest threading rule.

---

# 18. Shared Android/GTK/CLI capability model

Represent platform capabilities as data.

Example:

```clojure
{:platform :android
 :capabilities
 #{:notifications
   :clipboard
   :vibration
   :persistence
   :open-uri}}

{:platform :linux
 :capabilities
 #{:clipboard
   :persistence
   :open-uri}}

{:platform :cli
 :capabilities
 #{:persistence
   :open-uri}}
```

Domain code can issue:

```clojure
(request-effect state
                {:type :platform/open-uri
                 :uri "https://example.org"})
```

A platform adapter decides how to execute it.

Do not scatter:

```clojure
(if android? ...)
```

through the portable reducer.

---

# 19. Multiplatform acceptance boundary

The following should be identical source on Android, GTK, and CLI:

```text
domain model
reducers
event vocabulary
effect vocabulary
validation
state serialization
derived view model
business rules
tests
```

The following may differ:

```text
native lifecycle
UI widget implementation
platform capabilities
filesystem paths
notifications
permissions
intents
clipboard
thread scheduler
native bootstrap
```

A useful metric for the final report is:

```text
shared Jolt LOC
Android-specific Jolt LOC
Kotlin LOC
JNI/C LOC
GTK-specific Jolt LOC
CLI-specific Jolt LOC
```

Do not optimize for an artificially high sharing percentage.

---

# 20. Phase 8 — REPL and debug evaluation

There should be two levels of REPL support.

## Level A — mandatory portable-core nREPL

Run shared/domain code with normal Jolt nREPL through the CLI/REPL host on
native Linux and Apple Silicon macOS. This is the primary REPL-driven
development environment and must not depend on GTK.

For every domain change:

```text
edit
 ↓
evaluate namespace/function
 ↓
exercise state transition
 ↓
only then write/adjust test
```

Jolt's current nREPL implementation is specifically designed for redefining code in a live process. ([GitHub][5])

## Level B — Android debug-eval bridge

After embedding is stable, investigate an Android REPL-like mechanism.

Do **not** initially attempt to run full nREPL networking inside Android.

Instead add a debug-only exported function:

```text
poc_eval
```

and provide:

```bash
./scripts/android-repl
```

Protocol:

```text
host process
   │
ADB forwarded localhost socket
   │
Kotlin debug server
   │
JoltRuntime queue
   │
Jolt thread
   │
load-string/eval
```

Example:

```clojure
(poc.domain/current-state)

(swap! poc.domain/app-state update :counter inc)

(poc.android/runtime-info)
```

Every eval must go through the same dedicated Jolt thread.

### Stretch goal

Investigate live component/domain redefinition.

Do not make it a PoC success criterion.

The report should explicitly distinguish:

```text
interactive eval
vs.
code redefinition
vs.
full nREPL/CIDER compatibility
```

---

# 21. Phase 9 — automated Android interaction

There are now useful first-party Android CLI facilities specifically suited to coding agents.

The current Android CLI supports:

* starting/stopping emulators;
* installing/running APKs;
* returning the active UI layout as JSON;
* screenshots;
* annotated screenshots;
* resolving visual element labels to coordinates;
* rendering Compose previews with semantic trees. ([Android Developers][14])

Use these where available.

Fallback entirely to `adb` if integrating the newer CLI complicates the pinned Nix environment.

### Minimum visual test loop

For every major UI feature:

```text
build APK
   ↓
install
   ↓
launch
   ↓
interact
   ↓
capture screenshot
   ↓
inspect screenshot multimodally
   ↓
inspect semantic/layout tree
   ↓
assert expected state
```

ADB supports direct PNG capture:

```bash
adb exec-out screencap -p > artifacts/screenshots/foo.png
```

officially. ([Android Developers][15])

Do not treat "APK installed successfully" as UI validation.

---

# 22. Visual test artifacts

Every showcase state should produce:

```text
artifacts/screenshots/
├── 01-runtime.png
├── 02-counter-initial.png
├── 03-counter-mutated.png
├── 04-lifecycle.png
├── 05-permission-before.png
├── 06-permission-after.png
├── 07-persistence-restored.png
└── 08-platform-effects.png
```

Also store corresponding machine-readable layout trees:

```text
artifacts/layouts/
```

The multimodal agent should inspect:

* clipping;
* invisible text;
* dialogs;
* permission state;
* incorrect platform info;
* counter values;
* lifecycle state;
* visual crash/error screens.

Use semantic assertions in addition to visual judgment.

---

# 23. Testing pyramid

## Tier 1 — pure Jolt tests

Fastest and most important:

```text
reducers
validation
effect generation
serialization
capability decisions
```

Run constantly.

## Tier 2 — portable CLI and Linux GTK integration

Run deterministic event/wire fixtures through the CLI on every supported native
host, then run GTK/mock-platform integration on Linux.

Verify:

```text
same reducers
same events
same state evolution
canonical CLI output
```

## Tier 3 — native C ABI tests

Test the generated Jolt library from a tiny native host before Android.

For each export:

```text
init
lookup
call
GC
repeat
shutdown
```

## Tier 4 — JNI instrumentation

Test:

```text
Kotlin → JNI → Jolt
```

without depending on Compose.

## Tier 5 — Android instrumentation/UI

Test real application workflows.

## Tier 6 — screenshot/multimodal

Validate rendered output.

---

# 24. REPL-first rule

Unit tests should follow experiments rather than lead them.

For example:

```text
REPL:
(step state {:type :counter/inc})
```

Observe correct behavior.

Then:

```clojure
(deftest increment-test
  ...)
```

For JNI:

```text
small C harness
   ↓
manual call
   ↓
observed result
   ↓
automated native test
```

For Android:

```text
adb command / debug eval
   ↓
observe app
   ↓
instrumentation test
```

This preserves an actual REPL-driven development cycle rather than merely having an nREPL dependency installed.

---

# 25. Crash/stress experiments

Explicitly test:

### GC

Repeatedly pass increasingly large structures.

### JNI lifetime

Create/destroy Java strings and native values aggressively.

### process recreation

```bash
adb shell am force-stop ...
```

then restart.

### activity recreation

Rotate device repeatedly.

### background/foreground

Use emulator/ADB lifecycle operations.

### thread misuse

Have a test intentionally invoke Jolt from another thread.

Expected result should be an explicit bridge assertion, not memory corruption.

### repeated initialization

Call init incorrectly twice.

Must fail deterministically.

### Unicode

Test:

```text
ASCII
Finnish characters
emoji
multi-codepoint graphemes
```

Jolt uses codepoint-oriented strings rather than JVM UTF-16 semantics, so string-boundary testing is worthwhile. ([GitHub][5])

### large messages

Test at least approximately:

```text
1 KB
64 KB
1 MB
```

to expose JNI/wire assumptions.

---

# 26. Android-specific blocking-issue investigation matrix

The coding agent should actively attempt to falsify feasibility in these areas:

| Area              | Question                                                            |
| ----------------- | ------------------------------------------------------------------- |
| Chez target       | Can current Chez be cross-built against Android/Bionic?             |
| Jolt compiler     | Can target code generation be separated from host Jolt execution?   |
| linker            | Can `build --library` use NDK clang/lld?                            |
| runtime           | Are there glibc assumptions in Jolt/Chez?                           |
| dynamic libraries | Does Jolt attempt to load desktop `.so` names?                      |
| filesystem        | Are runtime paths valid under Android sandboxing?                   |
| signals           | Does Chez rely on unsupported/restricted signal behavior?           |
| executable memory | Does Chez native-code runtime require behavior Android blocks?      |
| GC                | Does embedded GC survive Android lifecycle/repeated JNI calls?      |
| threading         | Is the single-entry-thread model sufficient?                        |
| FFI               | Do callbacks or `:blocking` calls behave correctly on Bionic?       |
| ABI translation   | Does ARM64 Jolt execute on API-35 x86_64 emulator?                  |
| packaging         | Can Gradle package/load the generated library without modification? |
| REPL              | Can `eval` remain available in an embedded dev build?               |

Each negative result gets its own experiment directory and Bead.

---

# 27. Reproducible issue format

For every blocking or upstream-worthy issue create:

```text
experiments/EXP-012-jolt-cross-library-linker/
├── README.md
├── minimal/
├── commands.sh
├── expected.txt
└── actual.txt
```

`README.md` must contain:

```markdown
# Problem

One-sentence description.

## Environment

Exact commits and tool versions.

## Minimal reproduction

Commands from a clean Nix shell.

## Expected

...

## Actual

...

## Investigation

Facts only.

## Workaround

If any.

## Suspected layer

Jolt / Chez / Android NDK / emulator / our code.

## Upstream suitability

Yes / No / Uncertain.
```

A person should be able to copy only that directory into a clean checkout and reproduce the failure.

---

# 28. `GOTCHAS.md`

Maintain this continuously rather than writing it retrospectively.

Examples of the categories it should eventually contain:

```text
Android/Bionic vs Linux/glibc
Jolt library thread affinity
Chez cross compiler
Android NDK linker flags
PIC requirements
ABI translation
emulator quirks
Gradle/JNI packaging
Jolt string lifetime
JNI global/local references
Jolt eval in library builds
Android lifecycle
Compose/Jolt scheduling
Nix read-only SDK behavior
X11/emulator behavior
root-shell behavior
```

Jolt already notes that shared-library builds need an appropriately PIC Chez kernel on Linux; Android should be checked separately rather than assuming the host rule transfers unchanged. ([jolt-lang.net][2])

---

# 29. Commit discipline

After each successful small experiment:

```text
tests green
working live system demonstrated
Bead updated
git diff reviewed
atomic commit
Bead closed
```

Example history:

```text
chore: add reproducible Nix Android shell
test: prove API 35 emulator arm64 translation
build: cross-compile Chez kernel for Android arm64
test: embed Chez in minimal Android JNI app
build: produce Android arm64 Jolt shared library
test: call Jolt integer export through JNI
feat: serialize domain events across Jolt bridge
feat: serialize all Jolt calls on runtime thread
feat: render shared counter using Compose
feat: add GTK host for shared reducer
feat: add Android lifecycle event demo
feat: add platform effect demonstrations
dev: add Android eval bridge
docs: record Android integration gotchas
docs: publish final feasibility assessment
```

Avoid commits containing several independent platform discoveries.

---

# 30. Autonomous failure policy

When an experiment fails:

```text
1. preserve exact failure;
2. reduce it;
3. create/update Bead;
4. create reproducible experiment;
5. investigate until the responsible layer is reasonably known;
6. try one or more bounded alternative approaches;
7. document the conclusion;
8. continue with independent work.
```

Do not spend unlimited effort overcoming one failure.

Examples:

If API35 ARM translation fails:

```text
→ test ARM64 emulator
→ document ABI blocker
→ continue Linux/shared-domain and native cross-build work
```

If Jolt cross compilation fails:

```text
→ prove raw Chez Android embedding
→ reduce Jolt-specific failure
→ document required Jolt capability
→ continue portable-domain + GTK work
```

Thus even an unsuccessful Jolt/Android deployment produces a useful research result.

---

# 31. Clean-room verification

Before declaring success, perform a complete rebuild from:

```bash
git clean -xfd
```

except persistent Beads data as appropriate.

Then:

```bash
nix develop
./scripts/bootstrap
./scripts/verify
```

`verify` should:

```text
run shared tests
run Linux Jolt tests
build GTK app
build Android native artifacts
build APK
start/reset emulator
install APK
run instrumentation tests
exercise demo
capture screenshots
collect logcat
verify no crash markers
produce report summary
```

The repository must not depend on manually installed SDK components.

---

# 32. Final `REPORT.md`

The most important deliverable is not the demo—it is the resulting knowledge.

Use approximately:

```markdown
# Jolt on Android PoC Report

## Executive conclusion

Viable / viable with patches / blocked.

## Tested environment

Exact versions.

## Architecture proven

diagram

## What works

...

## What does not work

...

## Required Jolt changes

...

## Required Chez changes

...

## Android-specific constraints

...

## Emulator compatibility

...

## Threading model

...

## REPL experience

...

## Multiplatform code sharing

...

## GTK comparison

...

## Performance observations

startup
JNI round trip
dispatch
memory
APK size

## Production risks

...

## Recommended next step

...

## Reproducible experiments

links to EXP-* directories
```

---

# 33. PoC success levels

Use graded outcomes rather than one binary result.

### Level 0 — blocked

Chez cannot be made to execute in an API35 Android process.

### Level 1 — Chez works

```text
Android → JNI → Chez → result
```

### Level 2 — Jolt works

```text
Android → JNI → Jolt → result
```

### Level 3 — persistent Jolt runtime

Repeated stateful calls and GC are stable.

### Level 4 — bidirectional platform integration

```text
Android event
 → Jolt
 → effect
 → Android SDK
 → callback
 → Jolt
```

### Level 5 — development-quality PoC

REPL/debug eval + tests + automated emulator + screenshots.

### Level 6 — multiplatform proof

Same meaningful `.cljc` domain runs under:

```text
Android / Jolt / Chez ARM64
Linux / Jolt / Chez x86_64 / GTK4
CLI / Jolt / Chez native x86_64 Linux, ARM64 Linux, and ARM64 macOS
```

The CLI/REPL host is required for Level 6. GTK remains a Linux UI reference
host; macOS portable-core development must not depend on it.

**Level 6 is the target.**

---

# 34. Recommended architecture if Level 6 succeeds

The PoC should end up approximately here:

```text
                         shared .cljc
               ┌─────────────────────────┐
               │ domain                  │
               │ reducer                 │
               │ events                  │
               │ effects                 │
               │ view-model              │
               │ serialization           │
               └────────────┬────────────┘
                            │
                           Jolt
                            │
       ┌──────┴──────────┬───────────────┐
       │                 │               │
       ▼                 ▼               ▼
Android ARM64       Linux x86_64      CLI / nREPL
       │             Jolt process      Jolt process
libjoltcore.so          │               │
       │             Glimmer          native Linux/macOS
  JNI bridge              │
JoltRuntimeThread     glimmer-gtk
       │                 │
    Kotlin              GTK4
       │
    Compose
       │
 Android SDK
```

This is preferable to building a generic Java-object bridge initially.

The PoC should prove **a narrow data/effect interface first**. A Clojure-like generic Android SDK interop layer can be considered afterward.

---

# 35. First ten Beads to execute

The coding agent should begin in approximately this order:

```text
1  Bootstrap pinned Nix development shell
2  Verify KVM + API35 emulator + screenshot automation
3  Test ARM64 native library on API35 x86_64 emulator
4  Establish portable Jolt CLI/nREPL + unit-test baseline on Linux
5  Prove the same CLI fixtures on Apple Silicon macOS or native ARM64 Lima
6  Build shared reducer and GTK reference application
7  Cross-compile standalone Chez for Android ARM64
8  Embed Chez in minimal JNI Android application
9  Inspect/reduce Jolt build --library cross-target requirements
10 Build minimal Jolt ARM64 Android library exporting answer() = 42
11 Call Jolt export repeatedly from Android and stress GC
```

Only after **10** succeeds should the agent build the larger demo.

That ordering prevents spending most of the project creating Compose screens around an integration path that turns out not to work.

The newest Android tooling also makes the requested autonomous multimodal workflow substantially more practical than it used to be: the Android CLI can expose the current UI hierarchy as JSON, capture and annotate screenshots, resolve visual elements to coordinates, deploy APKs, and render Compose previews for agents. ([Android Developers][14]) This should supplement—not replace—normal `adb`, instrumentation tests, the Jolt REPL, and direct inspection of the native boundary.

[1]: https://github.com/cisco/chezscheme?utm_source=chatgpt.com "GitHub - cisco/ChezScheme: Chez Scheme · GitHub"
[2]: https://jolt-lang.net/docs/native-interop.html "Native Interop (FFI) — Jolt"
[3]: https://developer.android.com/studio/run/emulator-acceleration?hl=en&utm_source=chatgpt.com "Configure hardware acceleration for the Android Emulator  |  Android Studio  |  Android Developers"
[4]: https://android.googlesource.com/platform/prebuilts/android-emulator-build/system-images/%2B/refs/heads/main/generic/system-images/android-35/google_apis/x86_64/build.prop?utm_source=chatgpt.com "generic/system-images/android-35/google_apis/x86_64/build.prop - platform/prebuilts/android-emulator-build/system-images - Git at Google"
[5]: https://github.com/jolt-lang/jolt?utm_source=chatgpt.com "GitHub - jolt-lang/jolt: A Clojure compiler implemented on top of Chez Scheme · GitHub"
[6]: https://github.com/tadfisher/android-nixpkgs?utm_source=chatgpt.com "GitHub - tadfisher/android-nixpkgs: Nix-packaged Android SDK · GitHub"
[7]: https://github.com/android/ndk/wiki?utm_source=chatgpt.com "Home · android/ndk Wiki · GitHub"
[8]: https://developer.android.com/tools/releases/platforms?utm_source=chatgpt.com "SDK Platform release notes  |  Android Studio  |  Android Developers"
[9]: https://developer.android.com/studio/run/emulator-commandline?utm_source=chatgpt.com "Start the emulator from the command line  |  Android Studio  |  Android Developers"
[10]: https://android.googlesource.com/platform/prebuilts/android-emulator-build/system-images/%2B/refs/heads/main/generic/system-images/android-35/google_apis/arm64-v8a/build.prop?utm_source=chatgpt.com "generic/system-images/android-35/google_apis/arm64-v8a/build.prop - platform/prebuilts/android-emulator-build/system-images - Git at Google"
[11]: https://github.com/sheeeng/steveyegge-beads?utm_source=chatgpt.com "GitHub - sheeeng/steveyegge-beads: Beads - A memory upgrade for your coding agent · GitHub"
[12]: https://github.com/jolt-lang/glimmer "GitHub - jolt-lang/glimmer: GTK based Reactive UI toolkit · GitHub"
[13]: https://github.com/cisco/ChezScheme/blob/main/IMPLEMENTATION.md?utm_source=chatgpt.com "ChezScheme/IMPLEMENTATION.md at main · cisco/ChezScheme · GitHub"
[14]: https://developer.android.com/tools/agents/android-cli?authuser=5 "Overview of Android CLI  |  Android Studio  |  Android Developers"
[15]: https://developer.android.com/tools/adb?authuser=1&utm_source=chatgpt.com "Android Debug Bridge (adb)  |  Android Studio  |  Android Developers"
