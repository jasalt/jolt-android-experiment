# Jolt + Raylib Android Research Plan

## Purpose

This document defines a **separate experimental track** for running a Raylib application on Android with Jolt as the application language and runtime.

It continues the work of the primary Jolt Android PoC, but it must not alter the evidence boundary or architecture of that implementation.

The existing Android experiment demonstrates roughly:

```text
Android Activity / Kotlin / Compose
              │
              ▼
             JNI
              │
              ▼
       Jolt / Chez runtime
              │
              ▼
       shared application core
```

The Raylib experiment investigates the opposite ownership model:

```text
Android NativeActivity
        │
        ▼
raylib Android backend
android_main / native_app_glue
        │
        ▼
small native bootstrap
        │
        ▼
    Jolt / Chez
        │
        │ jolt.ffi
        ▼
      raylib
        │
        ▼
 OpenGL ES / Android NDK
```

The key question is therefore not merely whether Raylib can be linked into the existing application.

The experiment asks:

> Can Jolt itself own a practical Raylib application loop on Android, calling Raylib directly through CFFI, while reusing the Android cross-compilation work and portable Clojure code already demonstrated by this repository?

This is deliberately separate from the Compose/JNI architecture.

A failure of the Raylib experiment must not affect the primary Android PoC's success level.

---

# 1. Research goals

The experiment should determine, with reproducible evidence:

1. whether Raylib can be built reproducibly for Android ARM64 using the repository's pinned Nix Android toolchain;
2. whether an Android Raylib `NativeActivity` can initialize the already-proven Android Jolt/Chez runtime;
3. whether the Raylib Android native thread satisfies Jolt's current single-runtime-thread requirement naturally;
4. whether Jolt can call Android Raylib functions directly through `jolt.ffi`;
5. whether the existing `raylib-jlt` bindings can be reused substantially unchanged;
6. whether Jolt can own the complete Raylib frame loop;
7. whether Raylib input, rendering, lifecycle, audio, assets, and selected Android-specific functionality work from Jolt;
8. whether the same Raylib-oriented Jolt application can run on Linux x86_64 and Android ARM64 with a useful shared source subset;
9. whether `raygui-jlt` can provide an all-Jolt immediate-mode GUI on Android;
10. what changes, if any, are required upstream in Jolt, Raylib, `raylib-jlt`, or Android build tooling.

The output is a research result, not a production game/application framework.

---

# 2. Explicit non-goals

Do not:

- replace the existing Compose/JNI Android demo;
- convert the existing Android Activity into Raylib;
- make Raylib a dependency of the primary Android implementation;
- redesign the shared reducer around Raylib;
- build a generic Android SDK binding through Raylib;
- implement a full game engine;
- implement a Jolt wrapper for the whole Raylib API;
- fork Raylib before proving that an upstream-compatible approach fails;
- vendor large duplicated copies of `raylib-jlt`;
- make Raygui a prerequisite for proving basic Raylib operation.

The existing and new experiments should remain independently buildable.

---

# 3. Why this experiment is architecturally different

The primary Android implementation treats Jolt as an embedded application core:

```text
platform owns execution
      │
      ▼
   Jolt core
```

The Raylib experiment should instead test:

```text
Jolt application owns execution
      │
      ▼
Raylib native platform abstraction
```

Conceptually, Android becomes one Raylib target alongside Linux:

```text
                     Jolt application
                           │
                       jolt.ffi
                           │
                         raylib
                ┌──────────┴──────────┐
                │                     │
          Linux desktop            Android
          GLFW/X11/etc.         NativeActivity
                │                 OpenGL ES
                ▼                     ▼
             desktop               device
```

This distinction is important.

The existing Android PoC proves:

> Jolt can be embedded behind a native Android UI.

This experiment attempts to prove:

> Jolt can directly drive a native cross-platform graphical application whose Android host is provided primarily by Raylib and the NDK.

---

# 4. Reference projects

Use these projects as references, pinned to exact commits during implementation:

- `jlt-commons/raylib-jlt`
- `jlt-commons/raygui-jlt`
- `raysan5/raylib`
- `Bigfoot71/RayMob`

Their roles differ.

## raylib-jlt

Primary reference for:

- Jolt `jolt.ffi` bindings;
- Raylib constants and function definitions;
- ergonomic Clojure drawing wrappers;
- native struct handling;
- desktop REPL-driven development;
- screenshot-driven validation;
- automated timed exit;
- example organization.

Do not copy the complete example suite.

Prefer consuming the binding namespace as a pinned dependency if feasible.

## raygui-jlt

Secondary reference for:

- immediate-mode GUI patterns;
- mutable C state through temporary native buffers;
- GUI interaction without retained native widget objects;
- screenshot-based visual validation;
- AOT-specific validation.

Raygui is a stretch phase, not part of basic feasibility.

## raylib

Authority for:

- Android backend behavior;
- `PLATFORM_ANDROID`;
- `android_main`;
- `android_native_app_glue`;
- EGL/OpenGL ES initialization;
- input;
- assets;
- audio;
- lifecycle handling;
- build flags and platform libraries.

Always check the pinned Raylib source before relying on behavior described by third-party templates.

## RayMob

Reference for optional Android-specific additions including:

- vibration;
- sensors;
- soft keyboard;
- Android resource/cache helpers;
- Java/JNI helper calls;
- application customization.

Do not adopt RayMob wholesale initially.

Extract individual techniques only after core upstream Raylib operation is established.

---

# 5. Repository separation

Add a top-level experimental host rather than mixing the code into the existing Android application.

Suggested structure:

