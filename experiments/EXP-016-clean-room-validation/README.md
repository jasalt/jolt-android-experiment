# Clean-room validation

The documented baseline was rerun from a clean generated Android/Gradle/CMake
state. `JOLT_SOURCE` explicitly pointed to the sibling Jolt checkout at the
pinned revision; the build script itself creates a temporary worktree and
initializes its submodules.

See [actual.md](actual.md) and
[`artifacts/logs/exp016-clean-room-validation.txt`](../../artifacts/logs/exp016-clean-room-validation.txt).
