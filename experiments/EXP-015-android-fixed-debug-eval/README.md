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

A diagnostic rerun published every expected export and called the fixed
`load-string` function on `HandlerThread("JoltRuntime")`, returning `42`. See
[actual.txt](actual.txt) and
[`artifacts/logs/exp015-fixed-debug-eval-rerun.txt`](../../artifacts/logs/exp015-fixed-debug-eval-rerun.txt).

The first run's aggregate `{:error :exports}` did not identify which lookup had
failed, so commit `6a39331` incorrectly attributed it to
`poc_debug_eval_fixed`. The original failure log remains preserved, but is not
evidence of an eval or export-publishing limitation. Disposable sources and
artifacts were restored after the successful rerun.
