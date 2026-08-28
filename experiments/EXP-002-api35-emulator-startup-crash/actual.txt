$ coredumpctl --no-pager info 60571
           PID: 60571 (qemu-system-x86)
        Signal: 11 (SEGV)
  Command Line: /nix/store/1c4zwxyjwrsvrq3rgj3938n57jdh21ga-emulator-37.2.6/qemu/linux-x86_64/qemu-system-x86_64-headless @jolt_api_35_x86_64 -port 5554 -no-window -no-snapshot -no-snapshot-save -no-boot-anim -noaudio -no-metrics -gpu off -memory 1024 -accel auto
  Size on Disk: 150.1M

$ emulator -accel-check
accel:
0
KVM (version 12) is installed and usable.
accel

$ free -h
               total        used        free      shared  buff/cache   available
Mem:           3.8Gi       1.5Gi       1.8Gi       5.1Mi       877Mi       2.4Gi
Swap:          3.8Gi       1.8Gi       2.0Gi

The emulator log immediately before the crash reports:
WARNING: cannnot unmap ptr ... as it is in the protected range ...
USER_INFO | Emulator is performing a full startup. This may take upto two minutes, or more.
