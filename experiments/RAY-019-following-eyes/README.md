# Touch-first Following Eyes gallery scene

## Result

**Passed on the API-35 x86_64 emulator executing the ARM64 gallery through
Android translation.** The gallery now replaces its Following Eyes placeholder
with a touch-first adaptation of the pinned `raylib-jlt` / Raylib
`shapes_following_eyes` example. It reuses the pupil-clamp behavior, not the
upstream window or mouse-polling ownership.

[`following_eyes.cljc`](../../raylib/src/poc/raylib/following_eyes.cljc) is
pure: normalized `:press`/`:down` pointer snapshots select a target; release
retains that last position deterministically. The owner loop derives live
portrait/landscape eye geometry, then draws the eyes and pupils using scalar
Raylib calls. No native `Vector2` reaches scene state.

`jolt -M:test` passed **22 tests and 114 assertions**. The pure tests check
pupil distance against the eye-bound radius in portrait and landscape, far
corner targets, press/down/release retention, and adaptive layout.

## Android evidence

The static gallery scene descriptor and renderer required one debug AOT rebuild
and install. Afterwards the forwarded RAY-017 nREPL inspected the running
process without restarting it.

- [`android-corner.png`](evidence/android-corner.png) shows the rendered scene
  after an ADB corner tap.
- [`corner-state.jsonl`](evidence/corner-state.jsonl) records
  `:selected-scene :following-eyes`, owner thread `0`, and retained target
  `[50 2200]`.
- [`resumed-state.jsonl`](evidence/resumed-state.jsonl) shows the same selected
  scene/target after Android Home/background and NativeActivity resume.
- [`screenshots.sha256`](evidence/screenshots.sha256) hashes the captures.

The Linux visual smoke remains bounded by the documented Xvfb GLX limitation in
RAY-018; this experiment does not claim a Linux visual run. The scene has a
mouse-compatible host input path through normalized pointer fallback, but that
fallback has not been visually validated on Linux here.
