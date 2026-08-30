#!/usr/bin/env bash
# Linux direct aggregate matrix. Android failure reproduction is intentionally
# documented rather than enabled because it must not destabilize the gallery.
set -euo pipefail

root=$(cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$root"
nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/raylib-abi-verify-linux | tee \
  experiments/RAY-012-android-aggregate-abi/evidence/linux-direct.txt
