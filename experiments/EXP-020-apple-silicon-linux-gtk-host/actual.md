# Observed 2026-08-29

The requested GTK task was attempted from native Apple Silicon macOS:

```text
Darwin mbp14 23.6.0 Darwin Kernel Version 23.6.0: Tue Feb 24 20:49:46 PST 2026; root:xnu-10063.141.1.711.7~1/RELEASE_ARM64_T6000 arm64
arm64
Memory: 16 GiB
```

`limactl` was not available on the host. The project Lima template requires a
100 GiB guest disk plus Android/Nix downloads; the workspace volume had only
1.3 GiB available:

```text
Filesystem      Size    Used   Avail Capacity  Mounted on
/dev/disk3s5   460Gi   416Gi   1.3Gi   100%    /System/Volumes/Data
```

No native ARM64 Linux GTK host can be provisioned from this machine until Lima
is installed and sufficient disk space is available. No GTK implementation or
macOS substitution was attempted. The portable CLI/REPL and Android Nix build
workflows remain separate native macOS evidence.
