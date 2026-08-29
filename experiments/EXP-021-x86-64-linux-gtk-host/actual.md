# Observed 2026-08-29

The active host is a resource-capable native Linux (not emulated) x86_64 Fedora
44 Lima guest:

```text
Linux lima-jolt-android 6.19.10-300.fc44.x86_64
x86_64
Nix 2.34.8
Jolt 8fcba79f8b33628af926f88032d93a1b31c24235
GTK 4.22.4
GLib 2.88.3
DISPLAY=:99
```

The guest's managed `lima-x11.service` had Xvfb, Openbox, and x11vnc running.
The project Nix shell now supplies GTK4/GLib on Linux and sets
`LD_LIBRARY_PATH` to the Nix library paths required by Jolt's `dlopen`-based
FFI resolution.

The reproducible command in `commands.sh` checked out the exact upstream
`glimmer-gtk` revision `ce79d45698d36ccf496397bb85974e3cce6abfd8` into a
temporary directory. Its results were:

```text
Ran 6 tests. 24 assertions passed, 0 failures, 0 errors.
:smoke :result :pass :render-count 2
Ran 7 tests. 16 assertions passed, 0 failures, 0 errors.
```

The GTK smoke emitted warnings about the C locale and unavailable desktop portal
inside the intentionally minimal Xvfb desktop. It still completed its live
reactive render sequence successfully. These warnings do not represent GTK FFI
or Glimmer failures.

The project reference application launched through `DISPLAY=:99 ./scripts/gtk-run`.
`artifacts/screenshots/exp021-gtk-initial.png` shows the shared model at
`Counter: 0`; `artifacts/screenshots/exp021-gtk-incremented.png` shows two
actual GTK `+` clicks yielding `Counter: 2`; and
`exp021-gtk-decremented.png` / `exp021-gtk-reset.png` show the shared reducer
returning to `Counter: 1` and `Counter: 0`, respectively. Each non-initial
state displays the declarative reducer output, for example:

```text
Effects: [{:type :storage/write, :key "counter", :value 1}]
```

The GTK adapter executes that `:storage/write` effect through a host-side EDN
file and then relaunches the app. `exp021-gtk-restored.png` shows that the
persisted `Counter: 1` was restored into the shared model after the restart.
It intentionally has no GTK widget, Android object, or native pointer in the
reducer state. Clipboard and other platform effects remain unimplemented.

The result is native x86_64 Linux evidence only and does not satisfy the
independent native ARM64 Linux-host requirement in `jolt-android-jkb.2`.
