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

```text
shared reducer/view-model
          │ portable events and effects
          ▼
Glimmer reactive core
          │ eight-operation backend contract
          ▼
pinned glimmer-gtk backend ──► GTK4 native widgets
          ▲
          └── project persistence / clipboard / URI effect adapter
```

The project does not maintain a GTK reconciler. Requiring `glimmer-gtk.core`
installs the pinned upstream backend, which supplies widget creation and prop
application, child reconciliation, GTK-loop scheduling, and application startup.
The direct GDK/GTK FFI declarations in `poc.gtk-app` implement only platform
effects absent from the portable model.

- On native x86_64 Linux, Glimmer/GTK4 renders `poc.reducer/view-model` and
  converts GTK controls into portable reducer events. The reducer model and
  adapter diagnostics live in separate reactive cells; no GTK pointer or effect
  result enters authoritative domain state.
- Persistence restore enters the same reducer through `:storage/restore`. On
  dispatch, the pure model transition commits before effects execute. Effect
  outcomes are adapter diagnostics, so a failed desktop operation cannot roll
  back or contaminate the portable model.
- `:capabilities` means that an adapter implements an operation, not that the
  current desktop session can complete it. `:capability-status` distinguishes
  available, session-dependent, and request-only behavior. In particular,
  `gtk_show_uri` accepts a request, but the minimal Xvfb desktop has no URI
  handler and therefore does not establish a successfully opened URI.
- Glimmer retains collect-safe native callbacks and schedules reactive updates
  from nREPL/worker threads onto GTK's main loop. EXP-021 exercises both ordinary
  reactive repainting and that off-thread scheduling path.
- [EXP-021](../experiments/EXP-021-x86-64-linux-gtk-host) retains the exact smoke
  tests, screenshots, and platform boundary. Its X11 tooling is an automation
  choice, not a general GTK backend restriction.
- This is native x86_64 Linux GTK evidence only. It is not native ARM64 Linux,
  Android, or macOS GTK support. Upstream `glimmer-gtk` platform support is
  broader than this project's evidence. Native Apple Silicon macOS validation
  of portable CLI fixtures and normal Jolt nREPL is separately recorded in
  [EXP-017](../experiments/EXP-017-native-arm64-portable-cli).

## Explicitly not demonstrated

- Native ARM64 Android-process and native ARM64 Linux GTK validation.
- Android nREPL/CIDER or an unrestricted production eval protocol. The
  debug-only ADB-forwarded eval/redefinition evidence in [EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval)
  is deliberately narrower.
