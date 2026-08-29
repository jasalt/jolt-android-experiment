# Caller-supplied debug eval extension

This follow-up replaces the former fixed `(+ 40 2)` experiment with the smallest
caller-supplied debug entry point. The Android Jolt fixture exports:

```clojure
(ffi/export! "poc_debug_eval" debug-eval [:string] :string)
```

`debug-eval` calls `load-string`, serializes a value as `{:ok ...}`, catches a
runtime/syntax failure as `{:error {:type :eval/failed ...}}`, and limits the
serialized result to 64 KiB. JNI rejects inputs above 64 KiB and Jolt's `:string`
ABI copies the returned string before JNI creates the Java string. Calls queue
through the sole `HandlerThread("JoltRuntime")`; no lifecycle or UI callback
enters JNI directly. `JoltRuntime.eval` returns `:eval/disabled` for a
non-debug build.

## Observed API-35 translated-emulator evidence

After rebuilding `libjoltpoc.so` from pinned Jolt
`8fcba79f8b33628af926f88032d93a1b31c24235`, building/installing the debug APK,
and launching it on `emulator-5554`, semantic UI dumps observed:

```text
Debug eval: {:ok 42}
Debug eval: {:error {:type :eval/failed :message "clojure.lang.ExceptionInfo: expected {}"}}
Jolt model: {:model {:counter -1 ...}, :effects [{:type :storage/write ...}]}
```

The final dispatch occurred after the failed eval and proves the owner runtime
continued processing. The demonstration is API-35 x86_64 emulator execution of
an ARM64 library through Android translation, not native ARM64-host validation.

This is *not* Android nREPL, CIDER, code redefinition, or a security boundary.
It is a debug-build-only local evaluation seam.

## ADB-forwarded interactive transport

The debug app binds only `127.0.0.1:45678` and handles one UTF-8, newline-framed
form per connection. `scripts/android-repl '<form>'` installs an ADB forward,
uses a bounded TCP read, and prints the structured result. Transport work uses
a latch only to await `JoltRuntime.eval`; the server thread never calls JNI.
Malformed/disconnected frames are isolated and a 10-second timeout returns data
rather than poisoning the runtime. Observed through the script:

```text
$ scripts/android-repl '(+ 40 2)'
{:ok 42}
$ scripts/android-repl '(throw (ex-info "expected" {}))'
{:error {:type :eval/failed :message "clojure.lang.ExceptionInfo: expected {}"}}
$ scripts/android-repl '(+ 1 2)'
{:ok 3}
```

This confirms recovery after a failed request. It remains interactive evaluation,
not an nREPL/CIDER protocol or code-redefinition support.
