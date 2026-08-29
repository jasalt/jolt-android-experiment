#!/usr/bin/env bash
# Run from the repository root through the pinned shell:
#   nix develop -c ./experiments/EXP-021-x86-64-linux-gtk-host/commands.sh
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
work_dir=$(mktemp -d "${TMPDIR:-/tmp}/jolt-glimmer-gtk.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT

glimmer_gtk_revision=ce79d45698d36ccf496397bb85974e3cce6abfd8

printf 'kernel: '
uname -a
printf 'machine: '
uname -m
printf 'Nix: '
nix --version
printf 'Jolt: '
jolt --version
printf 'GTK4: '
pkg-config --modversion gtk4
printf 'GLib: '
pkg-config --modversion glib-2.0
printf 'display: %s\n' "${DISPLAY:-<unset>}"

# The Jolt FFI must resolve glimmer-gtk's GTK4/GLib library names from the
# pinned Nix store. flake.nix sets this for Linux development shells.
printf 'LD_LIBRARY_PATH: %s\n' "${LD_LIBRARY_PATH:-<unset>}"
[[ "${LD_LIBRARY_PATH:-}" == *gtk4* ]] || {
  printf 'GTK4 library path is absent from LD_LIBRARY_PATH\n' >&2
  exit 1
}

# Keep the upstream backend source outside the project and pin it to the exact
# revision that was observed. Its deps.edn pins glimmer independently.
git clone --quiet https://github.com/jolt-lang/glimmer-gtk.git "$work_dir"
git -C "$work_dir" checkout --quiet --detach "$glimmer_gtk_revision"

git -C "$work_dir" rev-parse HEAD
cd "$work_dir"
jolt test
DISPLAY="${DISPLAY:?set a display; the Lima Xvfb service uses :99}" \
  timeout --signal=TERM --kill-after=5s 60s jolt smoke

# The project portable suite remains the shared-core baseline.
cd "$repo_root"
./scripts/test-portable
