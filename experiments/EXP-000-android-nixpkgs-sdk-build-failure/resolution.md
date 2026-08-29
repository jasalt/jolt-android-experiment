## Resolution

The pinned command-line-tools 23 native `android` executable retained the host
ELF interpreter. `flake.nix` patches that executable with the Nix dynamic
linker. The upstream SDK composition then fails because its `sdkmanager --list
--verbose` probe is redirected through the new Android CLI, which tries to
create mutable state during the read-only Nix build.

`nix/android-sdk.nix` is a deliberately narrow copy of the upstream SDK
composition. It keeps the selected immutable package layout, generated Android
licenses, setup hook, and package wrappers, but removes only the incompatible
mutable-state probe. It is documented for removal when android-nixpkgs supports
this command-line-tools release.

Validation:

```text
$ nix develop -c ./scripts/bootstrap
Wrote /home/user/dev/jail/jolt/jolt-android/artifacts/reports/environment.txt
```

The resulting report confirms API 35 components, NDK r29, the Android emulator,
adb, avdmanager, and usable KVM acceleration are supplied by the Nix shell.
