debug:
	ant debug

bin/NotePad-debug.apk: res/values/arrays.xml
	ant debug

clean:
	ant clean

install: bin/NotePad-debug.apk
	adb install -r bin/NotePad-debug.apk

translations:
	get-diff-translations
	python listlang.py
	python escapetranslations.py

langs:
	python listlang.py
	python escapetranslations.py

res/values/arrays.xml:
	python listlang.py
	python escapetranslation.py
