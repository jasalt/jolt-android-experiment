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
artifact contract. The optional APK probe is reproducible with
`BOX3D_SOURCE=/path/to/pinned/box3d VOXEL_SOURCE=/path/to/voxel-siege` before
running `scripts/raylib-persistent-loop-build-android debug`; without those
variables the normal gallery build does not acquire Box3D.

## Emulator evidence

The API-35 emulator exposes a Game Rotation Vector and Rotation Vector sensor in
`sensorservice`; the emulator reports 12 hardware/synthetic sensors. The debug
APK containing the adapter rebuilt successfully and installed/launched as
`net.joltlang.raylibgallery`. This is host/gallery evidence only: Voxel Siege
has not yet been registered as a scene, so the adapter was not started by a
scene and no sensor sample is claimed.

[API-35 emulator gallery frame (screenshot)](evidence/voxel-sensor-emulator-gallery.png)

The captured 1080x2400 frame has SHA-256
`6ef63f3b7e67f6218bd083f79022d7d472c0db9086e91222c5399b37fd775eda`.

The bootstrap smoke log records `available=1`, `sample=1`, timestamp
`126892332512895`, and quaternion `0.478851,-0.478851,-0.520290,0.520290`
on owner thread `24063`; start/stop returned cleanly. The bounded poll waits
up to 250 ms for the first event and never calls Jolt from a sensor callback.

With the optional pinned Box3D source enabled, the owner-thread log records
`voxel Box3D probe world=65536 steps=10 thread=24541`. The Jolt library then
looked up `raylib_voxel_native_probe` and recorded `Jolt FFI Box3D probe
result=1 thread=24541 owner=24541`. Box3D, the Voxel pointer/scalar shim and
the Jolt FFI call therefore execute on the same Android owner thread. The Jolt scalar probe now advances the world for 10,000 steps before destroy;
the direct C bootstrap probe remains a ten-step startup check. This is still
not a full gameplay physics stream;
the adapter remains scene-scoped when wired into Voxel Siege.

## Runtime boundary

This sandbox has no `adb` executable or running Android emulator/device, so the
translated ARM64 Jolt call, APK packaging, and native-device result are not
claimed. The next probe must package this target in the established
NativeActivity topology, run the scalar world/body/recycle oracle on ARM64,
and compare it with the desktop `b3probe` result. A separate shared object is
not assumed until that topology is tested; linking the shim and Box3D into
`libmain.so` remains the preferred first experiment.

[Voxel Siege upstream visual reference (screenshot)](evidence/voxel-siege-upstream-baseline.png)

The embedded image is documentation evidence only, not an Android runtime
asset.
