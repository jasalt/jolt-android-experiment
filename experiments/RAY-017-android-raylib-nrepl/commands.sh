#!/usr/bin/env bash
# Build and validate debug Android Jolt nREPL with the Raylib owner loop, then
# prove the release APK starts no listener and requests no network permission.
set -euo pipefail

root=$(cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$root"
: "${JOLT_SOURCE:?set JOLT_SOURCE to the pinned clean Jolt checkout}"
: "${ANDROID_HOME:?run through nix develop so Android tools are pinned}"

serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibgallery
activity="$package/android.app.NativeActivity"
out="$root/experiments/RAY-017-android-raylib-nrepl/evidence"
fixture="$root/experiments/RAY-017-android-raylib-nrepl/poc/raylib/gallery_ui.clj"
debug_apk="$root/raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk"
release_apk="$root/raylib/loop-android/build/outputs/apk/release/raylib-loop-android-release-unsigned.apk"
mkdir -p "$out"

(cd raylib && jolt -M:test) 2>&1 | tee "$out/pure-tests.txt"
./scripts/raylib-persistent-loop-build-android debug 2>&1 | tee "$out/debug-build.txt"
./scripts/emulator-start headless | tee "$out/emulator-start.txt"
# A previous run ends with a disposable release signature, so remove any
# existing package before installing Gradle's differently signed debug APK.
adb -s "$serial" uninstall "$package" >/dev/null 2>&1 || true
adb -s "$serial" install "$debug_apk" | tee "$out/debug-install.txt"
adb -s "$serial" logcat -c
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" shell am start -W -n "$activity" | tee "$out/debug-launch.txt"
sleep 4

pid_before=$(adb -s "$serial" shell pidof "$package" | tr -d '\r')
printf 'pid_before=%s\n' "$pid_before" | tee "$out/process-identity.txt"
adb -s "$serial" exec-out screencap -p >"$out/01-baseline.png"
./scripts/raylib-android-nrepl describe | tee "$out/describe.jsonl"
./scripts/raylib-android-nrepl clone | tee "$out/clone.jsonl"
./scripts/raylib-android-nrepl eval '(current-runtime-state)' poc.raylib.loop |
  tee "$out/state-before.jsonl"
./scripts/raylib-android-nrepl eval \
  '{:eval-thread-id (jolt.host/thread-id) :owner-thread-id (:owner-thread-id (poc.raylib.loop/current-runtime-state))}' |
  tee "$out/thread-boundary.jsonl"
./scripts/raylib-android-nrepl eval \
  '(submit-owner! #(hash-map :execution-thread-id (jolt.host/thread-id) :screen-width (get-screen-width)))' \
  poc.raylib.loop | tee "$out/owner-queue-submit.jsonl"
sleep 1
./scripts/raylib-android-nrepl eval '(owner-result 1)' poc.raylib.loop |
  tee "$out/owner-queue-result.jsonl"
./scripts/raylib-android-nrepl eval \
  '(throw (ex-info "expected-nrepl-failure" {:probe true}))' |
  tee "$out/eval-error.jsonl"
./scripts/raylib-android-nrepl eval '(+ 40 2)' | tee "$out/eval-recovery.jsonl"

./scripts/raylib-android-nrepl eval \
  '(defn live-presentation [] {:revision :android-nrepl-v2 :title "ANDROID nREPL LIVE" :subtitle "No rebuild or process restart" :background [20 34 60 255] :accent [255 184 64 255] :card [50 130 115 255]})' \
  poc.raylib.gallery-ui | tee "$out/redefine-v2.jsonl"
sleep 2
adb -s "$serial" exec-out screencap -p >"$out/02-eval-v2.png"
./scripts/raylib-android-nrepl eval '(current-runtime-state)' poc.raylib.loop |
  tee "$out/state-v2.jsonl"

./scripts/raylib-android-nrepl load-file "$fixture" | tee "$out/load-file-v3.jsonl"
sleep 2
adb -s "$serial" exec-out screencap -p >"$out/03-load-file-v3.png"
./scripts/raylib-android-nrepl eval '(current-runtime-state)' poc.raylib.loop |
  tee "$out/state-v3.jsonl"

lifecycle_pid_before=$(adb -s "$serial" shell pidof "$package" | tr -d '\r')
adb -s "$serial" shell input keyevent 3
sleep 2
./scripts/raylib-android-nrepl eval '(current-runtime-state)' poc.raylib.loop |
  tee "$out/state-background.jsonl"
adb -s "$serial" shell am start -W -n "$activity" |
  tee "$out/debug-resume.txt"
sleep 2
./scripts/raylib-android-nrepl eval '(current-runtime-state)' poc.raylib.loop |
  tee "$out/state-resumed.jsonl"
adb -s "$serial" exec-out screencap -p >"$out/04-resumed-v3.png"
lifecycle_pid_after=$(adb -s "$serial" shell pidof "$package" | tr -d '\r')
printf 'background_resume_pid_before=%s\nbackground_resume_pid_after=%s\n' \
  "$lifecycle_pid_before" "$lifecycle_pid_after" |
  tee "$out/background-resume-process.txt"
test "$lifecycle_pid_before" = "$lifecycle_pid_after"

./scripts/raylib-android-nrepl close | tee "$out/close.jsonl"

pid_after=$(adb -s "$serial" shell pidof "$package" | tr -d '\r')
printf 'pid_after=%s\n' "$pid_after" | tee -a "$out/process-identity.txt"
test "$pid_before" = "$pid_after"
sha256sum "$out"/*.png | tee "$out/screenshots.sha256"
adb -s "$serial" logcat -d >"$out/debug-logcat.txt"
grep -E 'jolt_raylib_(gallery|nrepl)' "$out/debug-logcat.txt" >"$out/debug-runtime.log"
if grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibgallery' \
  "$out/debug-logcat.txt"; then
  echo 'debug nREPL workflow contains a crash or ANR marker' >&2
  exit 1
fi

grep -q '"jolt-nrepl"' "$out/describe.jsonl"
grep -q ':presentation :baseline' "$out/state-before.jsonl"
grep -q ':eval-thread-id' "$out/thread-boundary.jsonl"
grep -q ':status :queued' "$out/owner-queue-submit.jsonl"
grep -q ':id 1' "$out/owner-queue-submit.jsonl"
grep -q ':execution-thread-id 0' "$out/owner-queue-result.jsonl"
grep -q ':screen-width 1080' "$out/owner-queue-result.jsonl"
grep -q 'eval-error' "$out/eval-error.jsonl"
grep -q '"value": "42"' "$out/eval-recovery.jsonl"
grep -q ':presentation :android-nrepl-v2' "$out/state-v2.jsonl"
grep -q ':presentation :android-load-file-v3' "$out/state-v3.jsonl"
grep -q ':presentation :android-load-file-v3' "$out/state-background.jsonl"
grep -q ':presentation :android-load-file-v3' "$out/state-resumed.jsonl"

# Build a direct-linked release image from the same sources. Its NativeActivity
# selects raylib_gallery, not raylib_gallery_debug; its merged manifest has no
# INTERNET permission. Sign only a disposable validation copy.
./scripts/raylib-persistent-loop-build-android release 2>&1 | tee "$out/release-build.txt"
build_tools="$ANDROID_HOME/build-tools/35.0.0"
"$build_tools/aapt2" dump permissions "$release_apk" | tee "$out/release-permissions.txt"
if grep -q 'android.permission.INTERNET' "$out/release-permissions.txt"; then
  echo 'release APK unexpectedly requests INTERNET' >&2
  exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
unzip -p "$release_apk" lib/arm64-v8a/libmain.so >"$work/libmain.so"
strings "$work/libmain.so" | grep -E 'raylib_gallery|debug-nrepl|release' |
  tee "$out/release-main-strings.txt"
grep -q '^raylib_gallery$' "$out/release-main-strings.txt"
if grep -q 'debug-nrepl' "$out/release-main-strings.txt"; then
  echo 'release native bootstrap contains the debug nREPL selection' >&2
  exit 1
fi

keytool -genkeypair -noprompt -keystore "$work/validation.jks" -storepass android \
  -keypass android -alias validation -dname 'CN=RAY-017 validation' \
  -keyalg RSA -validity 1 >/dev/null 2>&1
"$build_tools/zipalign" -f -p 4 "$release_apk" "$work/aligned.apk"
printf 'android\nandroid\n' | "$build_tools/apksigner" sign \
  --ks "$work/validation.jks" --ks-key-alias validation --ks-pass stdin \
  --key-pass stdin --out "$work/release-validation.apk" "$work/aligned.apk"
# The debug APK uses Gradle's debug key; the disposable release-validation key
# is intentionally different, so remove the debug install before this probe.
adb -s "$serial" uninstall "$package" >/dev/null 2>&1 || true
adb -s "$serial" install "$work/release-validation.apk" |
  tee "$out/release-install.txt"
adb -s "$serial" logcat -c
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" shell am start -W -n "$activity" | tee "$out/release-launch.txt"
sleep 4
adb -s "$serial" logcat -d >"$out/release-logcat.txt"
grep -E 'jolt_raylib_(gallery|nrepl)' "$out/release-logcat.txt" |
  tee "$out/release-runtime.log"
grep -q 'mode=release' "$out/release-runtime.log"
grep 'jolt_lookup raylib_gallery_debug=missing mode=release' \
  "$out/release-runtime.log" | tee "$out/release-jolt-debug-export.txt"
if grep -q 'jolt_raylib_nrepl.*started' "$out/release-runtime.log"; then
  echo 'release process unexpectedly started nREPL' >&2
  exit 1
fi
if ./scripts/raylib-android-nrepl describe >"$out/release-nrepl-probe.txt" 2>&1; then
  echo 'release process unexpectedly answered nREPL' >&2
  exit 1
fi

{
  printf 'jolt_revision=%s\n' "$(git -C "$JOLT_SOURCE" rev-parse HEAD)"
  printf 'project_revision=%s\n' "$(git rev-parse HEAD)"
  printf 'emulator_abi=%s\n' "$(adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r')"
  printf 'emulator_abilist=%s\n' "$(adb -s "$serial" shell getprop ro.product.cpu.abilist | tr -d '\r')"
  printf 'debug_apk_sha256=%s\n' "$(sha256sum "$debug_apk" | awk '{print $1}')"
  printf 'release_apk_sha256=%s\n' "$(sha256sum "$release_apk" | awk '{print $1}')"
} | tee "$out/environment.txt"
printf 'Android Raylib nREPL live eval/load-file and release-exclusion gates passed.\n' |
  tee "$out/result.txt"
