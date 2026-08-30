# Development

## Portable Jolt core

Enter the pinned shell and run the portable test and CLI fixtures:

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/test-portable
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/cli --event '{:type :counter/inc}'
```

The CLI accepts exactly `--event <EDN map>` and writes one canonical EDN result
with `:model` and `:effects`. Invalid CLI arguments or invalid EDN fail with a
nonzero exit status. Run `./scripts/cli --conformance` to execute the shared
canonical fixture corpus.

Use Jolt's normal nREPL workflow for interactive portable-core development:

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c jolt nrepl-server
```

The server writes `.nrepl-port`; connect an nREPL client to loopback and evaluate
`(require '[poc.reducer :as r])` followed by `(r/step r/initial-state {:type
:counter/inc})`. The same fixture suite and a loopback nREPL evaluation passed
on native Apple Silicon macOS without GTK; see
[EXP-017](../experiments/EXP-017-native-arm64-portable-cli).

## Android Build

The pinned default shell supplies the immutable Android API 35 SDK, NDK r29,
`adb`, and emulator on `x86_64-linux` and native Apple Silicon macOS. Do not set
an Android Studio or host-installed SDK path.

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c gradle --no-daemon :app:assembleDebug
```

The command passes on the observed `aarch64-darwin` host and produces an APK
with the ARM64 Jolt libraries. GTK remains Linux-only. For the separately
locally signed Raylib gallery distribution artifact, follow
[APK-BUILD.md](APK-BUILD.md); its Android 15/ARM64 compatibility boundary and
release signing identity are deliberate and distinct from the debug workflow.
On Apple Silicon, `scripts/emulator-start headless` selects the API 35 ARM64
image and host GPU;
the observed machine could not boot it because it had 8.7 GiB free while the
emulator required 12 GiB for userdata. Do not substitute another SDK or claim
emulator validation until it boots on the host. See
[EXP-019](../experiments/EXP-019-native-darwin-android-nix-build).

## Linux GTK reference host

The default shell supplies GTK4 and GLib on Linux, including the Nix library
path required for Jolt's `dlopen`-based Glimmer GTK FFI. The project consumes
the pinned upstream `glimmer-gtk` backend; its own GTK FFI calls are narrow
clipboard and URI effect adapters, not a second widget backend.

Start the reference application on an available X11 display:

```sh
DISPLAY=:99 nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/gtk-run
```

`gtk-run` and `gtk-smoke` deliberately require X11 because deterministic
interaction and capture use `xdotool` and FFmpeg's `x11grab`. This is a project
automation constraint, not a GTK/Glimmer restriction; native GTK can use other
display backends, but this repository has not validated a Wayland-only run.

### Live GTK development over nREPL

Start Jolt's nREPL server inside the Linux shell with `DISPLAY` exported:

```sh
DISPLAY=:99 nix --extra-experimental-features 'nix-command flakes' develop -c \
  jolt nrepl-server
