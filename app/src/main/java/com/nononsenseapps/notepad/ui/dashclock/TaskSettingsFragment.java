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

package com.nononsenseapps.notepad.ui.dashclock;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import java.util.ArrayList;

public class TaskSettingsFragment extends PreferenceFragment {

    public TaskSettingsFragment() {
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener
            sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object value) {
                    String stringValue = value.toString();

                    if (preference instanceof ListPreference) {
                        // For list preferences, look up the correct display value in
                        // the preference's 'entries' list.
                        ListPreference listPreference =
                                (ListPreference) preference;
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
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.dashclock_pref_general);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        bindPreferenceSummaryToValue(findPreference("list_spinner"));
        setEntries(getActivity(),
                (ListPreference) findPreference("list_spinner"));

        bindPreferenceSummaryToValue(findPreference("list_due_upper_limit"));
    }

    /**
     * Use my own layout to override android's default margins
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater
                .inflate(R.layout.fragment_dialog_prefs, container, false);
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
        preference.setOnPreferenceChangeListener(
                sBindPreferenceSummaryToValueListener);

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
     *
     * @return
     */
    private static void setEntries(Activity activity,
            ListPreference listSpinner) {

        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        // Start with all lists
        entries.add("All lists");
        values.add("-1");
        // Set it as the default value also
        //listSpinner.setDefaultValue("-1");

        Cursor cursor = activity.getContentResolver()
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
            listSpinner.setEntries(
                    entries.toArray(new CharSequence[entries.size()]));
            listSpinner.setEntryValues(
                    values.toArray(new CharSequence[values.size()]));

            listSpinner.setSummary(listSpinner.getEntry());
        }
    }

}
