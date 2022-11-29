#!/bin/sh

# sources:  https://stackoverflow.com/a/66155744/6307322
#           https://stackoverflow.com/a/62723329/6307322
# will run all commands, even if one fails, and return the result code of the last one
echo "github action script started"

# wait a bit for it to finish loading
sleep 15

# take screenshot of the host PC after the emulator starts. MacOS specific
screencapture screenshot-desktop.jpg

# output may be useful
adb devices

# close all OS popups in the emulator, like "System UI stopped working"
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS

# close that useless popup that tells you how to exit fullscreen mode
adb shell settings put secure immersive_mode_confirmations confirmed

# uninstall the app, if possible. It doesn't uninstall it by itself. Ignore any error
adb shell pm uninstall --user 0 com.nononsenseapps.notepad.test
adb shell pm uninstall --user 0 com.nononsenseapps.notepad

# Take a screenshot of the emulator. See also
# https://developer.android.com/studio/run/advanced-emulator-usage#screenshots
adb emu screenrecord screenshot ./screenshot-emu-tests-starting.png

getScreenStreamFromEmu() {
  while true; do
    # exec-out: run command on emulated android, get the output on the host PC
    # bitrate & size: to get a smaller video
    # output-format and - at the end: stream to console output, don't save
    # bugreport: helpful info when a video starts
    # alternative: `adb emu screenrecord start --bit-rate 100000 --size 540x960 ./video.webm`,
    #   but the tool is hardcoded to die after 3 minutes. Thanks Google.
    adb exec-out screenrecord --output-format=h264 --bit-rate 100000 --size 480x854 --bugreport -
  done
}

# more info in case this script crashes
echo "---------- all OK as of now ----------"

# save the video stream to a file
{ getScreenStreamFromEmu | ffmpeg -i - -s 480x854 -loglevel error \
 -nostats -hide_banner -framerate 24 -bufsize 1M emu-video.mp4 ; } &

# get PID of the last line
VIDEO_PID=$!

# press "HOME" to close any "system UI not responding" popups still showing
# see https://stackoverflow.com/a/8483797/6307322
adb shell input keyevent 3
# The numbers are X and Y coordinates, in pixels, so 480 854 for the bottom right corner
# adb shell input tap 130 420

# check if emu-video.mp4 exists
ls -lah

# meanwhile, run tests
./gradlew connectedCheck

# Stopping the recording is useless: if the script succeeds, videos are useless.
# If it fails, the entire script is killed
# kill -INT $VIDEO_PID