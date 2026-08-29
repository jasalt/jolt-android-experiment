# Observed 2026-08-29

The host was native Apple Silicon macOS:

```text
Darwin mbp14 23.6.0 Darwin Kernel Version 23.6.0: Tue Feb 24 20:49:46 PST 2026; root:xnu-10063.141.1.711.7~1/RELEASE_ARM64_T6000 arm64 arm Darwin
arm64
nix (Nix) 2.35.1
```

The native Nix configuration leaves `nix-command` disabled by default, so every
flake command below explicitly enables it:

```sh
nix --extra-experimental-features 'nix-command flakes' flake check --all-systems --no-build
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/test-portable
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/cli --event '{:type :counter/inc}'
nix --extra-experimental-features 'nix-command flakes' develop -c jolt nrepl-server 7893
nix --extra-experimental-features 'nix-command flakes' develop -c gradle --no-daemon :app:assembleDebug
```

## Portable shell

`flake check --all-systems --no-build` evaluated all declared `x86_64-linux`,
`aarch64-linux`, and `aarch64-darwin` packages and development shells.

The native shell reported:

```text
jolt 8fcba79f8b33628af926f88032d93a1b31c24235
scheme 10.4.1
OpenJDK 21.0.11 LTS
Gradle 8.14.4
OS: Mac OS X 14.8.5 aarch64
```

`./scripts/test-portable` passed:

```text
Ran 6 tests. 13 assertions passed, 0 failures, 0 errors.
```

The direct CLI fixture produced the canonical result:

```clojure
{:model {:counter 1, :events [], :platform nil, :lifecycle nil, :worker nil,
         :notification-permission nil},
 :effects [{:type :storage/write, :key "counter", :value 1}]}
```

The nREPL server wrote `.nrepl-port` for port `7893`; a bencoded loopback
`eval` of the reducer increment event returned the expected counter `1` model
and a `done` status.

## Android boundary

The native shell did not expose `adb`, `emulator`, `sdkmanager`,
`ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `ANDROID_NDK_ROOT`. No host-installed
SDK was used as a substitute.

Accordingly, the project Android assembly command failed during Gradle project
configuration, before any Android compilation:

```text
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/Users/user/dev/jolt-android-experiment/local.properties'.

BUILD FAILED in 55s
```

This is the current explicit `aarch64-darwin` Nix-shell boundary, not evidence
of an Android source, Jolt, or Gradle defect. Android builds, emulator work, and
GTK remain Linux-only workflows under the project flake.
