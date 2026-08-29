# Android notification permission round trip

## Purpose

Prove a Jolt-declared runtime permission request reaches Android, and Android’s
callback returns to Jolt through the single `JoltRuntime` queue as data.

## Contract

The shared reducer emits:

```clojure
{:type :permission/request :permission :notifications}
```

for `:permission/request-notifications`. Android owns `POST_NOTIFICATIONS` and
its platform callback. `onRequestPermissionsResult` queues either
`:permission/result-granted` or `:permission/result-denied` through
`JoltRuntime`; Jolt owns the resulting domain state.

## Result

See [actual.md](actual.md),
[`artifacts/logs/exp014-notification-permission.txt`](../../artifacts/logs/exp014-notification-permission.txt),
and the dialog and granted-state screenshots under
[`artifacts/screenshots/`](../../artifacts/screenshots/).
