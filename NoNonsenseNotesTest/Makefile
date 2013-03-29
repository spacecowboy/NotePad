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

testeditor:
	adb shell am instrument -w \
-e class com.nononsenseapps.notepad.test.FragmentTaskDetailTest \
com.nononsenseapps.notepad.test/android.test.InstrumentationTestRunner

testlist:
	adb shell am instrument -w \
-e class com.nononsenseapps.notepad.test.FragmentTaskListsTest \
com.nononsenseapps.notepad.test/android.test.InstrumentationTestRunner

testlistpager:
	adb shell am instrument -w \
-e class com.nononsenseapps.notepad.test.FragmentTaskListsViewPagerTest \
com.nononsenseapps.notepad.test/android.test.InstrumentationTestRunner


testprovider:
	adb shell am instrument -w \
-e class com.nononsenseapps.notepad.test.DBProviderTest \
com.nononsenseapps.notepad.test/android.test.InstrumentationTestRunner
