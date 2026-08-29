# Observed source reduction — 2026-08-28

The pinned implementation rejects cross shared-library builds in two independent
places:

1. `jolt-core/jolt/main.clj` lines 550–551:

```clojure
(when (and target library?)
  (throw (ex-info "cross build (--target) does not support --library yet" {})))
```

1. `host/chez/build.ss` lines 1390–1393 repeats the same guard in the
cross-compilation path before it can call `build-shared`.

The existing cross path is executable-only. It uses a target pack containing
`scheme.h`, PIC `libkernel.a`, boot files, an xpatch, target static lz4/zlib,
and target link flags. `build-shared` (lines 1743–1823) has the needed library
stub (`jolt_library_init`, `jolt_lookup`, `jolt_library_shutdown`) but always
uses host CSV inputs and host `cc`; it has no target/xpatch branch.

The existing `tools/cross-compile/make-pack.sh` emits Linux link flags
`-llz4 -lz -lm -ldl -lrt -lpthread`. Those are invalid for the observed Android
Bionic target; EXP-003 proved an Android Chez kernel must instead be PIC and
link with `-llz4 -lz -lm -ldl`.

## CLI reproduction — observed 2026-08-28

A target pack was assembled successfully from the repository-pinned Chez
checkout:

```text
$ CHEZ_SRC=.cache/chezscheme-v10.4.1 \
    ../jolt/tools/cross-compile/make-pack.sh tarm64le /tmp/jolt-tarm64le-pack
wrote target pack: /tmp/jolt-tarm64le-pack
```

Running the pinned Jolt source against its existing `libadd` fixture:

```text
$ bin/jolt build --library -m libadd.core -o /tmp/libadd-android.so \
    --target tarm64le --target-pack /tmp/jolt-tarm64le-pack
Unhandled exception: cross build (--target) does not support --library yet
  ex-data: {}
  trace:
    cmd-build
exit=1
```

The command exited nonzero before generating a library. This confirms the
source guards are active in the pinned implementation.

## Upstreamed cross-library support and Android runtime

The generic source reduction was upstreamed as Jolt PR
[#778](https://github.com/jolt-lang/jolt/pull/778), merged in
`ae5c5a6d5be263a883e9b4b53f255b8c0b493d3e`. The project now pins that revision;
`scripts/jolt-android-library-build` uses its native cross-`--library` support
without applying the former `jolt-cross-library.patch` workaround. The script
still applies only `jolt-library-collect.patch` in a disposable worktree. That
one-function patch exposes `jolt_library_collect` for EXP-005 stress testing;
it was deliberately outside PR #778's generic scope and does not implement
cross compilation.

The script built
`native/jolt/android-arm64/arm64-v8a/libjoltpoc.so` using NDK r29:

```text
ELF 64-bit LSB shared object, ARM aarch64, for Android 35
NEEDED Shared library: [libm.so]
NEEDED Shared library: [libdl.so]
NEEDED Shared library: [libc.so]
T jolt_library_init
T jolt_library_shutdown
```

`poc_answer` is a Jolt ABI-table export, deliberately resolved by
`jolt_lookup("poc_answer")`, not an ELF symbol. The final APK packaged a 5,144-byte `lib/arm64-v8a/libjolt_probe.so` and a
20,083,232-byte `lib/arm64-v8a/libjoltpoc.so`. On the API 35 x86_64 emulator
the Android loader reported `libjolt_probe.so ...: ok`, the
JNI bridge logged `poc_answer() = 42`, and the UI hierarchy contained:

```text
Jolt answer() = 42
```

This validates build, packaging, managed-runtime initialization, lookup, and
invocation through the Android ARM64 translation path. It does not establish
thread affinity or repeated-GC safety; those are `jolt-android-d06.3`.
