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

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragment;

import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MainPrefs extends PreferenceFragment {

	public static final String KEY_THEME = "key_current_theme";

	public static final String SANS = "Sans";
	public static final String SERIF = "Serif";
	public static final String MONOSPACE = "Monospace";


	public static final String WEEK_START_DEFAULT = "-1";
	public static final String WEEK_START_SATURDAY = "7";
	public static final String WEEK_START_SUNDAY = "1";
	public static final String WEEK_START_MONDAY = "2";

	@Override
	public void onCreatePreferences(@Nullable Bundle savInstState, String rootKey) {

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_main);

		// Fill listpreferences
		setLangEntries(findPreference(getString(R.string.pref_locale)), getContext());
		setDateEntries(findPreference(getString(R.string.key_pref_dateformat_short)), R.array.dateformat_short_values);
		setDateEntries(findPreference(getString(R.string.key_pref_dateformat_long)), R.array.dateformat_long_values);

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
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(KEY_THEME));
	}

	private void setDateEntries(ListPreference prefDate, int array) {
		final String[] values = getResources().getStringArray(array);

		final ArrayList<CharSequence> entries = new ArrayList<>();

		final GregorianCalendar cal = new GregorianCalendar(
				2099, 2, 27, 0, 59);

		for (final String val : values) {
			entries.add(TimeFormatter.getLocalDateString(
					getActivity(), val, cal.getTimeInMillis()));
		}

		prefDate.setEntries(entries.toArray(new CharSequence[0]));
		prefDate.setEntryValues(values);
	}

	private static void setLangEntries(ListPreference prefLang, Context context) {
		ArrayList<CharSequence> entries = new ArrayList<>();
		ArrayList<CharSequence> values = new ArrayList<>();

		entries.add(context.getString(R.string.localedefault));
		values.add("");

		String[] langs = context.getResources().getStringArray(R.array.translated_langs);

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
		prefLang.setEntries(entries.toArray(new CharSequence[0]));
		prefLang.setEntryValues(values.toArray(new CharSequence[0]));

		// Set summary
		prefLang.setSummary(prefLang.getEntry());
	}
}
