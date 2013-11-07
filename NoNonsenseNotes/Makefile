debug:
	ant debug

release:
	ant release

bin/NotePad-debug.apk: res/values/arrays.xml
	ant debug

bin/NotePad-release.apk: res/values/arrays.xml
	ant release

clean:
	ant clean

install: bin/NotePad-debug.apk
	adb install -r bin/NotePad-debug.apk

uninstall:
	adb uninstall com.nononsenseapps.notepad

installrelease: bin/NotePad-release.apk
	adb install -r bin/NotePad-release.apk

send_translations:
	sh send-base-translation-notes

translations:
	sh get-diff-translations
	python listlang.py
	python escapetranslations.py
	python deletemissingtranslations.py

langs:
	python listlang.py
	python escapetranslations.py

res/values/arrays.xml:
	python listlang.py
	python escapetranslation.py

test:
	cd ../NoNonsenseNotesTest; make test

testandinstall:
	cd ../NoNonsenseNotesTest; make debug install test