```text
jolt-android-experiment/
├── app/                         # existing Compose/JNI PoC
├── src/
│   └── shared/                  # existing portable application core
│
├── raylib/                      # NEW: alternate host experiment
│   ├── README.md
│   ├── deps.edn
│   ├── android/
│   │   ├── build.gradle.kts
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── cpp/
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   └── jolt_raylib_bootstrap.c
│   │   │   └── res/
│   │   └── ...
│   │
│   ├── src/
│   │   └── poc/
│   │       └── raylib/
│   │           ├── app.cljc
│   │           ├── loop.jolt
│   │           ├── view.cljc
│   │           ├── input.cljc
│   │           └── platform.jolt
│   │
│   ├── test/
│   └── assets/
│
├── nix/
│   ├── ... existing modules ...
│   └── raylib.nix               # only Raylib-specific additions
│
├── scripts/
│   ├── ... existing scripts ...
│   ├── raylib-build
│   ├── raylib-run-linux
│   ├── raylib-build-android
│   ├── raylib-run-android
│   └── raylib-verify
│
├── experiments/
│   └── RAY-*/
│
└── docs/
    ├── ...
    ├── RAYLIB-ANDROID-PLAN.md
    └── RAYLIB-GOTCHAS.md
```

Alternative naming such as `hosts/raylib/` is acceptable if it fits the current repository better.

Do not move existing primary-host files merely to create symmetry.

---

# 6. Shared infrastructure

Reuse existing infrastructure only where the behavior is genuinely identical.

## Reuse

Prefer to reuse:

- pinned Jolt source/revision;
- pinned Chez source/revision;
- Android SDK API 35;
- Android NDK;
- JDK;
- Gradle;
- CMake/Ninja;
- Android emulator configuration;
- ARM64 translation validation;
- Nix Android SDK composition;
- Chez Android cross-build;
- Jolt Android cross-build machinery;
- APK inspection helpers;
- `adb` helpers;
- logcat capture;
- screenshot helpers;
- emulator boot/wait helpers;
- environment-report tooling;
- common test-result directory conventions;
- Beads workflow;
- experiment-report format.

Extract small reusable Nix modules or shell helpers where duplication would otherwise occur.

For example:

```text
nix/
├── android-sdk.nix
├── android-ndk.nix
├── jolt-cross.nix
├── chez-android.nix
├── emulator.nix
└── raylib.nix
```

Avoid creating a general abstraction merely because two commands currently look similar.

## Do not reuse

Do not reuse primary-host assumptions around:

- Kotlin `JoltRuntime`;
- Compose;
- HandlerThread ownership;
- EDN-over-JNI dispatch;
- Compose lifecycle mapping;
- Android UI effects.

The Raylib execution model is intentionally different.

---

# 7. Preferred execution architecture

The first architecture to test is Raylib's native Android execution model.

Raylib's Android backend provides an `android_main()` driven through Android native app glue and calls an application-provided native entry function.

Use that boundary.

Target:

```text
Android NativeActivity
        │
        ▼
ANativeActivity_onCreate
        │
android_native_app_glue
        │
        ▼
raylib android_main(app)
        │
stores android_app
        │
        ▼
android_run()
        │
        ▼
jolt_raylib_bootstrap.c
        │
        ├── jolt_library_init()
        ├── jolt_lookup(...)
        └── call Jolt entry
                 │
                 ▼
          poc.raylib/run!
                 │
                 ▼
             jolt.ffi
                 │
                 ▼
               raylib
```

Jolt should then own:

```clojure
(init-window ...)
(loop []
  (when-not (window-should-close?)
    (update!)
    (draw!)
    (recur)))
(close-window)
```

The exact code must be derived from the pinned `raylib-jlt` API rather than invented separately.

---

# 8. Why the native Raylib thread is attractive

The current embedded Jolt library requires calls to remain on one operating-system thread.

Raylib's Android native-app-glue model already provides a dedicated application thread for its native main loop.

That suggests:

```text
Android UI/main thread
        │
        │ native lifecycle events
        ▼
native_app_glue
        │
        ▼
Raylib native thread
        │
        ├── Jolt initialization
        ├── Jolt application loop
        ├── Raylib rendering
        └── Raylib input/lifecycle polling
```

If successful, no equivalent of the primary implementation's Kotlin `HandlerThread` should be necessary.

This must be demonstrated, not assumed.

Record:

- Android main-thread ID;
- native-app-glue thread ID;
- Jolt initialization thread ID;
- repeated Jolt call thread ID;
- render-loop thread ID.

They should show that all Jolt entry remains on one native thread.

---

# 9. Link topology is an explicit research topic

Do not commit to one library layout before testing it.

Test the following configurations in order.

## Topology A — Raylib statically linked into the NativeActivity library

Preferred initial model:

```text
libmain.so
├── android_native_app_glue
├── raylib static objects
└── jolt_raylib_bootstrap.c

libjoltraylib.so
└── Jolt/Chez application runtime
```

`libmain.so` invokes Jolt.

Jolt then needs access to Raylib symbols.

Experiment whether `jolt.ffi` with process-symbol lookup can resolve Raylib symbols from the loaded native process.

Conceptually:

```clojure
:jolt/native
[{:name "raylib-process"
  :process true}]
```

Success criterion:

```text
Jolt defcfn InitWindow
       │
       ▼
process symbol resolution
       │
       ▼
InitWindow inside libmain.so
```

This has the attractive property that Android contains only one Raylib instance.

## Topology B — separate `libraylib.so`

If process-symbol resolution is problematic:

```text
libmain.so
    │
    ├── libraylib.so
    └── libjoltraylib.so
             │
             └── dlopen/libraylib binding
```

Test whether Android's dynamic linker and Jolt's library loading can load the packaged shared object reliably.

Check:

```text
DT_NEEDED
RUNPATH
APK lib/arm64-v8a layout
dlopen behavior
symbol visibility
```

