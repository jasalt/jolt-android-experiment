# Observed 2026-08-29

The portable reducer suite passed 6 tests / 13 assertions for the declarative
notification permission request plus granted/denied state transitions.

On a fresh API 35 emulator run with the permission revoked, the app’s
`REQUEST NOTIFICATIONS` button was located from live UI hierarchy bounds and
tapped. The inspected Android system dialog read:

```text
Allow ABI Probe to send you notifications?
Allow
Don’t allow
```

Automation then tapped the live `Allow` control—not `pm grant`. The Android
callback queued the granted data event through `JoltRuntime`. The final layout
and inspected screenshot showed:

```text
Jolt model: ... :notification-permission :granted ...
Notification permission: granted
```

Logcat recorded the queued JoltRuntime JNI entry and no AndroidRuntime fatal
exception or native crash. This experiment proves the permission request/result
round trip; it deliberately does not post a notification.
