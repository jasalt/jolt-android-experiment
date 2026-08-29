# ADR-0004: Provide Android Nix tooling on Apple Silicon

- **Status:** Accepted
- **Date:** 2026-08-29

## Context

The original macOS workflow was deliberately limited to portable Jolt CLI and
nREPL work. That limitation prevented an Apple Silicon developer from assembling
the Android application through the project's pinned dependency boundary.

The pinned `android-nixpkgs` input supports `aarch64-darwin` and supplies the
API 35 SDK, NDK r29, platform tools, emulator, and ARM64 Google APIs image.
This project must not require an Android Studio-managed or otherwise
host-installed SDK to make the macOS Android build work.

## Decision

Expose the same immutable Android SDK composition on `aarch64-darwin` and
`x86_64-linux`. Select the host-native API 35 system image: ARM64 on Apple
Silicon and x86_64 on Linux. Export `ANDROID_HOME`, `ANDROID_SDK_ROOT`,
`ANDROID_NDK_ROOT`, `ANDROID_AVD_HOME`, and `GRADLE_USER_HOME` from the Nix
shell on both systems.

Make `scripts/emulator-start` select the host architecture's image and default
GPU mode. Continue to treat a successful emulator boot, APK install, and UI
workflow as separate observed claims.

## Consequences

- Apple Silicon developers can run the portable workflow, inspect/build Android
  artifacts, use `adb`, and invoke the emulator with only Nix-managed tooling.
- The Android build no longer has an implicit Linux-only dependency.
- GTK remains a Linux reference-host workflow.
- The pinned NDK's Darwin host tools are currently under `darwin-x86_64`; their
  successful execution is observed but must not be described as native ARM64
  tooling without separate proof.
- Emulator success still depends on host resources and must not be inferred from
  package availability or an acceleration check.

## Validation

On native Apple Silicon macOS, evaluate the flake, run the portable suite, and
run `gradle --no-daemon :app:assembleDebug` from the Nix shell. Confirm the APK
contains the expected ARM64 libraries. Run `emulator -accel-check` and start the
host-native AVD; preserve any boot failure with the host's resource evidence.
