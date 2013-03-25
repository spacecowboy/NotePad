debug:
	ant debug

clean:
	ant clean

test:
	adb shell am instrument -w \
com.nononsenseapps.notepad.test/android.test.InstrumentationTestRunner

install:
	adb install -r bin/Test-debug.apk

uninstall:
	adb uninstall com.nononsenseapps.notepad.test
