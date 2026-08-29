# Observed 2026-08-29

The portable suite passed 5 tests / 9 assertions after adding declarative
storage-write and storage-restore reducer behavior.

A clean emulator run cleared app data, started from counter 0, then tapped
Increment. Jolt returned a storage-write effect and Android visibly reported:

```text
Storage written: 1
```

After `adb shell am force-stop` and relaunch, the new JoltRuntime session logged
its normal queued initialization and received the storage restoration event.
The layout tree and inspected screenshot show:

```text
Jolt model: {:model {:counter 1, :events [], :platform nil, :lifecycle :resumed}, :effects []}
Storage restored: 1
```

This is a force-stop/new-process restoration test, not an orderly shutdown test.
Only the integer counter is stored by Android. Jolt owns the restored model and
no `SharedPreferences` object crosses the JNI/Jolt boundary. No AndroidRuntime
fatal exception or native crash occurred.
