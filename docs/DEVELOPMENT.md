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
with the ARM64 Jolt libraries. GTK remains Linux-only. On Apple Silicon,
`scripts/emulator-start headless` selects the API 35 ARM64 image and host GPU;
the observed machine could not boot it because it had 8.7 GiB free while the
emulator required 12 GiB for userdata. Do not substitute another SDK or claim
emulator validation until it boots on the host. See
[EXP-019](../experiments/EXP-019-native-darwin-android-nix-build).

## Linux GTK reference host

The default shell supplies GTK4 and GLib on Linux, including the Nix library
path required for Jolt's `dlopen`-based Glimmer GTK FFI. Start the reference
application on an available X11 display:

```sh
DISPLAY=:99 nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/gtk-run
```

The current Fedora x86_64 Lima guest runs the project host and the pinned
upstream Glimmer GTK reactivity smoke; see
[EXP-021](../experiments/EXP-021-x86-64-linux-gtk-host). That is Linux x86_64
reference-host evidence, not native ARM64 Linux, Android, or macOS GTK support.

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
