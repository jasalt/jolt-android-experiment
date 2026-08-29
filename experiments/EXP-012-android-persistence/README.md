# Android Jolt counter persistence

## Purpose

Prove a data-only storage effect from Jolt through Android persistence and back
to a freshly initialized Jolt session after process relaunch.

## Contract

Counter mutations produce declarative effects such as:

```clojure
{:type :storage/write :key "counter" :value 1}
```

Kotlin stores only that integer in `SharedPreferences`. On a new process it
reads the integer and queues `{:type :storage/restore :value 1}` through
`JoltRuntime`; Jolt owns the restored domain model. The native bridge accepts
only the bounded integer restore event and returns caller-owned result text.

## Result

See [actual.txt](actual.txt),
[`artifacts/logs/exp012-persistence.txt`](../../artifacts/logs/exp012-persistence.txt),
and [`artifacts/screenshots/exp012-persistence-restored.png`](../../artifacts/screenshots/exp012-persistence-restored.png).
