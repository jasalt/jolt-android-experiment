# Raylib Linux/Android parity report

## Shared source evidence

The shared pure gallery/model tests run from `raylib/` with the pinned Jolt shell:

```text
Ran 38 tests. 170 assertions passed, 0 failures, 0 errors.
```

The pinned Linux aggregate ABI script also passes all expected layouts and
return-value checks. The Voxel scene model, control routing and gallery
orientation events are platform-independent; NativeActivity orientation,
sensors and Box3D process symbols remain host adapters.

## Android evidence

The Android ARM64 debug gallery builds and launches on the API-35 emulator. A
live Voxel scene-shell frame and the landscape orientation frame are embedded
below rather than referenced only by links.

[Android Voxel scene shell (screenshot)](evidence/voxel-scene-live.png)

[Android landscape host (screenshot)](evidence/voxel-landscape-orientation.png)

## Linux boundary

The Linux minimal Raylib smoke was attempted but this sandbox's Xvfb/GLX
configuration returned `GLX: No GLXFBConfigs returned`; the upstream loop then
reported an invalid memory reference while no window existed. Consequently
this report does not claim Linux rendered-frame parity or a desktop screenshot.
The Linux aggregate ABI and pure shared-state tests pass; graphical Linux
parity remains an explicit environment-gated task.
