# API 35 ARM64 native library on x86_64 emulator

## Problem

Determine whether the API 35 Google APIs x86_64 emulator can load and execute an APK containing only an `arm64-v8a` native library.

## Environment

Run through the repository's pinned Nix shell. The initial native artifact uses NDK API 35 ARM64 Clang; emulator validation uses the deterministic API 35 x86_64 KVM/ANGLE AVD from `scripts/emulator-start`.

## Minimal reproduction

```sh
nix develop -c ./experiments/EXP-001-api35-arm64-on-x86-emulator/commands.sh
```

The source is deliberately only `int poc_answer(void) { return 42; }`. No Chez or Jolt component participates in this experiment.

## Expected

See [expected.md](expected.md).

## Actual

See [actual.md](actual.md). APK packaging, install, and native invocation are recorded only after those steps have been observed.

## Investigation

Facts only. The initial result establishes NDK output metadata; it does not prove Android ABI translation.

## Workaround

Not applicable until the translation result is observed.

## Suspected layer

Android package manager/native loader and API 35 emulator ABI translation.

## Upstream suitability

Uncertain.
