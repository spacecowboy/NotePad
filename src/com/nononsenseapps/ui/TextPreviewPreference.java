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

package com.nononsenseapps.ui;

import com.nononsenseapps.notepad.NotesPreferenceFragment;
import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TextPreviewPreference extends Preference {

	//private final String TAG = getClass().getName();

	protected TextView mText = null;

	public TextPreviewPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TextPreviewPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {

		View layout = null;

		try {
			LayoutInflater mInflater = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			layout = mInflater.inflate(R.layout.preference_text_preview,
					parent, false);
		} catch (Exception e) {
			//Log.e(TAG, "Error creating seek bar preference", e);
		}
		
		mText = (TextView) layout.findViewById(R.id.prefTextPreview);
		
		// Retrieve settings here and set them on the text
		mText.setTextSize(getSharedPreferences().getInt(NotesPreferenceFragment.KEY_FONT_SIZE_EDITOR, 22));
		setTextType(getSharedPreferences().getString(NotesPreferenceFragment.KEY_FONT_TYPE_EDITOR, NotesPreferenceFragment.SANS));

		return layout;

	}

	@Override
	public void onBindView(View view) {
		super.onBindView(view);

		//mText = (TextView) view.findViewById(R.id.prefTextPreview);
	}
	
	public static Typeface getTypeface(String type) {
		Typeface font;
		if (NotesPreferenceFragment.MONOSPACE.equals(type)) {
			font = Typeface.MONOSPACE;
		} else if (NotesPreferenceFragment.SERIF.equals(type)) {
			font = Typeface.SERIF;
		} else {
			font = Typeface.SANS_SERIF;
		}
		return font;
	}

	public void setTextType(String type) {
		if (mText != null) {
			
			mText.setTypeface(getTypeface(type));
//			mText.post(new Runnable() {
//
//				@Override
//				public void run() {
//					if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Runnabletype: getText: " + mText.getText().toString());
//					mText.setTypeface(font);
//					mText.setText("Font changed in runnable");
//				}
//				
//			});
		}
	}

	public void setTextSize(float size) {
		if (mText != null) {
			final float mySize = size;
			mText.post(new Runnable() {

				@Override
				public void run() {
					mText.setTextSize(mySize);
					mText.setText("Size changed in runnable");
				}
				
			});
		}
	}

}