```

From the connected REPL, launch the host and iterate without restarting it:

```clojure
(require '[poc.gtk-app :as gtk]
         '[glimmer.core :as ui])
(gtk/-main)                                  ; returns while GTK remains live
(gtk/dispatch! {:type :counter/inc})         ; window repaints
;; evaluate changed component definitions, then:
(ui/reload! gtk/app)
```

`app-state` and adapter diagnostics are top-level `defonce` reactive cells, so
they survive component redefinition and `reload!`. Glimmer schedules ratom
changes made by the nREPL worker onto GTK's main loop; GTK widgets must never be
mutated directly from the REPL thread. The pinned backend's
`repl-live-smoke` validates that scheduling path in EXP-021.

### State and effect semantics

The GTK host keeps authoritative reducer state separate from adapter diagnostics.
A dispatch commits the pure reducer transition before running effects, and an
adapter failure is reported in `:effect-results` without rolling back or adding
host data to the portable model. `:completed` means a synchronous operation such
as persistence finished; `:requested` means GTK accepted an asynchronous desktop
request; `:unavailable`, `:unsupported`, and `:failed` do not imply success.
Likewise, `:capabilities` describes operations implemented by the adapter while
`:capability-status` records session-dependent or request-only availability.

The current Fedora x86_64 Lima guest runs the project host and the pinned
upstream Glimmer GTK reactivity and nREPL scheduling smokes; see
[EXP-021](../experiments/EXP-021-x86-64-linux-gtk-host). That is this project's
Linux x86_64 evidence, not native ARM64 Linux, Android, or macOS GTK support.
Upstream `glimmer-gtk` contains additional platform support, but it is outside
this project's validated boundary.

## Linux Raylib live-layout development

For the independent Raylib host, Linux desktop nREPL remains the fastest
portable visual path, and Android debug builds now provide an on-device nREPL
loop after the initial AOT build/install. The desktop evidence is
[RAY-015](../experiments/RAY-015-linux-raylib-nrepl),
with the complete disposable upstream capture in
[`../../raylib-jlt/nrepl-results/`](../../raylib-jlt/nrepl-results/).

Start a disposable `raylib-jlt` desktop session using its normal server:

```sh
cd ../raylib-jlt
bb check
bb nrepl 17888
```

For desktop examples that create a window from an editor evaluation, use the
upstream Raylib-Jolt `rl/run!` helper rather than calling `(-main)` directly:
[REPL-driven development: why `(-main)` kills your editor connection](https://jlt-commons.github.io/raylib-jlt/guide/repl-driven-development.html).
`rl/run!` schedules the entry on Jolt's main-thread pump asynchronously, keeping
the editor responsive; the blocking `call-on-main-thread` form is for scripts
that intentionally wait for window closure. This is especially important on
macOS, where AppKit terminates the process when `InitWindow` runs off the main
thread. Use the upstream
[headless smoke guide](https://jlt-commons.github.io/raylib-jlt/guide/headless-smoke-testing.html)
for timer-based exits.

Connect only over loopback, load the retained `nrepl-results` probe forms, and
redefine a pure `defn` called dynamically by the drawing loop. The captured
experiment and the upstream guide show visible replacement without process
restart, but the Raylib context owner and nREPL request workers are different
Jolt/Chez threads. Never call Raylib drawing, lifecycle, input, or resource FFI
from an nREPL worker, and never retain a startup-captured draw function when it
is intended to be redefined. Use an application-owned bounded owner-thread
queue for any future evaluated work that needs to interact with the frame loop.

### Android Raylib nREPL

[RAY-017](../experiments/RAY-017-android-raylib-nrepl) proves the Android
debug workflow on the translated API-35 emulator, including nREPL continuity
through Home/background and NativeActivity resume. Build and install the
dev-mode image once, launch it, then forward its loopback nREPL:

```sh
JOLT_SOURCE=/path/to/pinned-jolt nix develop -c \
  ./scripts/raylib-persistent-loop-build-android debug
nix develop -c adb -s emulator-5554 install \
  raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk
nix develop -c adb -s emulator-5554 shell am start -n \
  net.joltlang.raylibgallery/android.app.NativeActivity
nix develop -c ./scripts/raylib-android-nrepl forward
```

Connect a generic editor nREPL client to `127.0.0.1:7888`, or use
`scripts/raylib-android-nrepl eval|load-file`. `brepl` is a verified generic
client after forwarding; it evaluates in its default namespace, so fully
qualify project symbols:

```sh
nix develop -c brepl -p 7888 \
  '(poc.raylib.loop/current-runtime-state)'
```

Pure definitions, reducers, layout, animation, input interpretation, and
dynamically called drawing functions can be reevaluated without restarting the
Activity/process or
rebuilding the APK. `load-file` sends host source content; it does not require
that source file to exist in the Android sandbox.

The nREPL evaluator is a Jolt worker, not the Raylib context owner. Evaluating a
`defn` that mentions Raylib is safe only because its body runs later when the
frame owner calls the Var; do not invoke that function in the eval request. For
a short one-off Raylib operation, submit a no-argument closure through
`poc.raylib.loop/submit-owner!`, then poll `owner-result`. The queue is limited
to 16 pending requests, executes one between frames, and retains 64 results.
Never submit blocking or unbounded work.

The server is the minimal built-in Jolt nREPL (`describe`, `clone`, `eval`,
`load-file`, `close`, and built-in completion); optional CIDER middleware was
not validated. Native C/Gradle/manifest/ABI/assets, new FFI signatures, captured
startup values, and release artifacts still require rebuild or explicit
component reset.

This is separate from the primary Compose host's `scripts/android-repl` line
protocol. The Raylib listener and `INTERNET` permission exist only in the debug
variant. Release builds from the non-debug entry, contains no
`raylib_gallery_debug` export, selects the direct-linked `raylib_gallery`, has
no network permission, starts no listener, and did not answer the RAY-017 nREPL
probe.

## Clean-room verification

From a clean generated state, use the pinned shell and a Jolt checkout at the
revision required by `scripts/jolt-android-library-build`:

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/bootstrap
JOLT_SOURCE=/path/to/pinned-jolt \
  nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/verify
```

`verify` is fail-fast and retains one log per tier under
`artifacts/logs/verify/`. It rebuilds the Android library/APK, starts an API-35
emulator only when needed, runs instrumentation/lifecycle/showcase/measurement
tiers, and writes `artifacts/reports/verify-summary.txt`. GTK is explicitly
skipped when its Linux X11 prerequisites are unavailable; the host-native C ABI
harness is explicitly skipped on the known EXP-022 non-PIC kernel boundary.
