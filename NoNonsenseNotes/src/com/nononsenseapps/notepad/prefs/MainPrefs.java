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

import java.util.ArrayList;
import java.util.Locale;

import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceFragment;
import android.provider.BaseColumns;

public class MainPrefs extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {
	public static final String KEY_THEME = "key_current_theme";
	public static final String KEY_SORT_ORDER = "key_sort_order";
	public static final String KEY_SORT_TYPE = "key_sort_type";
	public static final String KEY_FONT_TYPE_EDITOR = "key_font_type_editor";
	public static final String KEY_FONT_SIZE_EDITOR = "key_font_size_editor";
	public static final String KEY_TEXT_PREVIEW = "key_text_preview";
	public static final String KEY_WEEK_START_DAY = "preferences_week_start_day";
	public static final String KEY_DEFAULT_LIST = "key_default_list_id";
	public static final String KEY_LISTHEADERS = "key_listheaders";

	public static final String SANS = "Sans";
	public static final String SERIF = "Serif";
	public static final String MONOSPACE = "Monospace";

	public static final String THEME_DARK = "dark";
	public static final String THEME_BLACK = "black";
	public static final String THEME_LIGHT = "light";
	public static final String THEME_LIGHT_ICS_AB = "light_ab";

	public static final String DUEDATESORT = "duedate";
	public static final String TITLESORT = "title COLLATE NOCASE";
	public static final String MODIFIEDSORT = "modified";
	public static final String POSSUBSORT = "truepos";

	public static final String WEEK_START_DEFAULT = "-1";
	public static final String WEEK_START_SATURDAY = "7";
	public static final String WEEK_START_SUNDAY = "1";
	public static final String WEEK_START_MONDAY = "2";
	public static final String KEY_HIDDENCHECKBOX = "key_hiddencheckbox";
	public static final String KEY_HIDDENNOTE = "key_hiddennote";
	public static final String KEY_HIDDENDATE = "key_hiddendate";
	public static final String KEY_TITLEROWS = "key_titlerows";

	private ListPreference prefSortOrder;
	private ListPreference prefSortType;
	private ListPreference prefTheme;
	private ListPreference prefFontType;
	private ListPreference prefWeekStart;
	private ListPreference prefDefaultList;
	private ListPreference prefTitleRows;
	private ListPreference prefLang;

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
		prefWeekStart = (ListPreference) findPreference(KEY_WEEK_START_DAY);
		prefDefaultList = (ListPreference) findPreference(KEY_DEFAULT_LIST);
		prefTitleRows = (ListPreference) findPreference(KEY_TITLEROWS);
		prefLang = (ListPreference) findPreference(getString(R.string.pref_locale));

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Set summaries
		prefSortOrder.setSummary(prefSortOrder.getEntry());
		prefSortType.setSummary(prefSortType.getEntry());
		prefTheme.setSummary(prefTheme.getEntry());
		prefFontType.setSummary(prefFontType.getEntry());
		prefWeekStart.setSummary(prefWeekStart.getEntry());
		prefTitleRows.setSummary(prefTitleRows.getEntry());

		setEntries(prefDefaultList);
		setLangEntries(prefLang, sharedPrefs);
	}

	private void setLangEntries(ListPreference prefLang, SharedPreferences prefs) {
		ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
		ArrayList<CharSequence> values = new ArrayList<CharSequence>();

		entries.add(getString(R.string.localedefault));
		values.add("");

		String[] langs = getResources()
				.getStringArray(R.array.translated_langs);

		for (String lang : langs) {
			Locale l;
			if (lang.length() == 5) {
				l = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			} else {
				l = new Locale(lang.substring(0, 2));
			}
			
			entries.add(l.getDisplayName(l));
			values.add(lang);
		}
		prefLang.setEntries(entries.toArray(new CharSequence[entries.size()]));
		prefLang.setEntryValues(values.toArray(new CharSequence[values.size()]));

		// Set currently selected value
		//String lang = prefs.getString(getString(R.string.pref_locale), "");

		// Set summary
		prefLang.setSummary(prefLang.getEntry());
	}

	/**
	 * Reads the lists from database. Also adds "All lists" as the first item.
	 * 
	 * @return
	 */
	private void setEntries(ListPreference listSpinner) {

		ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
		ArrayList<CharSequence> values = new ArrayList<CharSequence>();

		// Start with all lists
		// entries.add(getText(R.string.show_from_all_lists));
		// values.add(Long.toString(MainActivity.ALL_NOTES_ID));
		// Set it as the default value also
		listSpinner.setDefaultValue(Long.toString(MainActivity.getAList(
				getActivity(), -1)));

		Cursor cursor = getActivity().getContentResolver().query(
				NotePad.Lists.CONTENT_VISIBLE_URI,
				new String[] { BaseColumns._ID,
						NotePad.Lists.COLUMN_NAME_TITLE }, null, null,
				NotePad.Lists.SORT_ORDER);
		if (cursor != null) {
			if (!cursor.isClosed() && !cursor.isAfterLast()) {
				while (cursor.moveToNext()) {
					entries.add(cursor.getString(cursor
							.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE)));
					values.add(Long.toString(cursor.getLong(cursor
							.getColumnIndex(BaseColumns._ID))));
				}
			}

			cursor.close();
		}

		// Set the values
		if (listSpinner != null) {
			listSpinner.setEntries(entries.toArray(new CharSequence[entries
					.size()]));
			listSpinner.setEntryValues(values.toArray(new CharSequence[values
					.size()]));

			listSpinner.setSummary(listSpinner.getEntry());
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(activity)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		try {
			if (activity.isFinishing()) {
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
			} else {
				if (KEY_THEME.equals(key)) {
					prefTheme.setSummary(prefTheme.getEntry());
				} else if (KEY_TITLEROWS.equals(key)) {
					prefTitleRows.setSummary(prefTitleRows.getEntry());
				} else if (KEY_SORT_TYPE.equals(key)) {
					prefSortType.setSummary(prefSortType.getEntry());
				} else if (KEY_SORT_ORDER.equals(key)) {
					prefSortOrder.setSummary(prefSortOrder.getEntry());
				} else if (KEY_FONT_TYPE_EDITOR.equals(key)) {
					prefFontType.setSummary(prefFontType.getEntry());
				} else if (KEY_WEEK_START_DAY.equals(key)) {
					prefWeekStart.setSummary(prefWeekStart.getEntry());
				} else if (KEY_FONT_SIZE_EDITOR.equals(key)) {
				} else if (KEY_DEFAULT_LIST.equals(key)) {
					prefDefaultList.setSummary(prefDefaultList.getEntry());
				} else if (getString(R.string.pref_locale).equals(key)) {
					prefLang.setSummary(prefLang.getEntry());
				}
			}
		} catch (IllegalStateException e) {
			// This is just in case the "isFinishing" wouldn't be enough
			// The isFinishing will try to prevent us from doing something
			// stupid
			// This catch prevents the app from crashing if we do something
			// stupid
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
