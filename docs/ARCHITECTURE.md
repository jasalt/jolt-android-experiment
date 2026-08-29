# Demonstrated architecture

This document describes **observed** architecture only. Design intent remains in
`docs/adr/` and `docs/PLAN.md`.

```text
shared .cljc reducer
       │
       │ Jolt Android ARM64 library (API 35, ARM64-v8a)
       ▼
caller-owned C/JNI result buffer
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
- Jolt returns primitive export values across the C ABI. C renders result data
  into bounded caller-owned buffers before JNI copies it into Java. No
  Jolt-managed returned string pointer is retained by Android.

## Observed shared contracts

The portable reducer owns counter, lifecycle, worker, and notification
permission state. It emits data-only effects for:

- `:platform/clipboard`;
- `:storage/write`;
- `:permission/request`.

Android adapters execute those effects with platform APIs and queue result events
back through `JoltRuntime`.

## Explicitly not demonstrated

- Native aarch64 Linux/macOS portable CLI or nREPL execution.
- GTK/Glimmer reference host.
- Compose UI; the demonstrated shell is a minimal native Android View layout.
- Android notification posting, URL intents, or generalized permissions.
- General Android debug evaluation, a socket server, Android nREPL, CIDER, or
  code redefinition. EXP-015 records a fixed `load-string` export limitation.
