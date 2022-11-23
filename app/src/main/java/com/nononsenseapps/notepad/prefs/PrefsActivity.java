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

package com.nononsenseapps.notepad.prefs;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import androidx.core.app.NavUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;

import java.util.List;
import java.util.Locale;

public class PrefsActivity extends PreferenceActivity {

	private boolean mIsRoot = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setLanguage();

		// Add the arrow to go back
		if (getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		// TODO we should migrate to androidx preferences, and then use getSupportActionbar() with an appcompat theme. see the xml manifest
	}

	private void setLanguage() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Configuration config = getResources().getConfiguration();

		String lang = prefs.getString(getString(R.string.pref_locale), "");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if ("".equals(lang))
				locale = Locale.getDefault();
			else if (lang.length() == 5) {
				locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			} else {
				locale = new Locale(lang.substring(0, 2));
			}
			// Locale.setDefault(locale);
			config.locale = locale;
			getResources().updateConfiguration(config, getResources().getDisplayMetrics());
		}
	}

	@Override
	protected void onDestroy() {
		// Request a backup in case prefs changed
		// Safe to call multiple times
		new BackupManager(this).dataChanged();
		super.onDestroy();
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return true;
	}

	/**
	 * Populate the activity with the top-level headers.
	 */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.app_pref_headers, target);
		// When headers show, it is the root activity which should navigate up and not back.
		mIsRoot = true; // TODO but nobody ever sets it to false ??
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure.
			if (mIsRoot)
				NavUtils.navigateUpFromSameTask(this);
			else
				finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value. Handles the {@link ListPreference} specially.
	 */
	private static final Preference.OnPreferenceChangeListener
			sBindPreferenceSummaryToValueListener = (preference, value) -> {
		final String stringValue = value.toString();

		if (preference instanceof ListPreference) {
			// For list preferences, look up the correct display value in
			// the preference's 'entries' list.
			ListPreference listPreference = (ListPreference) preference;
			int index = listPreference.findIndexOfValue(stringValue);

			// Set the summary to reflect the new value, if possible
			preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

		}  else {
			// For all other preferences, set the summary to the value's
			// simple string representation.
			preference.setSummary(stringValue);
		}
		return true;
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	public static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
				PreferenceManager
						.getDefaultSharedPreferences(preference.getContext())
						.getString(preference.getKey(), ""));
	}
}