## Topology C — Raylib statically linked into the Jolt library

Investigate only if A and B expose structural problems.

Jolt supports static native archives for built applications; determine whether the Android `build --library` cross-build path supports the same facility.

Potential form:

```text
libjoltraylib.so
├── Jolt/Chez
└── raylib static archive
```

Then determine how Raylib's required `android_main`/NativeActivity entry should be exported.

This topology may require more Jolt build-system work and should not be the starting point.

---

# 10. Phase 0 — create the independent research track

Create a Beads epic:

```text
Raylib Android alternate host
```

All Raylib tasks must be descendants/dependencies of this epic rather than the existing primary Android implementation epic.

Add:

```text
docs/RAYLIB-ANDROID-PLAN.md
docs/RAYLIB-GOTCHAS.md
raylib/README.md
```

`raylib/README.md` should state clearly:

> This is an alternate host experiment. It is not part of the primary Compose/JNI PoC and its results do not alter that PoC's evidence claims.

Acceptance:

- existing `scripts/verify` behavior unchanged;
- primary Android APK unchanged;
- no Raylib dependency enters the primary application;
- Raylib work has its own verification entry point.

---

# 11. Phase 1 — reproduce `raylib-jlt` on Linux

Before Android work, prove the exact pinned Jolt/Raylib combination locally.

Use the existing Fedora x86_64 environment.

Run at least:

```text
basic window
drawing primitives
mouse/input
text
texture
audio if available
one 2D camera example
```

Capture:

```text
Jolt version
raylib-jlt commit
Raylib version
ABI
native library path
```

Use `raylib-jlt`'s automated exit/screenshot conventions where feasible.

The objective is not to test Raylib generally.

It is to establish the exact binding layer that will later be cross-built.

Acceptance:

```text
Jolt
  ↓
jolt.ffi
  ↓
libraylib
  ↓
Linux window
```

with an automatically captured screenshot.

---

# 12. Phase 2 — isolate a minimal reusable binding subset

Do not immediately depend on all ~Raylib bindings.

Identify the minimal functions required for the first Android frame:

```text
InitWindow
WindowShouldClose
BeginDrawing
ClearBackground
DrawText
EndDrawing
CloseWindow
GetScreenWidth
GetScreenHeight
```

Prefer consuming the original `raylib-jlt` namespace.

If its project-level native declaration prevents Android use, separate:

```text
binding source
```

from:

```text
native library declaration
```

with the smallest possible local adapter.

Do not fork the API merely to rename functions.

Record any changes that could reasonably be contributed upstream.

---

# 13. Phase 3 — build plain Raylib Android before adding Jolt

Using the existing Nix Android SDK/NDK, build the smallest possible upstream-style Raylib Android application.

C only:

```c
void android_run(void)
{
    InitWindow(0, 0, "raylib probe");

    while (!WindowShouldClose()) {
        BeginDrawing();
        ClearBackground(...);
        DrawText(...);
        EndDrawing();
    }

    CloseWindow();
}
```

Target:

```text
Android 15 / targetSdk 35
arm64-v8a
```

Run on the same emulator infrastructure used by the primary PoC.

Capture:

- APK;
- `readelf`;
- `aapt`/APK metadata;
- logcat;
- screenshot;
- frame rendering;
- clean lifecycle shutdown.

This separates:

```text
Raylib Android issue
```

from:

```text
Jolt + Raylib issue
```

before the systems are combined.

---

# 14. Phase 4 — verify Raylib under ARM64 translation

Run the ARM64 Raylib APK on the API-35 x86_64 emulator already used by the project.

Verify:

```text
OpenGL ES context creation
frame presentation
touch/input
lifecycle
audio initialization
asset loading
```

Do not infer from the successful Jolt ARM64 library experiment that EGL/OpenGL ES behavior will also translate correctly.

Record this separately:

```text
experiments/RAY-001-raylib-arm64-translation/
```

If translation fails, test a native ARM64 Android host/device when available.

The failure should not block desktop Raylib/Jolt research.

---

# 15. Phase 5 — replace C application body with a Jolt call

Once plain Raylib works, make `android_run()` extremely small.

Conceptual C:

```c
void android_run(void)
{
    jolt_library_init(...);

    void *entry = jolt_lookup("raylib_app_main");

    if (entry == NULL) {
        // fail loudly and log
        return;
    }

    ((void (*)(void))entry)();

    jolt_library_shutdown();
}
```

Jolt:

```clojure
(defn run! []
  ...)

(ffi/export! "raylib_app_main"
             run!
             []
             :void)
```

Initially, `run!` should do nothing except return.

Then:

```text
initialize Jolt
→ call exported function
→ log from Jolt
→ return
→ clean shutdown
```

Only after this is stable should Jolt call Raylib.

---

# 16. Phase 6 — first Jolt-driven Android frame

Target the smallest complete path:

```text
android_main
   ↓
android_run
   ↓
Jolt run!
   ↓
jolt.ffi
   ↓
InitWindow
BeginDrawing
ClearBackground
DrawText
EndDrawing
CloseWindow
```

First version may render for a fixed number of frames:

```clojure
(dotimes [_ 120]
  ...)
```

rather than an infinite application loop.

This makes automated validation easier.

Expected screenshot:

```text
Jolt + Raylib
Android 15
Frame: ...
```

Store:

```text
artifacts/raylib/screenshots/001-first-frame.png
```

This is the critical feasibility milestone.

---

# 17. Phase 7 — persistent frame loop

Move to the normal Raylib loop:

```clojure
(loop [state initial-state]
  (when-not (rl/window-should-close?)
    (let [state' (step state)]
      (draw state')
      (recur state'))))
```

