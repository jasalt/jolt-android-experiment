# Observed 2026-08-28

A rebuilt Android ARM64 Jolt library contains `poc.native/app-state`, a
Jolt-owned atom initialized to `poc.reducer/initial-state`. The clean API 35
emulator APK uses one `HandlerThread("JoltRuntime")` session.

## Ordered session calls

```text
I/jolt_probe: owner JNI session initialized
I/jolt_probe: dispatch counter=1
I/jolt_probe: dispatch counter=0
I/jolt_probe: malformed event rejected
I/jolt_probe: owner JNI session shutdown
```

The UI hierarchy records the matching caller-owned result strings:

```text
Jolt dispatches = {:model {:counter 1, :events [], :platform nil}, :effects []}
then {:model {:counter 0, :events [], :platform nil}, :effects []};
malformed = {:error :invalid-event}
```

`inc` then `dec` demonstrates ordered updates against one Jolt-owned atom;
malformed input was rejected before Jolt entry and did not mutate it.

## Relaunch

A force-stop/relaunch run again started at counter `1` after its first increment
and returned to `0` after decrement, proving a new process/session starts with
initial state. Force-stop does not establish orderly Android lifecycle shutdown;
the separate clean run above explicitly posted and logged owner-queue shutdown.

No AndroidRuntime fatal exception or native crash occurred.
