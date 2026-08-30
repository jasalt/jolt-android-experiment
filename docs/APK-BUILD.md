# Locally signed Raylib gallery APK

This document describes how to produce a directly installable, locally signed
release APK for the Raylib gallery. It is for GitHub prereleases or other direct
distribution; it does **not** require a Play Store account or a certificate
authority. Android application certificates are normally self-signed.

The release script builds the pinned Jolt/Raylib native library, packages an
unsigned Android release variant, aligns and signs the APK, verifies its
signature, and writes only public distribution artifacts to `dist/`.

## TL;DR — build an installable APK now

From this checkout, the following single command runs in the current pinned Nix
environment and produces an installable **debug** APK. It needs no release
keystore and is appropriate for an immediate internal preview:

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  gradle --no-daemon :raylib-loop-android:assembleDebug
```

The output is:

```text
raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk
```

It is signed by the local Android debug key and can be sideloaded, but it is not
a stable public-release identity. Use the [Build a release APK](#build-a-release-apk)
procedure below when a private release keystore is available and testers need
reliable in-place updates.

## Compatibility and current evidence boundary

The gallery release module currently declares:

```kotlin
applicationId = "net.joltlang.raylibgallery"
minSdk = 35
ndk { abiFilters += "arm64-v8a" }
```

A gallery APK produced by this procedure therefore requires **Android 15 (API
35) or newer on an ARM64 device**. This is intentionally not represented as a
broad Android compatibility claim. The existing automated evidence is the
API-35 x86_64 emulator's ARM64 translation path; physical-device coverage is
limited to separately reported tests.

The application ID and signing certificate form Android's upgrade identity.
Keep both stable for every future gallery update. The release application ID is
`net.joltlang.raylibgallery`; it deliberately replaces the earlier
probe-oriented `net.joltlang.raylibloopprobe` ID. An installation of the old
probe is a different application and cannot update in place.

> **Suggested release-note wording:** Requires Android 15 or newer and an ARM64
> device. Experimental prerelease. Current emulator evidence uses API-35
> x86_64 ARM64 translation; physical-device coverage is limited to explicitly
> reported tests.

## One-time key setup

Create and retain a private keystore **outside this repository**. The following
uses the JDK `keytool` supplied by the pinned Nix shell:

```sh
mkdir -p "$HOME/.local/share/jolt-raylib-gallery"

nix --extra-experimental-features 'nix-command flakes' develop -c \
  keytool -genkeypair \
    -keystore "$HOME/.local/share/jolt-raylib-gallery/release.jks" \
    -alias gallery \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000
```

`keytool` prompts for a keystore password, certificate identity metadata, and,
optionally, a separate key password. The identity is self-signed certificate
metadata; no external organization needs to certify it.

Securely back up all of the following together:

- `release.jks`;
- its keystore password;
- the key alias (`gallery` in the example);
- the key password, if separate from the keystore password.

The same key must sign every future APK using this application ID. If it is
lost, users cannot install a future APK as an update to an existing
installation. Never commit or publish the keystore, passwords, or a Gradle
signing-properties file. Do not attach the keystore to a GitHub release.

## Build a release APK

The Nix shell supplies the JDK, Build Tools (`zipalign` and `apksigner`),
Android SDK/NDK, Gradle, and the pinned Raylib/Jolt sources. `JOLT_SOURCE` must
point at a clean checkout at the revision enforced by the native-library build.

At minimum, configure the keystore path and alias. The script prompts securely
on its controlling terminal for missing passwords:

```sh
export ANDROID_RELEASE_KEYSTORE="$HOME/.local/share/jolt-raylib-gallery/release.jks"
export ANDROID_RELEASE_KEY_ALIAS=gallery

JOLT_SOURCE=/path/to/pinned-jolt \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/raylib-gallery-release
```

To make a deliberate artifact-name version (rather than the module's current
`versionName`), provide `--version`:

```sh
JOLT_SOURCE=/path/to/pinned-jolt \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/raylib-gallery-release --version 0.1.0
```

The version used in the output filename should match the Gradle `versionName`.
`--version` only names the distribution artifact; it does not alter APK package
metadata. Change `versionCode` and `versionName` deliberately in
`raylib/loop-android/build.gradle.kts` before a new public release.

For non-interactive local automation only, passwords may be supplied as
process-local environment variables:

```sh
export ANDROID_RELEASE_STORE_PASSWORD='...'
export ANDROID_RELEASE_KEY_PASSWORD='...'
```

Do not put those exports in a tracked file, shell startup file, command-line
argument, or CI log. Prefer the interactive prompts for a human-operated build.
The script passes password values to `apksigner` over standard input, not in
its command-line arguments or child-process environment; the script itself
does not accept passwords as command-line flags.

A successful run writes these public files, all ignored by Git:

```text
dist/jolt-raylib-gallery-v0.1.0-arm64-v8a.apk
dist/jolt-raylib-gallery-v0.1.0-arm64-v8a.apk.sha256
dist/jolt-raylib-gallery-v0.1.0-build-info.txt
```

The build-info file records the artifact hash, application ID, Android bounds,
Git/Jolt revisions, NDK directory name, and signing-tool versions. It contains
no keystore path, password, alias, or private certificate material.

## What the script verifies

`scripts/raylib-gallery-release` performs the following fail-fast sequence:

1. validates the pinned Android/Jolt/Raylib environment and readable keystore;
2. builds the Android-target Jolt Raylib loop library and stages it only for the
   gallery module;
3. runs `:raylib-loop-android:assembleRelease`, which is intentionally unsigned
   in Gradle configuration;
4. runs `zipalign -f -p 4`, signs with `apksigner`, then checks alignment;
5. runs `apksigner verify --verbose --print-certs`;
6. writes a `sha256sum` checksum and public build metadata under `dist/`.

The unsigned intermediate remains in Gradle's ignored build directory. Do not
publish it.

## Verify and publish

Before uploading, independently inspect the generated artifact if desired:

```sh
sha256sum -c dist/jolt-raylib-gallery-v0.1.0-arm64-v8a.apk.sha256

nix --extra-experimental-features 'nix-command flakes' develop -c \
  apksigner verify --verbose --print-certs \
  dist/jolt-raylib-gallery-v0.1.0-arm64-v8a.apk
```

Upload the APK, its `.sha256` file, and its build-info file as GitHub prerelease
assets. Publish the checksum in the release notes as well. Do not upload the
unsigned APK or the keystore.

## Installation for testers

A tester can download the APK and optionally compare its SHA-256 checksum,
then open it. Android will ask them to allow the browser or file manager used
for that download to **Install unknown apps**. This permission is generally
per source application on current Android versions; it can be disabled again
after installation.

Play Protect may scan or warn about an APK distributed outside Play, and Android
may identify it as an unknown developer. These prompts do not prevent normal
sideloading. Later releases install as updates only when their application ID
and signing certificate match the installed APK.

## Debug APKs

A debug APK remains useful for a disposable, small internal technical preview:

```sh
JOLT_SOURCE=/path/to/pinned-jolt \
  nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/raylib-persistent-loop-build-android
```

It is installable and is automatically signed by a local debug keystore, but it
is debuggable and the debug key is insecure by design. A different developer or
clean environment can have a different debug key; changes between keys (or from
debug to release signing) require users to uninstall first. Use the locally
signed release procedure for a prerelease that is expected to receive updates.
