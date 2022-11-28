#!/bin/sh

# sources:  https://stackoverflow.com/a/66155744/6307322
#           https://stackoverflow.com/a/62723329/6307322
echo "github action script started"

# exit script on the 1° error
set -e

# take screenshot of the host PC after the emulator starts
# MacOS specific. Ignore in case of error
screencapture screenshot-desktop.jpg || true

# output may be useful
adb devices

# this will fail if no single device could be selected
adb shell uptime

# close all OS popups in the emulator, like "System UI stopped working"
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS

# close that useless popup that tells you how to exit fullscreen mode
adb shell settings put secure immersive_mode_confirmations confirmed

# uninstall the app (it can't uninstall it by itself). Ignore any error
adb shell pm uninstall --user 0 com.nononsenseapps.notepad.test || true
adb shell pm uninstall --user 0 com.nononsenseapps.notepad || true

# Take a screenshot of the emulator. See also
# https://developer.android.com/studio/run/advanced-emulator-usage#screenshots
adb emu screenrecord screenshot ./screenshot-emu-tests-starting.png

# record the tests in the emulator (dies after 3 minutes!)
# adb emu screenrecord start --bit-rate 100000 --size 540x960 ./emu-video.webm

# declare a function to record 3 minutes & then save the video to the host PC
record_and_move ()
{
  adb shell screenrecord --bit-rate 100000 --size 540x960 /sdcard/video"$1".mp4
  adb pull /sdcard/video"$1".mp4 .
}

# call the function 6 times, in series, in background
# { record_and_move V1 ; record_and_move V2 ; record_and_move V3 ; record_and_move V4 ;  record_and_move V5 ; record_and_move V6 ;  } &

# alternative:
funcScreenStream() {
  while true; do
    # exec-out: run command on emulated android, get the output on your "host" PC
    # bitrate & size: to get a small but still comprehensible video
    # alternative: `adb emu screenrecord start --bit-rate 100000 --size 540x960 ./emu-video.webm`
    # its problem: the tool is hardcoded to die after 3 minutes. Thanks Google.
    # - at the end: stream, don't save (?)
    adb exec-out screenrecord --output-format=h264 --bit-rate 100000 --size 540x960 --bugreport -
  done
}

# save to file
{ funcScreenStream | ffmpeg -i - -s 540x960 -hide_banner -framerate 24 -bufsize 1M emu-video-2.mp4 ; } &

# tap the screen to close a popup
# The numbers are X and Y coordinates, in pixels, so 1080 & 1920 for the bottom right corner
adb shell input tap 300 950 

# check if emu-video-2.mp4 exists
ls -lah

# meanwhile, run tests
./gradlew connectedCheck

# stop recording. Useless: if the script succeeds, videos are useless.
# If it fails, the entire script is killed
# adb emu screenrecord stop
