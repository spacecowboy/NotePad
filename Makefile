test: installapp installtest
	cd test; make test

installapp: updateall
	cd full; make clean debug install

installtest: updateall
	cd test; make clean debug install

updateall:
	cd core; android update project -p .
	cd lib; android update project -p .
	cd test; android update project -p .
	cd full; android update project -p .
	cd external/ActionBar-PullToRefresh;android update project -p .
	cd external/Android-ViewPagerIndicator;android update project -p .
	cd external/datetimepicker;android update project -p .
	cd external/drag-sort-listview;android update project -p .
	cd external/google-play-services;android update project -p .
	cd external/NineOldAndroids;android update project -p .
	cd external/ShowcaseView;android update project -p .
