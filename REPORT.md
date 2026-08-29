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
   (EXP-003).
2. A reduced Jolt cross-library patch builds an Android-35 AArch64 `.so` with
   Bionic dependencies only; it initializes, resolves exports, and executes
   them in the app (EXP-004).
3. The bridge survives 10,000 calls, allocation, explicit compaction, shutdown,
   and process relaunch under the tested emulator configuration (EXP-005).
4. Android calls are confined to `HandlerThread("JoltRuntime")`; UI, lifecycle,
   permission, and worker callbacks queue data rather than entering JNI/Jolt
   directly (EXP-006, EXP-009, EXP-013, EXP-014).
5. Shared reducer events drive Jolt-owned counter/lifecycle state, clipboard
   effects, integer persistence and fresh-process restoration, and notification
   permission request/result state (EXP-007 through EXP-014).

The source commits and evidence artifacts are intended to make every statement
above reproducible through the referenced experiment documents, Nix shell,
Android build, emulator scripts, logs, layout captures, and screenshots.

### Inferred

The demonstrated separation—portable reducer plus data-only effects plus a
single platform adapter thread—is viable for a constrained Android proof of
concept. It is not evidence that Jolt is a drop-in replacement for JVM Clojure
on Android, nor that this is a production-ready mobile runtime.

### Blocked

Native ARM64 portable-host validation is still blocked. The current environment
is an x86_64 Lima guest; emulation/cross-compilation does not satisfy
`jolt-android-a4e.2`. A native aarch64 Linux guest or Apple Silicon macOS host
must run the portable CLI fixtures and normal Jolt nREPL before multiplatform
portable-host support is claimed.

Android debug evaluation is also blocked at a narrower boundary: a disposable
fixed `load-string` export was not published through `jolt_lookup`, and leaving
subsequent queued work after that failed initialization reached Chez
`S_abnormal_exit`/SIGABRT (EXP-015). This is not a claim that `load-string`
itself fails, and no Android nREPL, remote evaluation, CIDER, or redefinition is
implemented.

### Unimplemented

- GTK/Glimmer reference application; it remains blocked by native ARM64 portable
  validation in the dependency graph.
- Compose UI; the observed Android shell uses native Android Views.
- Android notification posting, URL/intent effects, generalized permissions,
  and additional platform capabilities.
- A general Android eval server or nREPL protocol.

## Evidence index

| Area | Primary evidence |
| --- | --- |
| Emulator startup and screenshots | EXP-002; `artifacts/screenshots/` |
| ARM64 translation loader probe | EXP-001 |
| Standalone Chez Android JNI | EXP-003 |
| Jolt cross-library construction | EXP-004 |
| Lifecycle/GC stress | EXP-005 and EXP-008 |
| Thread confinement | EXP-006 and EXP-013 |
| Data ownership and dispatch | EXP-007 |
| Runtime/lifecycle UI | EXP-009 |
| Counter controls | EXP-010 |
| Clipboard effect | EXP-011 |
| Persistence restore | EXP-012 |
| Permission round trip | EXP-014 |
| Debug-eval limitation | EXP-015 |

## Reproducible validation baseline

```sh
nix develop -c ./scripts/test-portable
nix develop -c env JOLT_SOURCE=../jolt scripts/jolt-android-library-build
nix develop -c gradle --no-daemon :app:assembleDebug
```

Install and interact with the deterministic API 35 AVD using the command
sequences stored in each experiment. Run `nix flake check --all-systems --no-build`
for Nix output evaluation. These commands do not replace the outstanding native
ARM64-host validation.
