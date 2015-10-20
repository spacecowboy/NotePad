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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.legacy.Backup;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;
import com.nononsenseapps.notepad.sync.orgsync.SDSynchronizer;
import com.nononsenseapps.util.PreferenceHelper;
import com.nononsenseapps.util.SyncGtaskHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Main top level settings fragment
 */
public class FragmentSettings extends PreferenceFragment implements SharedPreferences
        .OnSharedPreferenceChangeListener {

    private static final int ACTIVITY_CODE_PICK_SD_DIR = 1;
    private SwitchPreference preferenceSdDir;
    private SwitchPreference preferenceSyncGTasks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        setLangEntries((ListPreference) findPreference(getString(R.string
                .const_preference_locale_key)));

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                (getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        buildGuard();
        setupAccount(sharedPreferences);
        setupDirectory(sharedPreferences);
        setupLegacyBackup();

        // Bind listeners to update summaries
        bindPreferenceSummaryToValue(R.string.const_preference_locale_key);
        bindPreferenceSummaryToValue(R.string.const_preference_theme_key);
        bindPreferenceSummaryToValue(R.string.const_preference_ringtone_key);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_CODE_PICK_SD_DIR:
                if (resultCode == Activity.RESULT_OK) {
                    saveNewDirectoryPath(data);
                } else {
                    disableSdCardSync();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void buildGuard() {
        // Disable prefs if this is not correct build
        findPreference(getString(R.string.const_preference_gtask_enabled_key)).setEnabled(null !=
                Config.getGtasksApiKey(getActivity()) && !Config.getGtasksApiKey(getActivity())
                .contains(" "));
    }

    private void setupDirectory(final SharedPreferences sharedPreferences) {
        preferenceSdDir = (SwitchPreference) findPreference(getString(R.string
                .const_preference_sdcard_enabled_key));
        setSdDirectorySummary(sharedPreferences);
    }

    private void showFilePicker(SharedPreferences sharedPreferences) {
        // Start the filepicker
        Intent i = new Intent(getActivity(), FilePickerActivity.class);

        i.putExtra(FilePickerActivity.EXTRA_START_PATH, sharedPreferences.getString
                (getSdDirectoryKey(), SDSynchronizer.DEFAULT_ORG_DIR)).putExtra
                (FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false).putExtra(FilePickerActivity
                .EXTRA_ALLOW_CREATE_DIR, true).putExtra(FilePickerActivity.EXTRA_MODE,
                FilePickerActivity.MODE_DIR);
        startActivityForResult(i, ACTIVITY_CODE_PICK_SD_DIR);
    }

    private void setSdDirectorySummary(final SharedPreferences sharedPreferences) {
        preferenceSdDir.setSummary(sharedPreferences.getString(getSdDirectoryKey(),
                SDSynchronizer.DEFAULT_ORG_DIR));
    }

    @SuppressLint("CommitPrefEdits")
    private void saveNewDirectoryPath(Intent data) {
        File path = new File(data.getData().getPath());
        if (path.exists() && path.isDirectory() && path.canWrite()) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                    (getActivity());
            sharedPreferences.edit().putString(getSdDirectoryKey(), path.toString()).commit();
            setSdDirectorySummary(sharedPreferences);
        } else {
            Toast.makeText(getActivity(), R.string.cannot_write_to_directory, Toast.LENGTH_SHORT)
                    .show();
            disableSdCardSync();
        }
    }

    private void disableSdCardSync() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(getString
                (R.string.const_preference_sdcard_enabled_key), false).apply();
    }

    private String getSdDirectoryKey() {
        return getString(R.string.const_preference_sdcard_dir_key);
    }

    private void setupAccount(SharedPreferences sharedPreferences) {
        preferenceSyncGTasks = (SwitchPreference) findPreference(getString(R.string
                .const_preference_gtask_enabled_key));
        setAccountSummary(sharedPreferences);
    }

    private void showAccountDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("accountdialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new DialogGoogleAccount();
        newFragment.setArguments(Bundle.EMPTY);
        newFragment.show(ft, "accountdialog");
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

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     * <p/>
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            if (getActivity().isFinishing()) {
                return;
            }

            if (key.equals(getString(R.string.const_preference_gtask_enabled_key))) {
                final boolean enabled = sharedPreferences.getBoolean(getString(R.string
                        .const_preference_gtask_enabled_key), false);
                if (enabled) {
                    showAccountDialog();
                } else {
                    SyncGtaskHelper.toggleSync(getActivity(), sharedPreferences);
                    // Synchronize view also
                    if (preferenceSyncGTasks.isChecked()) {
                        preferenceSyncGTasks.setChecked(false);
                    }
                }
            } else if (key.equals(getString(R.string.const_preference_gtask_account_key))) {
                setAccountSummary(sharedPreferences);
            } else if (key.equals(getString(R.string.const_preference_sdcard_enabled_key))) {
                final boolean enabled = sharedPreferences.getBoolean(getString(R.string
                        .const_preference_sdcard_enabled_key), false);
                if (enabled) {
                    showFilePicker(sharedPreferences);
                } else {
                    // Restart the service (started in activities)
                    OrgSyncService.stop(getActivity());
                    // Synchronize view also
                    if (preferenceSdDir.isChecked()) {
                        preferenceSdDir.setChecked(false);
                    }
                }
            }
        } catch (IllegalStateException ignored) {
            // In case isFinishing isn't guard enough
        }
    }

    private void setAccountSummary(SharedPreferences sharedPreferences) {
        preferenceSyncGTasks.setSummary(sharedPreferences.getString(getString(R.string
                .const_preference_gtask_account_key), getString(R.string
                .settings_account_summary)));
    }
}
