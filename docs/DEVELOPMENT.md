# Development

## Portable Jolt core on Linux

Enter the pinned shell and run the portable test and CLI fixtures:

```sh
nix develop -c ./scripts/test-portable
nix develop -c ./scripts/cli --event '{:type :counter/inc}'
```

The CLI accepts exactly `--event <EDN map>` and writes one canonical EDN result
with `:model` and `:effects`. Invalid CLI arguments or invalid EDN fail with a
nonzero exit status.

Use Jolt's normal nREPL workflow for interactive portable-core development:

```sh
nix develop -c jolt nrepl-server
```

The server writes `.nrepl-port`; connect an nREPL client to loopback and evaluate
`(require '[poc.reducer :as r])` followed by `(r/step r/initial-state {:type
:counter/inc})`. This workflow is Linux x86_64 evidence only. Native ARM64
validation is separately tracked by `jolt-android-a4e.2`.
