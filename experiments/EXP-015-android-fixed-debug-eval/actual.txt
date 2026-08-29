# Observed 2026-08-29

The pinned Jolt source includes `host/chez/compile-eval.ss`, which binds
`clojure.core/load-string`. A bounded export was therefore tested without
adding arbitrary input, a listener, or a protocol:

```clojure
(defn debug-eval-fixed []
  (load-string "(+ 40 2)"))

(ffi/export! "poc_debug_eval_fixed" debug-eval-fixed [] :int)
```

## Initial run

The initial disposable run built and loaded the library, then reported only:

```text
I/jolt_probe: {:error :exports}
```

The bridge tested all required lookups in one condition and did not log their
individual values. Subsequent queued lifecycle work entered a runtime that the
error path had already shut down and reached Chez `S_abnormal_exit`/SIGABRT.
The preserved
[`exp015-fixed-debug-eval-failure.txt`](../../artifacts/logs/exp015-fixed-debug-eval-failure.txt)
therefore proves an aggregate export check failed and that the error path was
unsafe; it does **not** prove `poc_debug_eval_fixed` was absent. Commit
`6a39331` overstated that inference.

## Diagnostic rerun

The same fixed export was rebuilt from pinned Jolt
`8fcba79f8b33628af926f88032d93a1b31c24235`. The bridge resolved it alongside
every baseline export, called it only on `HandlerThread("JoltRuntime")`, and
logged its primitive result before normal lifecycle dispatch:

```text
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: fixed debug eval result=42
I/jolt_probe: owner JNI session initialized
I/jolt_probe: dispatch counter=0
```

No abort occurred. Later lifecycle events continued to dispatch normally. See
[`exp015-fixed-debug-eval-rerun.txt`](../../artifacts/logs/exp015-fixed-debug-eval-rerun.txt).

## Conclusion

A fixed, local, no-argument Jolt `load-string` call works in this Android-35
ARM64 library under the x86_64 emulator's ARM64 translation path. This validates
only a bounded debug-evaluation seam. It does not validate caller-supplied
source, result serialization, error recovery, redefinition, a network listener,
Android nREPL/CIDER, or native ARM64-host execution.

The fixed-eval source extension, JNI diagnostic call, and rebuilt library were
disposable and were restored to the normal baseline after capturing evidence.
