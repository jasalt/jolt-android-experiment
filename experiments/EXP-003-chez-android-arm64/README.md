# Chez Scheme Android ARM64 JNI probe

## Problem

Determine whether standalone Chez Scheme can be cross-built for Android ARM64,
linked into an ARM64 JNI library, initialized in an API-35 x86_64 emulator
process through ABI translation, and evaluate a Scheme expression.

## Minimal reproduction

```sh
nix develop -c ./scripts/chez-cross-build
nix develop -c gradle :app:assembleDebug
nix develop -c ./scripts/emulator-start headless
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -W -n net.joltlang.androidpoc.abiprobe/.MainActivity
```

The JNI bridge registers unpacked `petite.boot` and `scheme.boot`, calls
`Sscheme_init`, `Sbuild_heap`, calls the top-level Scheme `+` procedure with 40 and 2, and returns the
fixnum to Java. Jolt is intentionally not involved.

## Result

Observed successful on 2026-08-28. See `actual.txt`.
