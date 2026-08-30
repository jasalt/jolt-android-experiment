# Raylib Android stress report

Target: `net.joltlang.raylibgallery`, API-35 ARM64 emulator, debug
NativeActivity build.

## Executed slice

A 30-second baseline gallery run included one Home/background and resume, then
continued rendering. `dumpsys meminfo` and logcat were captured before/after.
No `FATAL EXCEPTION`, `SIGABRT`, `Fatal signal`, or `ANR in` signatures occurred.

| Metric | Start | End |
| --- | ---: | ---: |
| Native heap PSS | 8,820 KiB | 9,044 KiB |
| Java heap PSS | 528 KiB | 516 KiB |
| Total PSS | 335,821 KiB | 353,999 KiB |
| Total RSS | 488,168 KiB | 510,368 KiB |

![Raylib stress run after resume](assets/raylib-stress-resume.png)

Screenshot SHA-256:
`56681e30656ddb6d7621326e5bb8f4c4c5a36bc290c804600e9964e7876320fe`.

## Boundary

This is a reproducible 30-second baseline slice, not completion of the
required 5-minute/15-minute matrix or all three allocation workloads. No FPS
histogram or collector-specific claim is made. Those longer runs remain open
work and must be collected separately rather than inferred from this sample.
