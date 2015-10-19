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

package com.nononsenseapps.notepad.fragments;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.legacy.Backup;
import com.nononsenseapps.util.PreferenceHelper;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Main top level settings fragment
 */
public class FragmentSettings extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        setLangEntries((ListPreference) findPreference(getString(R.string
                .const_preference_locale_key)));

        setupAccount();
        setupDirectory();
        setupLegacyBackup();

        // Bind listeners to update summaries
        bindPreferenceSummaryToValue(R.string.const_preference_locale_key);
        bindPreferenceSummaryToValue(R.string.const_preference_theme_key);
        bindPreferenceSummaryToValue(R.string.const_preference_ringtone_key);
        bindPreferenceSummaryToValue(R.string.const_preference_gtask_account_key);
        bindPreferenceSummaryToValue(R.string.const_preference_sdcard_dir_key);
    }

    private void setupDirectory() {
        // todo
    }

    private void setupAccount() {
        // todo
    }

    private void setupLegacyBackup() {
        Preference preference = findPreference(getString(R.string
                .const_preference_legacybackup_key));
        if (preference != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    DialogRestoreBackup.showDialog(getFragmentManager(), new DialogConfirmBaseV11
                            .DialogConfirmedListener() {

                        @Override
                        public void onConfirm() {
                            Backup.importLegacyBackup(getActivity());
                        }

                    });
                    return true;
                }
            });
        }
    }

    private void bindPreferenceSummaryToValue(@StringRes int key) {
        Preference preference = findPreference(getString(key));
        if (preference != null) {
            // Set change listener
            preference.setOnPreferenceChangeListener(PreferenceHelper.sSummaryUpdater);
            // Trigger the listener immediately with the preference's  current value.
            PreferenceHelper.sSummaryUpdater.onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext()).getString(preference
                            .getKey(), ""));
        }
    }

    private void setLangEntries(ListPreference prefLang) {
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();

        entries.add(getString(R.string.localedefault));
        values.add("");

        String[] langs = getResources().getStringArray(R.array.translated_langs);

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

        // Set summary
        prefLang.setSummary(prefLang.getEntry());
    }
}
