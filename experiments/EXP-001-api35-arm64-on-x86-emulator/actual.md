## Native artifact — observed 2026-08-28

```text
/home/user/dev/jail/jolt/jolt-android/artifacts/abi-probe/libabi_probe.so: ELF 64-bit LSB shared object, ARM aarch64, version 1 (SYSV), dynamically linked, for Android 35, built by NDK r29 (14206865), not stripped
Machine:                           AArch64
4: 00000000000045a0     8 FUNC    GLOBAL DEFAULT   12 poc_answer
```

The pinned NDK can therefore produce the required minimal Android ARM64 shared
object. This does not test Android loading or translation.

## APK packaging — observed 2026-08-28

The initial Gradle invocation occurred while `app/build.gradle.kts` was empty
due to an interrupted file write and is not evidence of a repository-resolution
problem. With the complete project configuration, AGP `8.8.2` resolved through
`google()`. AGP initially requested immutable NDK 27; setting
`ndkVersion = "29.0.14206865"` selected the pinned NDK r29 instead.

From an empty project Gradle cache:

```text
$ nix develop -c gradle :app:assembleDebug
BUILD SUCCESSFUL
35 actionable tasks: 35 executed
$ unzip -l app/build/outputs/apk/debug/app-debug.apk
4248 lib/arm64-v8a/libabi_probe.so
```

The APK has only the intended ARM64 native library. Install, load, and
`poc_answer() = 42` remain to be observed on the API 35 x86_64 emulator.

## Translation execution — observed 2026-08-28

The API 35 x86_64 emulator reported primary ABI `x86_64` and ABI list
`x86_64,arm64-v8a`. The ARM64-only APK installed and its activity launched
successfully. Logcat recorded:

```text
Load .../base.apk!/lib/arm64-v8a/libabi_probe.so ...: ok
```

The active UI hierarchy contained exactly:

```text
text="poc_answer() = 42"
```

Therefore Android translated and executed the minimal ARM64 JNI library in an
API 35 x86_64 emulator process. This proves the ABI-translation prerequisite
only; it does not prove Chez or Jolt compatibility.
