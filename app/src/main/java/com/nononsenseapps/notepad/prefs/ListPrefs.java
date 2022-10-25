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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.helpers.TimeFormatter;

import android.app.Activity;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;

public class ListPrefs extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_list);

		// Fill listpreferences
		setEntries((ListPreference) findPreference(getString(R.string.pref_defaultlist)));

		// Bind summaries
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_sorttype)));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_defaultlist)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_list_title_fontfamily)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_list_title_fontstyle)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_list_body_fontfamily)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_list_fontsize)));

		//PrefsActivity
		//		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_listtype)));
		
		// Make the show checkbox dependant on the list type preference
		final Preference hideCheckboxes = findPreference(getString(R.string.pref_hidecheckboxes));
		Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object value) {
				String stringValue = value.toString();

				if (preference instanceof ListPreference) {
					// For list preferences, look up the correct display value in
					// the preference's 'entries' list.
					ListPreference listPreference = (ListPreference) preference;
					int index = listPreference.findIndexOfValue(stringValue);

					// Set the summary to reflect the new value.
					preference
							.setSummary(index >= 0 ? listPreference.getEntries()[index]
									: null);

				}
				else {
					// For all other preferences, set the summary to the value's
					// simple string representation.
					preference.setSummary(stringValue);
				}
				
				if (stringValue.equals(getString(R.string.const_listtype_tasks))) {
					hideCheckboxes.setEnabled(true);
				}
				else {
					hideCheckboxes.setEnabled(false);
				}
				
				
				return true;
			}
		};
		final Preference listtype = findPreference(getString(R.string.pref_listtype));
		listtype.setOnPreferenceChangeListener(listener);
		listener.onPreferenceChange(listtype, PreferenceManager.getDefaultSharedPreferences(
				listtype.getContext()).getString(listtype.getKey(),
				""));
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

		Cursor cursor = getActivity().getContentResolver().query(TaskList.URI,
				new String[] { TaskList.Columns._ID, TaskList.Columns.TITLE },
				null, null, TaskList.Columns.TITLE);
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
