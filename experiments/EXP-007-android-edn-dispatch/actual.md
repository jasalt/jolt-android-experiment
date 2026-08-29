# Observed 2026-08-28

The isolated Jolt Android library was rebuilt from its pinned source/target
pack with the shared portable reducer on the library classpath. A clean API 35
APK build/install launched the calls through `HandlerThread("JoltRuntime")`.

## Valid serialized event

```text
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: owner JNI thread recorded
I/jolt_probe: dispatch counter=1 calls=10000 allocation=100000 compact=ok shutdown=ok
Jolt dispatch = {:model {:counter 1, :events [], :platform nil}, :effects []}
```

The counter value came from `poc.reducer/step` called inside Jolt, not from the
Kotlin/JNI bridge.

## Malformed input

The UI callback then enqueued `not EDN` on the same handler thread:

```text
I/JoltRuntime: entering JNI on JoltRuntime
I/jolt_probe: malformed event rejected
malformed = {:error :invalid-event}
```

No AndroidRuntime fatal exception or native crash occurred.

## Ownership conclusion

- JNI owns and releases the inbound Java UTF-8 input after the Jolt call.
- Jolt returns a primitive `:int` only.
- A 96-byte C stack buffer owns the canonical EDN response until `NewStringUTF`
  copies it to Java; `snprintf` validates bounds first.
- This does not claim safety for Jolt `:string` return values. Those are not
  used by the Android bridge.
