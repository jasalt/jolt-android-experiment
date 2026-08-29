# Jolt Android shared-library cross target

## Problem

Determine the source-level requirements for producing an Android ARM64 Jolt
managed-runtime shared library from the pinned Jolt implementation.

## Environment

- Jolt `8fcba79f8b33628af926f88032d93a1b31c24235`
- Chez `v10.4.1` (`e95a7efbafa2cf3bd5343ea542e6bc909a7ab2c4`)
- Android NDK r29, API 35 ARM64 toolchain
- Target Chez machine: `tarm64le`, with Bionic-specific `-fPIC`, `-lm`, and
  `-ldl` link configuration.

## Minimal reproduction

The rejection is intentional and occurs before compiling an application:

```text
jolt build --library -m poc.native -o libjoltpoc.so \
  --target tarm64le --target-pack <prepared-pack>
```

## Actual

See [actual.md](actual.md).

## Classification

See [requirements.md](requirements.md). The smallest resulting Jolt patch is
kept in this experiment and applied only to a disposable pinned-source worktree
by `scripts/jolt-android-library-build`; the pinned Jolt checkout is unchanged.
