# Observed 2026-08-28

The portable reducer suite passed 4 tests / 8 assertions, including the
clipboard effect as data.

On a clean API 35 x86_64 emulator run, the `COPY COUNTER` control had bounds
`[0,789][1080,915]`; tapping center `(540,852)` queued JoltRuntime dispatch.

The resulting UI hierarchy contained:

```text
Clipboard effect: Jolt counter: 0
```

The app generated this after calling Android `ClipboardManager.setPrimaryClip`
and immediately reading the primary clip back. The final screenshot additionally
shows Android’s system clipboard overlay with `Jolt counter: 0`, while the Jolt
model result includes the source effect:

```clojure
{:type :platform/clipboard, :text "Jolt counter: 0"}
```

The emulator’s `adb shell cmd clipboard get` replied `No shell command
implementation`; it is not used as proof. UI read-back plus the visible system
overlay are the observed Android evidence. Logcat records the queued JoltRuntime
entry and Jolt dispatch; no AndroidRuntime fatal exception or native crash
occurred.
