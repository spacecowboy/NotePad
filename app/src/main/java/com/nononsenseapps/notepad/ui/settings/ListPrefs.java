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

package com.nononsenseapps.notepad.ui.settings;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import java.util.ArrayList;

public class ListPrefs extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_list);

		// Fill listpreferences
		setEntries((ListPreference) findPreference(getString(R.string.pref_defaultlist)));
/*

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
*/

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
