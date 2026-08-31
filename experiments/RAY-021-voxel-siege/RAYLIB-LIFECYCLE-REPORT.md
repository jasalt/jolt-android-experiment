# Raylib NativeActivity lifecycle report

Target: `net.joltlang.raylibgallery` on the API-35 ARM64 emulator.

## Matrix result

The debug APK was force-stopped/launched, sent Home, resumed, force-stopped and
relaunched in three cycles. Each cycle returned to the Raylib NativeActivity;
logcat contained no `FATAL EXCEPTION`, `SIGABRT`, `signal 6` or fatal signal.
The final resumed frame was captured directly from the emulator.

```text
cycles: 3
host: one NativeActivity / one Raylib loop / one Jolt owner thread
result: PASS (no crash signatures)
resumed screenshot: 1080x2400 PNG
sha256: cfe83a2d8b4c127e7ffa09e2cee12e8799e0ae0f744e4ba7704c4aaa810598fe
```

[Raylib NativeActivity after lifecycle resume (screenshot)](evidence/raylib-lifecycle-resume.png)

This proves the exercised Home/resume/process-relaunch path for the current
host build. It does not claim physical-device behavior, surface destruction
under GPU loss, or full Voxel gameplay preservation across process death.
Those boundaries remain explicit in the Voxel acceptance work.
