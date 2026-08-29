# Agent instructions — Jolt Android PoC

This file contains coding-agent workflow instructions. Read the human-facing
project overview, status, architecture summary, technical context, and
development approach in [README.md](README.md), then read the governing
requirements and phased research plan in [docs/PLAN.md](docs/PLAN.md) completely before
planning or implementing work.

## Environment

This workspace may run in a restricted Jai jail, a Fedora x86_64 Lima VM, or a
native-architecture Apple Silicon Lima VM. Inspect the environment instead of
assuming root access, X11, KVM, hardware acceleration, host architecture, or
unrestricted networking.
Relevant indicators include:

```sh
env | grep -E '^(HOSTNAME|JAI_|DISPLAY|WAYLAND_DISPLAY)='
test -r /dev/kvm && ls -l /dev/kvm
```

In a Lima VM, `sudo` is generally available. In Jai, access is restricted. The
Fedora host compositor is Wayland, although an Xwayland `DISPLAY` may be
available for graphical emulator and GTK work. Apple Silicon macOS developers
use the native `aarch64-darwin` Nix shell for portable CLI/REPL work and Android
APK assembly; it supplies the project SDK/NDK, `adb`, and emulator without
Android Studio or host-installed SDK components. GTK remains Linux-only. The
macOS emulator must be validated on each host: package availability and
acceleration do not overcome a local disk-space shortage. Never encode
machine-local paths or manually installed SDK components into the project.

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

Initialize Beads after the project Git `origin` exists:

```sh
git remote get-url origin
bd init
bd hooks list
bd prime
```

Beads uses the **same project Git repository**, not a separately provisioned
Dolt remote or service. `bd init` configures the existing Git `origin` for Beads
sync; Beads data is stored in Dolt refs (`refs/dolt/data`), separate from normal
source Git refs and source files. The project repository tracks
`.beads/config.yaml`, `.beads/metadata.json`, and `.beads/.gitignore`; its
embedded database/runtime directories remain ignored. Never add
`.beads/embeddeddolt/` or `.beads/dolt/` to Git or Git LFS.

Use Beads commands rather than editing database files:

```sh
bd vc status
bd vc commit -m 'describe task-state update'
bd dolt pull
bd dolt push
```

`bd init` installs Git hooks by default; retain or refresh them with `bd hooks
install` so ordinary project Git operations integrate with Beads. Optional
`.beads/issues.jsonl` auto-export is for viewing and interchange, not the source
of truth or synchronization mechanism. Keep source commits and Beads/Dolt
commits logically aligned, but do not claim that one automatically commits the
other.

At every session start:

```sh
bd dolt pull
bd prime
bd ready
```

Before work, claim exactly one ready bead. If unexpected work appears, create a
new bead and wire dependencies with `bd dep add` instead of silently expanding
scope. Use `bd remember` for durable discoveries. Add concise progress and exact
validation evidence to the bead. Do not close a bead without reproducible
evidence.

A completed engineering bead should normally correspond to one small atomic
source Git commit and a corresponding Beads/Dolt commit. Do not commit unrelated platform discoveries together. Do not discard,
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

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:970c3bf2 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/SYNC_CONCEPTS.md for details and anti-patterns.

## Agent Context Profiles

The managed Beads block is task-tracking guidance, not permission to override repository, user, or orchestrator instructions.

- **Conservative (default)**: Use `bd` for task tracking. Do not run git commits, git pushes, or Dolt remote sync unless explicitly asked. At handoff, report changed files, validation, and suggested next commands.
- **Minimal**: Keep tool instruction files as pointers to `bd prime`; use the same conservative git policy unless active instructions say otherwise.
- **Team-maintainer**: Only when the repository explicitly opts in, agents may close beads, run quality gates, commit, and push as part of session close. A current "do not commit" or "do not push" instruction still wins.

## Session Completion

This protocol applies when ending a Beads implementation workflow. It is subordinate to explicit user, repository, and orchestrator instructions.

1. **File issues for remaining work** - Create beads for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Handle git/sync by active profile**:
   ```bash
   # Conservative/minimal/default: report status and proposed commands; wait for approval.
   git status

   # Team-maintainer opt-in only, unless current instructions forbid it:
   git pull --rebase
   bd dolt push
   git push
   git status
   ```
5. **Hand off** - Summarize changes, validation, issue status, and any blocked sync/commit/push step

**Critical rules:**
- Explicit user or orchestrator instructions override this Beads block.
- Do not commit or push without clear authority from the active profile or the current user request.
- If a required sync or push is blocked, stop and report the exact command and error.
<!-- END BEADS INTEGRATION -->

<!-- BEGIN BEADS CODEX SETUP: generated by bd setup codex -->
## Beads Issue Tracker

Use Beads (`bd`) for durable task tracking in repositories that include it. Use the `beads` skill at `.agents/skills/beads/SKILL.md` (project install) or `~/.agents/skills/beads/SKILL.md` (global install) for Beads workflow guidance, then use the `bd` CLI for issue operations.

### Quick Reference

```bash
bd ready                # Find available work
bd show <id>            # View issue details
bd update <id> --claim  # Claim work
bd close <id>           # Complete work
bd prime                # Refresh Beads context
```

### Rules

- Use `bd` for all task tracking; do not create markdown TODO lists.
- Run `bd prime` when Beads context is missing or stale. Codex 0.129.0+ can load Beads context automatically through native hooks; use `/hooks` to inspect or toggle them.
- Keep persistent project memory in Beads via `bd remember`; do not create ad hoc memory files.

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/SYNC_CONCEPTS.md for details and anti-patterns.
<!-- END BEADS CODEX SETUP -->
