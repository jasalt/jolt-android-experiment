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
[`../../../raylib-jlt/nrepl-results/`](../../../raylib-jlt/nrepl-results/):
[`REPORT.md`](../../../raylib-jlt/nrepl-results/REPORT.md),
[`nrepl-transcript.txt`](../../../raylib-jlt/nrepl-results/nrepl-transcript.txt),
and three hashed screenshots. It used Fedora 44 x86_64, Jolt 0.7.27, Raylib
6.0, private Xvfb `:99`, and a source-built desktop Raylib shared library.
This is separate from the pinned `raylib-jlt` baseline in RAY-001, so its
versions must not be substituted for the Android gallery pin.

The one Jolt process (PID 15687) remained alive across v1, v2, and v3 layout
captures. Its Raylib loop ran on Jolt/Chez thread 48; nREPL requests ran on
threads 49–51. The 60-FPS timing probe rendered 301 frames in five seconds
(minimum 0.0166669 s, maximum 0.0194466 s, mean 0.0166767 s). The captured
screenshots visibly change from the initial text layout to a blue rectangle and
then an orange circle, without restarting or rebuilding.

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

**Not implemented and not proven.** The current gallery is an AOT ARM64 Jolt
library invoked by NativeActivity; `raylib/loop-android/src/main/cpp/main.c`
initializes, looks up, invokes, and shuts down Jolt on the Raylib native owner
thread. It contains no nREPL server. `scripts/android-repl` is only a
single-form, ADB-forwarded localhost debug-eval client; it explicitly is not
nREPL/CIDER or a general redefinition protocol.

Therefore the supported Android workflow remains:

```text
live pure layout work on Linux nREPL + Raylib
→ portable tests
→ build the AOT ARM64 library/APK
→ install/run and validate on Android
```

A future debug-only Android live-eval experiment is feasible in principle only
with this unproven ownership model:

```text
ADB-forwarded loopback request
→ authenticated/debug-only local transport
→ bounded request queue
→ Raylib/Jolt owner thread at a frame boundary
→ eval/load and framed response
```

It must never ship in a release APK, must not allow a transport worker to enter
Jolt or Raylib, must bound evaluation/frame disruption, and must stop accepting
requests before Raylib/Jolt shutdown. Native FFI declarations, ABI topology,
lifecycle/bootstrap code, packaged assets, and AOT changes remain rebuild-only.
This result makes a future Android queue experiment worthwhile; it does not
establish Android hot reload, Android nREPL, or CIDER compatibility.
