# Native Apple Silicon Android Nix build

Verify that the pinned Nix flake supplies the Android SDK/NDK build workflow on
native Apple Silicon macOS without Android Studio or host-installed SDK tools.
The emulator is checked independently because package availability does not
prove an AVD can boot on the current host.

See [actual.md](actual.md) for commands and observed results.
