# Observed 2026-08-28

The portable suite passed 3 tests / 7 assertions after lifecycle state was added
to the shared reducer.

A clean API 35 x86_64 emulator APK run invoked three lifecycle callbacks through
the same queue:

```text
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: owner JNI session initialized
I/jolt_probe: dispatch counter=0
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: dispatch counter=0
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: dispatch counter=0
```

The UI hierarchy and `exp009-runtime-lifecycle.png` show:

```text
Runtime: initialized
Process ABI: x86_64
Jolt thread: HandlerThread(JoltRuntime)
Lifecycle model: {:model {:counter 0, :events [], :platform nil, :lifecycle :resumed}, :effects []}
```

The process ABI is correctly reported as the x86_64 emulator process. The Jolt
library packaged in that APK is ARM64 and is executed through the separately
proven Android translation path; this screen does not claim native ARM64-host
execution. No AndroidRuntime fatal exception or native crash appeared.
