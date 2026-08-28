# Required changes classification

| Requirement | Evidence | Classification |
| --- | --- | --- |
| Permit `--target` with `--library` | Explicit guards in `jolt.main` and `build.ss` reject it. | **B — generic cross-compilation capability missing** |
| Make the library branch run the spawned target-xpatch compile/boot path | `build-shared` has library stub generation but no target pack/xpatch inputs. | **B — generic cross-compilation capability missing** |
| Select target compiler rather than literal host `cc` for shared link | `build-shared` hard-codes `cc`; cross binary path uses `bld-cc`. | **B — generic cross-compilation capability missing** |
| Use target CSV, static compression archives, and target link flags in shared link | Cross binaries use `bld-csv-dir`/`bld-link-libs`; library branch is host-oriented. | **B — generic cross-compilation capability missing** |
| Supply Android/Bionic target-pack link flags `-llz4 -lz -lm -ldl` | EXP-003 target Chez link failed with generic Linux `-lrt -lpthread`; Bionic build succeeded after removing them. | **C — Android-specific runtime/toolchain assumption** |
| Keep Chez kernel PIC | Android JNI shared-object link rejected non-PIC AArch64 relocations; EXP-003 succeeded with `-fPIC`. | **D — Chez build configuration for Android** |
| Package `petite.boot` and `scheme.boot` losslessly | Android asset copies in this shared workspace corrupted `scheme.boot`; symlink-backed Gradle assets preserved its SHA-256. | **F — packaging limitation** |
| Preserve Jolt library thread-affinity contract once cross build works | Existing library stub calls `Sscheme_init`/`Sscheme_start`; Android callers need a dedicated runtime thread. | **C — Android integration design requirement** |

No evidence currently attributes the cross-library guard to a Chez limitation.
