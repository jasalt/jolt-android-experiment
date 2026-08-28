# Android Nixpkgs SDK build failure

## Problem

The pinned `android-nixpkgs` stable input cannot construct its immutable Android
SDK environment on this Linux host because the newly packaged Android CLI
wrapper cannot execute its `.android-wrapped` binary.

## Environment

- Host: Fedora 44 x86_64 Lima VM, kernel `6.19.10-300.fc44.x86_64`
- Nix: `2.34.8`, flakes and sandboxing enabled
- `flake.lock` Android input: `tadfisher/android-nixpkgs` stable,
  `6b4cca34c57b18f256c04c7c83db73ca7134eceb`
- `flake.lock` Nixpkgs input:
  `9fbb54b33e91ee4ca368e35a78e0613c720600b3`

## Minimal reproduction

From a clean checkout containing this repository's `flake.nix` and
`flake.lock`:

```sh
nix develop -c true
```

The SDK composition includes Android API 35, the `google_apis` x86_64 image,
the emulator, platform tools, build tools 35.0.0, CMake 3.22.1, and NDK r29.

## Expected

Nix constructs `android-sdk-env` and enters a shell containing `adb`,
`emulator`, and `avdmanager`.

## Actual

The build fails while `android-sdk-env` runs the command-line tools' `android`
program. See [actual.txt](actual.txt).

## Investigation

Observed only. The error is emitted by the `android-nixpkgs` SDK environment
derivation, after all requested Android archives have been fetched and before
the project bootstrap script runs. It is not an AVD, KVM, emulator, Gradle, or
Jolt failure.

## Workaround

Resolved locally and reproducibly; see [resolution.txt](resolution.txt). The
repository patches the new CLI executable's ELF interpreter and composes the
same pinned SDK packages without the upstream mutable-state verification probe.
It does not use a host-installed SDK.

## Suspected layer

`android-nixpkgs` packaging and SDK composition for the current Android
command-line-tools Android CLI on Linux.

## Upstream suitability

Yes, after the failure has been reduced to the smallest input/package set and
an upstream issue search confirms it is not already known.
