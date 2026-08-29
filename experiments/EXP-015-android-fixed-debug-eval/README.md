# Android fixed debug-evaluation boundary

## Purpose

Determine whether a bounded local Jolt evaluation can be exposed from the
Android managed-library image before attempting any Android socket server or
nREPL protocol.

## Experiment

A disposable extension to the Android Jolt fixture defined:

```clojure
(defn debug-eval-fixed []
  (load-string "(+ 40 2)"))

(ffi/export! "poc_debug_eval_fixed" debug-eval-fixed [] :int)
```

The Android bridge resolved this as a no-argument export through the existing
`JoltRuntime` queue. No user source string, socket listener, or remote caller
was added.

## Result

The library built but did not publish `poc_debug_eval_fixed` through
`jolt_lookup`. See [actual.txt](actual.txt) and
[`artifacts/logs/exp015-fixed-debug-eval-failure.txt`](../../artifacts/logs/exp015-fixed-debug-eval-failure.txt).
The disposable experiment sources were restored after preserving the failure.
