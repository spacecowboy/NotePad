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

makeinstallmother:
	cd ../NoNonsenseNotes/;make debug install

#For class: -Dspoon.test.class=fully.qualified.ClassName
#For method in that class, also: -Dspoon.test.method=testAllTheThings
testspoon:
	java -jar spoon-runner-1.0.2-jar-with-dependencies.jar \
--apk ../NoNonsenseNotes/bin/NotePad-debug.apk \
--test-apk bin/Test-debug.apk \
--sdk ~/android-sdk-linux

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


testgtask:
	adb shell am instrument -w \
-e class com.nononsenseapps.notepad.test.GTaskSyncTest \
com.nononsenseapps.notepad.test/android.test.InstrumentationTestRunner

testrfc:
	adb shell am instrument -w \
-e class com.nononsenseapps.notepad.test.RFCDateTest \
com.nononsenseapps.notepad.test/android.test.InstrumentationTestRunner
