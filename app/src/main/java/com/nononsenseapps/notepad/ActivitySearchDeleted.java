package com.nononsenseapps.notepad;

import android.support.v4.app.Fragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import com.nononsenseapps.notepad.fragments.FragmentSearch;
import com.nononsenseapps.notepad.fragments.FragmentSearchDeleted_;

@EActivity(resName="fullscreen_fragment")
public class ActivitySearchDeleted extends ActivitySearch {
	
	@Override
	protected Fragment getFragment() {
		return new FragmentSearchDeleted_();
	}
}
