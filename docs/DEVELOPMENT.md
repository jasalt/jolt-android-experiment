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

For the independent Raylib host, Linux desktop nREPL is the fast visual
iteration path; Android remains an AOT build/install/run integration target.
The completed desktop evidence is [RAY-015](../experiments/RAY-015-linux-raylib-nrepl),
with the complete disposable upstream capture in
[`../../raylib-jlt/nrepl-results/`](../../raylib-jlt/nrepl-results/).

Start a disposable `raylib-jlt` desktop session using its normal server:

```sh
cd ../raylib-jlt
bb check
bb nrepl 17888
```

Connect only over loopback, load its retained `nrepl-results` probe forms, and
redefine a pure `defn` called dynamically by the drawing loop. The captured
experiment shows that visible layout replacement works without process restart,
but the Raylib context owner and nREPL request workers are different Jolt/Chez
threads. Never call Raylib drawing, lifecycle, input, or resource FFI from an
nREPL worker, and never retain a startup-captured draw function when it is
intended to be redefined. Use an application-owned bounded owner-thread queue
for any future evaluated work that needs to interact with the frame loop.

The current Android gallery has **no** nREPL server. `scripts/android-repl` is
a debug-only, one-form ADB-forwarded evaluator—not nREPL/CIDER or an established
hot-reload facility. Keep Android development to Linux live iteration followed
by the normal ARM64 library/APK rebuild and runtime validation. A future Android
live-eval feature requires a separate debug-only, localhost/ADB-forwarded,
bounded queue experiment on the existing Raylib/Jolt owner thread; it must not
enter Jolt from a transport worker or be included in a release APK.

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
