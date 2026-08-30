#!/usr/bin/env bash
# Exercise the plain-C Raylib ARM64 NativeActivity on the pinned API-35 x86_64 AVD.
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"

serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibprobe
activity=android.app.NativeActivity
out="${RAYLIB_TRANSLATION_WORK_DIR:-$repo_root/experiments/RAY-003-plain-raylib-arm64-translation/evidence}"
mkdir -p "$out"

./scripts/raylib-build-android
./scripts/emulator-start headless
adb -s "$serial" logcat -c
./scripts/raylib-run-android | tee "$out/launch.txt"
sleep 2

adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r' >"$out/primary-abi.txt"
adb -s "$serial" shell getprop ro.product.cpu.abilist | tr -d '\r' >"$out/abi-list.txt"
adb -s "$serial" shell getprop ro.hardware.egl | tr -d '\r' >"$out/egl-property.txt"
adb -s "$serial" exec-out screencap -p >"$out/first-frame.png"
identify "$out/first-frame.png" >"$out/first-frame-identify.txt"

# A NativeActivity surface may be recreated while the process persists. This
# bounded sequence records whether the fixed C frame returns after resume, then
# whether a fresh process can launch again. It does not assert a Jolt lifecycle.
adb -s "$serial" shell input tap 100 100
adb -s "$serial" shell input keyevent 26
sleep 1
adb -s "$serial" shell input keyevent 26
sleep 2
adb -s "$serial" exec-out screencap -p >"$out/post-resume.png"
identify "$out/post-resume.png" >"$out/post-resume-identify.txt"
adb -s "$serial" shell am force-stop "$package"
./scripts/raylib-run-android | tee "$out/relaunch.txt"
sleep 2
adb -s "$serial" exec-out screencap -p >"$out/relaunch.png"
identify "$out/relaunch.png" >"$out/relaunch-identify.txt"

adb -s "$serial" logcat -d -v threadtime >"$out/logcat.txt"
if grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibprobe' "$out/logcat.txt"; then
  printf 'detected a current-run fatal native crash, Java crash, or ANR\n' >&2
  exit 1
fi
if ! grep -Fq 'PLATFORM: ANDROID: Initialized successfully' "$out/logcat.txt"; then
  printf 'Raylib Android backend initialization was not observed in logcat\n' >&2
  exit 1
fi
if ! grep -Fq 'GL: OpenGL device information:' "$out/logcat.txt" ||
  ! grep -Fq 'Renderer: Android Emulator OpenGL ES Translator' "$out/logcat.txt"; then
  printf 'Raylib GLES renderer initialization was not observed in logcat\n' >&2
  exit 1
fi

(
  cd "$out"
  sha256sum first-frame.png post-resume.png relaunch.png >SHA256SUMS
)
printf 'Raylib ARM64 translation evidence written to %s\n' "$out"
