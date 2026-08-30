# Raylib asset and storage report

The gallery now carries the same logical asset set in both
`raylib/assets/raylib-gallery/` and the Android APK asset tree:

- `voxel-marker.png` — 8x8 RGBA project marker.
- `DroidSans.ttf` — Droid Sans, Apache 2.0, attributed in `NOTICE`.
- `voxel-state.edn` — deterministic Voxel metadata fixture.

The Android debug APK was rebuilt with the pinned Gradle/SDK toolchain. APK
listing verification found all three assets and the notice:

```text
assets/raylib-gallery/DroidSans.ttf 190776 bytes
assets/raylib-gallery/NOTICE             191 bytes
assets/raylib-gallery/voxel-marker.png    81 bytes
assets/raylib-gallery/voxel-state.edn     84 bytes
APK SHA-256: ec1fc725f91c613b8fac2ea751f0fc17b358caf49c4349135c95b16aee42b9f3
```

![Android gallery asset packaging context](assets/voxel-gallery-registered.png)

The embedded image is a direct emulator capture. This task proves logical
packaging and attribution; it does not claim Raylib texture/font decoding or
writable-state persistence until the host asset loader is connected.
