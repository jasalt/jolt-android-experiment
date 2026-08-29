# Persistent Jolt-owned Raylib loop

## Result

The post-first-frame R4 loop ran for more than 15 minutes on the API-35
x86_64 emulator translating ARM64, remained on the Raylib/native-app-glue
thread, survived one lock/background and unlock/resume cycle, closed normally,
and relaunched successfully.

The C NativeActivity enters Jolt once. The Jolt export then owns
`InitWindow`, `WindowShouldClose`, frame timing, drawing, `EndDrawing`, and
`CloseWindow` for a bounded 905-second run. No C callback or second thread
re-enters Jolt. The screen itself renders the current frame, elapsed time,
frame-time microseconds, target FPS, and API/ABI diagnostic text.

## Reproduction

From the repository root, inside the pinned development shell, with
`JOLT_SOURCE` pointing at a clean checkout of Jolt revision
`ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e`:

```sh
JOLT_SOURCE=/path/to/jolt-at-ae5c5a6d \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  bash -ceu 'JOLT_SOURCE="$JOLT_SOURCE" \
    experiments/RAY-009-jolt-persistent-loop/commands.sh'
```

The command builds and inspects the independent APK, captures ADB screenshots,
`dumpsys meminfo`, and `dumpsys gfxinfo` at 30 seconds, after background/resume,
at 5 minutes, and at 15 minutes, then waits for orderly close and performs a
fresh relaunch. The run takes approximately 16 minutes.

## Evidence

- `30s.png`, `post-resume.png`, `5m.png`, and `15m.png` are visually inspected
  ADB framebuffer images. The final image shows frame `27003`, elapsed
  `902513 ms`, frame time `33335 us`, and target FPS `30`.
- `frame-time-summary.txt` records the rendered diagnostic values; each
  milestone has matching `*-identify.txt` and `*-framestats.txt` files.
- `*-meminfo.txt` files preserve native/TOTAL memory snapshots. Native heap is
  approximately 8.2 MB at every milestone in this run.
- `first-logcat.txt` records the full 905-second run, including
  `APP_CMD_TERM_WINDOW`/`APP_CMD_INIT_WINDOW` around lock/resume, Raylib GLES
  initialization, and final `raylib_persistent_loop result=27078` followed by
  same-thread shutdown.
- `relaunch-logcat.txt` records a fresh translated process reaching the loop;
  `relaunch-pid.txt` proves it remained alive during the relaunch capture.
- `device-primary-abi.txt` is `x86_64` and `device-abi-list.txt` includes
  `arm64-v8a`; this remains translated-emulator evidence, not native ARM64
  hardware evidence.

A short-lived diagnostic attempt to call Raylib's variadic `TraceLog` from Jolt
was removed after reduction because it terminated the translated process before
returning. The accepted loop uses stable on-screen diagnostics and ADB/system
snapshots instead; no TraceLog or broad API claim is made.

No shared reducer, assets, audio, Raygui, nREPL, or performance-batching work
is included.
