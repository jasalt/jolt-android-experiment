# Architecture decision records

This directory records architectural decisions for the Jolt Android PoC. ADRs
capture decisions and their consequences; experimental evidence belongs under
`experiments/`, and the architecture actually demonstrated belongs in
`docs/ARCHITECTURE.md`.

## Status values

- **Proposed** — under consideration and not yet binding.
- **Accepted** — the project should follow this decision.
- **Superseded** — replaced by another ADR, which must be linked.
- **Rejected** — considered but not adopted.

Acceptance means the design direction is intentional; it does not claim that
Android/Jolt feasibility has already been demonstrated. Record that evidence in
tests, experiments, and `REPORT.md`.

## Index

- [ADR-0001: Shared Jolt core with platform host adapters](0001-shared-jolt-core-platform-adapters.md)
- [ADR-0002: Confine embedded Jolt access to one runtime thread](0002-single-jolt-runtime-thread.md)
- [ADR-0003: Use a narrow data-oriented native boundary](0003-data-oriented-native-boundary.md)
- [ADR-0004: Provide Android Nix tooling on Apple Silicon](0004-apple-silicon-android-nix-tooling.md)

## Adding a decision

Use the next four-digit number and the sections shown in the existing records:
status, context, decision, consequences, and validation. Keep mutable task state
in Beads, not in ADRs. If a decision changes, add a new ADR and mark the old one
superseded rather than rewriting history.
