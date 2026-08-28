# Lima development VM

This project can run in a disposable native-architecture Fedora
[Lima](https://lima-vm.io/) VM. Lima supplies the operating-system and process
isolation, a persistent guest disk, SSH, and optional host filesystem sharing.
Nix supplies the pinned Jolt, Chez, Android, Java, GTK, and build dependencies.
Podman, Docker, and containerd are intentionally absent.

The VM configuration is [`../lima-vm.yaml`](../lima-vm.yaml).

## What the VM provides

The mutable VM base contains only facilities needed before `nix develop` can
run:

- Fedora, SSH, Git, and the Fedora Nix multi-user daemon with flakes and build
  sandboxing;
- Pi and basic coding-agent utilities;
- X11 forwarding for quick one-off GUI checks;
- Xvfb, Openbox, x11vnc, FFmpeg, and xdotool for a stable inspectable desktop;
- 8 virtual CPUs, 16 GiB memory, and a 100 GiB guest disk;
- a writable host-home mount at the same path in the guest.

The Android SDK, NDK, emulator image, JDK, Gradle support, Jolt/Chez versions,
GTK4, and project compiler/debug tools belong in `flake.nix` and `flake.lock`,
not in `lima-vm.yaml`.

The template deliberately leaves `arch` unset: Lima chooses the host-native
architecture. On the primary Fedora x86_64 host it creates an x86_64 guest; on
Apple Silicon macOS it creates an aarch64 guest, normally with Lima's native VZ
driver. A foreign-architecture guest is possible with QEMU but is system
emulation, is slow, and is not the portable-core development path.

On the Linux Android-integration host, `/dev/kvm` availability in the guest is
still not guaranteed. Host firmware, kernel KVM module settings, QEMU, and Lima
all participate. Project scripts must diagnose acceleration and retain a
software-emulation fallback.

## Host prerequisites

Install Lima 2.0 or newer. On Fedora, install QEMU; on Apple Silicon macOS,
Lima's native VZ driver is the normal choice. Use the host's package
manager or the official Lima installation instructions; do not install Lima
inside the guest.

Check the installation:

```sh
limactl --version
```

On Fedora, also check:

```sh
qemu-system-x86_64 --version
```

For accelerated Android virtualization on Fedora, verify host KVM before
creating the VM:

```sh
test -c /dev/kvm && ls -l /dev/kvm
groups | tr ' ' '\n' | grep '^kvm$' || true
cat /sys/module/kvm_intel/parameters/nested 2>/dev/null || \
  cat /sys/module/kvm_amd/parameters/nested 2>/dev/null || true
```

The nested parameter should normally report `Y` or `1`. Changing firmware,
kernel-module, or group configuration is host administration and is not done by
the Lima template. Log out and back in after adding the host user to the `kvm`
group.

The requested VM resources are substantial. Ensure the host can spare at least
8 CPUs, 16 GiB RAM, and disk space for the 100 GiB sparse guest disk plus
Android/Nix downloads. On Apple Silicon, the portable CLI/REPL workflow normally
needs less; use a local resource override when not doing Android work. Adjust `cpus`, `memory`, and `disk` in a local copy of the
configuration before creation if necessary. Disk shrinking is generally not a
safe in-place operation.

## Apple Silicon macOS development

The preferred macOS workflow for the portable core is direct native Nix:

```sh
nix develop
./scripts/cli --event '{:type :counter/inc}'
jolt nrepl-server
```

The flake must support `aarch64-darwin` for this shell. GTK and the Linux
Android-emulator configuration are not prerequisites and must not be pulled into
the macOS portable-core shell.

If a Linux environment is needed—for example, to match CI shell behavior—start
this same template on an Apple Silicon Mac. With `arch` unset, Lima chooses an
`aarch64` Fedora guest and, on supported macOS versions, its native VZ driver:

```sh
limactl start --name=jolt-android-arm64 ./lima-vm.yaml
limactl shell jolt-android-arm64 -- uname -m
# expected: aarch64
```

Use the normal provisioning and VNC instructions below, replacing the instance
name. This native ARM64 VM is suitable for Jolt CLI/nREPL and portable-core
tests. Android emulator, Android SDK packaging, GTK availability, and nested
virtualization on macOS are separate capabilities to detect and document; they
are not implied by the VM booting.

Lima can also create a foreign-architecture VM with QEMU by explicitly choosing
an `arch`, but that is full system emulation and is deliberately not configured
here. Do not use an x86_64 guest on Apple Silicon merely to develop the portable
core. See Lima's [multi-architecture documentation](https://lima-vm.io/docs/config/multi-arch/)
for the trade-offs.

## Provision a new VM

Run these commands on the **host**, from the repository root:

```sh
limactl validate ./lima-vm.yaml
limactl start -y --name=jolt-android --mount-only .:w ./lima-vm.yaml
```

If the installed Lima does not provide `limactl validate`, creation itself still
validates the file:

```sh
limactl create --name=jolt-android ./lima-vm.yaml
limactl start jolt-android
```

Provisioning installs Fedora packages and global npm tools and therefore needs
network access. Lima prints the template's usage message after a successful
start. Inspect state at any time with:

```sh
limactl list
limactl info jolt-android
```

If provisioning fails, inspect the guest through Lima and review cloud-init:

```sh
limactl shell jolt-android
sudo less /var/log/cloud-init-output.log
systemctl status nix-daemon.socket
```

The provisioning scripts are intended to be safe to rerun. To apply changes to
`lima-vm.yaml` to an existing instance, copying commands manually is error-prone;
for this pre-project sandbox, prefer deleting and recreating the VM.

## Enter and use the VM

Enter from the host:

```sh
limactl shell jolt-android
```

The host home is mounted into the guest at the same absolute path, so change to
the same repository path used on the host. For example:

```sh
cd ~/dev/jail/jolt/jolt-android
nix develop
```

Once the project flake and scripts exist, the normal workflow is expected to be:

```sh
nix develop
./scripts/bootstrap
./scripts/verify
```

Run Pi from the repository inside the development shell:

```sh
nix develop
pi
```

Pi itself is installed during VM provisioning because it is the coding harness,
not a project build dependency. The writable host-home mount also exposes the
host's Pi configuration when it resides under the home directory. Review that
configuration before using an untrusted guest; credentials and agent tools have
the same access to the mounted home as the host user.

Useful one-shot commands from the host are:

```sh
limactl shell jolt-android -- pwd
limactl shell jolt-android -- bash -lc \
  'cd ~/dev/jail/jolt/jolt-android && nix develop -c ./scripts/verify'
```

Use the interactive shell form if command parsing or environment initialization
differs across Lima versions.

## Isolation and persistence boundary

The VM isolates processes, packages, services, and its guest root filesystem.
Nix builds are additionally sandboxed by `nix-daemon`. The following data is
persistent:

- the guest disk, including `/nix`, until the instance is deleted;
- repository and other home files on the host, because `~` is mounted writable;
- project build artifacts written into the mounted checkout.

A writable home mount is convenient but is **not** a security boundary against
malicious code: a guest process can modify or delete host-home files and read
credentials available there. It is chosen because Lima has no portable
configuration-relative token for mounting only this checkout, and source changes
must survive VM recreation.

For stronger isolation, remove the `mounts` and `mountInotify` entries from a
local template, then clone or copy the repository onto the guest disk. Export
commits or patches explicitly before deleting the VM. Another option is to
replace the home mount with an absolute, host-specific project path in a local
uncommitted configuration. Do not commit a developer's absolute path.

The Nix store should remain on the guest disk rather than a host filesystem
mount. This preserves Nix ownership, hard-link, and sandbox semantics and avoids
slow Android/NDK builds over a shared filesystem.

## VM lifecycle

From the host:

```sh
limactl stop jolt-android
limactl start jolt-android
limactl shell jolt-android
```

Stop the VM when it is not needed. Stopping preserves the guest disk. Delete and
recreate it when a clean operating-system baseline is required:

```sh
limactl stop jolt-android
limactl delete jolt-android
limactl start --name=jolt-android ./lima-vm.yaml
```

Deletion removes the guest disk and its Nix store but does not remove files in
the host-mounted checkout. Review `git status` before deletion and preserve any
work that was kept only on the guest disk.

## GUI architecture

Routine GUI inspection uses a direct virtual X11 desktop, based on the workflow
described in
[`VISION-X11.md`](https://github.com/jasalt/time-tracker-factory/blob/master/docs/VISION-X11.md):

```text
Android emulator / GTK application ─┐
Openbox window manager ─────────────┼─> Xvfb :99 ─┬─> FFmpeg x11grab
xdotool automation ─────────────────┘              └─> x11vnc
                                                        │
                                                   SSH tunnel
                                                        │
                                                   host viewer
```

This is simpler for agent inspection and automation than nesting a Wayland
compositor. Use a real Wayland/compositor setup only when Wayland behavior is
itself under test.

Xvfb listens on no TCP socket. x11vnc binds only to guest loopback, allows
shared viewers, and has no password. It is safe only because the endpoint is
reached through authenticated Lima SSH. Never remove `-localhost` or expose the
VNC port on a public interface while using `-nopw`.

## Virtual desktop service

The `lima-x11.service` system service starts the Xvfb/Openbox/x11vnc desktop
automatically during every VM boot. It runs as the unprivileged Lima user. Do
**not** start it manually during normal use. In the guest, select the display
for GUI commands and inspect the service:

```sh
export DISPLAY=:99
lima-x11 status
systemctl status lima-x11.service
```

The default desktop is `1280x800x24`. To change it persistently, add a systemd
drop-in in the guest, then restart the service:

```sh
sudo systemctl edit lima-x11.service
# Add:
# [Service]
# Environment=LIMA_X11_SCREEN=1920x1080x24
sudo systemctl daemon-reload
sudo systemctl restart lima-x11.service
```

`lima-x11 restart` and `lima-x11 stop` are recovery and diagnostic commands;
using them temporarily stops or restarts the auto-started service's processes.
Restore normal service management with:

```sh
sudo systemctl restart lima-x11.service
```

The helper starts the components in order, waits for Xvfb rather than sleeping a
fixed interval, and stops x11vnc and Openbox before Xvfb. Do not delete
`/tmp/.X99-lock` or `/tmp/.X11-unix/X99` while Xvfb is running.

Run GTK applications or the graphical Android emulator with the same explicit
display:

```sh
DISPLAY=:99 ./scripts/gtk-run
DISPLAY=:99 ./scripts/emulator-start gui
```

Those project scripts are planned and may not exist yet. Headless emulator tests
do not require the Xvfb desktop.

## View the desktop over VNC

The desktop is already running after the VM starts. Lima normally forwards a
guest loopback TCP listener to the same loopback port on the host. First check
whether its automatic forwarding made VNC available:

```sh
limactl list
# Connect a VNC viewer to 127.0.0.1:5900 if that port is shown as forwarded.
```

If the automatic forwarding is unavailable or port 5900 conflicts locally,
create an explicit SSH tunnel on the host:

```sh
ssh \
  -F ~/.lima/jolt-android/ssh.config \
  -N -T \
  -L 127.0.0.1:5901:127.0.0.1:5900 \
  lima-jolt-android
```

Keep that terminal open and connect any host VNC viewer to `127.0.0.1:5901`.
No VNC username or password is configured; Lima SSH authenticates the tunnel.
The explicit host port is deliberately `5901` so it need not collide with
another local VNC service. Choose another free host port by changing only the
left side of the forward, for example `5902:127.0.0.1:5900`.

`x11vnc -shared` permits the host viewer and a guest-side automation/capture
client to connect concurrently. Stop the tunnel with Ctrl-C and stop the guest
desktop with `lima-x11 stop`.

## Screenshots and input automation

Capture the complete virtual desktop inside the guest without disturbing a VNC
viewer:

```sh
DISPLAY=:99 ffmpeg \
  -hide_banner -loglevel error -y \
  -f x11grab -video_size 1280x800 -i :99.0 \
  -frames:v 1 artifacts/screenshots/lima-desktop.png
```

If `LIMA_X11_SCREEN` was changed, use the corresponding width and height.

Find and activate a visible application window:

```sh
window=$(DISPLAY=:99 xdotool search \
  --sync --onlyvisible --name 'window title expression' | head -1)
DISPLAY=:99 xdotool windowactivate --sync "$window"
DISPLAY=:99 xdotool getwindowgeometry --shell "$window"
```

Use the reported geometry rather than hard-coded desktop coordinates. Android
UI tests should prefer Android semantic/layout tooling and `adb` interaction;
`xdotool` is useful for emulator-window controls and GTK applications.

X11 forwarding is also enabled for occasional one-window checks. From a host
shell with a working `DISPLAY`, use Lima's generated SSH configuration:

```sh
ssh -F ~/.lima/jolt-android/ssh.config -X lima-jolt-android
```

Forwarded X11 is not the stable automation target. Prefer Xvfb/VNC for
screenshots and repeatable input.

## KVM and Android emulator diagnostics

Inside the guest, record rather than assume acceleration support:

```sh
uname -a
lscpu
test -c /dev/kvm && ls -l /dev/kvm || echo '/dev/kvm unavailable'
cat /sys/module/kvm_intel/parameters/nested 2>/dev/null || \
  cat /sys/module/kvm_amd/parameters/nested 2>/dev/null || true
```

After entering the Nix development shell and installing the pinned emulator:

```sh
emulator -accel-check
```

If `/dev/kvm` or nested acceleration is unavailable, use the emulator's software
mode for bounded integration tests and record the limitation in the environment
report. Do not weaken VM isolation or silently run host-installed Android tools
to make the check appear successful.

## Troubleshooting

### Provisioning or Nix fails

```sh
sudo less /var/log/cloud-init-output.log
systemctl status nix-daemon.socket
cat /etc/nix/nix.conf
nix --extra-experimental-features 'nix-command flakes' --version
```

If Fedora's `nix` package or service layout changes, update the VM base setup;
do not move project dependencies out of the flake.

### Xvfb or VNC fails

```sh
lima-x11 status
log_dir=$(lima-x11 logs)
ls -la "$log_dir"
cat "$log_dir/Xvfb.log"
cat "$log_dir/openbox.log"
cat "$log_dir/x11vnc.log"
DISPLAY=:99 xdpyinfo >/dev/null
ss -ltnp | grep '127.0.0.1:5900'
```

If rendering is black or OpenGL initialization fails:

```sh
DISPLAY=:99 glxinfo -B
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 your-gui-command
```

Confirm FFmpeg captures `:99.0`, not an SSH-forwarded display, and retain a
24-bit Xvfb screen.

### The VNC tunnel fails

Confirm the instance name and generated SSH configuration:

```sh
limactl list
ls ~/.lima/jolt-android/ssh.config
```

Check that `lima-x11 status` succeeds in the guest and that host port 5901 is
free. The SSH destination is `lima-jolt-android`, not merely `jolt-android`, when
using Lima's generated SSH config directly.

### Shared-tree file watching fails

`mountInotify` is enabled, but Lima documents limitations for some mount types
and nested removals. If a watcher misses changes, restart it or run the relevant
build explicitly. Correctness must not depend solely on shared-mount inotify.

### Reset to a known baseline

Preserve source changes, inspect `git status`, delete the instance, and provision
it again from the committed template. This is preferable to accumulating
undocumented DNF or service changes in a supposedly reproducible VM.
