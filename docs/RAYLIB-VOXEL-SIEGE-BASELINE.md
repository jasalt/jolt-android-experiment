# Voxel Siege desktop baseline

Baseline source: `yogthos/voxel-siege` at
`9d285987282b26b4dbd14a06838b01ed70c989f1`. Box3D was checked out at
`v0.1.0` / `8441b4a06d6d09dcfb0b0f704df4d847d1437b92`.

## Validation

Run from the clean upstream checkout with the repository's pinned Jolt shell:

```sh
nix develop /home/user/dev/jail/jolt/jolt-android -c jolt -M:test
nix develop /home/user/dev/jail/jolt/jolt-android -c jolt -M:b3probe
```

Results on 2026-08-30:

- Pure and integration tests: **45 tests, 240 assertions, 0 failures, 0 errors**.
- `b3probe`: **PASS** — gravity/contact, sleep, explosion, ballistics, world
  recycle and physics restart all completed.
- The current checkout reports more tests than the earlier upstream review;
  the executed result above is authoritative for this pinned snapshot.

The native probe required Box3D `v0.1.0` and a locally built `native/libvoxel_b3.so`.
No host library or generated native binary is committed to this repository.

## Interactive baseline limitation

The normal game entry was attempted with `jolt -M:run`. Raylib initialized, but
this sandbox has no supported display backend (`GLFW: Failed to initialize
GLFW`), then the upstream loop raised an invalid-memory-reference exception at
`src/voxel/main.clj:177`. Therefore no interactive FPS, auto-fire, or runtime
screenshot is claimed here. This is a host/display limitation to reproduce in
a graphical Linux environment before gallery adaptation, not a test failure
that should be hidden.

## Upstream reference screenshot

This is the upstream documentation screenshot, copied for local evidence. It
is not a runtime APK asset and is not used by the planned gallery scene.

![Voxel Siege upstream reference screenshot](assets/voxel-siege-upstream-baseline.png)

SHA-256: `755cdd7d5f8297d2bc5bb32c3f5cef8a31473f2f07258846b2b02593578b1b68`

## Behavioral baseline to preserve

The game uses absolute mouse aiming, LMB hold/release charging, `R` restart,
five shots, destructible voxel terrain, and a 70% destruction win threshold.
These behaviors remain characterization requirements for the gallery scene;
interactive verification is still outstanding because this environment cannot
create a desktop window.