Investigate:

- stack behavior;
- Jolt recursion/tail calls;
- allocation per frame;
- GC behavior;
- frame latency;
- Jolt FFI call volume;
- Android lifecycle events;
- background/foreground transitions.

Avoid allocating large persistent structures every frame unless intentionally testing GC.

Measure:

```text
30 seconds
5 minutes
15 minutes
```

of continuous execution.

Track:

```text
FPS
frame-time distribution
native heap
Jolt heap
GC events if observable
crashes
ANRs
```

---

# 18. Phase 8 — reuse the existing shared reducer

After standalone Raylib operation succeeds, connect it to the repository's shared `.cljc` application/domain code.

Keep Raylib-specific input separate:

```text
Raylib touch/key state
       │
       ▼
raylib input adapter
       │
       ▼
shared domain event
       │
       ▼
existing reducer
       │
       ▼
shared state / effects
       │
       ▼
Raylib renderer/effect adapter
```

Example:

```clojure
{:type :counter/inc}
```

should be the same event already used by the other hosts.

Do not mutate the shared reducer to expose Raylib concepts such as:

```text
Vector2
Texture2D
Color
Rectangle
```

Those belong to the Raylib host.

---

# 19. Phase 9 — Linux/Android shared Raylib host

The Raylib host itself should share substantial Jolt source between Linux and Android.

Target:

```text
raylib/src/poc/raylib/
├── app.cljc
├── loop.cljc
├── input.cljc
├── render.cljc
└── platform/
    ├── desktop.jolt
    └── android.jolt
```

Aim for the following to remain identical:

```text
frame update
domain-event generation
domain reducer calls
drawing commands
basic input interpretation
screen layout
animation
```

Only isolate things that genuinely differ:

```text
filesystem locations
Android lifecycle hooks
soft keyboard
sensors
permissions
Android-specific JNI calls
desktop quit behavior
```

The research report should measure actual shared LOC rather than estimating it.

---

# 20. Phase 10 — touch-first Android interaction

Desktop examples tend to assume mouse and keyboard.

The Android demo must be touch-first.

Implement:

```text
single touch position
tap
hold
drag
multitouch if easily available
back/close behavior
```

Create a visual touch diagnostic:

```text
Touch points: 2

●  x=...
●  x=...
```

Use touch to manipulate shared application state.

If Raylib maps touch into mouse semantics, record precisely where the abstraction works and where Android-specific touch APIs remain necessary.

---

# 21. Phase 11 — screen metrics and orientation

Test:

```text
portrait
landscape
rotation/recreation
different emulator resolutions
high DPI
display cutouts if observable
```

Display diagnostic information:

```text
logical width
logical height
render width
render height
DPI scale
orientation
```

Do not hard-code desktop assumptions such as:

```text
800 x 450
```

into application layout.

This should reveal whether Raylib is practical for ordinary Android full-screen applications rather than only game-style fixed canvases.

---

# 22. Phase 12 — APK asset loading

Package:

```text
PNG texture
font
small audio sample
EDN data file
```

into Android assets.

Validate from Jolt through Raylib:

```text
LoadTexture
font loading
audio loading
read textual asset
```

Pay special attention to Raylib's Android `fopen` wrapping and asset-manager behavior.

Compare behavior with Linux filesystem loading.

The portable application must use logical asset identifiers rather than absolute host paths.

---

# 23. Phase 13 — audio

Exercise Raylib audio from Jolt:

```text
InitAudioDevice
LoadSound
PlaySound
UnloadSound
CloseAudioDevice
```

Test:

```text
launch
play
background
resume
play again
shutdown
```

Observe whether the audio backend survives Android lifecycle transitions.

Do not make audio a prerequisite for basic Raylib success.

Document it as a platform capability.

---

# 24. Phase 14 — Android lifecycle

Raylib internally processes Android application commands.

Instrument enough information to observe:

```text
APP_CMD_INIT_WINDOW
APP_CMD_TERM_WINDOW
APP_CMD_GAINED_FOCUS
APP_CMD_LOST_FOCUS
pause
resume
destroy
```

Determine what Jolt can observe through public Raylib APIs.

If important lifecycle information is not exposed:

1. avoid patching Raylib initially;
2. add a tiny C hook;
3. expose normalized events to Jolt.

For example:

```clojure
{:type :platform/focus-lost}

{:type :platform/focus-gained}
```

Do not expose raw `android_app *` pointers to portable application code.

---

# 25. Phase 15 — Android SDK capability through Raylib

Raylib itself already reaches some Android Java APIs internally.

Test platform-neutral Raylib calls where available, for example:

```text
OpenURL
```

This is architecturally interesting because the call path becomes:

```text
Jolt
 ↓
jolt.ffi
 ↓
Raylib C API
 ↓
Raylib Android backend
 ↓
JNI
 ↓
Android SDK
```

Compare this to the primary experiment:

```text
Jolt
 ↓
EDN effect
 ↓
JNI
 ↓
Kotlin
 ↓
Android SDK
```

Document which model is more convenient and which is easier to reason about.

---

# 26. Phase 16 — RayMob-inspired capability probe

Only after upstream Raylib works reliably, investigate a few capabilities that RayMob demonstrates.

Good candidates:

```text
vibration
soft keyboard
accelerometer
gyroscope
cache/internal paths
simple Java method invocation
```

Do not import all RayMob code.

Instead create a minimal optional native layer:

```text
raylib/android/raylib_android_extra.c
```

Jolt-facing API:

```clojure
(android-extra/vibrate! 50)

(android-extra/show-keyboard!)

(android-extra/accelerometer)
```

Keep this namespace explicitly Android-specific.

Architecture:

