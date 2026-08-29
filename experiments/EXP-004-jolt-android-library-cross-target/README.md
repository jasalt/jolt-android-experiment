# Jolt Android shared-library cross target

## Problem

Determine the source-level requirements for producing an Android ARM64 Jolt
managed-runtime shared library from the pinned Jolt implementation.

## Environment

- Jolt `ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e` (upstream PR #778)
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
