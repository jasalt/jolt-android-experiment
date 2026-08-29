# Cross-library requirements classification

| Requirement | Evidence | Classification |
| --- | --- | --- |
| Permit `--target` with `--library` | Jolt PR #778 (pinned at `ae5c5a6d`) provides this capability. | Upstream generic cross-compilation capability |
| Build the library through target xpatch/CSV/compiler paths | Jolt PR #778 makes `build-shared` use the target xpatch, compiler, CSV, and platform linker path. | Upstream generic cross-compilation capability |
| Link the Android target with target archives and Bionic flags | The Android target uses PIC Chez, static lz4/zlib, and `-llz4 -lz -lm -ldl`. | Android toolchain/runtime configuration |
| Expose explicit heap compaction | EXP-005 adds the isolated `jolt_library_collect` wrapper around Chez `Scompact_heap`. | Experiment-only Jolt ABI extension |
| Package generated library in ABI-specific APK layout | Gradle packages `native/jolt/android-arm64/arm64-v8a/libjoltpoc.so`. | Packaging configuration |
| Serialize runtime entry | EXP-006 routes calls through `HandlerThread("JoltRuntime")`; the JNI bridge records/rejects non-owner entry. | Android integration design |

Only the EXP-005-specific `jolt-library-collect.patch` is applied in a
temporary worktree by `scripts/jolt-android-library-build`; the pinned Jolt
checkout is not modified. Cross-library compilation itself is upstream.
