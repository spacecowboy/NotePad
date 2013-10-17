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

import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.helpers.TimeFormatter;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class MainPrefs extends PreferenceFragment {
	public static final String KEY_THEME = "key_current_theme";
	// public static final String KEY_WEEK_START_DAY =
	// "preferences_week_start_day";

	public static final String SANS = "Sans";
	public static final String SERIF = "Serif";
	public static final String MONOSPACE = "Monospace";

	// public static final String THEME_DARK = "dark";
	// public static final String THEME_BLACK = "black";
	// public static final String THEME_LIGHT_ICS_AB = "light_ab";

	public static final String WEEK_START_DEFAULT = "-1";
	public static final String WEEK_START_SATURDAY = "7";
	public static final String WEEK_START_SUNDAY = "1";
	public static final String WEEK_START_MONDAY = "2";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_main);

		// Fill listpreferences
		setLangEntries((ListPreference) findPreference(getString(R.string.pref_locale)));
		setDateEntries(
				(ListPreference) findPreference(getString(R.string.key_pref_dateformat_short)),
				R.array.dateformat_short_values);
		setDateEntries(
				(ListPreference) findPreference(getString(R.string.key_pref_dateformat_long)),
				R.array.dateformat_long_values);

		// Bind summaries
		// PrefsActivity
		// .bindPreferenceSummaryToValue(findPreference(KEY_FONT_TYPE_EDITOR));
		// PrefsActivity
		// .bindPreferenceSummaryToValue(findPreference(KEY_WEEK_START_DAY));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_locale)));

		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_dateformat_long)));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_dateformat_short)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_editor_title_fontfamily)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_editor_title_fontstyle)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_editor_body_fontfamily)));
		PrefsActivity
		.bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_editor_fontsize)));
		updateDonateStatus();
	}

	private void updateDonateStatus() {
		final boolean isPremium = PreferenceManager
				.getDefaultSharedPreferences(getActivity()).getBoolean(
						ActivityMain.PREMIUMSTATUS, false);
		Preference pref = findPreference(KEY_THEME);
		findPreference(KEY_THEME).setEnabled(isPremium);
		if (isPremium) {
			// Bind summary
			PrefsActivity.bindPreferenceSummaryToValue(pref);
		}
		else {
			// Set donate message
			pref.setSummary(R.string.donate_to_unlock);
		}
	}

	private void setDateEntries(ListPreference prefDate, int array) {
		final String[] values = getResources().getStringArray(array);

		final ArrayList<CharSequence> entries = new ArrayList<CharSequence>();

		final GregorianCalendar cal = new GregorianCalendar(2099, 2, 27, 0, 59);

		for (final String val : values) {
			entries.add(TimeFormatter.getLocalDateString(getActivity(), val,
					cal.getTimeInMillis()));
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
}
