#!/usr/bin/env bash
# Repository-local Gradle entrypoint. The pinned Gradle is supplied by the Nix shell.
set -euo pipefail
exec nix develop "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)" -c gradle "$@"
