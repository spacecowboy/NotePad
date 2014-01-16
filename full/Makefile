debug:
	ant debug

release:
	ant release

bin/NotePad-debug.apk:
	ant debug

bin/NotePad-release.apk:
	ant release

clean:
	ant clean

install: bin/NoNonsenseNotes_full-debug.apk
	adb install -r bin/NoNonsenseNotes_full-debug.apk

uninstall:
	adb uninstall com.nononsenseapps.notepad

installrelease: bin/NoNonsenseNotes_full-release.apk
	adb install -r bin/NoNonsenseNotes_full-release.apk
