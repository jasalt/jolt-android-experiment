# Raylib alternate-host gotchas

This document records **observed** constraints and explicitly labels future
work. It applies only to the independent Raylib NativeActivity research track
in the [Raylib research plan](../docs/RAYLIB-PLAN.md). Primary Compose/JNI constraints remain
in [`GOTCHAS.md`](GOTCHAS.md).

## Observed during baseline pinning

### The pinned Android entry point is `main`, not `android_run`

**Observed source:** raylib
[`9f3cadf1e618f125bd9b282c7759f8cb26ce17fc`](../raylib/pins.edn),
`src/platforms/rcore_android.c`, lines 317–331.

The Android backend declares `extern int main(int argc, char *argv[])`.
`android_main(struct android_app *app)` stores the native application pointer,
then calls `main(1, (char *[]) { "raylib", NULL })`, and requests activity
finish after it returns. The older conceptual `android_run` diagram in the plan
is illustrative rather than an API provided by this pin.

**Consequence:** the future plain-C probe and Jolt bootstrap must start with the
pinned `main(int, char **)` contract. Re-pin and repeat this audit before
relying on a different upstream entry contract.

### Android asset opening is a final-link concern

**Observed source:** the same `rcore_android.c`, comments at lines 300–303.

Raylib's Android `fopen` asset wrapping requires `-Wl,--wrap=fopen` on the
command that links the final `.so`; adding it only while constructing a static
archive is insufficient.

**Consequence:** the future asset task must inspect its final NativeActivity
link command and prove packaged-asset behavior on Android. This is not yet a
successful asset experiment.

### Upstream Jolt bindings are desktop-declared

**Observed source:** pinned raylib-jlt
[`cf16df3d323726dc8b100225eeb1156607ae4a55`](../raylib/pins.edn), its
dependency manifest, and README.

Its `:jolt/native` declaration lists Darwin and Linux `libraylib` paths, and
its README requires Jolt 0.7.23+ with Raylib 6.0+. It is consequently a useful
binding/source baseline, not Android library-loading proof.

**Consequence:** establish the Linux baseline first. Isolate an Android native
library declaration only after plain-C Raylib Android evidence and without
forking the complete binding suite. The refreshed pin includes
`input-gestures`; its presence is source/binding evidence only until the
Android gallery validates it through the existing process-symbol topology.

### The pinned Camera2D workaround is not valid on x86_64

**Observed experiment:** [RAY-001](../experiments/RAY-001-raylib-jlt-linux-baseline).

The pinned `camera2d` example opened, rendered its screen-space HUD, and wrote a
PNG on x86_64 Linux, but its camera-transformed world geometry was absent. Its
pinned source binds `BeginMode2D(Camera2D)` as `[:pointer]` and passes a
manually allocated 24-byte aggregate. The upstream guide explicitly limits that
pointer substitution to AArch64: x86_64 System V passes the by-value aggregate
on the stack instead.

**Consequence:** do not claim the pointer workaround is portable and do not use
it as the x86_64 desktop shared-app path. Preserve the scalar/packed-Color
first-frame subset and verify direct current Jolt aggregate support in the
separate Android AArch64 ABI experiment.

### The managed Xvfb uses host GLVND/Mesa

**Observed experiment:** [RAY-001](../experiments/RAY-001-raylib-jlt-linux-baseline).

Nix Raylib/GLVND against the existing managed `Xvfb :99` returned no usable
GLX framebuffer configuration. Prepending `/lib64` selected the host
GLVND/Mesa implementation serving that X display; the pinned Nix Raylib and
Jolt binaries then created a real OpenGL 4.6 llvmpipe window and screenshots.

**Consequence:** the desktop headless binding check remains fully Nix-pinned,
but this current visual-host route has an explicit host-Mesa compatibility
boundary. It is not Android evidence and must not be described as a fully
self-contained Nix display server result.

### Source-mode builds can omit CLI-preloaded namespace code

