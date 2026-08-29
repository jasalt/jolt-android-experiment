#!/usr/bin/env bash
# Reproduce the pinned raylib-jlt x86_64 Linux baseline from the repository root.
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
work_root=${RAYLIB_JLT_WORK_ROOT:-/tmp/raylib-jlt-linux-baseline}
source_revision=15c4c6d5757c5c592983166626fd32341c6fc45e

: "${RAYLIB_JLT_SOURCE:?run via the repository Nix shell}"
: "${RAYLIB_LIBRARY_PATH:?run via the repository Nix shell}"

if [[ ! -d /lib64 || ! -r /lib64/libGLX.so.0 ]]; then
  printf 'This recorded Linux visual run requires the host GLVND/Mesa /lib64 implementation used by DISPLAY=:99.\n' >&2
  exit 1
fi

rm -rf "$work_root"
mkdir -p "$work_root"/{logs,screenshots}
# The flake establishes the immutable revision; clone that exact object into a
# writable disposable worktree because raylib's screenshot API writes to CWD.
git clone https://github.com/jlt-commons/raylib-jlt.git "$work_root/source" \
  >"$work_root/logs/clone.log" 2>&1
git -C "$work_root/source" checkout --detach "$source_revision" \
  >>"$work_root/logs/clone.log" 2>&1
git -C "$work_root/source" rev-parse HEAD >"$work_root/revision.txt"

cd "$work_root/source"
jolt -M:check | tee "$work_root/logs/headless-check.log"

# raylib-jlt uses `libraylib.so.6`, supplied by the pinned Raylib package.
# Its TakeScreenshot binding writes to CWD, so run in the writable checkout and
# move the final image to the evidence directory after every successful alias.
for spec in run:basic input:input shapes:shapes text:text texture-tiling:texture camera2d:camera2d; do
  alias=${spec%%:*}
  name=${spec#*:}
  rm -f "$name.png"
  DISPLAY=:99 LD_LIBRARY_PATH="/lib64:$LD_LIBRARY_PATH" \
    RAYLIB_APP_AUTO_QUIT_MS=1200 RAYLIB_APP_SHOT="$name.png" \
    jolt -M:"$alias" >"$work_root/logs/$name.log" 2>&1
  test -s "$name.png"
  mv "$name.png" "$work_root/screenshots/"
done

# Record audio accurately: this pin has no audio alias or direct audio binding.
if jolt -M:audio >"$work_root/logs/audio.log" 2>&1; then
  printf 'unexpected :audio alias success; inspect and update the baseline evidence\n' >&2
  exit 1
fi

test "$(cat "$work_root/revision.txt")" = "$source_revision"
sha256sum "$work_root"/screenshots/*.png >"$work_root/screenshots/SHA256SUMS"
printf 'Evidence written to %s\n' "$work_root"
