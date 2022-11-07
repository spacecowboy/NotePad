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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTasksClient;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;
import com.nononsenseapps.notepad.sync.orgsync.SDSynchronizer;
import com.nononsenseapps.util.PermissionsHelper;
import com.nononsenseapps.util.SharedPreferencesHelper;
import com.nononsenseapps.util.SyncGtaskHelper;

import java.io.IOException;

// import com.nononsenseapps.notepad.NotePad;

public class SyncPrefs extends PreferenceFragment implements OnSharedPreferenceChangeListener {

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
	public static final String KEY_SD_DIR_URI = "pref_sync_sd_dir_uri";
	private static final int PICK_SD_DIR_CODE = 1;


	private Activity activity;

	private Preference prefAccount;
	private Preference prefSdDir;


	// private Preference prefSyncFreq;


	public static void setSyncInterval(Context activity, SharedPreferences sharedPreferences) {
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");
		boolean backgroundSync = sharedPreferences.getBoolean(KEY_BACKGROUND_SYNC, false);

		if (accountName != null && !accountName.isEmpty()) {
			Account account = SyncGtaskHelper.getAccount(AccountManager.get(activity), accountName);
			if (account != null) {
				if (!backgroundSync) {
					// Disable periodic syncing
					ContentResolver
							.removePeriodicSync(account, MyContentProvider.AUTHORITY, new Bundle());
				} else {
					// Convert from minutes to seconds
					long pollFrequency = 3600;
					// Set periodic syncing
					ContentResolver.addPeriodicSync(
							account,
							MyContentProvider.AUTHORITY, new Bundle(),
							pollFrequency);
				}
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_sync);

		prefAccount = findPreference(KEY_ACCOUNT);
		// prefSyncFreq = findPreference(KEY_SYNC_FREQ);


		final SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Set summaries
		setAccountTitle(sharedPrefs);

		prefAccount.setOnPreferenceClickListener(preference -> {
			boolean granted = PermissionsHelper
					.hasPermissions(this.getContext(), PermissionsHelper.PERMISSIONS_GTASKS);
			if (granted) {
				// Show dialog
				showAccountDialog();
			} else {
				this.requestPermissions(
						PermissionsHelper.PERMISSIONS_GTASKS,
						PermissionsHelper.REQUEST_CODE_GTASKS_PERMISSIONS);
			}

			return true;
		});

		// Disable prefs if this is not correct build
		String API_KEY = Config.getGtasksApiKey(getActivity());
		findPreference(KEY_SYNC_ENABLE).setEnabled(null != API_KEY && !API_KEY.contains(" "));

		// SD Card
		prefSdDir = findPreference(KEY_SD_DIR_URI);
		setSdDirSummary(sharedPrefs);
		prefSdDir.setOnPreferenceClickListener(preference -> {
			boolean ok = PermissionsHelper
					.hasPermissions(this.getContext(), PermissionsHelper.PERMISSIONS_SD);
			if (ok) {
				// we can read the filesystem => show the filepicker
				showFolderPickerActivity();
			} else {
				this.requestPermissions(
						PermissionsHelper.PERMISSIONS_SD,
						PermissionsHelper.REQUEST_CODE_SD_PERMISSIONS);
			}
			return true;
		});
	}

	@Override
	public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		boolean granted = PermissionsHelper.permissionsGranted(permissions, grantResults);
		switch (reqCode) {
			case PermissionsHelper.REQUEST_CODE_SD_PERMISSIONS:
				if (granted) {
					// we got the permission to read the file system: now, show the filepicker
					showFolderPickerActivity();
				} else {
					// warn the user that the permission was denied
					Toast.makeText(this.getContext(), R.string.permission_denied,
							Toast.LENGTH_SHORT).show();
					SharedPreferencesHelper.disableSdCardSync(this.getContext());
				}
				break;
			case PermissionsHelper.REQUEST_CODE_GTASKS_PERMISSIONS:
				if (granted) {
					// Success => open the dialog
					showAccountDialog();
				} else {
					// user refused: show warning and disable sync
					Toast.makeText(this.getContext(), R.string.permission_denied,
							Toast.LENGTH_SHORT).show();
					SharedPreferencesHelper
							.put(getActivity(), SyncPrefs.KEY_SYNC_ENABLE, false);
				}
				break;
			default:
				break;
		}

		super.onRequestPermissionsResult(reqCode, permissions, grantResults);
	}

	/**
	 * Shows the system's default filepicker, to  let the user choose a directory. See:
	 * https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
	 */
	private void showFolderPickerActivity() {
		var i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

		var sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

		// get the previously selected Uri
		String defaultDir = SDSynchronizer.getDefaultOrgDir(this.getContext());
		String oldSetting = sharedPrefs.getString(KEY_SD_DIR_URI, defaultDir);
		Uri uriToLoad = Uri.parse(oldSetting);

		// don't add this: it stops working on some devices, like the emulator with API 25!
		// i.setType(DocumentsContract.Document.MIME_TYPE_DIR);

		// specify a URI for the directory that should be opened in
		// the system file picker when it loads.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
		}

