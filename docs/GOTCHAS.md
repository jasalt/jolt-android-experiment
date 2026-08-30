# Integration gotchas

Each item is an observed constraint with an evidence reference.

- **Do not infer native ARM64 portability from this emulator.** API 35 x86_64
 emulator translates the ARM64 library ([EXP-001](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-001-api35-arm64-on-x86-emulator)), but `jolt-android-a4e.2`
  required a separate native host run. Native Apple Silicon portable CLI/nREPL
  evidence is recorded in [EXP-017](../experiments/EXP-017-native-arm64-portable-cli).
- **Use ANGLE for the deterministic API 35 AVD.** SwiftShader/off paths crashed
  under the tested emulator version; `scripts/emulator-start` defaults to ANGLE
  ([EXP-002](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-002-api35-emulator-startup-crash)).
- **Chez Android kernel must be PIC and Bionic-linked.** The tested target uses
  `-fPIC` and Bionic `-llz4 -lz -lm -ldl`, not generic Linux `-lrt -lpthread`
  ([EXP-003](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-003-chez-android-jni)).
- **Cross shared-library support is upstream; compaction is not.** Jolt PR #778
  (the pinned revision) supports `--target` with `--library`. [EXP-004](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-004-jolt-android-library-cross-target)
  applies only the EXP-005-specific `jolt_library_collect` hook in a disposable
  worktree; Android target-pack PIC/Bionic link flags remain downstream.
- **Jolt FFI needs Nix library paths for GTK.** Glimmer GTK declares Linux
  library names such as `libglib-2.0.so.0`, but Nix keeps GTK/GLib outside the
  system loader paths. The Linux development shell must export its GTK/GLib
  `LD_LIBRARY_PATH`; this is validated on x86_64 Linux in
  [EXP-021](../experiments/EXP-021-x86-64-linux-gtk-host).
- **GTK rerenders belong on its main loop.** The pinned backend retains native
  signal callables as collect-safe callbacks and schedules ratom changes from
  nREPL/worker threads through GTK's idle queue. Do not invoke GTK FFI directly
  from an nREPL or future thread. EXP-021's `repl-live-smoke` exercises this
  scheduling contract.
- **GTK capability does not prove desktop availability.** The Linux adapter can
  request `:platform/open-uri`, but `gtk_show_uri` returning does not mean a
  portal or browser completed it. Keep implemented capabilities separate from
  `:capability-status` and per-dispatch adapter outcomes; the Xvfb session's
  missing URI handler is retained in EXP-021.
- **Never retain Jolt-managed string pointers in JNI.** The Android bridge's
  canonical `poc_dispatch` response is a `:string` copied across the C ABI and
  copied again into Java; C no longer formats domain EDN
  ([EXP-007](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-007-android-edn-dispatch)).
- **Every Jolt entry must use `JoltRuntime`.** Worker/UI/platform callbacks queue
  data to one HandlerThread; direct runtime entry is not a supported Android
  contract ([EXP-006](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-006-android-jolt-handler-thread), [EXP-013](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-013-android-worker-callback)).
- **Force-stop is not orderly shutdown evidence.** It proves a fresh process
  restore path ([EXP-012](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-012-android-persistence)); owner-queue shutdown is separately observed in
  [EXP-008](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-008-android-jolt-session).
- **The emulator lacks a usable `cmd clipboard get`.** Clipboard verification
  uses Android app read-back plus the observed system overlay ([EXP-011](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-011-android-clipboard-effect)).
- **An aggregate export-check failure does not identify the missing export.**
  [EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval) initially attributed `{:error :exports}` to its new fixed-eval export,
  but the bridge had checked all pointers at once. Per-export diagnostics on a
  rerun resolved the fixed export and `load-string` returned `42`. Keep failed
  initialization terminal so queued work cannot enter a shut-down Chez runtime.
  The subsequent debug-only ADB-forwarded eval and reduced redefinition result
  remain interactive development evidence, not Android nREPL/CIDER
  ([EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval)).
