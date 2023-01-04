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

import android.accounts.Account;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.helpers.FileHelper;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.PreferencesHelper;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;

public class SyncPrefs extends PreferenceFragmentCompat
		implements OnSharedPreferenceChangeListener {

	/**
	 * Used for sync on start and on change
	 */
	public static final String KEY_LAST_SYNC = "lastSync";
	private static final int PICK_ACCOUNT_CODE = 2;

	// SD sync
	public static final String KEY_SD_ENABLE = "pref_sync_sd_enabled";
	public static final String KEY_SD_SYNC_INFO = "pref_sdcard_sync_info";

	@Override
	public void onCreatePreferences(@Nullable Bundle savInstState, String rootKey) {

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_sync);

		final SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this.getContext());
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		findPreference(KEY_SD_ENABLE).setOnPreferenceClickListener(p -> {
			// if the ORG dir is inaccessible, disable SD sync
			String dir = FileHelper.getUserSelectedOrgDir(this.getContext());
			if (dir == null) {
				PreferencesHelper.disableSdCardSync(this.getContext());
				NnnLogger.warning(SyncPrefs.class, "Can't access org dir");
				return false;
			} else
				return true;
		});

		// write the folder path on the summary
		String orgdirpath = FileHelper.getUserSelectedOrgDir(this.getContext());
		String sdInfoSummary = this.getString(R.string.directory_summary_msg, orgdirpath);
		findPreference(KEY_SD_SYNC_INFO).setSummary(sdInfoSummary);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager
				.getDefaultSharedPreferences(this.getContext())
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Called when a shared preference is changed, added, or removed. This
	 * may be called even if a preference is set to its existing value.
	 * <p/>
	 * <p>This callback will be run on your main thread.
	 *
	 * @param prefs The {@link SharedPreferences} that received the change.
	 * @param key   The key of the preference that was changed, added, or removed
	 */
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		NnnLogger.debug(SyncPrefs.class, "onChanged");
		final String keySyncMaster = this.getString(R.string.key_pref_sync_enabled_master);
		try {

			if (this.getActivity().isFinishing()) {
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
				return;
			}

			// => now we can safely continue
			if (KEY_SD_ENABLE.equals(key)) {
				// Restart the sync service
				OrgSyncService.stop(getActivity());
			} else if (keySyncMaster.equals(key)) {
				// TODO force stop / re-enable all (user selected) sync services
			}
		} catch (IllegalStateException e) {
			// This is just in case the "isFinishing" wouldn't be enough
			// The isFinishing will try to prevent us from doing something stupid
			// This catch prevents the app from crashing if we do something stupid
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			// it was cancelled by the user. Let's ignore it in both cases
			return;
		}
		if (requestCode == PICK_ACCOUNT_CODE) {
			// the user has confirmed with a valid account on the account picker
			// String chosenAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			// then make and call something like userChoseAnAccountWithName(chosenAccountName);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Called when the user has selected an account when pressing the enable sync
	 * switch. User wants to select an account to sync with. If we get an approval,
	 * activate sync and set periodicity also.
	 */
	private void afterGettingAuthToken(AccountManagerFuture<Bundle> future, Account account) {
		try {
			NnnLogger.debug(SyncPrefs.class, "step two");


			if (account != null) {

				// Also mark enabled as true, as the dialog was shown from enable button
				NnnLogger.debug(SyncPrefs.class, "step three: " + account.name);

				SharedPreferences customSharedPreference = PreferenceManager
						.getDefaultSharedPreferences(this.getContext());
				customSharedPreference
						.edit()
						.putString("pref_key_for_the_account", account.name)
						.putBoolean("pref_to_enable_this_sync", true)
						.commit();

				// Set it syncable
				ContentResolver
						.setSyncAutomatically(account, MyContentProvider.AUTHORITY, true);
				ContentResolver
						.setIsSyncable(account, MyContentProvider.AUTHORITY, 1);
			}
		} catch (Exception e) {
			// OperationCanceledException:
			// * if the request was canceled for any reason
			// AuthenticatorException:
			// * if there was an error communicating with the authenticator or
			// * if the authenticator returned an invalid response or
			// * if the user did not register on the api console
			// IOException:
			// * if the authenticator returned an error response that
			// * indicates that it encountered an IOException while
			// * communicating with the authentication server
			String errMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
			Toast.makeText(this.getContext(), errMsg, Toast.LENGTH_SHORT).show();

		}
	}

}