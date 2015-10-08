/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.ui;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;

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
		int size = getContext().getResources().getInteger(R.integer.default_editor_font_size);
		mText.setTextSize(getSharedPreferences().getInt("removed", size));
		setTextType(getSharedPreferences().getString("removed", MainPrefs.SANS));

		return layout;

	}

	@Override
	public void onBindView(View view) {
		super.onBindView(view);

		//mText = (TextView) view.findViewById(R.id.prefTextPreview);
	}
	
	public static Typeface getTypeface(String type) {
		Typeface font;
		if (MainPrefs.MONOSPACE.equals(type)) {
			font = Typeface.MONOSPACE;
		} else if (MainPrefs.SERIF.equals(type)) {
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
