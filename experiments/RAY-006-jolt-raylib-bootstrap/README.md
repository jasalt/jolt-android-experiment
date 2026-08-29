# Jolt bootstrap on the Raylib NativeActivity thread

## Purpose

Validate the R2 boundary: the pinned Raylib NativeActivity entry starts the
application thread, the C bootstrap initializes a separately packaged Jolt
library on that thread, resolves and calls the no-op export, shuts Jolt down on
the same thread, and returns. This experiment does not draw, call Raylib from
Jolt, add a HandlerThread, or reuse the primary Compose/JNI host.

## Topology

The independent `:raylib-jolt-android` module contains static Raylib in
`libmain.so`. The no-op managed library is separately packaged as
`libjoltraylib.so` and loaded by `dlopen`. The NativeActivity manifest still
uses Raylib's pinned `android_main -> main(int, char **)` contract. The
bootstrap calls:

```text
dlopen(libjoltraylib.so)
jolt_library_init(argc, argv)
jolt_lookup("raylib_host_noop")()
jolt_library_shutdown()
```

Each step logs the Linux/Android thread ID and the owner ID. No Raylib symbol
is called by the bootstrap or the Jolt fixture.

## Reproduction

From the repository root, inside the pinned development shell, with
`JOLT_SOURCE` pointing at a clean checkout of Jolt revision
`ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e`:

```sh
JOLT_SOURCE=/path/to/jolt-at-ae5c5a6d \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  bash -ceu 'JOLT_SOURCE="$JOLT_SOURCE" \
    experiments/RAY-006-jolt-raylib-bootstrap/commands.sh'
```

The command builds the ARM64 Jolt library, stages only that library into the
independent APK, assembles it with pinned Raylib/NDK tooling, inspects the APK,
then installs and launches it twice on the existing API-35 x86_64 emulator.

## Evidence

- `apk-libraries.txt`, `apk-badging.txt`, and the two `*-file.txt` files prove
  the NativeActivity package contains both ARM64 `libmain.so` and
  `libjoltraylib.so` at API 35.
- `device-primary-abi.txt` is `x86_64`; `device-abi-list.txt` is
  `x86_64,arm64-v8a`; `Berberis` in each key log records translated AArch64
  execution.
- `first-logcat.txt` and `relaunch-logcat.txt` show successful `dlopen`, Jolt
  init, `jolt_lookup` resolution, result `7`, shutdown, and completion. Each
  operation uses one owner thread ID per run (`15522` and `15561` in this
  evidence run).
- `first-launch.txt` and `relaunch-launch.txt` show both NativeActivity launch
  commands completed successfully.
- `result.txt` records that the two runs passed the current-run fatal crash and
  ANR checks.

This is translated ARM64 emulator evidence, not native ARM64 hardware evidence.
The bootstrap gate is now positive; the next topology experiment may test one
Raylib process symbol and one Raylib function. Persistent loops, rendering,
input state, and lifecycle features remain downstream of that gate.
