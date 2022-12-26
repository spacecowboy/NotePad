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
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.helpers.FileHelper;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.PermissionsHelper;
import com.nononsenseapps.helpers.PreferencesHelper;
import com.nononsenseapps.helpers.SyncGtaskHelper;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTasksClient;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;

import java.io.IOException;

public class SyncPrefs extends PreferenceFragmentCompat
		implements OnSharedPreferenceChangeListener {

	// TODO these 6 are useles. Maybe we can reuse them if we find a newer sync service
	//  to replace google tasks
	public static final String KEY_SYNC_ENABLE = "syncEnablePref";
	public static final String KEY_ACCOUNT = "accountPref";
	public static final String KEY_FULLSYNC = "syncFull";
	public static final String KEY_SYNC_ON_START = "syncOnStart";
	public static final String KEY_SYNC_ON_CHANGE = "syncOnChange";
	public static final String KEY_BACKGROUND_SYNC = "syncInBackground";

	/**
	 * Used for sync on start and on change
	 */
	public static final String KEY_LAST_SYNC = "lastSync";
	private static final int PICK_ACCOUNT_CODE = 2;

	// SD sync
	public static final String KEY_SD_ENABLE = "pref_sync_sd_enabled";
	public static final String KEY_SD_SYNC_INFO = "pref_sdcard_sync_info";

	private FragmentActivity activity;

	private SwitchPreference prefSyncEnable;
	private Preference prefAccount;

	public static void setSyncInterval(Context activity, SharedPreferences sharedPreferences) {
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");
		boolean backgroundSync = sharedPreferences.getBoolean(KEY_BACKGROUND_SYNC, false);

		if (!accountName.isEmpty()) {
			Account account = SyncGtaskHelper.getAccount(AccountManager.get(activity), accountName);
			if (account != null) {
				if (!backgroundSync) {
					// Disable periodic syncing
					ContentResolver.removePeriodicSync(account,
							MyContentProvider.AUTHORITY, new Bundle());
				} else {
					// Convert from minutes to seconds
					long pollFrequency = 3600;
					// Set periodic syncing
					ContentResolver.addPeriodicSync(account, MyContentProvider.AUTHORITY,
							new Bundle(), pollFrequency);
				}
			} else {
				// can't do anything: the user did not add a google account to the device
			}
		}
	}

	@Override
	public void onCreatePreferences(@Nullable Bundle savInstState, String rootKey) {

		this.activity = this.getActivity();

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_sync);

		final SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// TODO this is useless, since all Google Tasks settings are disabled.
		prefAccount = findPreference(KEY_ACCOUNT);
		setAccountTitle(sharedPrefs);
		prefAccount.setOnPreferenceClickListener(preference -> {
			// ask for permissions needed to use google tasks
			// TODO useless, remove
			boolean granted = PermissionsHelper
					.hasPermissions(this.getContext(), PermissionsHelper.FOR_GOOGLETASKS);
			if (granted) {
				// Show dialog
				showAccountDialog();
			} else {
				this.requestPermissions(
						PermissionsHelper.FOR_GOOGLETASKS,
						PermissionsHelper.REQCODE_GOOGLETASKS);
			}
			return true;
		});

		prefSyncEnable = findPreference(KEY_SYNC_ENABLE);
		// Disable prefs if this is not correct build
		String API_KEY = Config.getGtasksApiKey(getActivity());
		prefSyncEnable.setEnabled(null != API_KEY && !API_KEY.contains(" "));

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
	public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		// if we got all permissions
		boolean granted = PermissionsHelper.permissionsGranted(permissions, grantResults);

		if (reqCode == PermissionsHelper.REQCODE_GOOGLETASKS) {
			if (granted) {
				// Success => open the dialog
				showAccountDialog();
			} else {
				// user refused: show warning and disable sync
				Toast.makeText(this.getContext(), R.string.permission_denied,
						Toast.LENGTH_SHORT).show();
				PreferencesHelper
						.put(getActivity(), SyncPrefs.KEY_SYNC_ENABLE, false);
			}
		}

		super.onRequestPermissionsResult(reqCode, permissions, grantResults);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(activity)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Shows a system popup to choose the google account to use for synchronizing notes.
	 * If the user has no google accounts on the device, a prompt will open, asking to
	 * add a new one
	 */
	private void showAccountDialog() {
		// do we need to check for permissions ? (there's 3 in the manifest)
		String hint = this.getString(R.string.select_account);
		var allowedAccountTypes = new String[] { "com.google" };
		Intent i = AccountManager.newChooseAccountIntent(null, null,
				allowedAccountTypes, hint, null, null,
				null);
		startActivityForResult(i, PICK_ACCOUNT_CODE);
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
		try {
			NnnLogger.debug(SyncPrefs.class, "onChanged");
			if (activity.isFinishing()) {
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
				return;
			}

			// => now we can safely continue
			switch (key) {
				case KEY_SYNC_ENABLE:
					toggleSync(prefs);
					break;
				case KEY_BACKGROUND_SYNC:
					setSyncInterval(activity, prefs);
					break;
				case KEY_ACCOUNT:
					NnnLogger.debug(SyncPrefs.class, "account");
					prefAccount.setTitle(prefs.getString(KEY_ACCOUNT, ""));
					// prefAccount.setSummary(getString(R.string.settings_account_summary));
					break;
				case KEY_SD_ENABLE:
					// Restart the sync service
					OrgSyncService.stop(getActivity());
					break;
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
			String chosenAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			userChoseAnAccountWithName(chosenAccountName);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Called when the user chooses one {@link Account} from the system popup
	 */
	private void userChoseAnAccountWithName(String chosenAccountName) {
		Account[] allAccounts = AccountManager
				.get(this.activity)
				.getAccountsByType("com.google");

		for (var chosenAccount : allAccounts) {
			if (!chosenAccount.name.equalsIgnoreCase(chosenAccountName)) continue;

			// we got the 1° (and hopefully only) match: proceed
			NnnLogger.debug(SyncPrefs.class, "step one");

			// work continues in callback, method afterGettingAuthToken()
			AccountManagerCallback<Bundle> callback =
					(b) -> afterGettingAuthToken(b, chosenAccount);

			// Request user's permission
			GoogleTasksClient.getAuthTokenAsync(activity, chosenAccount, callback);

			// do that only for the 1° match
			return;
		}
	}

	/**
	 * Called when the user has selected a Google account when pressing the enable Gtask
	 * switch. User wants to select an account to sync with. If we get an approval,
	 * activate sync and set periodicity also.
	 */
	private void afterGettingAuthToken(AccountManagerFuture<Bundle> future, Account account) {
		try {
			NnnLogger.debug(SyncPrefs.class, "step two");
			// If the user authorized your app to use the tasks API, a token is available
			// TODO here it crashes because the app is not registered into some kind of console
			String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);

			// Now we are authorized by the user.
			NnnLogger.debug(SyncPrefs.class, "step two-b: " + token);

			if (token != null && !token.isEmpty() && account != null) {

				// Also mark enabled as true, as the dialog was shown from enable button
				NnnLogger.debug(SyncPrefs.class, "step three: " + account.name);

				SharedPreferences customSharedPreference = PreferenceManager
						.getDefaultSharedPreferences(activity);
				customSharedPreference
						.edit()
						.putString(SyncPrefs.KEY_ACCOUNT, account.name)
						.putBoolean(KEY_SYNC_ENABLE, true)
						.commit();

				// Set it syncable
				ContentResolver
						.setSyncAutomatically(account, MyContentProvider.AUTHORITY, true);
				ContentResolver
						.setIsSyncable(account, MyContentProvider.AUTHORITY, 1);
				// Set sync frequency
				SyncPrefs.setSyncInterval(activity, customSharedPreference);
				// Set it syncable
				SyncGtaskHelper.toggleSync(this.activity, customSharedPreference);
				// And schedule an immediate sync
				SyncGtaskHelper.requestSyncIf(this.activity, SyncGtaskHelper.MANUAL);
			}
		} catch (OperationCanceledException | AuthenticatorException | IOException e) {
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
			Toast.makeText(this.activity, errMsg, Toast.LENGTH_SHORT).show();
			SyncGtaskHelper.disableSync(this.activity);
		}
	}


	private void toggleSync(SharedPreferences sharedPreferences) {
		boolean enabled = SyncGtaskHelper.toggleSync(getActivity(), sharedPreferences);
		if (enabled) {
			showAccountDialog();
		} else {
			// Synchronize view also
			if (prefSyncEnable.isChecked()) prefSyncEnable.setChecked(false);
		}
	}

	private void setAccountTitle(final SharedPreferences sharedPreferences) {
		prefAccount.setTitle(sharedPreferences.getString(KEY_ACCOUNT, ""));
		prefAccount.setSummary(R.string.settings_account_summary);
	}
}