# No-op Jolt library for the Raylib Android host

## Purpose

Build a distinct managed Jolt/Chez shared library for the alternate Raylib
host without linking Raylib, calling Raylib, changing the primary Compose/JNI
library, or packaging the result into a NativeActivity. This is the cross-
library construction gate before the separate Raylib-thread bootstrap task.

## Pinned boundary

The build uses Jolt `ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e`, Chez
`v10.4.1`, and the NDK r29 API-35 `tarm64le` target pack. The fixture at
[`raylib/android-jolt`](../../raylib/android-jolt) contains only:

```clojure
(defn raylib-host-noop [] 7)
(ffi/export! "raylib_host_noop" raylib-host-noop [] :int)
```

It has no Raylib FFI declaration. The export name is a managed Jolt ABI-table
entry, not an ELF symbol.

## Reproduction

Run from the repository root in the pinned development shell, with
`JOLT_SOURCE` pointing at a clean checkout of the pinned Jolt revision:

```sh
JOLT_SOURCE=/path/to/jolt-at-ae5c5a6d \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  bash -ceu 'JOLT_SOURCE="$JOLT_SOURCE" \
    experiments/RAY-005-jolt-raylib-library/commands.sh'
```

The command performs two clean Android builds (both must succeed and produce
an ELF), creates `native/jolt/android-arm64/arm64-v8a/libjoltraylib.so`, and
then independently builds a host copy from the same fixture and uses
`jolt_lookup("raylib_host_noop")` to call it and shut it down. It also runs the
unchanged primary `scripts/test-android-library` gate and `:app:assembleDebug`.

## Observed evidence

- `android-file.txt` and `android-elf-header.txt`: ELF64 AArch64, Android 35.
- `android-jolt-symbols.txt`: `jolt_library_init`, `jolt_lookup`, and
  `jolt_library_shutdown` are exported.
- `android-dynamic.txt`: only Bionic/platform dependencies are present:
  `libm.so`, `libdl.so`, and `libc.so`; no Raylib library is linked.
- `host-lookup.log`: `raylib_host_noop lookup/call/shutdown: OK`.
- `primary-library-gate.txt` and `primary-apk-build.log`: primary library and
  `:app` build remain valid.
- `SHA256SUMS`: checksum for the generated ARM64 library.

The generated image includes its temporary build path, so clean rebuilds are
success-gated rather than expected to have identical bytes; the final artifact
is still checksum-pinned for this evidence run.

The Android ELF is intentionally not executed or packaged here. Android
loading, same-thread initialization, process lookup, and the eventual Raylib
call are separate evidence-gated tasks.
