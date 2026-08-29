# First fixed Android frame from Jolt through Raylib

## Gate result

**GO.** The independent API-35 `arm64-v8a` NativeActivity runs Jolt on the
Raylib/native-app-glue thread. Jolt resolves the scalar Raylib bindings through
the selected topology-A `libmain.so` image, initializes the window, clears and
draws text for 120 frames, and closes. The fixed frame is visible in the ADB
framebuffer screenshot.

The Jolt export owns the bounded loop:

```text
raylib_first_frame()
  InitWindow(0, 0, ...)
  BeginDrawing -> ClearBackground -> DrawText -> EndDrawing
  [120 frames at 30 FPS]
  CloseWindow()
```

Only scalar arguments cross the current FFI boundary; colors use the packed
`:uint` representation already validated against raylib-jlt. No shared reducer,
touch state, assets, audio, Raygui, or persistent-loop claim is made here.

## Reproduction

From the repository root, inside the pinned development shell, with
`JOLT_SOURCE` pointing at a clean checkout of Jolt revision
`ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e`:

```sh
JOLT_SOURCE=/path/to/jolt-at-ae5c5a6d \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  bash -ceu 'JOLT_SOURCE="$JOLT_SOURCE" \
    experiments/RAY-008-jolt-first-frame/commands.sh'
```

The command builds the managed ARM64 library and separate static-Raylib
NativeActivity module, inspects the APK/ELF files, starts the existing API-35
x86_64 emulator, installs and launches the APK, captures an ADB screenshot
while the bounded loop is active, and scans the current invocation for crash or
ANR markers.

## Evidence

- `first-frame.png` and the required
  [`artifacts/raylib/screenshots/001-first-frame.png`](../../artifacts/raylib/screenshots/001-first-frame.png)
  are the inspected 1080×2400 ADB framebuffer image. It visibly contains
  Jolt-generated text, the Raylib host/version, API 35, `arm64-v8a`, and a live
  frame count (`50 / 120` in this run). Both copies have checksum files.
- `key-lines.txt` records Berberis AArch64 translation, Raylib Android backend
  initialization, the ANGLE/Mesa OpenGL ES Translator, Jolt export lookup, the
  120-frame result, and same-thread shutdown.
- `libmain-symbols.txt` and `libmain-needed.txt` verify the NativeActivity entry
  and the static Raylib host image. `jolt-symbols.txt` verifies the managed
  library ABI entry points.
- `apk-libraries.txt` and `apk-badging.txt` verify independent ARM64 APK
  packaging and NativeActivity launch configuration.
- `device-primary-abi.txt` is `x86_64` and `device-abi-list.txt` is
  `x86_64,arm64-v8a`; this is translation evidence, not native ARM64 hardware.
- `result.txt` records the successful crash/ANR gate.

This positive first-frame gate unblocks persistent-loop and post-frame
experiments. It does not alter the primary Compose/JNI host or its evidence.
