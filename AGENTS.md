# Agent instructions — Jolt Android PoC

This file contains coding-agent workflow instructions. Read the human-facing
project overview, status, architecture summary, technical context, and
development approach in [README.md](README.md), then read the governing
requirements and phased research plan in [docs/PLAN.md](docs/PLAN.md) completely before
planning or implementing work.

## Environment

This workspace may run in a restricted Jai jail or a Fedora-based Lima VM mapped
from a Fedora x86_64 Wayland host. Inspect the environment instead of assuming
root access, X11, KVM, hardware acceleration, or unrestricted networking.
Relevant indicators include:

```sh
env | grep -E '^(HOSTNAME|JAI_|DISPLAY|WAYLAND_DISPLAY)='
test -r /dev/kvm && ls -l /dev/kvm
```

In a Lima VM, `sudo` is generally available. In Jai, access is restricted. The
host compositor is Wayland, although an Xwayland `DISPLAY` may be available for
graphical emulator and GTK work. Never encode machine-local paths or manually
installed SDK components into the project.

Follow the environment contract described in [README.md](README.md). Verify
version claims in `docs/PLAN.md` against pinned/current upstream sources—the plan is
a specification and initial design, not proof that time-sensitive facts remain
true.

## Required Jolt reading

Before making project changes, read:

1. this file and [docs/PLAN.md](docs/PLAN.md);
2. the Jolt repository guidance at [../jolt/llms.txt](../jolt/llms.txt);
3. the user-facing Jolt index at
   [../jolt-lang.github.io/docs/llms.txt](../jolt-lang.github.io/docs/llms.txt),
   or <https://jolt-lang.net/llms.txt> if the local site checkout is absent;
4. the specific Jolt documentation, specification, RFCs, and source implicated
   by the task.

Useful local sibling checkouts are:

- `../jolt/` — Jolt compiler/runtime source, tests, and implementation guidance;
- `../jolt-lang.github.io/` — Jolt documentation source.

For language, API, tooling, interop, libraries, specifications, and design, use
<https://jolt-lang.net/docs/> and follow relevant specification/RFC links. Before
implementing common functionality, inspect Jolt's existing libraries and
examples:

- <https://jolt-lang.net/docs/libraries.html>
- <https://github.com/jolt-lang/examples/>

For recent implementation context, search the Jolt Clojurians/Zulip archive when
useful. Prefer current Jolt source, tests, and
`../jolt/test/conformance/known-divergences.edn` over stale documentation. Treat
RFCs primarily as design material unless the current implementation confirms
them.

## Project rules

Follow the project context and incremental development approach in
[README.md](README.md), the detailed phase order and acceptance criteria in
[docs/PLAN.md](docs/PLAN.md), and the decisions indexed in
[docs/adr/README.md](docs/adr/README.md).

Never infer Jolt runtime behavior from JVM Clojure. When documentation and
behavior disagree, reduce the discrepancy and test the pinned implementation.
Do not obscure a platform failure, silently substitute another runtime, or
claim an untested architecture works.

For every non-trivial feature, start with the smallest observable experiment,
use REPL/manual evaluation where possible, turn successful observations into
tests, preserve evidence, update Beads, review the diff, and commit atomically.
Do not skip ahead in the proof order from `README.md` and `docs/PLAN.md`.

When a decision changes, add an ADR that supersedes the old record rather than
burying the change in this file. Treat native ownership, ABI translation, and
runtime stability as unproven until the prescribed experiments pass.

## Beads is the only task system

Use Beads for all planning, task state, dependencies, and durable discoveries.
Do not create `TODO.md`, `TASKS.md`, another plan file, or ad-hoc task lists.
`docs/PLAN.md` is the existing project specification, not a live task tracker.

Initialize Beads once when implementation begins:

```sh
bd init
bd prime
```

At every session start:

```sh
bd prime
bd ready
```

Before work, claim exactly one ready bead. If unexpected work appears, create a
new bead and wire dependencies with `bd dep add` instead of silently expanding
scope. Use `bd remember` for durable discoveries. Add concise progress and exact
validation evidence to the bead. Do not close a bead without reproducible
evidence.

A completed engineering bead should normally correspond to one small atomic git
commit. Do not commit unrelated platform discoveries together. Do not discard,
rewrite, or commit unrelated user changes. Never claim a commit was made unless
it actually was.

Create the initial dependency graph from Phase 0 and “First ten Beads” in
`docs/PLAN.md`; use generated Beads IDs rather than inventing stable IDs in docs.

## Experiments, validation, and documentation

Follow the human-facing experiment method and validation progression in
[README.md](README.md), and the exact failure policy, experiment format, test
matrix, artifact requirements, and clean-room procedure in
[docs/PLAN.md](docs/PLAN.md).

For each failure, update its bead, preserve exact evidence, and create the
required reduced `experiments/EXP-xxxx-*/` reproduction when applicable. Do not
patch upstream prematurely or confuse an unimplemented PoC feature with an
upstream limitation. Continue maintaining `docs/GOTCHAS.md` as discoveries are
made.

Use the documentation roles listed in [README.md](README.md). Keep task state in
Beads, architectural decisions in `docs/adr/`, observed architecture in
`docs/ARCHITECTURE.md`, and evidence-based conclusions in `REPORT.md`. Mark
proposed, observed, inferred, and blocked behavior clearly and record exact
versions with experimental evidence.

## Working conventions

- Inspect existing files and git status before editing.
- Prefer small, reviewable changes and targeted tests.
- Use project scripts for repeatability rather than undocumented shell history.
- Make scripts fail fast and produce useful diagnostics.
- Never report a test as passing if a pipeline masked its exit status.
- Do not use JVM assumptions to explain Jolt failures.
- Do not broaden a bead merely because adjacent work is convenient.
- Do not overwrite user changes or generated research evidence.
- Report commands run, evidence obtained, and remaining uncertainty concisely.
