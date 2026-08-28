# API 35 x86_64 emulator startup crash in the current Lima guest

## Problem

The pinned Android 15 / API 35 Google APIs x86_64 emulator creates the AVD and
briefly exposes `emulator-5554` through adb, but its QEMU process then
segfaults before Android reports boot completion.

## Environment

- Fedora 44 x86_64 Lima VM, Linux `6.19.10-300.fc44.x86_64`
- 3.8 GiB total guest memory available, despite `lima-vm.yaml` specifying a
  16 GiB target for a newly provisioned VM
- Nix Android emulator `37.2.6.0` (build `16138043`)
- API 35 `google_apis` x86_64 system image
- `/dev/kvm` readable; `emulator -accel-check` reports KVM version 12 usable

## Minimal reproduction

From the repository root:

```sh
nix develop -c ./scripts/emulator-start headless
nix develop -c ./scripts/emulator-start software
```

The AVD was created with an explicit `--path` below `ANDROID_AVD_HOME`; without
that option current command-line tools fail to locate the destination for the
AVD `.ini` file.

## Expected

The emulator reaches `sys.boot_completed=1`, then adb can report API level and
ABI properties.

## Actual

Both acceleration modes end in `SIGSEGV` from
`qemu-system-x86_64-headless` before boot completion. The initial headless
attempt used the default graphics selection. Subsequent attempts used the
smallest bounded mitigation: `-gpu off`, `-noaudio`, `-no-metrics`, and 1024
MiB guest RAM. The explicit `-accel off` software fallback also segfaulted.

`coredumpctl` retained cores for QEMU PIDs `57061`, `58049`, `60571`, and
`62785`. The first three are available at the time of writing; PID `60571`
shows `SIGSEGV` with a 150.1 MiB core. See [actual.txt](actual.txt).

## Investigation

Observed facts only:

- ADB listed `emulator-5554` as `device` after the GPU-off KVM invocation,
  proving the process reached a later startup point than the first attempt.
- Every invocation emitted an address-range unmap warning immediately before
  QEMU crashed.
- Kernel logs contained no host OOM kill record.
- The guest has materially less memory than the 16 GiB requested by the
  committed Lima template. The emulator itself warns that 4 GiB is the
  suggested minimum for this AVD.
- GUI mode was not testable because the running VM does not provide the
  documented `lima-x11` service or `DISPLAY=:99` desktop.

## Workaround

Unresolved in this VM. Recreate or resize the Lima VM to the documented 16 GiB
configuration, verify its virtual X11 service, and rerun these commands before
changing Android, Jolt, or Chez code. Do not substitute a host-installed SDK or
another runtime.

## Suspected layer

Current Lima VM resource/provisioning state and/or Android Emulator 37.2.6
runtime in that environment. This is not evidence of an Android native-ABI,
Chez, or Jolt limitation.

## Upstream suitability

Uncertain. Collect a symbolized QEMU backtrace after reproducing on a VM that
matches the repository's documented 16 GiB environment.

## Rerun 2026-08-28 — documented 16 GiB guest

The Lima VM was recreated to the documented `lima-vm.yaml` (16 GiB memory,
active `lima-x11` on `:99`). The original reproduction was repeated verbatim:

```sh
nix develop -c ./scripts/emulator-start headless   # KVM (-accel auto)
nix develop -c ./scripts/emulator-start software   # TCG (-accel off)
```

**Result: the crash reproduces identically.** Both modes end in `SIGSEGV` from
`qemu-system-x86_64-headless` (PIDs 19672 headless, 20601 software; cores
retained by systemd), and both emit the same protected-range unmap warning seen
in the 3.8 GiB run:

```text
WARNING: cannnot unmap ptr 0x7fffcb801000 as it is in the protected range from 0x7fff2b800000 to 0x7fffcba00000
```

The guest now reports 15.6 GiB (`MemTotal: 16357676 kB`) and `lima-x11` is up,
yet `adb` never reaches `sys.boot_completed=1`. Increasing guest RAM from 3.8
GiB to 16 GiB **did not change the outcome**. The crash lands during emulator
startup graphics init (`gfxstream` → `lavapipe`/`swangle` fallback,
`enableProtectedMemoryEmulation: false`), before Android boots.

**Disambiguation:** the suspected layer is the **Android Emulator 37.2.6
runtime** in this Fedora 44 / kernel 6.19 environment (gfxstream/lavapipe/
swangle graphics init colliding with QEMU protected-memory unmap), **not** the
Lima VM resource/provisioning state. Full evidence:
[rerun-16gib-2026-08-28.md](rerun-16gib-2026-08-28.md).

This is an observed, reproduced platform blocker, not evidence of an
Android-native-ABI, Chez, or Jolt limitation.
