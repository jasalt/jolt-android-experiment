# Host Jolt shared-library harness PIC boundary

## Problem

The Tier-3 host-native Jolt shared-library harness cannot link against the
current host Chez kernel because the installed `libkernel.a` is not PIC.

## Reproduction

```sh
nix develop -c env JOLT_SOURCE=../jolt ./scripts/test-host-jolt-library
```

## Actual

The Jolt library build reaches the host linker, then fails with:

```text
relocation R_X86_64_PC32 against symbol `stderr@@GLIBC_2.2.5'
can not be used when making a shared object; recompile with -fPIC
```

This is the documented Chez shared-library PIC requirement. The C harness is
preserved in `native/jolt-library-harness.c`; it calls init, lookup, exported
answer/allocation, repeated allocation, and shutdown once supplied a host
library built with a PIC Chez kernel.

## Scope

This does not affect the separately proven Android ARM64 library, which is
built from its Android-specific PIC target pack. On this x86_64 host that ARM64
ELF is not executed by an emulator substitute; `scripts/test-android-library`
performs `file`, dynamic dependency, and exported-symbol checks, while Android
instrumentation supplies runtime evidence.
