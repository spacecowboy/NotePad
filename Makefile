test: installapp installtest
	cd test; make test

debug:
	cd full; make clean debug
release:
	cd full; make clean release

installapp: updateall
	cd full; make clean debug install

installtest: updateall
	cd test; make clean debug install

updateall:
	cd core; android update project -p . -t android-19
	cd lib; android update project -p . -t android-19
	cd test; android update project -p . -t android-19
	cd full; android update project -p . -t android-19
	cd external/ActionBar-PullToRefresh;android update project -p . -t android-19
	cd external/Android-ViewPagerIndicator;android update project -p . -t android-19
	cd external/datetimepicker;android update project -p . -t android-19
	cd external/drag-sort-listview;android update project -p . -t android-19
	cd external/google-play-services;android update project -p . -t android-19
	cd external/NineOldAndroids;android update project -p . -t android-19
	cd external/ShowcaseView;android update project -p . -t android-19
