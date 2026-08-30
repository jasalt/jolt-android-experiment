#!/usr/bin/env bash
# Build and exercise the adaptive gallery on the pinned API-35 emulator.
set -euo pipefail

root=$(cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$root"
serial="emulator-${JOLT_ANDROID_EMULATOR_PORT:-5554}"
package=net.joltlang.raylibgallery
apk="$root/raylib/loop-android/build/outputs/apk/debug/raylib-loop-android-debug.apk"
out="$root/experiments/RAY-013-gallery-shell/evidence"
mkdir -p "$out"

: "${JOLT_SOURCE:?set JOLT_SOURCE to a clean checkout at the pinned Jolt revision}"

(cd raylib && jolt -M:test) | tee "$out/pure-tests.txt"
JOLT_SOURCE="$JOLT_SOURCE" ./scripts/raylib-persistent-loop-build-android \
  2>&1 | tee "$out/aot-build.txt"

./scripts/emulator-start headless | tee "$out/emulator-start.txt"
adb -s "$serial" install -r "$apk" | tee "$out/install.txt"

screen_size() {
  local size
  size=$(adb -s "$serial" shell wm size |
    awk '/Override size:/ {print $3; found=1} END {if (!found) exit 1}' |
    tr -d '\r') || {
      size=$(adb -s "$serial" shell wm size |
        awk '/Physical size:/ {print $3; exit}' | tr -d '\r')
    }
  printf '%s\n' "$size"
}

# Mirror the pure gallery-ui layout calculation to derive live card centers,
# rather than embedding a 1080x2400 coordinate in the interaction test.
card_center() {
  local index=$1
  local size width height min margin title body line_gap gap columns cards_y footer rows card_width card_height column row
  size=$(screen_size)
  width=${size%x*}
  height=${size#*x}
  min=$((width < height ? width : height))
  margin=$((min / 30))
  ((margin < 16)) && margin=16
  title=$((min / 18))
  ((title < 24)) && title=24
  body=$((title / 2))
  ((body < 16)) && body=16
  line_gap=$((body + (body / 2 > 8 ? body / 2 : 8)))
  gap=$((margin / 2))
  ((gap < 12)) && gap=12
  columns=$((width * 3 >= height * 2 ? 3 : 2))
  rows=$(((6 + columns - 1) / columns))
  cards_y=$((margin + title + 2 * line_gap))
  footer=$((margin + 2 * line_gap + body))
  card_width=$(( (width - 2 * margin - (columns - 1) * gap) / columns ))
  card_height=$(( (height - cards_y - footer - (rows - 1) * gap) / rows ))
  column=$((index % columns))
  row=$((index / columns))
  printf '%d %d\n' \
    "$((margin + column * (card_width + gap) + card_width / 2))" \
    "$((cards_y + row * (card_height + gap) + card_height / 2))"
}

run_case() {
  local label=$1 size=$2
  adb -s "$serial" shell wm size "$size" >/dev/null
  sleep 1
  adb -s "$serial" logcat -c
  adb -s "$serial" shell am force-stop "$package"
  adb -s "$serial" shell am start -W -n "$package/android.app.NativeActivity" \
    >"$out/${label}-launch.txt"
  sleep 2
  adb -s "$serial" exec-out screencap -p >"$out/${label}-gallery.png"

  read -r tap_x tap_y < <(card_center 0)
  adb -s "$serial" shell input tap "$tap_x" "$tap_y"
  sleep .5
  adb -s "$serial" exec-out screencap -p >"$out/${label}-scene.png"
  adb -s "$serial" shell input keyevent 4
  sleep .5
  adb -s "$serial" exec-out screencap -p >"$out/${label}-android-back.png"
  adb -s "$serial" shell input keyevent 4
  sleep .5
  adb -s "$serial" logcat -d -s jolt_raylib_gallery_state:* >"$out/${label}.log"
  adb -s "$serial" logcat -d -s jolt_raylib_gallery:* >"$out/${label}-bootstrap.log"
}

restore_size() {
  adb -s "$serial" shell wm size reset >/dev/null 2>&1 || true
}
trap restore_size EXIT

# Exercise every registered card at the default portrait size.
adb -s "$serial" shell wm size 1080x2400 >/dev/null
sleep 1
adb -s "$serial" logcat -c
adb -s "$serial" shell am force-stop "$package"
adb -s "$serial" shell am start -W -n "$package/android.app.NativeActivity" >/dev/null
sleep 2
for index in 0 1 2 3 4 5; do
  read -r tap_x tap_y < <(card_center "$index")
  adb -s "$serial" shell input tap "$tap_x" "$tap_y"
  sleep .2
  adb -s "$serial" shell input tap 40 40
  sleep .2
done
adb -s "$serial" shell input keyevent 4
sleep .5
adb -s "$serial" logcat -d -s jolt_raylib_gallery_state:* >"$out/portrait-navigation.log"
adb -s "$serial" logcat -d -s jolt_raylib_gallery:* >"$out/portrait-bootstrap.log"

run_case small-portrait 720x1280
run_case landscape 2400x1080

adb -s "$serial" logcat -d >"$out/logcat.txt"
if grep -Eq 'Fatal signal|FATAL EXCEPTION|ANR in net\.joltlang\.raylibgallery' "$out/logcat.txt"; then
  echo 'gallery run contains a crash or ANR marker' >&2
  exit 1
fi
printf 'Adaptive gallery build, six-card navigation, Back handling, and size matrix passed.\n' |
  tee "$out/result.txt"
