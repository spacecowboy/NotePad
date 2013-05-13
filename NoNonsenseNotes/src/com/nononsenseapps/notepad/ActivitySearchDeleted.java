package com.nononsenseapps.notepad;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.nononsenseapps.notepad.fragments.FragmentSearchDeleted_;

@EActivity(R.layout.fullscreen_fragment)
public class ActivitySearchDeleted extends ActivitySearch {

	@AfterViews
	@Override
	void loadContent() {
		getSupportFragmentManager().beginTransaction()
				.add(R.id.fragmentPlaceHolder, new FragmentSearchDeleted_())
				.commit();
	}
}
