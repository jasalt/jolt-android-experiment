# Plain Raylib ARM64 execution through API-35 emulator translation

## Problem

Determine whether the independent ARM64 plain-C Raylib NativeActivity can load,
create an EGL/OpenGL ES rendering path, present a real frame, and survive a
bounded focus/resume/relaunch sequence on the existing API-35 x86_64 emulator.

## Environment

- API-35 Google APIs x86_64 AVD `jolt_api_35_x86_64`, KVM plus ANGLE;
- emulator primary ABI `x86_64`, ABI list `x86_64,arm64-v8a`;
- plain-C `net.joltlang.raylibprobe`, whose APK contains only ARM64
  `libmain.so`; and
- Raylib `9f3cadf1e618f125bd9b282c7759f8cb26ce17fc` / NDK r29.

## Reproduction

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./experiments/RAY-003-plain-raylib-arm64-translation/commands.sh
```

The command rebuilds the independent APK, uses the repository AVD helper,
installs/launches it, injects a tap, locks/unlocks once, captures three ADB
framebuffer PNGs, force-stops/relaunches, and collects current-run logcat.

## Actual

The first run failed before Raylib entered because static-link extraction had
discarded `ANativeActivity_onCreate`; Android's NativeActivity loader reported
that missing symbol. The reduced fix is `-u ANativeActivity_onCreate` on the
final `libmain.so` link, the same entry-retention requirement documented by
Raylib's Android CMake configuration. It does not add Jolt or another renderer.

After that fix the repeat run passed. Logcat shows Android's AArch64 translator
(`Berberis`), Raylib's Android backend, `PLATFORM: ANDROID: Initialized
successfully`, EGL loading its GLES emulation libraries, and a GLES 3.1 ANGLE
context. Raylib reports an Android Emulator OpenGL ES Translator renderer over
ANGLE/Mesa llvmpipe. It initializes its default texture/shaders/font/render
batch and presents the fixed diagnostic frames.

The inspected ADB screenshots show the plain-C title and frame counter at frame
65, after resume at frame 218, and after fresh process relaunch at frame 69.
No current-run Java/native fatal crash or ANR was found after the successful
run. This establishes **plain Raylib Android R1 under emulator translation**;
it does not prove native ARM64 hardware, Jolt, CFFI, assets, or audio.

## Evidence

- [`evidence/primary-abi.txt`](evidence/primary-abi.txt) and
  [`evidence/abi-list.txt`](evidence/abi-list.txt) record the translation ABI
  facts;
- [`evidence/logcat.txt`](evidence/logcat.txt) records the backend, EGL/GLES,
  renderer, and lifecycle/relaunch observations;
- [`evidence/first-frame.png`](evidence/first-frame.png),
  [`evidence/post-resume.png`](evidence/post-resume.png), and
  [`evidence/relaunch.png`](evidence/relaunch.png) are authoritative ADB
  framebuffer captures; and
- [`evidence/SHA256SUMS`](evidence/SHA256SUMS) protects those images.

## Boundary

A tap was injected without an application-level observable effect because this
minimal C program intentionally has no input state; the successful continued
frame count after the lock/unlock cycle proves the Raylib loop remained active.
Touch semantics, assets, and audio are separate tasks. This result must not be
interpreted as evidence for the primary Compose/JNI host or for Jolt ownership.
