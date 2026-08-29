# Native Apple Silicon Android workflow boundary

Verify the full project development workflow on native `aarch64-darwin` Nix
without using host-installed Android tools. The portable-core portion must pass;
the Android assembly result establishes whether the declared macOS shell provides
the Android SDK.

See [actual.md](actual.md) for exact commands and results.
