# Voxel Siege Android Box3D probe

Probe date: 2026-08-30. Sources were the Voxel Siege revision recorded in
[`RAYLIB-VOXEL-SIEGE-PROVENANCE.md`](RAYLIB-VOXEL-SIEGE-PROVENANCE.md) and
Box3D `v0.1.0`.

## Cross-build result

Box3D was configured with the pinned NDK toolchain, `ANDROID_ABI=arm64-v8a`,
and `ANDROID_PLATFORM=android-35`, with samples, tests, benchmarks and docs
disabled. The static library and pointer/scalar shim linked successfully.

```text
ELF64, Machine: AArch64, Android 35
NEEDED: liblog.so, libdl.so, libc.so
exported shim symbols: vb3_world_create, vb3_world_step,
  vb3_world_explode, vb3_body_create, vb3_body_transform, ...
shim SHA-256: 1b6e4847f22ca791c8e58981d14c65239ca8134f1112b3a5e132835bc498fccb
```

The generated `.so` is intentionally not committed. The command used the
Nix-provided `ANDROID_NDK_ROOT`, so no machine-local SDK path is part of the
artifact contract.

## Runtime boundary

This sandbox has no `adb` executable or running Android emulator/device, so the
translated ARM64 Jolt call, APK packaging, and native-device result are not
claimed. The next probe must package this target in the established
NativeActivity topology, run the scalar world/body/recycle oracle on ARM64,
and compare it with the desktop `b3probe` result. A separate shared object is
not assumed until that topology is tested; linking the shim and Box3D into
`libmain.so` remains the preferred first experiment.

![Voxel Siege upstream visual reference](assets/voxel-siege-upstream-baseline.png)

The embedded image is documentation evidence only, not an Android runtime
asset.
