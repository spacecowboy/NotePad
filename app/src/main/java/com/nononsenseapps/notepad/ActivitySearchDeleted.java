package com.nononsenseapps.notepad;

import androidx.fragment.app.Fragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

import com.nononsenseapps.notepad.fragments.FragmentSearch;
import com.nononsenseapps.notepad.fragments.FragmentSearchDeleted_;

@EActivity(resName = "fullscreen_fragment")
public class ActivitySearchDeleted extends ActivitySearch {

	@Override
	protected Fragment getFragment() {
		return new FragmentSearchDeleted_();
	}
}
