# Shared reducer driven by the Raylib host

## Result

The Raylib scene host now translates metric-derived scene controls into the
existing `poc.reducer` event vocabulary. `+ Counter`, `- Counter`, and `Reset`
produce `:counter/inc`, `:counter/dec`, and `:counter/reset`; Enter and Left
remain desktop keyboard fallbacks. The unchanged reducer owns the model and
returns its existing data effect (`:storage/write`). Raylib renders the derived
counter and logs the model/effects without importing Raylib values into shared
code or executing platform effects itself.

## Reproduce

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  env JOLT_SOURCE="$JOLT_SOURCE" \
  experiments/RAY-014-shared-reducer-raylib/commands.sh
```

`JOLT_SOURCE` must be a clean checkout at the revision required by the Raylib
Android build scripts. The command builds the managed ARM64 library/APK and
uses the API-35 emulator to select a scene, tap the increment/decrement
controls, and retrieve the machine-readable log state.

## Evidence

- [`evidence/pure-tests.txt`](evidence/pure-tests.txt): 12 tests and 53
  assertions pass, including pure event translation and reducer-effect checks.
- [`evidence/aot-build.txt`](evidence/aot-build.txt): ARM64 Jolt library and
  Android APK assembly succeed.
- [`evidence/reducer.log`](evidence/reducer.log): a touch at the metric-derived
  `+ Counter` control records `:counter/inc`, counter 1, and the unchanged
  reducer's `:storage/write` effect; a `- Counter` touch records counter 1 and
  `:counter/dec` after two increments.
- [`evidence/reducer-counter.png`](evidence/reducer-counter.png): presented
  Android framebuffer visibly shows the shared counter and three scene-local
  controls.
- [`evidence/bootstrap.log`](evidence/bootstrap.log): init, gallery execution,
  and shutdown use the same native thread and complete without a crash/ANR
  marker.

The adapter intentionally does not claim that storage was persisted: the
returned effect remains data for the later host-effect task.
