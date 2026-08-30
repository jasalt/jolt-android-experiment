# Adaptive gallery shell and navigation

## Result

The existing single Jolt-owned Raylib loop now starts in a touch-first gallery
with six statically registered placeholder scenes: Following Eyes, Touch Trail,
Flappy Bird, Virtual Controls, Touch Diagnostics, and Gesture Diagnostics.
Cards and the scene Back target are derived from live screen metrics. A primary
pointer press opens a card; canvas Back disposes the active scene and returns to
the gallery; Android Back does the same in a scene and requests orderly close
only when already in the gallery. The host performs one window/runtime init and
shutdown pair.

## Reproduce

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  env JOLT_SOURCE="$JOLT_SOURCE" \
  experiments/RAY-013-gallery-shell/commands.sh
```

`JOLT_SOURCE` must be a clean checkout at the revision required by the Raylib
Android build scripts. The script builds the ARM64 managed library and APK,
then exercises the installed API-35 emulator with portrait, small-portrait,
and landscape framebuffer sizes.

## Evidence

- [`evidence/pure-tests.txt`](evidence/pure-tests.txt): 12 pure tests and 53
  assertions pass, including the gallery lifecycle contract and adaptive card
  geometry/hit testing.
- [`evidence/aot-build.txt`](evidence/aot-build.txt): the Jolt ARM64 managed
  library and NativeActivity APK build pass.
- [`evidence/portrait-navigation.log`](evidence/portrait-navigation.log): all
  six scene IDs are selected and returned through canvas Back; the runtime has
  one init/shutdown pair.
- [`evidence/android-back.log`](evidence/android-back.log): Android Back from
  a scene returns to gallery, then Android Back from gallery requests close.
- [`evidence/small-portrait.log`](evidence/small-portrait.log) and
  [`evidence/landscape.log`](evidence/landscape.log): live metrics and scene
  selection at 720x1280 and 2400x1080.
- PNG evidence shows the gallery, scene, canvas return, and both size/orientation
  variants. Internal selected-scene state is taken from the log rather than
  OCR.

This task supplies the shell and navigation only. The registered scene bodies
remain deliberately bounded placeholders for their separate adaptation tasks.
