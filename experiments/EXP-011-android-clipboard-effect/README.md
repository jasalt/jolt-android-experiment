# Android Jolt clipboard effect

## Purpose

Prove one data-only Jolt platform effect end to end: Jolt returns clipboard
intent data, while the Android adapter performs the native operation.

## Contract

The shared reducer maps `:platform/copy-counter` to:

```clojure
{:type :platform/clipboard :text "Jolt counter: <n>"}
```

Jolt exposes only a primitive effect code. The C bridge renders effect data into
its caller-owned response; `MainActivity` detects that result on the main
looper, then uses `ClipboardManager` to write and read the content. Neither
Jolt nor JNI receives Android clipboard objects.

## Result

See [actual.md](actual.md),
[`artifacts/logs/exp011-clipboard-effect.txt`](../../artifacts/logs/exp011-clipboard-effect.txt),
and [`artifacts/screenshots/exp011-clipboard-effect.png`](../../artifacts/screenshots/exp011-clipboard-effect.png).