```text
Jolt
 ↓
jolt.ffi
 ↓
small Android C helper
 ↓
JNI / NDK
 ↓
Android
```

This intentionally contrasts with the primary Kotlin-effect architecture.

---

# 27. Phase 17 — direct NDK APIs

Where Android exposes suitable functionality through the NDK, prefer:

```text
Jolt
 ↓
CFFI
 ↓
NDK
```

over adding Java/Kotlin merely for symmetry with the primary host.

Possible experiments:

```text
sensors
native activity data
asset manager
logging
native window information
```

However, do not let direct NDK access leak into portable `.cljc` namespaces.

---

# 28. Phase 18 — Raygui stretch experiment

After basic Raylib Android operation is proven, add a separate Raygui Beads epic.

Question:

> Can a usable Android UI be authored almost entirely in Jolt using Raygui's immediate-mode controls?

Port one small `raygui-jlt` example:

```text
button
label
counter
toggle
slider
```

Target:

```text
Jolt
 ↓
jolt.ffi
 ↓
raygui
 ↓
raylib
 ↓
OpenGL ES
```

This creates an interesting third UI model in the repository:

```text
1. Compose
   retained native Android UI
   Kotlin host

2. GTK/Glimmer
   retained native Linux UI
   Jolt host

3. Raygui
   immediate-mode rendered UI
   Jolt host
```

The experiment should compare developer experience rather than declaring one superior.

---

# 29. Raygui Android ergonomics investigation

Specifically evaluate:

```text
touch hit targets
text input
soft keyboard
scrolling
DPI scaling
portrait layouts
accessibility limitations
focus
modal controls
font rendering
theme handling
```

Raygui may be technically functional while still being unsuitable for conventional mobile applications.

The report must distinguish:

```text
renders correctly
```

from:

```text
provides acceptable Android application UX
```

---

# 30. CFFI struct ABI verification

Raylib has many structs passed by value.

This must be treated as an explicit ABI research area.

Test representative cases:

```text
Color
Vector2
Vector3
Rectangle
Camera2D
Camera3D
Texture2D
```

Do not assume an Apple AArch64 result automatically transfers to Android AArch64.

For every representative signature:

```text
1. inspect C declaration;
2. inspect exact jolt.ffi declaration;
3. run a visually or numerically verifiable test;
4. compare Android ARM64 with desktop;
5. preserve minimal reproduction if behavior differs.
```

Prefer current `[:by-value [:struct ...]]` support where correct.

Avoid ABI tricks that merely happen to work unless they are documented and validated for Android AArch64.

If an existing `raylib-jlt` binding uses an architecture-specific workaround, verify it separately.

---

# 31. CFFI library-loading experiments

Test these independently:

## Process binding

```clojure
(ffi/load-library)
```

or project equivalent.

Can Jolt resolve statically linked Raylib symbols from the process?

## Explicit shared object

Can:

```text
libraylib.so
```

be packaged and loaded through Android's linker?

## Static Jolt native dependency

Can the cross-build path consume:

```text
libraylib.a
```

through Jolt's static-native machinery?

Each result should have a minimal experiment.

Do not conflate:

```text
Jolt can call Raylib
```

with:

```text
this particular linking topology works
```

---

# 32. Android Jolt runtime lifecycle

The primary project has already tested Jolt startup/shutdown in a different host.

Repeat relevant stress tests in the Raylib ownership model:

```text
launch
run
background
resume
rotate
destroy
relaunch
```

Because NativeActivity libraries and static global state can behave differently from a conventional Kotlin Activity, do not assume previous process-lifecycle conclusions transfer automatically.

Check:

```text
Raylib global state reset
Chez/Jolt global state reset
double initialization
double shutdown
activity recreation
process recreation
```

---

# 33. Garbage collection during rendering

The frame loop creates a new stress pattern:

```text
Jolt computation
  ↕
hundreds/thousands of CFFI calls per second
  ↕
OpenGL rendering
```

Create three workloads.

## Low allocation

Mutable/primitive-oriented frame state.

## Normal persistent-state workload

Use idiomatic persistent Clojure data.

## Deliberate allocation stress

Create temporary vectors/maps every frame.

Measure:

```text
FPS
frame-time spikes
GC frequency
memory
stability
```

The objective is not competitive benchmarking.

The objective is to discover whether Jolt GC interaction makes a direct graphical loop impractical.

---

# 34. FFI call-volume experiment

Rendering can involve many small C calls.

Measure representative frame workloads:

```text
100 calls/frame
1,000 calls/frame
10,000 calls/frame
```

at:

```text
30 FPS
60 FPS
```

Separate:

```text
CFFI overhead
```

from:

```text
GPU workload
```

using trivial scalar functions if necessary.

If call overhead becomes meaningful, investigate:

```text
batching
rlgl
native helper functions
data-oriented command buffers
```

but only after measuring the problem.

---

# 35. Optional render-command batching experiment

If many Jolt→C calls are expensive, prototype:

```clojure
[[:rect 10 10 50 50 red]
 [:text "hello" 20 20 24 white]
 ...]
```

passed to a native batch helper.

Compare:

```text
direct CFFI per draw
```

against:

```text
one CFFI call + native command loop
```

Do not adopt batching unless evidence justifies the complexity.

One of the purposes of this experiment is to determine whether direct CFFI is already sufficient.

---

# 36. REPL-driven development

Desktop Raylib should provide the primary interactive development environment.

Use:

```text
Jolt nREPL
Linux Raylib window
```

for rapid iteration on:

```text
domain logic
drawing
input interpretation
layout
animation
```

Investigate how much can be redefined while a Raylib loop is running.

Potential architecture:

```text
render loop
   │
dereference current draw fn
   │
nREPL redefines fn
   │
next frame uses new implementation
```

