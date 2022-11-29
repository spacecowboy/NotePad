#!/bin/bash

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

function getScreenStreamFromEmu() {
  while true; do
    # exec-out: run command on emulated android, get the output on the host PC
    # bitrate & size: to get a smaller video
    # output-format and - at the end: stream to console output, don't save
    # bugreport: helpful info when a video starts
    # alternative: `adb emu screenrecord start --bit-rate 100000 --size 540x960 ./video.webm`,
    #   but the tool is hardcoded to die after 3 minutes. Thanks Google.
    adb exec-out screenrecord --output-format=h264 --bit-rate 100000 --size 480x854 --bugreport -
    # should go to STDERR, without polluting the video stream, hopefully
    echo "restarting screenrecord from adb" >&2
  done
}

# TODO android API 32 images have a built-in screen record feature that can make long videos, but you have to start it from the top drawer menu. Check if API 23 has it, and try to use it with ADB. Videos go on /sdcard/movies/

# more info in case this script crashes
echo "---------- all OK as of now ----------"

# save the video stream to a file, then get this process PID
{ getScreenStreamFromEmu | ffmpeg -i - -s 480x854 -loglevel error \
 -nostats -hide_banner -framerate 24 -bufsize 1M emu-video.mp4 ; } &

VIDEO_PID=$!

# press "HOME" to close any "system UI not responding" popups still showing
# see https://stackoverflow.com/a/8483797/6307322
adb shell input keyevent 3
# The numbers are X and Y coordinates, in pixels, so 480 854 for the bottom right corner
# adb shell input tap 150 440

# run tests
./gradlew connectedCheck
GRADLE_RETURN_CODE=$?

# check if emu-video.mp4 exists
ls | grep mp4
ls | grep png

# Stop the recording
echo "killing process id=" $VIDEO_PID
kill -s QUIT $VIDEO_PID
sleep 10
sudo kill -s QUIT $VIDEO_PID
sleep 10
echo "try the last 2"
killall --signal INT  ffmpeg
sleep 10
sudo killall ffmpeg

# return with the code from gradle, so the github action can fail if the tests failed
exit $GRADLE_RETURN_CODE
