# Topology A: Jolt FFI to static Raylib in `libmain.so`

## Question

Can the separate ARM64 Jolt library resolve one Raylib symbol from the Raylib
NativeActivity image without packaging or linking a second Raylib copy?

## Selected topology

**Topology A passes.** The `:raylib-topology-android` module statically links
pinned Raylib into `libmain.so` and packages the separately built
`libjoltraylib-topology.so`. Its Jolt fixture declares the current Android
process image as an optional Linux native candidate:

```clojure
:jolt/native [{:name "Raylib main image"
               :linux ["libmain.so"]
               :optional true}]
```

The candidate is optional so the cross build does not try to load a host
`libmain.so`; at Android runtime, the scoped loader opens the already-loaded
image. `--export-dynamic` makes Raylib's `GetScreenWidth` visible in the
`libmain.so` dynamic symbol table. Jolt's `raylib_process_screen_width` export
then calls `GetScreenWidth` through `jolt.ffi`. It returns the expected
zero-initialized width before any window/EGL initialization, so this probe does
not draw a frame or start a persistent loop.

No Raylib shared library is packaged, and no Raylib archive is linked into the
Jolt library. The APK therefore has exactly one Raylib instance.

## Reproduction

Run from the repository root in the pinned development shell, with
`JOLT_SOURCE` pointing at a clean checkout of Jolt revision
`ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e`:

```sh
JOLT_SOURCE=/path/to/jolt-at-ae5c5a6d \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  bash -ceu 'JOLT_SOURCE="$JOLT_SOURCE" \
    experiments/RAY-007-jolt-raylib-topology/commands.sh'
```

The command assembles and inspects the APK, verifies `GetScreenWidth`,
`android_main`, and `ANativeActivity_onCreate` in `libmain.so`, verifies the
Jolt ABI entry points, and runs the probe twice on the API-35 x86_64 emulator.

## Evidence

- `libmain-raylib-symbols.txt`: the exported Raylib and NativeActivity symbols.
- `libmain-needed.txt`: `libmain.so` depends only on Android platform libraries.
- `apk-libraries.txt`: only `arm64-v8a/libmain.so` and
  `arm64-v8a/libjoltraylib-topology.so` are packaged.
- `first-logcat.txt` and `relaunch-logcat.txt`: Berberis translated ARM64,
  successful Jolt initialization, `jolt_lookup` resolution, the Raylib result
  `0`, same-thread shutdown, and topology completion.
- `process-only-reduction.txt`: the pure `:process true` attempt and why the
  explicit optional `libmain.so` scoped declaration was required.

This is translated ARM64 emulator evidence, not native ARM64 hardware
behavior. The selected topology is sufficient for the next first-frame task;
no fallback topology B or C is activated.