If full live function replacement works, document it carefully.

If the render loop blocks nREPL servicing, treat that as a research problem.

---

# 37. Android debug-eval

Reuse the primary project's debug-eval infrastructure only if doing so does not require introducing Kotlin ownership into the Raylib host.

Possible approaches:

## A. Native socket debug server

Jolt or C opens a localhost socket reached through:

```text
adb forward
```

## B. Raylib-specific native command channel

Small C socket server posts evaluation requests to the same Raylib/Jolt thread.

## C. No Android live eval initially

Use desktop nREPL and Android rebuild/run.

This is acceptable for the first success level.

Do not compromise thread safety merely to preserve interactive eval.

---

# 38. Screenshot automation

Reuse the existing Android screenshot helpers.

Raylib also provides screenshot functionality, so collect both where practical:

```text
Android framebuffer screenshot through adb
Raylib-generated screenshot
```

Compare them.

The ADB screenshot is authoritative for what Android presented.

Store:

```text
artifacts/raylib/screenshots/
├── 001-first-frame.png
├── 002-touch.png
├── 003-shared-state.png
├── 004-texture.png
├── 005-lifecycle-resume.png
├── 006-raygui.png
└── ...
```

The multimodal coding agent must inspect them.

---

# 39. Automated interaction

Use `adb` input commands and semantic/native tooling where applicable.

Raylib is rendered into a graphics surface, so Android's normal semantic UI tree may contain very little useful information.

This itself is an important finding.

Automated tests should therefore combine:

```text
adb tap/swipe
known screen coordinates
Raylib internal test state
screenshots
log output
```

For deterministic visual automation, provide a diagnostic mode with fixed layout dimensions relative to the current framebuffer.

---

# 40. Test instrumentation inside the Raylib application

Because rendered controls do not necessarily expose Android semantics, add a debug state endpoint or log output.

For example:

```clojure
{:screen [1080 2400]
 :frame 312
 :touches [...]
 :counter 4
 :lifecycle :focused
 :asset-loaded? true}
```

Make the testing harness able to retrieve this without OCR.

Screenshots should verify rendering, not serve as the only way to determine internal state.

---

# 41. Unit testing

Continue the repository's REPL-first discipline.

Shared domain behavior:

```text
REPL
→ observe
→ unit test
```

Raylib-independent host logic should also be testable without a display:

```text
touch → domain-event translation
layout calculations
animation state
render-command generation
capability decisions
```

Avoid unit-testing Raylib itself.

Use integration/screenshot tests for actual rendering.

---

# 42. Headless compilation gate

Borrow the useful pattern from `raylib-jlt`:

```text
require every Raylib namespace
compile every binding
fail on unresolved Clojure symbols
```

This catches source/binding errors without starting a display.

However, do not treat it as rendering evidence.

Maintain separate gates:

```text
raylib-check
raylib-linux-smoke
raylib-android-smoke
raylib-visual
```

---

# 43. AOT/build-image gate

Because Jolt behavior can differ between:

```text
jolt run
```

and:

```text
jolt build
```

the experiment must include a real built-image test.

Every core binding used on Android should therefore be exercised in:

```text
desktop Jolt run
desktop built binary
Android built shared library
```

This is especially important for:

```text
top-level FFI layouts
native symbol resolution
static native libraries
exported entry points
```

---

# 44. Android logging

Provide a tiny logging abstraction:

```clojure
(log/info "frame initialized")
```

Backend:

```text
Linux → stderr
Android → logcat
```

Possible Android implementation:

```text
Jolt CFFI → __android_log_print
```

This is a good first direct Android NDK binding.

Logs should identify:

```text
thread ID
frame number
lifecycle event
Jolt init/shutdown
Raylib init/shutdown
```

in debug builds.

---

# 45. Performance measurements

Collect at minimum:

```text
APK size
native libraries and sizes
cold startup
time to first frame
steady FPS
frame time
memory after startup
memory after 5 minutes
Jolt init time
Raylib InitWindow time
```

Compare cautiously with the Compose host.

The objective is architectural understanding, not a benchmark contest.

A useful table:

| Measurement | Compose/JNI host | Raylib/Jolt host |
|---|---:|---:|
| APK size | | |
| cold startup | | |
| native heap | | |
| Jolt startup | | |
| time to visible UI | | |
| host-specific LOC | | |

Only compare measurements collected under comparable emulator/device conditions.

---

# 46. Developer-experience assessment

The final report should explicitly compare development workflows.

## Primary Compose/JNI host

```text
edit portable Jolt
→ nREPL/test
→ build Jolt .so
→ Gradle
→ Kotlin/Compose host
→ emulator
```

## Raylib host

Potentially:

```text
edit Jolt
→ nREPL + desktop Raylib
→ visually verify immediately
→ cross-build Jolt + Raylib
→ package NativeActivity APK
→ emulator
```

Assess:

```text
setup complexity
incremental-build time
REPL usefulness
UI iteration speed
cross-platform fidelity
debugging difficulty
Android API access
native crash risk
tooling quality
```

This is one of the experiment's most important outputs.

---

# 47. Platform abstraction comparison

Document the three host models side by side.

| Concern | Compose Android | GTK/Glimmer | Raylib |
|---|---|---|---|
| UI ownership | Kotlin | Jolt/Glimmer | Jolt |
| Rendering | Android native widgets | GTK native widgets | Raylib/OpenGL |
| Jolt→platform | effects/JNI | CFFI/Glib/GTK | CFFI/Raylib |
| host loop | Android | GTK | Raylib |
| native controls | yes | yes | no |
| Android SDK depth | high | n/a | limited/bridge |
| cross-platform rendering | low | GTK only | high |
| accessibility | Android native | GTK native | application responsibility |
| game/graphics suitability | moderate | low | high |

