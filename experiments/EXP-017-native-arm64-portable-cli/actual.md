# Observed 2026-08-29

Host platform:

```text
Darwin mbp14 23.6.0 Darwin Kernel Version 23.6.0: Tue Feb 24 20:49:46 PST 2026; root:xnu-10063.141.1.711.7~1/RELEASE_ARM64_T6000 arm64 arm Darwin
arm64
nix (Nix) 2.35.1
jolt 8fcba79f8b33628af926f88032d93a1b31c24235
```

The host Nix configuration had `nix-command` disabled by default, so the
reproducible commands explicitly enable the features needed for flakes:

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/test-portable
nix --extra-experimental-features 'nix-command flakes' develop -c ./scripts/cli --event '{:type :counter/inc}'
nix --extra-experimental-features 'nix-command flakes' develop -c jolt nrepl-server 7891
```

The portable suite result was:

```text
Ran 6 tests. 13 assertions passed, 0 failures, 0 errors.
```

The valid CLI fixture produced:

```text
{:model {:counter 1, :events [], :platform nil, :lifecycle nil, :worker nil, :notification-permission nil}, :effects [{:type :storage/write, :key "counter", :value 1}]}
```

The malformed EDN fixture exited nonzero, as required by `scripts/test-portable`.

For the nREPL check, `jolt nrepl-server 7891` created `.nrepl-port` containing
`7891`. A bencoded loopback `eval` request evaluated:

```clojure
(do (require (quote poc.reducer))
    (poc.reducer/step poc.reducer/initial-state {:type :counter/inc}))
```

The response contained a `done` status and this value:

```clojure
[{:counter 1, :events [], :platform nil, :lifecycle nil, :worker nil,
  :notification-permission nil}
 [{:type :storage/write, :key "counter", :value 1}]]
```

This establishes native `aarch64-darwin` portable CLI and nREPL evidence. It
does not establish native Android ARM64 execution or GTK support.
