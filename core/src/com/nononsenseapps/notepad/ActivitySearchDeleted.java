package com.nononsenseapps.notepad;

import android.support.v4.app.Fragment;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.nononsenseapps.notepad.fragments.FragmentSearch;
import com.nononsenseapps.notepad.fragments.FragmentSearchDeleted_;

@EActivity(R.layout.fullscreen_fragment)
public class ActivitySearchDeleted extends ActivitySearch {
	
	@Override
	protected Fragment getFragment() {
		return new FragmentSearchDeleted_();
	}
}