Treat this as an empirical comparison and update it as evidence accumulates.

---

# 48. Accessibility investigation

Raylib-rendered UI does not automatically become ordinary Android native widgets.

For a non-game application, investigate the consequences for:

```text
TalkBack
semantic labels
keyboard navigation
focus
font scaling
system themes
accessibility actions
```

This may be a fundamental trade-off.

Do not attempt to solve full accessibility in the PoC.

Document whether lack of native semantics makes Raylib unsuitable for certain application classes.

---

# 49. Text input

Text entry is likely to expose an important mobile-specific gap.

Test progressively:

```text
hardware keyboard
emulator keyboard
Android soft keyboard
Unicode
backspace
selection if supported
IME composition
```

RayMob's soft-keyboard helpers are useful research material here.

Do not claim general Android form support based solely on `GetCharPressed`.

---

# 50. Assets and writable filesystem

Map these concepts separately:

```text
APK packaged asset
internal writable data
cache
external/shared storage
```

Raylib APIs may abstract some of these differently from ordinary Linux filesystem semantics.

Portable code should depend on logical services such as:

```clojure
(load-asset "logo.png")

(save-state! data)
```

rather than platform paths.

---

# 51. Networking

Networking is not needed for basic feasibility.

If tested later, distinguish:

```text
Jolt networking library
Raylib-related networking
Android permissions
```

Do not add network access merely to demonstrate another platform feature.

---

# 52. Failure categories

Every failure should be classified as one of:

```text
RAYLIB
RAYLIB_ANDROID
RAYLIB_JLT_BINDING
JOLT_FFI
JOLT_ANDROID_BUILD
CHEZ_ANDROID
ANDROID_LINKER
ANDROID_EMULATOR_TRANSLATION
NIX_ENVIRONMENT
APPLICATION_CODE
UNKNOWN
```

Preserve minimal reproductions.

---

# 53. Experiment format

Use the existing repository experiment conventions.

Suggested IDs:

```text
RAY-001 plain Raylib Android
RAY-002 Raylib ARM64 translation
RAY-003 Jolt bootstrap from android_run
RAY-004 process-symbol Raylib CFFI
RAY-005 first Jolt-rendered frame
RAY-006 persistent Jolt frame loop
RAY-007 touch input
RAY-008 APK assets
RAY-009 audio
RAY-010 lifecycle
RAY-011 shared reducer
RAY-012 Android OpenURL
RAY-013 vibration helper
RAY-014 soft keyboard
RAY-015 Raygui
RAY-016 FFI stress
RAY-017 GC/frame-time stress
RAY-018 desktop/Android parity
```

Each should contain:

```text
README.md
commands.sh
expected.txt
actual.txt
logs/
screenshots/
minimal/ where relevant
```

---

# 54. `RAYLIB-GOTCHAS.md`

Maintain separately from the existing Android gotchas.

Potential categories:

```text
Raylib Android NativeActivity lifecycle
native_app_glue
android_run ownership
EGL/OpenGL ES
ARM64 struct ABI
Jolt FFI struct-by-value
static vs dynamic Raylib linking
Android process-symbol lookup
APK assets
audio lifecycle
touch vs mouse mapping
screen scaling
soft keyboard
Jolt GC during frame loop
REPL while main loop runs
Raygui mobile ergonomics
Android accessibility
```

If a gotcha applies equally to the primary Jolt Android host, link or promote it to the general document rather than duplicate conflicting explanations.

---

# 55. Beads task graph

Create tasks approximately as follows.

```text
EPIC Raylib Android alternate host
│
├── R1 Pin Raylib and raylib-jlt revisions
├── R2 Reproduce raylib-jlt Linux basic window
├── R3 Add shared Nix Raylib tooling
│
├── R4 Build plain Raylib Android API35 ARM64
│   └── R5 Run plain Raylib through x86_64 emulator translation
│
├── R6 Build minimal Jolt Raylib Android library
│   └── R7 Bootstrap Jolt from android_run
│
├── R8 Test Raylib symbol lookup topology A
├── R9 Test topology B if required
├── R10 Test topology C if required
│
├── R11 Render first frame from Jolt
│   └── R12 Run persistent frame loop
│       ├── R13 Touch input
│       ├── R14 Lifecycle
│       ├── R15 Assets
│       └── R16 Audio
│
├── R17 Integrate shared domain reducer
│   └── R18 Verify Linux/Android behavior parity
│
├── R19 Test Raylib Android SDK bridge call
├── R20 Test optional RayMob-style vibration
├── R21 Test optional soft keyboard
│
├── R22 Stress CFFI call volume
├── R23 Stress Jolt GC during rendering
│
├── EPIC Raygui Android stretch
│   ├── RG1 Pin/reproduce raygui-jlt desktop
│   ├── RG2 Build Raygui for Android
│   ├── RG3 Render controls
│   ├── RG4 Touch interaction
│   └── RG5 Mobile ergonomics report
│
└── R24 Final Raylib feasibility report
```

Only topology tasks required by actual failures should be activated.

---

# 56. Autonomous-agent workflow

For every task:

```text
read Bead
   ↓
state hypothesis
   ↓
smallest experiment
   ↓
run it
   ↓
inspect logs/screenshot
   ↓
reduce unexpected failures
   ↓
REPL experiment where possible
   ↓
automated regression test
   ↓
update experiment evidence
   ↓
atomic commit
   ↓
close Bead
```

Do not spend multiple tasks building abstractions before first-frame feasibility.

---

# 57. Suggested implementation ordering

Strict early order:

