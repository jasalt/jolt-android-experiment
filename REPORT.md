# Jolt Android PoC feasibility report

**Assessment date:** 2026-08-29

**Pinned Jolt:** `8fcba79f8b33628af926f88032d93a1b31c24235`

**Pinned Chez:** v10.4.1

**Android target:** API 35 `arm64-v8a`; observed under API 35 x86_64 emulator
translation.

## Conclusion

### Observed

The PoC demonstrates a persistent Jolt/Chez ARM64 managed library embedded in
an Android application through a Kotlin/JNI boundary:

1. Chez ARM64 target artifacts initialize and evaluate Scheme independently
   ([EXP-003](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-003-chez-android-jni)).
2. A reduced Jolt cross-library patch builds an Android-35 AArch64 `.so` with
   Bionic dependencies only; it initializes, resolves exports, and executes
   them in the app ([EXP-004](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-004-jolt-android-library-cross-target)).
3. The bridge survives 10,000 calls, allocation, explicit compaction, shutdown,
   and process relaunch under the tested emulator configuration ([EXP-005](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-005-jolt-android-lifecycle-stress)).
4. Android calls are confined to `HandlerThread("JoltRuntime")`; UI, lifecycle,
   permission, and worker callbacks queue data rather than entering JNI/Jolt
   directly ([EXP-006](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-006-android-jolt-handler-thread), [EXP-009](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-009-android-runtime-lifecycle), [EXP-013](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-013-android-worker-callback), [EXP-014](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-014-android-notification-permission)).
5. Shared reducer events drive Jolt-owned counter/lifecycle state, clipboard
   effects, integer persistence and fresh-process restoration, and notification
   permission request/result state ([EXP-007](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-007-android-edn-dispatch) through [EXP-014](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-014-android-notification-permission)).

The source commits and evidence artifacts are intended to make every statement
above reproducible through the referenced experiment documents, Nix shell,
Android build, emulator scripts, logs, layout captures, and screenshots.

### Inferred

The demonstrated separation—portable reducer plus data-only effects plus a
single platform adapter thread—is viable for a constrained Android proof of
concept. It is not evidence that Jolt is a drop-in replacement for JVM Clojure
on Android, nor that this is a production-ready mobile runtime.

### Portable-host evidence

Native Apple Silicon macOS ran the pinned `aarch64-darwin` Jolt shell without
GTK. The portable suite passed six tests and 13 assertions, its valid and
malformed CLI fixtures behaved as specified, and a loopback Jolt nREPL server
evaluated the reducer increment event ([EXP-017](experiments/EXP-017-native-arm64-portable-cli)).
This is native host evidence, not Android ARM64 execution evidence.

Android has one narrower debug-evaluation result: a disposable fixed
`load-string` export resolved through `jolt_lookup` and returned `42` on the
Jolt runtime thread ([EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval)). The initial aggregate export-check failure was
not evidence that this export was absent; a diagnostic rerun corrected that
attribution. No caller-supplied evaluation, Android nREPL, remote evaluation,
CIDER, error-recovery path, or redefinition is implemented.

### Unimplemented

- GTK/Glimmer reference application.
- Compose UI; the observed Android shell uses native Android Views.
- Android notification posting, URL/intent effects, generalized permissions,
  and additional platform capabilities.
- A general Android eval server or nREPL protocol.

## Evidence index

| Area | Primary evidence |
| --- | --- |
| Emulator startup and screenshots | [EXP-002](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-002-api35-emulator-startup-crash); `artifacts/screenshots/` |
| ARM64 translation loader probe | [EXP-001](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-001-api35-arm64-on-x86-emulator) |
| Standalone Chez Android JNI | [EXP-003](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-003-chez-android-jni) |
| Jolt cross-library construction | [EXP-004](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-004-jolt-android-library-cross-target) |
| Lifecycle/GC stress | [EXP-005](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-005-jolt-android-lifecycle-stress) and [EXP-008](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-008-android-jolt-session) |
| Thread confinement | [EXP-006](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-006-android-jolt-handler-thread) and [EXP-013](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-013-android-worker-callback) |
| Data ownership and dispatch | [EXP-007](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-007-android-edn-dispatch) |
| Runtime/lifecycle UI | [EXP-009](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-009-android-runtime-lifecycle) |
| Counter controls | [EXP-010](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-010-android-counter-controls) |
| Clipboard effect | [EXP-011](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-011-android-clipboard-effect) |
| Persistence restore | [EXP-012](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-012-android-persistence) |
| Permission round trip | [EXP-014](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-014-android-notification-permission) |
| Bounded fixed debug eval | [EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval) |
| Native Apple Silicon portable CLI/nREPL | [EXP-017](experiments/EXP-017-native-arm64-portable-cli) |

## Reproducible validation baseline

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/test-portable
nix --extra-experimental-features 'nix-command flakes' develop -c env JOLT_SOURCE=../jolt scripts/jolt-android-library-build
nix --extra-experimental-features 'nix-command flakes' develop -c gradle --no-daemon :app:assembleDebug
```

Install and interact with the deterministic API 35 AVD using the command
sequences stored in each experiment. Run `nix flake check --all-systems --no-build`
for Nix output evaluation.
