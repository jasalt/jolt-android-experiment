# Android Jolt EDN dispatch and output ownership

## Purpose

Prove the smallest data-only boundary through the established `JoltRuntime`
queue without returning a pointer owned by Jolt’s managed heap.

## Contract

Input is a Java `String` passed to the Jolt export as `:string`; JNI owns its
UTF-8 view for the duration of that call and releases it before returning.

The Jolt export `poc_dispatch_counter` parses the input with `clojure.edn`,
applies the shared `poc.reducer/step` function, and returns only a primitive
counter. The JNI bridge formats the canonical EDN result into `char output[96]`
using `snprintf`; it rejects a negative or truncated return before `NewStringUTF`
copies the C-owned bytes into Java. No Jolt-managed output string pointer crosses
JNI.

The reduced Android schema deliberately accepts only the exact event
`{:type :counter/inc}`. Other input is rejected before Jolt entry with
`{:error :invalid-event}`; this establishes deterministic malformed-input
behavior without relying on an undocumented exception ABI.

## Result

See [actual.md](actual.md) and
[`artifacts/logs/exp007-edn-dispatch.txt`](../../artifacts/logs/exp007-edn-dispatch.txt).
