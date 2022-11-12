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

import static com.nononsenseapps.util.SharedPreferencesHelper.disableSdCardSync;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
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
class FragmentSettings_USELESS extends PreferenceFragment implements SharedPreferences
		.OnSharedPreferenceChangeListener {

	// TODO useless ? you may want to delete this


	private SwitchPreference preferenceSyncSdCard;
	private SwitchPreference preferenceSyncGTasks;


	private void setSdDirectorySummary(final SharedPreferences sharedPreferences) {
		preferenceSyncSdCard.setSummary(sharedPreferences.getString(SyncPrefs.KEY_SD_DIR_URI,
				SDSynchronizer.getDefaultOrgDir(getContext())));
	}


	@SuppressLint("CommitPrefEdits")
	private void saveNewDirectoryPath(Intent data) {
		File path = new File(data.getData().getPath());
		if (path.exists() && path.isDirectory() && path.canWrite()) {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
					(getActivity());
			sharedPreferences.edit().putString(SyncPrefs.KEY_SD_DIR_URI, path.toString()).commit();
			setSdDirectorySummary(sharedPreferences);
		} else {
			Toast.makeText(getActivity(), R.string.cannot_write_to_directory, Toast.LENGTH_SHORT)
					.show();
			disableSdCardSync(getActivity());
		}
	}


	private void setupPassword() {
		Preference preference = findPreference(getString(R.string.const_preference_password_key));
		if (preference != null) {
			preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					// DialogPasswordSettings.showDialog(((AppCompatActivity) getActivity())
//							.getSupportFragmentManager());
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

			switch (key) {
				case SyncPrefs.KEY_SYNC_ENABLE: {
					final boolean enabled = sharedPreferences.getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false);
					if (enabled) {
						//showAccountDialog();
					} else {
						SyncGtaskHelper.toggleSync(getActivity(), sharedPreferences);
						// Synchronize view also
						if (preferenceSyncGTasks.isChecked()) {
							preferenceSyncGTasks.setChecked(false);
						}
					}
					break;
				}
				case SyncPrefs.KEY_ACCOUNT:
					setAccountSummary(sharedPreferences);
					break;
				case SyncPrefs.KEY_SD_ENABLE: {
					final boolean enabled = sharedPreferences.getBoolean(SyncPrefs.KEY_SD_ENABLE, false);
					if (enabled) {
//                     showFilePicker();
					} else {
						// Restart the service (started in activities)
						OrgSyncService.stop(getActivity());
						// Synchronize view also
						if (preferenceSyncSdCard.isChecked()) {
							preferenceSyncSdCard.setChecked(false);
						}
					}
					break;
				}
			}
		} catch (IllegalStateException ignored) {
			// In case isFinishing isn't guard enough
		}
	}

	private void setAccountSummary(SharedPreferences sharedPreferences) {
		preferenceSyncGTasks.setSummary(sharedPreferences.getString(SyncPrefs.KEY_ACCOUNT, getString(R.string
				.settings_account_summary)));
	}
}
