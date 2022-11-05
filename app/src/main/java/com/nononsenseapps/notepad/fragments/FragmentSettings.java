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
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import android.widget.Toast;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.R;

import com.nononsenseapps.notepad.sync.files.JSONBackup;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;
import com.nononsenseapps.notepad.sync.orgsync.SDSynchronizer;
import com.nononsenseapps.util.AsyncTaskHelper;
import com.nononsenseapps.util.PermissionsHelper;
import com.nononsenseapps.util.PreferenceHelper;
import com.nononsenseapps.util.SharedPreferencesHelper;
import com.nononsenseapps.util.SyncGtaskHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import static com.nononsenseapps.util.PermissionsHelper.hasPermissions;
import static com.nononsenseapps.util.PermissionsHelper.permissionsGranted;

import static com.nononsenseapps.util.SharedPreferencesHelper.disableSdCardSync;

import static com.nononsenseapps.util.SharedPreferencesHelper.getSdDir;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Main top level settings fragment
 */
public class FragmentSettings extends PreferenceFragment implements SharedPreferences
        .OnSharedPreferenceChangeListener {

    // TODO useless ? you may want to delete this

    private static final int ACTIVITY_CODE_PICK_SD_DIR = 1;
    private static final int ACTIVITY_CODE_PICK_DROPBOX_DIR = 2;
    private static final int PERMISSION_CODE_GTASKS = 1;
    private static final int PERMISSION_CODE_SDCARD = 2;
    private SwitchPreference preferenceSyncSdCard;
    private SwitchPreference preferenceSyncGTasks;
    private SwitchPreference preferenceSyncDropbox;



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

        setupPassword();
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
                    disableSdCardSync(getActivity());
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
        preferenceSyncSdCard = (SwitchPreference) findPreference(getString(R.string
                .const_preference_sdcard_enabled_key));
        setSdDirectorySummary(sharedPreferences);
    }

    private void showFilePicker() {
        if (hasSDCardPermissions()) {
            // Start the filepicker
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(i, ACTIVITY_CODE_PICK_SD_DIR);
        } else {
            requestSDCardPermissions();
        }
    }



    private void setSdDirectorySummary(final SharedPreferences sharedPreferences) {
        preferenceSyncSdCard.setSummary(sharedPreferences.getString(getSdDirectoryKey(),
                SDSynchronizer.getDefaultOrgDir(getContext())));
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
            disableSdCardSync(getActivity());
        }
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
        if (hasGoogleAccountPermissions()) {
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
        } else {
            requestGoogleAccountPermissions();
        }
    }


    private void requestGoogleAccountPermissions() {
        requestPermissions(PermissionsHelper.PERMISSIONS_GTASKS, PERMISSION_CODE_GTASKS);
    }


    private void requestSDCardPermissions() {
        requestPermissions(PermissionsHelper.PERMISSIONS_SD, PERMISSION_CODE_SDCARD);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE_GTASKS:
                if (permissionsGranted(permissions, grantResults)) {
                    // Success, open dialog
                    showAccountDialog();
                } else {
                    Toast.makeText(getActivity(), "Permission denied :(. Show explanation for contacts",
                            Toast.LENGTH_LONG).show();
                    SharedPreferencesHelper.put(getActivity(),
                            R.string.const_preference_gtask_enabled_key, false);
                }
                break;
            case PERMISSION_CODE_SDCARD:
                if (permissionsGranted(permissions, grantResults)) {
                    // Success, open picker
                    showFilePicker();
                } else {
                    Toast.makeText(getActivity(), "Permission denied :(. Show explanation for SD card",
                            Toast.LENGTH_LONG).show();
                    disableSdCardSync(getActivity());
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();


    }


    private boolean hasGoogleAccountPermissions() {
        return hasPermissions(getActivity(), PermissionsHelper.PERMISSIONS_GTASKS);
    }

    private boolean hasSDCardPermissions() {
        return hasPermissions(getActivity(), PermissionsHelper.PERMISSIONS_SD);
    }

    private void setupPassword() {
        Preference preference = findPreference(getString(R.string.const_preference_password_key));
        if (preference != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    DialogPasswordSettings.showDialog(((AppCompatActivity) getActivity())
                            .getSupportFragmentManager());
                    return true;
                }
            });
        }
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
                            // (JSON) Backup.importLegacyBackup(getActivity());
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
                    showFilePicker();
                } else {
                    // Restart the service (started in activities)
                    OrgSyncService.stop(getActivity());
                    // Synchronize view also
                    if (preferenceSyncSdCard.isChecked()) {
                        preferenceSyncSdCard.setChecked(false);
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
