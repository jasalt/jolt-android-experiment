# Voxel Siege performance boundary

The complete native gameplay phases are not yet available in the gallery scene;
this report therefore combines measured shared-host and Voxel model facts
without inventing impact timing.

## Measured host bounds

- Direct scalar Raylib FFI: 10,000 calls max `0.499682 ms` in five runs
  (3.00% of a 16.67 ms frame); see `RAYLIB-FFI-REPORT.md`.
- Android baseline gallery: 15 minutes with no fatal/ANR signatures; native
  PSS `8,792 -> 4,708 KiB`, total PSS `335,485 -> 328,162 KiB`.
- Deliberate 10,000-element vector allocation: 15 minutes with no fatal/ANR
  signatures; native PSS `8,912 -> 4,780 KiB`, total PSS `374,140 -> 360,237 KiB`.
- Pure Voxel fixture: 75 deterministic castle cells, five shots, bounded
  `0.033 s` simulation delta and 240-cell render fixture capacity.

![Voxel scene performance visual context](assets/voxel-scene-live.png)

The image is embedded directly and is a runtime scene-shell capture. It is not
an impact-FPS measurement.

## Unmeasured phases and decision

Cold entry, charging, flight, explosion, maximal rubble, actual Box3D body
counts, Voxel-specific rlgl counts, and native-device measurements remain
unmeasured because the current gallery scene uses the procedural shell and the
sandbox has no native ARM64 device. Do not add batching or reduce visual
fidelity from the host-only numbers above. Collect game-specific counters once
the destructible Box3D scene is connected.
