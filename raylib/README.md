# Jolt + Raylib alternate host

This directory is an **independent research host** for running a Jolt-owned
Raylib application loop on Linux and Android NativeActivity. It is not part of
the primary Compose/JNI PoC in [`../app`](../app), and its results must not
alter the primary PoC's evidence claims in [`../REPORT.md`](../REPORT.md).

## Scope and boundary

The alternate host may reuse only infrastructure whose behavior is genuinely
shared: the pinned Jolt and Chez source, Nix API-35 SDK/NDK toolchain, Android
ARM64 translation emulator, APK inspection, logcat, and screenshot helpers.
It must not modify or depend on the primary host's Kotlin `JoltRuntime`,
Compose UI, EDN-over-JNI dispatch, lifecycle/effect adapters, or `:app`
module. `scripts/verify` also remains the primary-host verifier; a future
Raylib task supplies a separate `scripts/raylib-verify`.

The plain-C baseline is in `android/`; the separate Jolt bootstrap probe is in
`jolt-android/`. They are intentionally independent modules: the baseline
remains a Raylib-only NativeActivity, while the bootstrap probe packages the
separate `libjoltraylib.so` and tests one no-op Jolt export. Neither changes the
primary `:app` host or copies the upstream binding suite.

## Immutable upstream baselines

[`pins.edn`](pins.edn) is the source-of-truth manifest for this track. It pins
all external source by full revision rather than a moving branch:

| Project | Revision | License | Role |
| --- | --- | --- | --- |
| [Jolt](https://github.com/jolt-lang/jolt) | `ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e` | project license; locked in [`../flake.lock`](../flake.lock) | Jolt/Chez application runtime |
| [raylib](https://github.com/raysan5/raylib) | `9f3cadf1e618f125bd9b282c7759f8cb26ce17fc` | zlib | native graphics/platform backend |
| [raylib-jlt](https://github.com/jlt-commons/raylib-jlt) | `cf16df3d323726dc8b100225eeb1156607ae4a55` | zlib | primary Jolt FFI binding and mobile-gallery example reference |
| [raygui-jlt](https://github.com/jlt-commons/raygui-jlt) | `cdab5f97e1cd97e9ea4b7776b1dc0bd161ad4720` | zlib | R7-only immediate-mode UI reference |
| [RayMob](https://github.com/Bigfoot71/RayMob) | `6ff85822872391f399c80771981d8ce25e0a4cfd` | MIT; includes raylib zlib notice | optional Android technique reference |

The raylib-jlt README at its pinned revision requires Jolt **0.7.23+** and a
system Raylib **6.0+** shared library. Its pinned `deps.edn` declares desktop
Darwin/Linux library names only. Therefore it is a desktop baseline and binding
source reference, not proof of Android loading or a dependency to add directly
to this manifest. This pin deliberately includes the upstream
`input-gestures` example and the scalar touch/gesture bindings needed by the
planned gallery. The gallery excludes `input-gamepad`; sensor-driven ball
physics remains later conditional work.

Raygui and RayMob are recorded now for reproducibility but are not active
first-frame dependencies. Their implementation work is conditional on the
separate R6/R7 decisions recorded in `docs/RAYLIB-PLAN.md`.

## Confirmed Raylib Android entry contract

The source pinned above is authoritative. In
`src/platforms/rcore_android.c`, lines 317–331, Raylib declares
`extern int main(int argc, char *argv[])`; `android_main(struct android_app *)`
stores `platform.app`, invokes `main(1, (char *[]) { "raylib", NULL })`, and
then calls `ANativeActivity_finish`. It does **not** expose the plan's
illustrative `android_run` seam. The future plain-C NativeActivity and Jolt
bootstrap must implement the pinned `main(int, char **)` contract (or record a
new pin and re-audit it), not invent a different entry point.

The same Android backend documents a further build concern: APK asset `fopen`
wrapping requires `-Wl,--wrap=fopen` on the final shared-library link, rather
than only when Raylib is archived. This remains an unproven future asset-build
requirement, recorded in [`../docs/RAYLIB-GOTCHAS.md`](../docs/RAYLIB-GOTCHAS.md).

## Minimal local manifest check

This `deps.edn` contains only the desktop native declaration for the minimal
scalar binding; Android native topology is kept in the separate bootstrap
module. The check validates that the independent manifest parses:

```sh
(
  cd raylib
  nix --extra-experimental-features 'nix-command flakes' develop -c jolt -M:check
)
```

The pinned flake exposes `.#raylib` (the Linux desktop shared library) and
`.#raylib-source` (the same immutable source for future Android builds). The default shell sets
`RAYLIB_SOURCE`, `RAYLIB_VERSION`, and `RAYLIB_LIBRARY_PATH`, includes the
library in its dynamic loader path, and supplies ImageMagick for later image
inspection. `scripts/bootstrap` reports those values, the header version,
pkg-config library facts, and the API-35 ARM64 compiler path in a separate
Raylib section.

The current explicit commands are `scripts/raylib-build-android` for the
Raylib-only baseline, `scripts/raylib-jolt-android-library-build` for the
managed library, `scripts/raylib-jolt-bootstrap-build` for the packaged
bootstrap probe, and `scripts/raylib-topology-build-android` plus
`scripts/raylib-first-frame-build-android` for the topology/frame gates. Do not
use host-installed Android Studio, SDK, NDK, or a floating upstream checkout.

## Android nREPL development

The gallery debug build is dynamically Var-routed and starts Jolt's minimal
nREPL server on Android loopback. Build/install it once, then forward port 7888:

```sh
JOLT_SOURCE=/path/to/pinned-jolt nix develop -c \
  ./scripts/raylib-persistent-loop-build-android debug
nix develop -c ./scripts/raylib-android-nrepl forward
```

An editor can connect to `127.0.0.1:7888`; the script also provides
`describe`, `clone`, `eval`, `load-file`, and `close` commands. `brepl` also
works after forwarding; use fully qualified symbols because it does not select
the project namespace automatically:

```sh
nix develop -c brepl -p 7888 '(poc.raylib.loop/current-runtime-state)'
```

For Linux/macOS desktop examples, use the upstream `rl/run!` entry helper
instead of a bare `(-main)` when launching from nREPL; see the
[Raylib-Jolt REPL guide](https://jlt-commons.github.io/raylib-jlt/guide/repl-driven-development.html).
Redefine pure
functions or drawing function bodies without invoking them in the nREPL
request—the frame owner resolves and calls the replacement later. Short
owner-affine Raylib probes must use the bounded `submit-owner!` queue. See
[RAY-017](../experiments/RAY-017-android-raylib-nrepl/) for the exact workflow,
proof, limits, and release-exclusion gate.

The release image builds from the non-debug entry, contains no debug nREPL
export, remains direct-linked, requests no network permission, and starts no
server. Native/Gradle/manifest/ABI/asset and release changes still require
rebuilds.

## Evidence conventions

New Raylib work stores reduced experiments below `../experiments/RAY-*/` and
runtime artifacts below `../artifacts/raylib/`. Follow the repository experiment
format and distinguish observed behavior from proposed design. A failure in
this alternate host is a valid result but never reduces the demonstrated
Compose/JNI outcome.
