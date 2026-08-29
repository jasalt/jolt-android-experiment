# Observed 2026-08-29

Host:

```text
Darwin mbp14 23.6.0 Darwin Kernel Version 23.6.0: Tue Feb 24 20:49:46 PST 2026; root:xnu-10063.141.1.711.7~1/RELEASE_ARM64_T6000 arm64 arm Darwin
arm64
```

The flake's `aarch64-darwin` SDK composition evaluated and built with the
pinned `android-nixpkgs` input. Its Nix shell set:

```text
ANDROID_HOME=/nix/store/...-android-sdk-env/share/android-sdk
ANDROID_NDK_ROOT=/nix/store/...-android-sdk-env/share/android-sdk/ndk/29.0.14206865
adb: Android Debug Bridge 1.0.41, version 37.0.1
emulator: 37.2.6.0
```

The NDK directory contained `darwin-x86_64` host tools. Gradle and CMake used
them successfully on this Apple Silicon host; no claim about their host-native
architecture is made.

```sh
nix --extra-experimental-features 'nix-command flakes' flake check --all-systems --no-build
nix --extra-experimental-features 'nix-command flakes' develop -c gradle --no-daemon :app:assembleDebug
```

The Android build completed:

```text
> Task :app:configureCMakeDebug[arm64-v8a]
> Task :app:buildCMakeDebug[arm64-v8a]
> Task :app:assembleDebug
BUILD SUCCESSFUL in 1m 16s
```

The APK contained:

```text
lib/arm64-v8a/libjolt_probe.so
lib/arm64-v8a/libjoltpoc.so
```

`emulator -accel-check` reported Hypervisor.Framework OS X 14.8 with acceleration
available. `scripts/emulator-start headless` created the API 35 Google APIs
ARM64 AVD and selected `-gpu host`, then exited before boot because the host had
8.7 GiB free and the emulator required 12 GiB for userdata:

```text
FATAL | Not enough space to create userdata partition. Available: 8739.59 MB ... need 12288.00 MB.
```

The Nix SDK/NDK Android build workflow is therefore observed working on native
Apple Silicon. Emulator boot, APK installation, and UI validation remain blocked
on this host until sufficient disk space is available.
