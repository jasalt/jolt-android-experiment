# Integration gotchas

Each item is an observed constraint with an evidence reference.

- **Do not infer native ARM64 portability from this emulator.** API 35 x86_64
  emulator translates the ARM64 library (EXP-001), but `jolt-android-a4e.2`
  remains blocked until a native ARM64 host runs portable CLI/nREPL fixtures.
- **Use ANGLE for the deterministic API 35 AVD.** SwiftShader/off paths crashed
  under the tested emulator version; `scripts/emulator-start` defaults to ANGLE
  (EXP-002).
- **Chez Android kernel must be PIC and Bionic-linked.** The tested target uses
  `-fPIC` and Bionic `-llz4 -lz -lm -ldl`, not generic Linux `-lrt -lpthread`
  (EXP-003).
- **Jolt cross shared libraries require an isolated patch.** Pinned Jolt rejects
  `--target` combined with `--library`; EXP-004 applies the reduced patch only
  in a disposable worktree.
- **Never retain Jolt-managed string pointers in JNI.** The Android bridge uses
  primitive Jolt exports and formats bounded caller-owned result buffers
  (EXP-007).
- **Every Jolt entry must use `JoltRuntime`.** Worker/UI/platform callbacks queue
  data to one HandlerThread; direct runtime entry is not a supported Android
  contract (EXP-006, EXP-013).
- **Force-stop is not orderly shutdown evidence.** It proves a fresh process
  restore path (EXP-012); owner-queue shutdown is separately observed in
  EXP-008.
- **The emulator lacks a usable `cmd clipboard get`.** Clipboard verification
  uses Android app read-back plus the observed system overlay (EXP-011).
- **Android debug evaluation is blocked at export publishing.** Adding the
  fixed `load-string` export left it absent from `jolt_lookup`; later queued
  work reached Chez `S_abnormal_exit`/SIGABRT. This does not prove that
  `load-string` itself fails, and no Android nREPL is claimed (EXP-015).
