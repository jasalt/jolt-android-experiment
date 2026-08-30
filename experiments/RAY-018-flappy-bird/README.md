# Touch-first delta-time Flappy Bird gallery scene

## Result

**Passed on the API-35 x86_64 emulator executing the ARM64 Raylib gallery
through Android translation.** This experiment replaces the gallery's Flappy
Bird placeholder with a deterministic, metric-scaled scene adapted from the
pinned `raylib-jlt` example. It deliberately reuses gameplay intent only: it
does not call the upstream `-main`, create another window, or transfer Raylib
ownership out of the existing Jolt-owned loop.

The pure simulation in
[`raylib/src/poc/raylib/flappy_bird.cljc`](../../raylib/src/poc/raylib/flappy_bird.cljc)
contains no polling, drawing, or native values. The owner loop supplies a
normalized input snapshot and `GetFrameTime`; it clamps a lifecycle-sized delta
to 50 ms before advancing. A touch/Enter **press edge** flaps once. A press
edge after collision restarts the seeded fixture; a held touch does not create
additional flap edges.

`jolt -M:test` ran **19 tests and 96 assertions** with no failures/errors. The
fixtures verify seeded games, metric-scaled geometry, 30/60/variable partition
equivalence for an uncollided 100 ms interval, press/hold/restart behavior,
pipe scoring/collision, and the delta clamp.

## Android evidence

The debug image was rebuilt only after the pure contract and tests were
complete, because static scene registration and owner-thread drawing are AOT
boundaries. It was then installed once and driven with `adb input tap`; the
existing RAY-017 nREPL was used for state inspection without restarting the
Activity.

- [`android-game-over.png`](evidence/android-game-over.png) shows adaptive
  portrait rendering with the initial visible pipe and deterministic game-over
  state.
- [`android-touch-play.png`](evidence/android-touch-play.png) captures the
  touch-driven scene after a short sequence of tap edges.
- [`touch-play-state.jsonl`](evidence/touch-play-state.jsonl) records the live
  gallery selection, owner thread `0`, and Flappy state after the tap sequence.
- [`resumed-state.jsonl`](evidence/resumed-state.jsonl) records that the scene
  stayed selected after Android Home/background and NativeActivity resume. Its
  capped elapsed value demonstrates that no giant delta-time jump was applied.
- [`screenshots.sha256`](evidence/screenshots.sha256) hashes every retained
  screenshot. [`runtime.log`](evidence/runtime.log) was scanned for current-run
  fatal-signal, fatal-exception, and ANR markers; none were found.

The interaction evidence confirms touch gameplay entry and deterministic
collision/restart rendering on the translated emulator. It does **not** prove a
native ARM64 device, a physical keyboard/Linux smoke, or arbitrary safe nREPL
calls into Raylib; those remain bounded by the gallery and RAY-017 contracts.

## Development workflow

Iterate the pure state machine with tests or the forwarded debug nREPL before a
build. A running Android image can inspect already-built pure state using:

```sh
nix develop -c ./scripts/raylib-android-nrepl eval \
  '(current-runtime-state)' poc.raylib.loop
```

Do not evaluate `DrawCircle`, input polling, window lifecycle, or renderer
functions from an nREPL worker. Definitions only become visible without a
rebuild when a running image calls their Vars dynamically; adding the scene to
the static registry and its native draw declarations required the documented
AOT rebuild.
