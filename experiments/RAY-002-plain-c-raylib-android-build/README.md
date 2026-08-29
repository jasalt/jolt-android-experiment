# Plain-C Raylib Android NativeActivity build

## Problem

Build an Android API-35 ARM64 Raylib NativeActivity before introducing Jolt,
so graphics/linking failures remain attributable to Raylib rather than the
managed runtime or FFI.

## Environment

- Raylib `9f3cadf1e618f125bd9b282c7759f8cb26ce17fc` from the pinned flake;
- Android SDK API 35 and NDK r29 (`29.0.14206865`); and
- Gradle Android plugin already used by this repository.

## Reproduction

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/raylib-build-android
```

The standalone module is `:raylib-android`, with application ID
`net.joltlang.raylibprobe`. Its `NativeActivity` loads `libmain.so`; the pinned
Raylib `android_main` invokes the application-defined `main(int, char **)`.
The C program renders a fixed 120-frame diagnostic, then closes.

## Expected

The command produces an ARM64 API-35 APK containing a single `libmain.so` that
statically includes Raylib and its native-app-glue path, without Jolt/Chez.

## Actual

The build passed on 2026-08-29. APK inspection shows
`lib/arm64-v8a/libmain.so`. `file` identifies it as an AArch64 Android-35 shared
object built by NDK r29; its dynamic dependencies are Android platform libraries
(`log`, `android`, `EGL`, `GLESv2`, `OpenSLES`, `c`, `m`, `dl`) and it exports
`android_main`. No Jolt/Chez library or symbol is packaged.

The first CMake attempt exposed that Raylib's `src/CMakeLists.txt` assumes the
repository-root `cmake/` module path. The host CMakeLists now appends that
pinned path before `add_subdirectory`; no Raylib source is copied or modified.

## Boundary

This is an assembly/ABI result only. It does not prove installation, EGL
presentation, lifecycle, touch, or ARM64 translation; those remain blocked on
the next emulator-validation task.
