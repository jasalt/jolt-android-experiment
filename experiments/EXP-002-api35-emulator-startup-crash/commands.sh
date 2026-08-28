#!/usr/bin/env bash
# Run from the repository root, using the committed flake.lock.
set -euo pipefail

nix develop -c ./scripts/emulator-start headless
nix develop -c ./scripts/emulator-start software