**Observed experiment:** [RAY-012](../experiments/RAY-012-android-aggregate-abi),
fixed by [Jolt PR #787](https://github.com/jolt-lang/jolt/pull/787), “Fix
source-mode builds omitting CLI-preloaded namespace code.”

The source-mode CLI loads `jolt.main` before invoking the build driver. That
also loads lazy namespaces such as `jolt.ffi`. The build driver previously
snapshotted the process's `loaded-ns` table after this happened, then treated
those namespaces as inherited by the distinct generated app/library image.
Their vars were therefore interned but unbound; `jolt run` masked the problem
because it recompiles source at runtime.

The reduced Android failure was a clean status-255 exit when calling
`jolt.ffi/layout-size`, before any aggregate ABI call. The fix snapshots the
runtime-image namespace set before CLI loading. With it, the Android matrix
passes for the representative Raylib aggregates, including direct arguments
and returns.

**Consequence:** apply PR #787 (or the pinned experiment patch) when using the
older Jolt revision. Do not diagnose this as an AArch64 calling-convention
failure or replace direct aggregates with the stale pointer workaround.

### Scalar touch works, but all-point coordinates remain ABI-gated

**Observed experiment:** [RAY-010](../experiments/RAY-010-touch-adaptive-diagnostics).

On the translated API-35 emulator, ADB tap/hold/drag reached Raylib as both one
active touch (`GetTouchPointCount`/`GetTouchPointId`) and mouse-compatible
press/down/release edges. `GetTouchX`/`GetTouchY` provided point-zero
coordinates. The Jolt loop visibly rendered the active point and logged
portable state; Back produced an orderly close request and same-thread shutdown.

Raylib exposes all active IDs through scalar calls, but per-index coordinates
require `GetTouchPosition`, whose `Vector2` by-value return belongs to the
separate Android AArch64 aggregate-ABI gate. The host therefore reports that
all-point coordinates are unavailable rather than constructing or leaking a
native value. Standard ADB input evidenced one active pointer, not real
multitouch.

A fresh launch after selecting 1080×2400 portrait, 2400×1080 landscape, or the
720×1280 size override reported matching live screen/render metrics and readable
layout. With the current `configChanges` declaration, rotation can briefly
stretch the old Raylib surface before host recreation; detailed in-process
resize/lifecycle behavior remains a separate experiment.

### Desktop nREPL redefinition needs a Raylib owner-thread boundary

**Observed experiment:** [RAY-015](../experiments/RAY-015-linux-raylib-nrepl),
with complete retained capture in
[`../../raylib-jlt/nrepl-results/`](../../raylib-jlt/nrepl-results/).

On Linux desktop, a normal Jolt nREPL server remained responsive while a
Raylib window rendered from a dynamically called Var. Two nREPL `defn`
replacements visibly changed subsequent frames without rebuilding or restarting
the process. The Raylib loop was on Jolt/Chez thread 48, while nREPL handlers
ran on threads 49–51. A captured startup function value remained stale after a
redefinition; the per-frame Var call observed it.

**Consequence:** use desktop nREPL for pure layout/update/draw iteration, but
never issue Raylib FFI from an nREPL worker. The server does not transfer a
request onto the Raylib context owner. Any future request that must execute
against the live application needs an application-owned bounded queue drained
by the owner at frame boundaries. This evidence does not establish Android
nREPL, CIDER, or hot reload.

### Primary Android facts do not automatically transfer

The API-35 x86_64 emulator's ARM64 translation,
Jolt/Chez Android cross-library construction, and same-thread runtime evidence
already exist for the Compose/JNI host (for example
[EXP-001](../experiments/EXP-001-api35-arm64-on-x86-emulator) and
[EXP-004](../experiments/EXP-004-jolt-android-library-cross-target)). They do
not prove Raylib's NativeActivity, EGL/OpenGL ES, lifecycle, asset, symbol
visibility, or Jolt-to-Raylib FFI behavior.

**Consequence:** retain separate `RAY-*` experiments; do not overstate reused
infrastructure as Raylib rendering evidence.

## Future categories

Add evidence-backed entries as the work reaches each boundary:

- NativeActivity and `native_app_glue` lifecycle;
- EGL/OpenGL ES under ARM64 translation;
- static versus dynamic Raylib symbol topology;
- Android AArch64 Jolt FFI aggregate ABI;
- audio and asset lifecycle;
- Jolt GC/frame-time behavior;
- debug-only Android live evaluation through a bounded owner-thread queue;
- text input, accessibility, and Raygui mobile ergonomics.