```text
1. desktop raylib-jlt works
2. plain C Raylib Android works
3. Jolt initializes from Raylib Android thread
4. Jolt calls one trivial process C symbol
5. Jolt resolves one Raylib symbol
6. Jolt InitWindow works
7. Jolt draws one frame
8. screenshot proves frame
9. persistent loop
10. touch
11. shared reducer
```

This order should not be reversed.

In particular:

> Do not add Raygui, audio, sensors, or shared-domain abstractions before Jolt renders a verified Android frame through Raylib.

---

# 58. First-frame acceptance criterion

The central experiment passes when a clean checkout can perform:

```sh
nix develop
./scripts/raylib-build-android
./scripts/emulator-start
./scripts/raylib-run-android
```

and automatically produce evidence showing:

```text
Android 15 process
ARM64 Jolt/Chez
Raylib Android backend
Jolt calling Raylib through jolt.ffi
visible rendered frame
```

The frame should display runtime information generated from the Jolt application, for example:

```text
Jolt + Raylib Android

Jolt: ...
Chez: ...
Raylib: ...
ABI: arm64-v8a
Frame: 120
```

---

# 59. Development-quality acceptance criterion

A stronger success requires:

- deterministic Nix environment;
- Linux Raylib host;
- Android Raylib host;
- same meaningful Jolt application logic on both;
- touch input;
- persistent render loop;
- assets;
- lifecycle validation;
- stress testing;
- automated screenshots;
- reproducible clean build;
- documented limitations;
- no modifications required to the primary Android host.

---

# 60. Research success levels

## R0 — environment only

Raylib Android toolchain configured but no native app execution.

## R1 — plain Raylib Android

C Raylib application renders successfully.

## R2 — Jolt hosted by Raylib Android

Raylib's native application thread initializes and calls Jolt.

## R3 — Jolt → Raylib CFFI

Jolt successfully invokes Raylib through `jolt.ffi`.

## R4 — complete Jolt frame loop

Jolt owns update/render/input loop and runs stably.

## R5 — portable application host

Meaningful shared application/domain code runs under both:

```text
Linux Raylib
Android Raylib
```

## R6 — development-quality alternate host

Adds:

```text
REPL-oriented desktop workflow
Android automation
screenshots
lifecycle
assets
stress testing
documented limitations
```

## R7 — Raygui/mobile UI research

Raygui controls run and are evaluated for mobile usability.

Target:

**R6**

Raygui R7 remains optional.

---

# 61. Blocking issues worth upstreaming

Potential upstream Jolt issues:

```text
Android static native archives with build --library
Android native-library lookup
FFI struct ABI
AOT-only FFI behavior
cross-target native dependency selection
```

Potential `raylib-jlt` contributions:

```text
Android-compatible native declaration strategy
portable struct bindings
Android smoke example
binding fixes exposed by ARM64 Android
```

Potential Raylib issues:

```text
only if reproduced in a plain C Android example
```

Do not file Raylib issues for failures that occur only when Jolt is present until the C boundary has been reduced.

---

# 62. Final report

Create:

```text
docs/RAYLIB-REPORT.md
```

Structure:

```markdown
# Jolt + Raylib Android Report

## Conclusion

## Proven architecture

## Environment

## Raylib build

## Jolt bootstrap

## CFFI binding strategy

## Native library topology

## Threading model

## Rendering

## Touch/input

## Lifecycle

## Assets

## Audio

## Android SDK integration

## Desktop portability

## Shared source

## REPL/development workflow

## Performance observations

## GC observations

## FFI call overhead

## Raygui assessment

## Accessibility implications

## Comparison with Compose/JNI host

## Blocking issues

## Upstream opportunities

## Reproducible experiments

## Recommended next step
```

---

# 63. Expected architectural result if successful

The repository would then demonstrate three meaningfully different Jolt host models:

```text
                       shared application/domain code
                                  │
                 ┌────────────────┼─────────────────┐
                 │                │                 │
                 ▼                ▼                 ▼

             Android           Linux GTK        Raylib
             Compose            Glimmer        Linux/Android
                 │                │                 │
              Kotlin             GTK            jolt.ffi
                 │                │                 │
               JNI            native FFI          Raylib
                 │                                  │
               Jolt                          native platform
```

Or viewed by runtime ownership:

```text
Compose host:
Android owns UI/event loop
        ↓
      Jolt

GTK/Glimmer host:
Jolt/Glimmer coordinates native widgets
        ↓
       GTK

Raylib host:
Jolt owns application/frame loop
        ↓
     Raylib
        ↓
native platform
```

This makes the Raylib work useful even if it never becomes the preferred Android UI approach.

It tests a different and important proposition:

> Whether Jolt is practical not only as an embedded portable core, but also as the primary language controlling a native, graphical, cross-platform C runtime directly through its FFI.

---

# 64. Immediate first Beads

Start with these tasks only:

```text
1. Pin current raylib, raylib-jlt and raygui-jlt revisions.
2. Reproduce a raylib-jlt basic window under the repository Nix environment.
3. Factor only the Android SDK/NDK/Jolt-cross Nix helpers needed by both hosts.
4. Build a plain C Raylib Android 15 ARM64 NativeActivity APK.
5. Run that APK on the existing API-35 x86_64 emulator and capture a screenshot.
6. Replace the C application body with an android_run bootstrap that initializes Jolt.
7. Prove a no-argument Jolt export executes on the Raylib native thread.
8. Test process-symbol resolution from Jolt to one Raylib C function.
9. Render one fixed Jolt-driven Raylib frame.
10. Preserve the experiment and decide whether the architecture is viable before implementing anything further.
```

Task 9 is the first major go/no-go point.

If it succeeds, continue toward the persistent loop and shared application.

If it fails, reduce the linker/FFI/runtime problem before adding features.