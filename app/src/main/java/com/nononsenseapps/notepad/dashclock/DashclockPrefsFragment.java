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

package com.nononsenseapps.notepad.dashclock;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;

import java.util.ArrayList;

/**
 * This app can be used with dashclock. This fragment shows the settings to configure
 * the dashclock plug-in
 */
public class DashclockPrefsFragment extends PreferenceFragmentCompat {

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static final Preference.OnPreferenceChangeListener
			sBindPreferenceSummaryToValueListener = (preference, value) -> {
		String stringValue = value.toString();

		if (preference instanceof ListPreference listPreference) {
			// For list preferences, look up the correct display value in
			// the preference's 'entries' list.
			int index =
					listPreference.findIndexOfValue(stringValue);

			// Set the summary to reflect the new value.
			preference.setSummary(index >= 0 ?
					listPreference
							.getEntries()[index] :
					null);

		} else {
			// For all other preferences, set the summary to the value's
			// simple string representation.
			preference.setSummary(stringValue);
		}
		return true;
	};

	@Override
	public void onCreatePreferences(@Nullable Bundle savInstState, String rootKey) {

		addPreferencesFromResource(R.xml.dashclock_pref_general);

		// Bind the summaries of EditText/List/Dialog/Ringtone preferences
		// to their values. When their values change, their summaries are
		// updated to reflect the new value, per the Android Design
		// guidelines.
		bindPreferenceSummaryToValue(findPreference("list_spinner"));
		setEntries(getActivity(), findPreference("list_spinner"));

		bindPreferenceSummaryToValue(findPreference("list_due_upper_limit"));
	}

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format
	 * is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
				PreferenceManager
						.getDefaultSharedPreferences(preference.getContext())
						.getString(preference.getKey(), "")
		);
	}

	/**
	 * Reads the lists from database. Also adds "All lists" as the first item.
	 */
	private static void setEntries(Context context, ListPreference listSpinner) {

		ArrayList<CharSequence> entries = new ArrayList<>();
		ArrayList<CharSequence> values = new ArrayList<>();

		// Start with all lists
		entries.add("All lists");
		values.add("-1");
		// Set it as the default value also
		//listSpinner.setDefaultValue("-1");

		Cursor cursor = context
				.getContentResolver()
				.query(TaskList.URI, TaskList.Columns.FIELDS, null, null,
						TaskList.Columns.TITLE);
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
			listSpinner.setEntries(entries.toArray(new CharSequence[0]));
			listSpinner.setEntryValues(values.toArray(new CharSequence[0]));
			listSpinner.setSummary(listSpinner.getEntry());
		}
	}

}
