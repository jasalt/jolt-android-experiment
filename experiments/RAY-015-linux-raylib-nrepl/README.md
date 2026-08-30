# Linux Raylib nREPL live-layout workflow

## Result

**Observed on Linux desktop:** Jolt nREPL can redefine a pure, Var-backed
Raylib layout function while a running Raylib window renders the replacement on
the next frame. This is the primary rapid-iteration workflow for the Raylib
host:

```text
edit pure layout/update/draw function
→ evaluate it through loopback Jolt nREPL
→ the Linux Raylib frame loop calls the Var on its next frame
→ inspect the live window
→ turn the observation into a portable test
→ use the normal Android AOT build/run path for Android integration
```

The captured upstream-style validation is retained at
[`../../../raylib-jlt/nrepl-results/`](../../../raylib-jlt/nrepl-results/).
The upstream Raylib-Jolt guide
[REPL-driven development: why `(-main)` kills your editor connection](https://jlt-commons.github.io/raylib-jlt/guide/repl-driven-development.html)
adds an important launch rule: use `rl/run!` for an editor-launched desktop
window, rather than calling `(-main)` directly. The guide's
[`headless-smoke-testing`](https://jlt-commons.github.io/raylib-jlt/guide/headless-smoke-testing.html)
page is also useful for timer-based automated exits.

The complete captured validation is retained at
[`REPORT.md`](../../../raylib-jlt/nrepl-results/REPORT.md) and
[`nrepl-transcript.txt`](../../../raylib-jlt/nrepl-results/nrepl-transcript.txt),
with three hashed screenshots. It used Fedora 44 x86_64, Jolt 0.7.27, Raylib
6.0, private Xvfb `:99`, and a source-built desktop Raylib shared library.
This is separate from the pinned `raylib-jlt` baseline in RAY-001, so its
versions must not be substituted for the Android gallery pin.

The one Jolt process (PID 15687) remained alive across v1, v2, and v3 layout
captures. Its Raylib loop ran on Jolt/Chez thread 48; nREPL requests ran on
threads 49–51. The 60-FPS timing probe rendered 301 frames in five seconds
(minimum 0.0166669 s, maximum 0.0194466 s, mean 0.0166767 s). The captured
screenshots visibly change from the initial text layout to a blue rectangle and
then an orange circle, without restarting or rebuilding.

## Upstream launch guidance and applicability

The new upstream guide reports that macOS AppKit terminates the process when
`InitWindow` is entered from an nREPL worker. Its `rl/run!` helper uses
`jolt.host/call-on-main-thread-async`: the eval returns immediately while the
primordial Jolt thread's `park-until-interrupt` pump starts the window. The
blocking `call-on-main-thread` variant is appropriate for scripts that need to
wait for window closure, but is a poor editor operation for a long-running
loop. The guide also records a live 0 → 5 → 15 counter change, independently
confirming the dynamic-Var requirement already demonstrated by this experiment.

This project should use that rule for future desktop examples: launch a
Raylib-owned window through the host's owner-thread scheduler, and keep the
nREPL free while it runs. It does not mean that `rl/run!` can be copied into
Android. The Android NativeActivity already enters the exported loop on its
Raylib owner thread; Android's RAY-017 debug bootstrap starts the nREPL server
from that owner and only evaluates definitions on nREPL workers. Android
therefore needs the explicit debug owner queue for owner-affine probes rather
than a desktop `park-until-interrupt` assumption.

The upstream guide's macOS crash report is especially useful when diagnosing a
disappearing REPL: inspect native crash logs and Raylib's last banner, rather
than expecting a Clojure exception. The Linux and Android evidence here did not
crash because their launch paths already establish an owner, but the same
thread rule applies.

## Required safety boundary

The result is **not** permission for an nREPL worker to invoke Raylib FFI.
Normal Jolt nREPL accepts connections and evaluates requests on worker threads;
it does not serialize those calls onto the Raylib context owner. All Raylib
window, drawing, input, resource, and shutdown calls must remain on the one
Raylib/Jolt owner thread.

A live function must be called through its Var on every frame:

```clojure
(defn draw-frame! [frame]
  (live-layout! frame)) ; observes a later defn of live-layout!
```

Do not capture its initial function value:

```clojure
(def draw-at-start live-layout!) ; stale after a redefinition
```

The captured experiment proved this distinction: a dynamic call returned
`:version-2` after redefinition while the captured value still returned
`:version-1`.

Pure `def`/`defn` updates issued by nREPL are the supported desktop use here.
A future request that must execute Jolt/Raylib work belongs in an
application-owned, bounded FIFO processed by the frame owner at a frame
boundary. Process a bounded number of requests per frame; do not drain an
unbounded backlog or execute request forms on the network worker.

## Reproduce the desktop result

The supplied evidence is a completed result, not a project build target. To
repeat it without altering the pinned Android gallery, work in a disposable
`raylib-jlt` checkout and retain exact versions, logs, screenshots, and thread
IDs:

```sh
cd ../raylib-jlt
bb check
bb nrepl 17888
```

Use a loopback nREPL client to load the disposable forms in
`nrepl-results/raylib-live-defs.clj` and
`nrepl-results/raylib-live-run.clj`. Start the loop using the tested
main-thread scheduling form in `raylib-queued-run.clj` when applicable. While
the loop is live, redefine only a pure Var-backed layout/draw function, capture
before/after frames, and verify that the Raylib owner and nREPL workers have
distinct thread IDs. The external report contains the exact protocol transcript
and environment-specific Xvfb/loader setup.

## Android applicability

The desktop result was followed by the separate Android validation in
[RAY-017](../RAY-017-android-raylib-nrepl/). That experiment applies the same
principles with Android-specific plumbing: a debug-only `--dev` Jolt image,
loopback nREPL reached through ADB forwarding, and a bounded owner queue for
short Raylib-affine operations. It visibly applied `eval` and `load-file`
without APK or process restart on the translated ARM64 emulator.

The upstream `rl/run!` helper should not be copied into Android: the Android
NativeActivity already enters the exported loop on its Raylib owner thread.
Native FFI declarations, ABI topology, lifecycle/bootstrap code, packaged
assets, and AOT/release changes still require rebuilds. RAY-017 does not prove
native ARM64 hardware, optional CIDER middleware, or production remote eval.