		// Start the filepicker
		startActivityForResult(i, PICK_SD_DIR_CODE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(activity)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Shows a system popup to choose the google account to use for synchronizing notes. If the
	 * user has no google accounts on the device, a prompt will open, asking to add a new one
	 */
	private void showAccountDialog() {
		// TODO do we need to check for permissions ? (there's 3 in the manifest)
		String hint = this.getString(R.string.select_account);
		var allowedAccountTypes = new String[] { "com.google" };
		Intent i = AccountManager.newChooseAccountIntent(null, null,
				allowedAccountTypes, hint, null, null,
				null);
		startActivityForResult(i, PICK_ACCOUNT_CODE);
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		try {
			Log.d("syncPrefs", "onChanged");
			if (activity.isFinishing()) {
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
			} else {
				if (KEY_SYNC_ENABLE.equals(key)) {
					toggleSync(prefs);
				} else if (KEY_BACKGROUND_SYNC.equals(key)) {
					setSyncInterval(activity, prefs);
				} else if (KEY_ACCOUNT.equals(key)) {
					Log.d("syncPrefs", "account");
					prefAccount.setTitle(prefs.getString(KEY_ACCOUNT, ""));
				} else if (KEY_SD_ENABLE.equals(key)) {
					// Restart the sync service
					OrgSyncService.stop(getActivity());
				} else if (KEY_SD_DIR_URI.equals(key)) {
					setSdDirSummary(prefs);
				}
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
			// the user has confirmed with a valid account
			String chosenAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			userChoseAnAccountWithName(chosenAccountName);
			return;
		}

		if (requestCode != PICK_SD_DIR_CODE) {
			// if it wasn't the filepiker nor the account picker, exit now
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}

		// "data" contains the URI for the user-selected directory, A.K.A. the "document tree"
		Uri uri = data.getData();

		// represents the directory that the user just picked. Use this instead of the "File" class
		var docDir = DocumentFile.fromTreeUri(this.getContext(), uri);

		boolean isOk = docDir != null && docDir.exists() && docDir.isDirectory() && docDir.canWrite();
		if (isOk) {
			PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.edit()
					.putString(KEY_SD_DIR_URI, uri.toString())
					.apply();
			Toast.makeText(getActivity(), R.string.feature_is_WIP, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getActivity(), R.string.cannot_write_to_directory, Toast.LENGTH_SHORT)
					.show();
		}
	}

	/**
	 * Called when the user chooses one {@link Account} from the system popup
	 */
	private void userChoseAnAccountWithName(String chosenAccountName) {
		Account[] allAccounts = AccountManager.get(this.activity).getAccountsByType("com.google");

		for (var chosenAccount : allAccounts) {
			if (!chosenAccount.name.equalsIgnoreCase(chosenAccountName)) continue;

			// we got the 1° (and hopefully only) match: proceed
			Log.d("prefsActivity", "step one");

			// work continues in callback, method afterGettingAuthToken()
			AccountManagerCallback<Bundle> callback = (b) -> afterGettingAuthToken(b, chosenAccount);

			// Request user's permission
			GoogleTasksClient.getAuthTokenAsync(activity, chosenAccount, callback);

			// do that only for the 1° match
			return;
		}
	}

	/**
	 * Called when the user has selected a Google account when pressing the enable Gtask switch.
	 * User wants to select an account to sync with. If we get an approval, activate sync
	 * and set periodicity also.
	 */
	private void afterGettingAuthToken(AccountManagerFuture<Bundle> future, Account account) {
		try {
			Log.d("prefsActivity", "step two");
			// If the user has authorized your application to use the tasks API a token is available.
			// TODO here it crashes because the app is not registered into some kind of console
			String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);

			// Now we are authorized by the user.
			Log.d("prefsActivity", "step two-b: " + token);

			if (token != null && !token.isEmpty() && account != null) {

				// Also mark enabled as true, as the dialog was shown from enable button
				Log.d("prefsActivity", "step three: " + account.name);

				SharedPreferences customSharedPreference = PreferenceManager
						.getDefaultSharedPreferences(activity);
				customSharedPreference
						.edit()
						.putString(SyncPrefs.KEY_ACCOUNT, account.name)
						.putBoolean(KEY_SYNC_ENABLE, true)
						.commit();

				// Set it syncable
				ContentResolver.setSyncAutomatically(account, MyContentProvider.AUTHORITY, true);
				ContentResolver.setIsSyncable(account, MyContentProvider.AUTHORITY, 1);
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
		boolean enabled = sharedPreferences.getBoolean(KEY_SYNC_ENABLE, false);
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");

		if (accountName != null && !accountName.isEmpty()) {
			Account account = SyncGtaskHelper.getAccount(AccountManager.get(activity), accountName);
			if (account != null) {
				if (enabled) {
					// set syncable
					ContentResolver.setSyncAutomatically(account, MyContentProvider.AUTHORITY, true);
					ContentResolver.setIsSyncable(account, MyContentProvider.AUTHORITY, 1);
					// Also set sync frequency
					setSyncInterval(activity, sharedPreferences);
				} else {
					// set unsyncable
					// ContentResolver.setIsSyncable(getAccount(AccountManager.get(activity), accountName), MyContentProvider.AUTHORITY, 0);
				}
			}
		} else if (enabled) {
			showAccountDialog();
		}
	}

	private void setAccountTitle(final SharedPreferences sharedPreferences) {
		prefAccount.setTitle(sharedPreferences.getString(KEY_ACCOUNT, ""));
		prefAccount.setSummary(R.string.settings_account_summary);
	}

	private void setSdDirSummary(final SharedPreferences sharedPreferences) {
		final String defaultDir = SDSynchronizer.getDefaultOrgDir(this.getContext());
		String valToSet = sharedPreferences.getString(KEY_SD_DIR_URI, null);
		if (valToSet == null) {
			// show the default directory in the setting's description
			valToSet = defaultDir;
		} else {
			// show the file path that the uri is pointing to
			valToSet = "Uri: " + Uri.parse(valToSet).getPath();

			// TODO instead of moving this to a string resource, fix the code! Maybe it would be
			//  easier to just remove the option to set a custom folder?
			valToSet += "\nwhile we fix this, " + defaultDir + "\nwill be used instead";
		}
		prefSdDir.setSummary(valToSet);
	}

}