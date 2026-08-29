# Minimal reusable Raylib binding subset

## Problem

Isolate the scalar binding surface required for the first Android frame without
forking the pinned raylib-jlt binding suite or deciding the Android link
topology prematurely.

## Selected surface

[`../../raylib/src/poc/raylib/minimal.clj`](../../raylib/src/poc/raylib/minimal.clj)
contains exactly these Raylib symbols:

```text
InitWindow            WindowShouldClose
BeginDrawing          ClearBackground
DrawText              EndDrawing
CloseWindow            GetScreenWidth
GetScreenHeight       SetTargetFPS (desktop smoke helper)
```

The binding source has **no** `:jolt/native` declaration. The desktop project
manifest supplies a dynamic `libraylib.so.6` only for run/built-image validation.
Android process lookup, a packaged `libraylib.so`, or a static archive remains a
later topology decision. This separation is the only local adaptation; no
upstream binding namespace or example source is copied.

`Color` crosses these calls as packed little-endian `:uint`, matching the
pinned raylib-jlt representation. No aggregate argument/return is used.

## Validation

```sh
cd raylib
nix --extra-experimental-features 'nix-command flakes' develop .. -c \
  jolt -M:minimal-smoke
nix --extra-experimental-features 'nix-command flakes' develop .. -c \
  jolt build -m poc.raylib.minimal -o /tmp/raylib-minimal-image
```

The first command opened a real 640×360 GLFW/X11 Raylib window. The second
produced a release Jolt image; when invoked with the Nix Raylib library path it
opened the same real window and shut down cleanly. The image intentionally
retains a dynamic-native dependency; running it with no `libraylib.so.6` loader
path fails explicitly, which confirms that Android must supply its topology
rather than accidentally inheriting desktop loading.

[`evidence/native-symbols.txt`](evidence/native-symbols.txt) verifies every
selected first-frame function in the pinned native library.
[`evidence/desktop-built-image`](evidence/desktop-built-image) and
[`evidence/SHA256SUMS`](evidence/SHA256SUMS) preserve the built-image artifact.

## Reuse result

The pinned upstream `raylib.clj` is 1,122 lines and includes every example's
rendering/input/texture/shader/camera convenience surface. The local binding is
62 lines: it reuses the same exact scalar C ABI declarations and packed-Color
approach for the 9 first-frame functions, while intentionally excluding the
remaining broad API and the upstream's AArch64-only camera pointer workaround.
The intended upstream-suitable change is an Android-native-declaration seam,
not a fork of these bindings.

## Boundary

This proves desktop run and built-image behavior for the selected API only. It
does not claim that an Android Jolt library can yet resolve a Raylib symbol;
that is the separate topology experiment after no-op Jolt bootstrap.
