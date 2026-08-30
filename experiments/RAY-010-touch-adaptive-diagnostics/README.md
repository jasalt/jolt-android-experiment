# Touch-first adaptive Raylib diagnostics

## Result

The API-35 x86_64 emulator executed the ARM64 Jolt/Raylib NativeActivity and
proved scalar touch polling, normalized input edges, adaptive screen metrics,
and deterministic Back closure in the existing one-thread loop.

Jolt polls Raylib's scalar APIs for screen/render dimensions, active touch
count and IDs, point-zero coordinates, and touch-as-mouse press/down/release.
It keeps native values out of portable state, renders a visible touch marker,
increments local state on presses, counts hold/drag frames, and emits bounded
machine-readable state through Android logcat.

## Reproduce

From the pinned Nix shell, with `JOLT_SOURCE` at
`ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e`:

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  env JOLT_SOURCE="$JOLT_SOURCE" \
  experiments/RAY-010-touch-adaptive-diagnostics/commands.sh
```

The command runs the pure transition tests, cross-builds the AOT managed
library, assembles/installs the APK, injects tap/hold/drag/Back, captures state
and screenshots, exercises 1080×2400 portrait, 2400×1080 landscape, and a
720×1280 size override, and scans current-run logs for crashes and ANRs.

## Evidence

- [`pure-tests.txt`](evidence/pure-tests.txt): 3 tests and 13 assertions cover
  live metrics, two synthetic touch IDs, press/down/release, movement, and Back.
- [`portrait-logcat.txt`](evidence/portrait-logcat.txt): real ADB input produces
  count `1`, ID `0`, non-zero point-zero coordinates, every pointer edge,
  incremented tap state, hold frames, and drag samples.
- [`portrait-hold.png`](evidence/portrait-hold.png): inspected 1080×2400 frame
  visibly shows the active touch and its marker.
- [`landscape.png`](evidence/landscape.png): inspected 2400×1080 frame reports
  live landscape screen/render dimensions and a readable adaptive layout.
- [`small-portrait.png`](evidence/small-portrait.png): inspected 720×1280 frame
  reports the overridden dimensions without a fixed 800×450 assumption.
- [`back-logcat.txt`](evidence/back-logcat.txt): Back records
  `:close-requested? true`, returns from the Jolt loop, and performs same-thread
  shutdown with no current-run fatal signal, exception, or ANR.
- [`SHA256SUMS`](evidence/SHA256SUMS) checks all preserved PNGs.

## Boundaries

Raylib's scalar API provides every active touch ID but only point-zero scalar
coordinates. Per-index `GetTouchPosition` returns `Vector2` by value and remains
reserved for the separate Android AArch64 aggregate-ABI experiment; this host
explicitly reports `:all-coordinates-available? false` rather than guessing.
ADB's standard `input` command evidenced one active pointer. The pure contract
proves multiple IDs are retained, but this experiment does not claim a real
multi-pointer injection or all-point coordinates.

Changing orientation while the activity declares `configChanges` can briefly
present the old Raylib surface stretched by Android. This task therefore
relaunches after selecting landscape to prove fresh live-metric layout. Detailed
in-process resize/recreation behavior remains part of the dedicated
NativeActivity lifecycle task.
