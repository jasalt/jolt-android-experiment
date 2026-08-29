# Android live definition re-evaluation

## Question

Can a definition evaluated through the debug-only caller-supplied seam persist
in the already running Android Jolt library process?

## Commands

```sh
nix develop -c ./scripts/android-repl \
  '(do (defn poc.native/redef-test [] 99) (poc.native/redef-test))'
nix develop -c ./scripts/android-repl '(poc.native/redef-test)'
```

## Observed result

Both separate ADB-forwarded requests returned:

```text
{:ok 99}
```

The second request establishes that the definition remained in the live Jolt
runtime. The request path is loopback socket → `DebugEvalServer` →
`JoltRuntime` handler → JNI → Jolt `load-string`; all runtime entries use the
same owner thread.

## Scope

This demonstrates a narrow **live definition re-evaluation** result for the
current debug library image on the API-35 x86_64 emulator through ARM64
translation. It does not establish general reload semantics, dependency reload,
UI recomposition, native ARM64-host behavior, production safety, Android nREPL,
or CIDER compatibility.
