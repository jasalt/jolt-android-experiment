# Raylib FFI call-volume report

This bounded desktop measurement checks whether direct scalar `jolt.ffi` calls
need batching before Voxel Siege rendering is expanded. It is not a GPU
benchmark.

## Measurement

Command, from `raylib/` in the pinned Nix shell:

```sh
nix develop .. -c jolt -e '(do (require (quote [jolt.ffi :as ffi]))
  (ffi/defcfn get-time "GetTime" [] :double)
  (defn bench [n] (let [start (System/nanoTime)]
    (dotimes [_ n] (get-time))
    (/ (- (System/nanoTime) start) 1.0e6)))
  (println (bench 100)) (println (bench 1000)) (println (bench 10000)))'
```

After a 1,000-call warmup, five runs produced these millisecond samples:

| Calls | Samples | Min–max | Max share of 16.67 ms frame |
| ---: | --- | ---: | ---: |
| 100 | 0.004549, 0.005390, 0.002725, 0.005060, 0.004500 | 0.002725–0.005390 | 0.03% |
| 1,000 | 0.034856, 0.034326, 0.044946, 0.032732, 0.034506 | 0.032732–0.044946 | 0.27% |
| 10,000 | 0.379473, 0.357472, 0.338144, 0.499682, 0.474714 | 0.338144–0.499682 | 3.00% |

The calls target Raylib's scalar `GetTime` ABI and exclude GPU work, allocation
and logging. Direct calls are comfortably below a 60 FPS CPU budget in this
sample. Do not infer Android translation or Voxel's eventual cube-call cost
from this synthetic scalar run; repeat on the target scene and device before
batching.

## Visual context

[Direct FFI measurement visual context (screenshot)](evidence/voxel-scene-live.png)

The embedded image is a live Voxel scene-shell capture, not a performance
claim. Its SHA-256 is recorded in `RAYLIB-VOXEL-SIEGE-SCENE.md`.

## Android OpenURL probe

Using the Android debug nREPL over a loopback ADB forward, the owner queue
returned `{:status :ok, :value {:status :rejected, :reason :unsafe-url}}` for
`http://unsafe`, and `{:status :ok, :value {:status :requested, :url
"https://jolt-lang.net"}}` for the HTTPS URL. Both calls were queued from the
REPL and invoked on the Raylib owner thread. The emulator transferred focus to
the available Android handler; the return-to-gallery path was not claimed in
this run. This proves URL validation and request dispatch only, not universal
browser availability.

## Decision

Do not add a command-buffer batching layer based on this measurement. Keep the
existing direct-call architecture and measure real Voxel draw counts separately
when the owner-affine renderer is connected.
