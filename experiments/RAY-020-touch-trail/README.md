# Bounded Touch Trail gallery scene

**Passed on the API-35 x86_64 emulator executing the ARM64 gallery through Android translation.** The Touch Trail card now uses a pure primary-pointer history capped at 64 samples. A press resets it deterministically; only `:down` samples append; idle/release cannot grow it. The owner thread renders the retained points with an ordered radius/color fade.

`jolt -M:test` passed 23 tests and 118 assertions. The Android swipe capture
[`android-swipe.png`](evidence/android-swipe.png) visibly shows the adaptive
trail. [`swipe-state.jsonl`](evidence/swipe-state.jsonl) records the live
`:touch-trail` selection, owner thread `0`, and 31 ordered samples from
`[100 900]` to `[875 1772]`; [`screenshots.sha256`](evidence/screenshots.sha256)
hashes the capture.

Static registration and rendering required one debug AOT build/install. State
inspection used the running RAY-017 nREPL and did not invoke Raylib from its
worker. Linux visual smoke remains limited by the documented Xvfb GLX failure.
