#!/usr/bin/env bash
# Runs inside the Android emulator step: installs the real APK and captures real screenshots.
set -x
PKG=com.adhkaritv.app
OUT=shots
mkdir -p "$OUT"
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
adb shell wm size
adb install -r app/build/outputs/apk/debug/app-debug.apk

shot() { adb exec-out screencap -p > "$OUT/$1.png"; ls -la "$OUT/$1.png"; }
launch() { adb shell am force-stop $PKG; adb shell am start -n $PKG/.MainActivity; }

# 1-3: main screen with default settings, ~32 s apart (adhkar change every 30 s, photos every 60 s)
launch
sleep 25; shot 01-main
sleep 32; shot 02-main
sleep 64; shot 03-main

# 4: settings screen (OK on the remote)
adb shell input keyevent KEYCODE_DPAD_CENTER
sleep 4; shot 04-settings
adb shell input keyevent KEYCODE_BACK

# 5: alternative look — Playfair clock, prayer box at 50 %
setprefs() {
  local b64; b64=$(printf '%s' "$1" | base64 -w0)
  adb shell am force-stop $PKG
  adb shell "run-as $PKG mkdir -p shared_prefs"
  adb shell "echo $b64 | base64 -d | run-as $PKG sh -c 'cat > shared_prefs/athkari_settings.xml'"
}
setprefs "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name=\"clock_font\" value=\"2\" /><int name=\"card_opacity\" value=\"50\" /></map>"
launch
sleep 25; shot 05-playfair-50pct

# 6: Outfit clock, no box
setprefs "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name=\"clock_font\" value=\"1\" /><int name=\"card_opacity\" value=\"0\" /></map>"
launch
sleep 25; shot 06-outfit-nobox
exit 0
