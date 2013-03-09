debug:
	ant debug

clean:
	ant clean

install: bin/NotePad-debug.apk
	adb install -r bin/NotePad-debug.apk

