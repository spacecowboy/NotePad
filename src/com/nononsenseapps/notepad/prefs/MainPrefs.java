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

package com.nononsenseapps.notepad.prefs;

import com.nononsenseapps.notepad.FragmentLayout;
import com.nononsenseapps.notepad.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceFragment;
import android.util.Log;

public class MainPrefs extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {
	public static final String KEY_THEME = "key_current_theme";
	public static final String KEY_SORT_ORDER = "key_sort_order";
	public static final String KEY_SORT_TYPE = "key_sort_type";
	public static final String KEY_FONT_TYPE_EDITOR = "key_font_type_editor";
	public static final String KEY_FONT_SIZE_EDITOR = "key_font_size_editor";
	public static final String KEY_TEXT_PREVIEW = "key_text_preview";

	public static final String SANS = "Sans";
	public static final String SERIF = "Serif";
	public static final String MONOSPACE = "Monospace";

	public static final String THEME_DARK = "dark";
	public static final String THEME_LIGHT = "light";
	public static final String THEME_LIGHT_ICS_AB = "light_ab";

	public static final String DUEDATESORT = "duedate";
	public static final String TITLESORT = "title COLLATE NOCASE";
	public static final String MODIFIEDSORT = "modified";

	private ListPreference prefSortOrder;
	private ListPreference prefSortType;
	private ListPreference prefTheme;
	private ListPreference prefFontType;

	private Activity activity;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_main);

		prefSortOrder = (ListPreference) findPreference(KEY_SORT_ORDER);
		prefSortType = (ListPreference) findPreference(KEY_SORT_TYPE);
		prefTheme = (ListPreference) findPreference(KEY_THEME);
		prefFontType = (ListPreference) findPreference(KEY_FONT_TYPE_EDITOR);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Set summaries
		prefSortOrder.setSummary(prefSortOrder.getEntry());
		prefSortType.setSummary(prefSortType.getEntry());
		prefTheme.setSummary(prefTheme.getEntry());
		prefFontType.setSummary(prefFontType.getEntry());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(activity)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		try {
			if (activity.isFinishing()) {
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("settings",
							"isFinishing, should not update summaries");
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
			} else {
				if (KEY_THEME.equals(key)) {
					prefTheme.setSummary(prefTheme.getEntry());
				} else if (KEY_SORT_TYPE.equals(key)) {
					prefSortType.setSummary(prefSortType.getEntry());
				} else if (KEY_SORT_ORDER.equals(key)) {
					prefSortOrder.setSummary(prefSortOrder.getEntry());
				} else if (KEY_FONT_TYPE_EDITOR.equals(key)) {
					prefFontType.setSummary(prefFontType.getEntry());
				} else if (KEY_FONT_SIZE_EDITOR.equals(key)) {
				} else if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("settings", "Somethign changed!");
			}
		} catch (IllegalStateException e) {
			// This is just in case the "isFinishing" wouldn't be enough
			// The isFinishing will try to prevent us from doing something
			// stupid
			// This catch prevents the app from crashing if we do something
			// stupid
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d("settings", "Exception was caught: " + e.getMessage());
		}
	}

	/*
	 * private void updatePreviewFontSize(SharedPreferences sharedPreferences) {
	 * int size = sharedPreferences.getInt(KEY_FONT_SIZE_EDITOR,
	 * R.integer.default_editor_font_size); if (textPreview != null) { if
	 * (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings",
	 * "updatePreviewFontSize textPreview"); textPreview.setTextSize(size); }
	 * else { if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings",
	 * "updatePreviewFontSize textPreview was null!"); } }
	 * 
	 * private void updatePreviewFontType(SharedPreferences sharedPreferences) {
	 * String type = sharedPreferences.getString(KEY_FONT_TYPE_EDITOR, SANS); if
	 * (textPreview != null) { if (FragmentLayout.UI_DEBUG_PRINTS)
	 * Log.d("settings", "updatePreviewFontType textPreview!");
	 * textPreview.setTextType(type); } else { if
	 * (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings",
	 * "updatePreviewFontType textPreview was null!"); } }
	 */
}