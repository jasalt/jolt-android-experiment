# Voxel Siege provenance

This record governs the planned Voxel Siege gallery adaptation. It is a
reproducibility and attribution record, not a substitute for a copyright
license.

## Upstream inputs

| Input | Immutable revision | Role | License/provenance status |
| --- | --- | --- | --- |
| `yogthos/voxel-siege` | `9d285987282b26b4dbd14a06838b01ed70c989f1` | Game model, voxel mesh, rendering design and regression fixtures | The author has confirmed willingness to collaborate and make the code freely licenseable for this demo. The upstream snapshot has no top-level license file; obtain the author's written license/permission and attribution text before distributing adapted source or APKs. |
| `erincatto/box3d` | `8441b4a06d6d09dcfb0b0f704df4d847d1437b92` (`v0.1.0`) | Physics implementation linked by the Voxel C shim | MIT; license text is present in the pinned upstream checkout. API compatibility with the adapted shim remains a build/probe acceptance gate. |

The Box3D revision is pinned here and in `raylib/pins.edn`; it replaces the
upstream floating `HEAD` clone. The commit was resolved from the `v0.1.0`
tag on 2026-08-30. Do not update it without rerunning the API and license
checks in Bead `jolt-android-lfu.6.14.3`.

## Consumption strategy

Use selective adaptation into the Raylib gallery rather than copying the
upstream `-main` loop or opening another window. Preserve the pure world,
mesh, terrain and input rules where tests demonstrate compatibility; put
Raylib values, Box3D IDs, native buffers and lifecycle state behind the gallery
scene's owner-affine adapters. The procedural renderer needs no image, font,
model or audio runtime assets. `img/voxel-siege.png` is documentation only and
must not be packaged as a runtime dependency.

Required notices must distinguish the Voxel author, Box3D MIT, Raylib zlib,
raylib-jlt zlib, Jolt project license and this repository's MIT license. The
final APK/source distribution must include the approved Voxel permission or
license and attribution alongside those notices.

## Open provenance gate

The collaboration confirmation enables implementation and compatibility
experiments. Distribution remains gated until the author supplies one of:

1. an upstream license file at the pinned revision; or
2. written permission naming this repository/demo, allowed adaptation and
   distribution channels, plus attribution requirements; or
3. a new approved license commit from the author.

Record the evidence and exact wording here before closing the provenance Bead.
No claim that Voxel Siege is currently redistributable is made by this record.

## Evidence

- Upstream snapshot reviewed: `9d285987282b26b4dbd14a06838b01ed70c989f1`.
- Box3D tag and commit resolved: `v0.1.0` / `8441b4a06d6d09dcfb0b0f704df4d847d1437b92`.
- Runtime assets/audio observed: none; procedural content only.
- Author collaboration/free-licensing intent: user-provided project authorization,
  pending durable written permission or license evidence.
