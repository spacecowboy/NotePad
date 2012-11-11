package com.nononsenseapps.helpers.dualpane;

/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.nononsenseapps.notepad_donate.R;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class NoNonsenseListFragment extends ListFragment{

	protected DualLayoutActivity activity;
	/**
	 * This method will grab the frame layout specified in the xml and fill it with the standard list layout
	 * used for list fragments normally. This is to allow for better animations.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		int layout = R.layout.list_layout;

		// Gets a handle to the EditText in the the layout.
		ViewGroup fl = (ViewGroup) inflater.inflate(layout, container, false);
		inflater.inflate(R.layout.list_content, fl, true);
		
		return fl;
	}
	
	/**
	 * Will expect to be part of a DualLayoutActivity
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = (DualLayoutActivity) activity;
	}
}
