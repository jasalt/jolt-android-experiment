# Gallery scene and normalized-input contracts

## Result

A pure, AOT-friendly contract now lets one persistent Jolt/Raylib loop manage a
static scene registry without dynamic namespace discovery or duplicate
window/runtime ownership.

Each descriptor has exactly the required `:id`, `:title`, `:init`, `:update`,
`:draw`, and `:dispose` lifecycle surface. Top-level state distinguishes gallery
and active-scene modes. Back disposes an active scene and returns to the gallery;
Back in the gallery requests host-loop closure. Reset orders dispose before a
new init. The host remains responsible for polling and for the one Raylib/Jolt
thread.

The normalized frame snapshot contains live screen/render metrics, pointer
press/down/release, touch count/IDs and explicitly available coordinates,
gesture code, Android Back, and optional Enter/Left/Right/Escape keyboard
fallback. It contains no gamepad or native pointer/Vector2 value.

## Reproduce

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  env JOLT_SOURCE="$JOLT_SOURCE" \
  experiments/RAY-011-gallery-contract/commands.sh
```

`JOLT_SOURCE` must be a clean checkout at the repository's pinned Jolt revision.

## Evidence

- [`pure-tests.txt`](evidence/pure-tests.txt): 7 tests and 33 assertions pass.
  A two-scene fake registry proves deterministic lookup, init/update/draw/
  dispose order, reset, scene Back, gallery Back, live metrics, input edges,
  synthetic multiple IDs, gestures, and keyboard fallback.
- [`aot-build.txt`](evidence/aot-build.txt): the contract is required by the
  persistent loop and compiles into the Android ARM64 Jolt managed shared
  library; APK assembly succeeds.
- [`source-boundary.txt`](evidence/source-boundary.txt): executable checks reject
  window/runtime/gamepad/native-pointer ownership in the pure contracts.
- [`runtime-contract-state.txt`](evidence/runtime-contract-state.txt): the
  translated API-35 process logs contract version 1 plus normalized gesture and
  keyboard state from the built image.
- [`logcat.txt`](evidence/logcat.txt): the current invocation contains no fatal
  signal, Java fatal exception, or gallery ANR marker.

This is a contract and fake-registry proof. The next task supplies the rendered
scene selector and real scene registry; individual gallery examples remain
separate tasks.
