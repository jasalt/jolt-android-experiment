# Jolt Android PoC feasibility report

**Assessment date:** 2026-08-29

**Pinned Jolt:** `ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e` (merged PR #778)

**Pinned Chez:** v10.4.1

**Android target:** API 35 `arm64-v8a`; observed under API 35 x86_64 emulator
translation.

## Conclusion

### Observed

The PoC demonstrates a persistent Jolt/Chez ARM64 managed library embedded in
an Android application through a Kotlin/JNI boundary:

1. Chez ARM64 target artifacts initialize and evaluate Scheme independently
   ([EXP-003](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-003-chez-android-jni)).
2. Upstream Jolt PR #778 builds an Android-35 AArch64 `.so` with Bionic
   dependencies only; it initializes, resolves exports, and executes them in
   the app. The only remaining downstream patch is EXP-005's explicit
   compaction ABI hook ([EXP-004](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-004-jolt-android-library-cross-target)).
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

Native Apple Silicon macOS also built `:app:assembleDebug` with the pinned Nix
SDK/NDK, producing the ARM64 Android libraries in the debug APK. Its API 35
ARM64 AVD was created and Hypervisor.Framework acceleration was available, but
the emulator could not boot with only 8.7 GiB free where its userdata partition
required 12 GiB ([EXP-019](experiments/EXP-019-native-darwin-android-nix-build)).

Android has one narrower debug-evaluation result: a disposable fixed
`load-string` export resolved through `jolt_lookup` and returned `42` on the
Jolt runtime thread ([EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval)). The initial aggregate export-check failure was
not evidence that this export was absent; a diagnostic rerun corrected that
attribution. Caller-supplied debug evaluation is available only through the debug-only,
ADB-forwarded loopback protocol, with structured success/error recovery and a
separate reduced live-redefinition observation. It is not Android nREPL or
CIDER; its exact boundary is [EXP-015](https://github.com/jasalt/jolt-android-experiment/tree/master/experiments/EXP-015-android-fixed-debug-eval).

### Linux GTK reference host

The native x86_64 Fedora Lima host runs the pinned Glimmer/GTK4 backend against
the same portable reducer. Its live GTK reactivity smoke passed, and the
reference app rendered counter state, accepted GTK increment/decrement/reset
events, persisted the reducer's `:storage/write` effect through a host-side EDN
file, and restored state in a fresh process. The exact environment, tests,
screenshots, and scope boundary are in
[EXP-021](experiments/EXP-021-x86-64-linux-gtk-host). This is not native ARM64
Linux, Android, or macOS GTK evidence.

### Graded outcome

**Level 5 — development-quality PoC is evidenced.** The project has a
persistent runtime, data-oriented Android effects/callbacks, debug eval,
automated portable/JNI/UI checks, screenshots, measurements, and a fail-fast
clean-room verifier ([EXP-016](experiments/EXP-016-clean-room-validation)).

**Level 6 is not evidenced.** Native ARM64 Linux portable CLI/GTK validation is
blocked by the missing host, and API-35 x86_64 emulator execution remains ARM64
translation evidence rather than native ARM64 Android-process evidence.

### Remaining boundaries and production risks

- Native ARM64 Linux GTK/portable validation and native ARM64 Android execution.
- GTK `gtk_show_uri` is invoked but the minimal Xvfb desktop has no portal or
  browser handler; it must not be reported as a successful open-uri action.
- The single-thread embedded-library restriction, Android lifecycle ownership,
  raw JNI/C boundary, downstream compaction ABI extension, and Bionic/PIC
  target-pack requirements remain production integration risks.
- The Compose shell, notification posting, clipboard/vibration/URL effects,
  locale/package information, and permission flow are implemented as bounded
  platform adapters; they are not a general mobile framework or Android nREPL.

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
| Native Apple Silicon Android Nix build | [EXP-019](experiments/EXP-019-native-darwin-android-nix-build) |
| Linux x86_64 Glimmer/GTK4 reference host | [EXP-021](experiments/EXP-021-x86-64-linux-gtk-host) |
| Canonical CLI/GTK/Android fixtures | `test/conformance/fixtures.tsv`; `./scripts/cli --conformance`; Android instrumentation |
| Automated clean-room verifier | [EXP-016](experiments/EXP-016-clean-room-validation); `scripts/verify` |

## Reproducible validation baseline

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/bootstrap
JOLT_SOURCE=/path/to/jolt-at-ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e \
  nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/verify
```

Install and interact with the deterministic API 35 AVD using the command
sequences stored in each experiment. Run `nix flake check --all-systems --no-build`
for Nix output evaluation.
