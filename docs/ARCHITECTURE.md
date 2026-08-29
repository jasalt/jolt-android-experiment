# Demonstrated architecture

This document describes **observed** architecture only. Design intent remains in
`docs/adr/` and `docs/PLAN.md`.

```text
shared .cljc reducer
       │
       │ Jolt Android ARM64 library (API 35, ARM64-v8a)
       ▼
canonical EDN string response copied through C/JNI
       │
       ▼
Kotlin JoltRuntime HandlerThread
       ├── native Android View shell
       ├── ClipboardManager effect adapter
       ├── SharedPreferences persistence adapter
       ├── POST_NOTIFICATIONS permission adapter
       └── AndroidWorker callback queue
```

## Observed runtime boundary

- `native/jolt/android-arm64/arm64-v8a/libjoltpoc.so` is an Android-35 AArch64
  shared object. Its observed dynamic dependencies are only `libm.so`,
  `libdl.so`, and `libc.so`.
- The app packages that ARM64 library and loads it through a thin JNI bridge.
  API 35 x86_64 emulator runs it through Android translation; this is **not**
  native ARM64-host evidence.
- `JoltRuntime` owns `HandlerThread("JoltRuntime")`. Android UI, lifecycle,
  permission, and worker callbacks enqueue data onto that thread. JNI records
  its owner thread; AndroidWorker does not call JNI directly.
- Jolt domain state is retained in a Jolt atom, not in Android views. The
  Android shell renders a caller-owned serialized result after each queued call.
- Jolt's `poc_dispatch` export decodes/validates canonical EDN, advances the
  persistent model, and returns a copied `:string` response. JNI immediately
  copies it into Java; no Jolt-managed pointer is retained by Android.

## Observed shared contracts

The portable reducer owns counter, lifecycle, worker, and notification
permission state. It emits data-only effects for:

- `:platform/clipboard`;
- `:storage/write`;
- `:permission/request`, `:notification/show`, `:platform/vibrate`, and
  `:platform/open-uri`.

Android adapters execute those effects with platform APIs and queue result events
back through `JoltRuntime`.

## Observed Linux GTK reference boundary

- On native x86_64 Linux, Glimmer/GTK4 renders the shared reducer model and
  converts GTK counter controls into reducer events. Its `:storage/write` and
  clipboard effects execute through host adapters; `gtk_show_uri` is invoked
  but the minimal Xvfb desktop has no URI handler. A fresh GTK process restores
  the persisted counter. [EXP-021](../experiments/EXP-021-x86-64-linux-gtk-host)
  retains the exact smoke tests, screenshots, and platform boundary.
- This is native x86_64 Linux GTK evidence only. It is not native ARM64 Linux,
  Android, or macOS GTK support. Native Apple Silicon macOS validation of the
  portable CLI fixtures and normal Jolt nREPL is separately recorded in
  [EXP-017](../experiments/EXP-017-native-arm64-portable-cli).

## Explicitly not demonstrated

- Native ARM64 Android-process and native ARM64 Linux GTK validation.
- Android nREPL/CIDER or an unrestricted production eval protocol. The
  debug-only ADB-forwarded eval/redefinition evidence in [EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval)
  is deliberately narrower.
