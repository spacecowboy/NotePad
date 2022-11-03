/*
 * Copyright (C) 2014 Jonas Kalderstam
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTasksClient;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;
import com.nononsenseapps.notepad.sync.orgsync.SDSynchronizer;

import java.io.File;
import java.io.IOException;

// import com.nononsenseapps.notepad.NotePad;

public class SyncPrefs extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_SYNC_ENABLE = "syncEnablePref";
	public static final String KEY_ACCOUNT = "accountPref";
	// public static final String KEY_SYNC_FREQ = "syncFreq";
	public static final String KEY_FULLSYNC = "syncFull";
	public static final String KEY_SYNC_ON_START = "syncOnStart";
	public static final String KEY_SYNC_ON_CHANGE = "syncOnChange";
	public static final String KEY_BACKGROUND_SYNC = "syncInBackground";
	// Used for sync on start and on change
	public static final String KEY_LAST_SYNC = "lastSync";

	// SD sync
	public static final String KEY_SD_ENABLE = "pref_sync_sd_enabled";
	public static final String KEY_SD_DIR_URI = "pref_sync_sd_dir_uri";
	private static final int PICK_SD_DIR_CODE = 1;


	private Activity activity;

	private Preference prefAccount;
	private Preference prefSdDir;


	// private Preference prefSyncFreq;

	/**
	 * Finds and returns the account of the name given
	 *
	 * @param accountName
	 * @return
	 */
	public static Account getAccount(AccountManager manager, String accountName) {
		Account[] accounts = manager.getAccountsByType("com.google");
		for (Account account : accounts) {
			if (account.name.equals(accountName)) {
				return account;
			}
		}
		return null;
	}

	public static void setSyncInterval(Context activity, SharedPreferences sharedPreferences) {
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");
		boolean backgroundSync = sharedPreferences.getBoolean(KEY_BACKGROUND_SYNC, false);

		if (accountName != null && !accountName.isEmpty()) {
			Account account = getAccount(AccountManager.get(activity), accountName);
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
			// Show dialog
			showAccountDialog();
			return true;
		});

		// Disable prefs if this is not correct build
		findPreference(KEY_SYNC_ENABLE).setEnabled(
				null != Config.getGtasksApiKey(getActivity()) &&
						!Config.getGtasksApiKey(getActivity()).contains(" "));

		// SD Card
		prefSdDir = findPreference(KEY_SD_DIR_URI);
		setSdDirSummary(sharedPrefs);
		prefSdDir.setOnPreferenceClickListener(preference -> {

			// Let the user choose a directory using the system's file picker. See
			// https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
			var i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

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
			return true;
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(activity)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	private void showAccountDialog() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("accountdialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		// Bundle args = new Bundle();
		// args.putString(KEY_ACCOUNT, newPassword);
		DialogFragment newFragment = new AccountDialog();
		// newFragment.setArguments(args);
		newFragment.show(ft, "accountdialog");
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
					prefAccount.setTitle(prefs.getString(
							KEY_ACCOUNT, ""));
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
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != PICK_SD_DIR_CODE) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}

		if (resultCode != Activity.RESULT_OK) {
			// it was cancelled by the user. Let's ignore it
			return;
		}

		// "data" contains the URI for the directory (A.K.A. the "document tree")
		// that the user selected
		Uri uri = data.getData();

		// represents the directory that the user just picked. Use this instead of the "File" class
		var docDir = DocumentFile.fromTreeUri(this.getContext(), uri);

		boolean isOk = docDir != null && docDir.exists() && docDir.isDirectory() && docDir.canWrite();
		if (isOk) {
			PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.edit()
					.putString(KEY_SD_DIR_URI, uri.toString())
					.commit();
			Toast.makeText(getActivity(), "Custom directories are still W.I.P.", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getActivity(), R.string.cannot_write_to_directory, Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void toggleSync(SharedPreferences sharedPreferences) {
		boolean enabled = sharedPreferences.getBoolean(KEY_SYNC_ENABLE, false);
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");

		if (accountName != null && !accountName.isEmpty()) {
			Account account = getAccount(AccountManager.get(activity), accountName);
			if (account != null) {
				if (enabled) {
					// set syncable
					ContentResolver.setSyncAutomatically(
							account,
							MyContentProvider.AUTHORITY, true);
					ContentResolver.setIsSyncable(
							account,
							MyContentProvider.AUTHORITY, 1);
					// Also set sync frequency
					setSyncInterval(activity, sharedPreferences);
				} else {
					// set unsyncable
					// ContentResolver.setIsSyncable(
					// getAccount(AccountManager.get(activity), accountName),
					// MyContentProvider.AUTHORITY, 0);
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
		String defaultDir = SDSynchronizer.getDefaultOrgDir(this.getContext());
		prefSdDir.setSummary(sharedPreferences.getString(KEY_SD_DIR_URI, defaultDir));
	}

	public static class AccountDialog extends DialogFragment implements AccountManagerCallback<Bundle> {
		private Activity activity;
		private Account account;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			this.activity = activity;
		}

		@Override
		public Dialog onCreateDialog(Bundle args) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.select_account);
			final Account[] accounts = AccountManager.get(activity)
					.getAccountsByType("com.google");
			final int size = accounts.length;
			String[] names = new String[size];
			for (int i = 0; i < size; i++) {
				names[i] = accounts[i].name;
			}
			// TODO
			//  Could add a clear alternative here
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Stuff to do when the account is selected by the user
					accountSelected(accounts[which]);
				}
			});
			return builder.create();
		}

		/**
		 * Called from the activity, since that one builds the dialog
		 *
		 * @param account
		 */
		public void accountSelected(final Account account) {
			if (account != null) {
				Log.d("prefsActivity", "step one");
				this.account = account;
				// Request user's permission
				GoogleTasksClient.getAuthTokenAsync(activity, account, this);
				// work continues in callback, method run()
			}
		}

		/**
		 * User wants to select an account to sync with. If we get an approval,
		 * activate sync and set periodicity also.
		 */
		@SuppressLint("CommitPrefEdits")
		@Override
		public void run(AccountManagerFuture<Bundle> future) {
			try {
				Log.d("prefsActivity", "step two");
				// If the user has authorized
				// your application to use the
				// tasks API
				// a token is available.
				String token = future.getResult().getString(
						AccountManager.KEY_AUTHTOKEN);
				// Now we are authorized by the user.
				Log.d("prefsActivity", "step two-b: " + token);

				if (token != null && !token.isEmpty() && account != null) {
					Log.d("prefsActivity", "step three: " + account.name);
					SharedPreferences customSharedPreference = PreferenceManager
							.getDefaultSharedPreferences(activity);
					customSharedPreference.edit()
							.putString(SyncPrefs.KEY_ACCOUNT, account.name)
							.putBoolean(KEY_SYNC_ENABLE, true).commit();

					// Set it syncable
					ContentResolver.setSyncAutomatically(account,
							MyContentProvider.AUTHORITY, true);
					ContentResolver.setIsSyncable(account,
							MyContentProvider.AUTHORITY, 1);
					// Set sync frequency
					SyncPrefs.setSyncInterval(activity, customSharedPreference);
				}
			} catch (OperationCanceledException e) {
				// if the request was canceled for any reason
			} catch (AuthenticatorException e) {
				// if there was an error communicating with the authenticator or
				// if the authenticator returned an invalid response
			} catch (IOException e) {
				// if the authenticator returned an error response that
				// indicates that it encountered an IOException while
				// communicating with the authentication server
			}

		}
	}
}