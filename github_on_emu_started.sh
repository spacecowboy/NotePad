#!/bin/bash

# sources:  https://stackoverflow.com/a/66155744/6307322
#           https://stackoverflow.com/a/62723329/6307322
# will run all commands, even if one fails, and return the result code of gradle
echo "github action script started"

# wait a bit for the emulator to finish loading
sleep 15

# take screenshot of the host PC after the emulator starts
screencapture screenshot-desktop.jpg

# output may be useful
adb devices

# try to close all OS popups in the emulator, like "System UI stopped working"
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS

# close that useless popup that tells you how to exit fullscreen mode
adb shell settings put secure immersive_mode_confirmations confirmed

# manually uninstall the app, if possible. Sometimes it's needed
adb shell pm uninstall --user 0 com.nononsenseapps.notepad.test
adb shell pm uninstall --user 0 com.nononsenseapps.notepad

# if the host PC is under heavy load, the emulator sometimes receives a "long click" instead
# of a normal "click" signal, thus some tests will fail. The workaround is to raise the
# treshold for a "long click" from ~400ms to 3s, so it will be harder to send long clicks
# by mistake
adb shell settings put secure long_press_timeout 3000

# Take a screenshot of the emulator. See also
# https://developer.android.com/studio/run/advanced-emulator-usage#screenshots
adb emu screenrecord screenshot ./screenshot-emu-tests-starting.png

# disable animations once again, for safety
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

function getScreenStreamFromEmu() {
	while true; do
	# exec-out: run command on emulated android, get the output on the host PC
	# bitrate & size: to get a smaller video
	# output-format and - at the end: stream to console output, don't save
	# bugreport: helpful info when a video starts
	# alternative: `adb emu screenrecord start --bit-rate 100000 --size 540x960 ./vid.webm`,
	#   but the tool is hardcoded to die after 3 minutes. Thanks Google.
	# returns: 0 if 3 minutes passed, a number > 0 if there was an error
	adb exec-out screenrecord --output-format=h264 --bit-rate 1M --size 480x854 --bugreport -
	ADB_RET_CODE=$?
  if [ $ADB_RET_CODE -gt 0 ] ; then
    # the emulator died: exit the loop & the function.
    # Hopefully this also stops ffmpeg gracefully
    echo "Exiting..." >&2
    break
  fi
  # should go to STDERR, without polluting the video stream, hopefully
  echo "restarting screenrecord, PID = " $! ". " $ADB_RET_CODE " was the return code" >&2
	done
}

# TODO android API 32 images have a built-in screen record feature that can make long videos, but you have to start it from the top drawer menu. Check if API 23 has it, and try to use it with ADB. Videos go on /sdcard/movies/

# save the video stream to a file, then get this process PID
{ getScreenStreamFromEmu | ffmpeg -i - -s 480x854 -loglevel error \
 -nostats -hide_banner -framerate 20 -bufsize 1M emu-video.mp4 ; } &

VIDEO_PID=$!

# press "HOME" to close any "system UI not responding" popups still showing.
# Notice that these popups appear when the host PC is too slow, so the best solution
# is to run the tests under a different host OS, device skin, API version, ...
adb shell input keyevent 3

# clear logcat before tests begin
adb logcat -c

# run tests
./gradlew connectedCheck
GRADLE_RETURN_CODE=$?

# dump the logcat to a file. Log level: Debug
adb logcat -d *:D > logcat-dump.txt

# check if pictures, videos & logs exist
echo "----------"
ls -lh -- *.mp4 *.png *.jpg logcat-dump.txt
echo "----------"

# getScreenStreamFromEmu() already stops the recording, we don't have to do it manually.
# The (clean) stop happens after the github action closes the emulator.

# return with the code from gradle, so the github action can fail if the tests failed
exit $GRADLE_RETURN_CODE
