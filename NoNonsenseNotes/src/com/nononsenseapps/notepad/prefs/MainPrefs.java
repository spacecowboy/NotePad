/*
 * Copyright (C) 2012 Jonas Kalderstam
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nononsenseapps.notepad.prefs;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.text.format.DateFormat;
import android.text.format.Time;

public class MainPrefs extends PreferenceFragment {
	public static final String KEY_THEME = "key_current_theme";
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

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_main);

		// Fill listpreferences
		setEntries((ListPreference) findPreference(KEY_DEFAULT_LIST));
		setLangEntries((ListPreference) findPreference(getString(R.string.pref_locale)));
		setDateEntries((ListPreference) findPreference(getString(R.string.key_pref_dateformat_short)), R.array.dateformat_short_values);
		setDateEntries((ListPreference) findPreference(getString(R.string.key_pref_dateformat_long)), R.array.dateformat_long_values);

		// Bind summaries
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(KEY_SORT_TYPE));
		PrefsActivity.bindPreferenceSummaryToValue(findPreference(KEY_THEME));
		// PrefsActivity
		// .bindPreferenceSummaryToValue(findPreference(KEY_FONT_TYPE_EDITOR));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(KEY_WEEK_START_DAY));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(KEY_DEFAULT_LIST));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_locale)));

		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_dateformat_long)));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_dateformat_short)));
	}

	private void setDateEntries(ListPreference prefDate, int array) {
		final String[] values = getResources().getStringArray(
				array);

		final ArrayList<CharSequence> entries = new ArrayList<CharSequence>();

		final GregorianCalendar cal = new GregorianCalendar(2099, 2, 27, 23, 59);
		// Format values
		for (final String val : values) {
			entries.add(DateFormat.format(val, cal));
		}
		
		prefDate.setEntries(entries.toArray(new CharSequence[entries.size()]));
		prefDate.setEntryValues(values);
	}

	private void setLangEntries(ListPreference prefLang) {
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
			}
			else {
				l = new Locale(lang.substring(0, 2));
			}

			entries.add(l.getDisplayName(l));
			values.add(lang);
		}
		prefLang.setEntries(entries.toArray(new CharSequence[entries.size()]));
		prefLang.setEntryValues(values.toArray(new CharSequence[values.size()]));

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

		// TODO fix from old version
		// listSpinner.setDefaultValue(Long.toString(MainActivity.getAList(
		// getActivity(), -1)));

		Cursor cursor = getActivity().getContentResolver().query(
				TaskList.URI,
				new String[] { TaskList.Columns._ID, TaskList.Columns.TITLE },
				null,
				null,
				getResources().getString(R.string.const_as_alphabetic,
						TaskList.Columns.TITLE));
		if (cursor != null) {
			if (!cursor.isClosed() && !cursor.isAfterLast()) {
				while (cursor.moveToNext()) {
					entries.add(cursor.getString(1));
					values.add(Long.toString(cursor.getLong(0)));
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
}
