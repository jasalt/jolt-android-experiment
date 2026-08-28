#!/usr/bin/env bash
# Run from the repository root, using the committed flake.lock.
set -euo pipefail
nix develop -c true
