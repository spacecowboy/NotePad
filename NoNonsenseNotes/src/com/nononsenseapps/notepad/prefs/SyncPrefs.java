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

import java.io.IOException;

// import com.nononsenseapps.notepad.NotePad;
import com.dropbox.sync.android.DbxAccountManager;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.sync.SyncAdapter;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskSync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.helpers.Log;

public class SyncPrefs extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	public static final int DROPBOX_RESULT = 68;

	public static final String KEY_SYNC_ENABLE = "syncEnablePref";
	public static final String KEY_ACCOUNT = "accountPref";
	// public static final String KEY_SYNC_FREQ = "syncFreq";
	public static final String KEY_FULLSYNC = "syncFull";
	public static final String KEY_SYNC_ON_START = "syncOnStart";
	public static final String KEY_SYNC_ON_CHANGE = "syncOnChange";
	public static final String KEY_BACKGROUND_SYNC = "syncInBackground";
	// Used for sync on start and on change
	public static final String KEY_LAST_SYNC = "lastSync";

	private Activity activity;

	private Preference prefAccount;

	// private Preference prefSyncFreq;

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

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Set summaries

		setAccountTitle(sharedPrefs);

		prefAccount
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						// Show dialog
						showAccountDialog();
						return true;
					}
				});

		// Disable prefs if this is not correct build
		findPreference(KEY_SYNC_ENABLE).setEnabled(
				!Config.GTASKS_API_KEY.contains(" "));

		try {
			findPreference(getString(R.string.pref_dropbox)).setEnabled(
					!Config.DROPBOX_APP_SECRET.contains(" "));
			Log.d("nononsenseapps dropbox", Config.DROPBOX_APP_SECRET);
		}
		catch (Exception e) {
			// TODO: re-enable
		}

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

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		try {
			Log.d("syncPrefs", "onChanged");
			if (activity.isFinishing()) {
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
			}
			else {
				if (KEY_SYNC_ENABLE.equals(key)) {
					toggleSync(sharedPreferences);
				}
				else if (KEY_BACKGROUND_SYNC.equals(key)) {
					setSyncInterval(activity, sharedPreferences);
				}
				else if (KEY_ACCOUNT.equals(key)) {
					Log.d("syncPrefs", "account");
					prefAccount.setTitle(sharedPreferences.getString(
							KEY_ACCOUNT, ""));
				}
				else if (key.equals(getString(R.string.pref_dropbox))) {
					Log.d("nononsenseapps dropbox", "Trying to link...");
					final DbxAccountManager dbxAccountManager = DbxAccountManager
							.getInstance(getActivity().getApplicationContext(),
									Config.DROPBOX_APP_KEY,
									Config.DROPBOX_APP_SECRET);
					if (!dbxAccountManager.hasLinkedAccount()) {
						dbxAccountManager.startLink(this, DROPBOX_RESULT);
					}
					// TODO should offer a way to unlink
				}
			}
		}
		catch (IllegalStateException e) {
			// This is just in case the "isFinishing" wouldn't be enough
			// The isFinishing will try to prevent us from doing something
			// stupid
			// This catch prevents the app from crashing if we do something
			// stupid
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DROPBOX_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				// ... Start using Dropbox files.
				// TODO
				Toast.makeText(getActivity(), "Dropbox linked!",
						Toast.LENGTH_SHORT).show();
			}
			else {
				// ... Link failed or was cancelled by the user.
				PreferenceManager.getDefaultSharedPreferences(getActivity())
						.edit()
						.putBoolean(getString(R.string.pref_dropbox), false)
						.commit();
				((SwitchPreference) findPreference(getString(R.string.pref_dropbox)))
						.setChecked(false);
			}
		}
		else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

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

	public static void setSyncInterval(Context activity,
			SharedPreferences sharedPreferences) {
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");
		boolean backgroundSync = sharedPreferences.getBoolean(
				KEY_BACKGROUND_SYNC, false);

		if (accountName != null && !accountName.isEmpty()) {
			if (!backgroundSync) {
				// Disable periodic syncing
				ContentResolver.removePeriodicSync(
						getAccount(AccountManager.get(activity), accountName),
						MyContentProvider.AUTHORITY, new Bundle());
			}
			else {
				// Convert from minutes to seconds
				long pollFrequency = 3600;
				// Set periodic syncing
				ContentResolver.addPeriodicSync(
						getAccount(AccountManager.get(activity), accountName),
						MyContentProvider.AUTHORITY, new Bundle(),
						pollFrequency);
			}
		}
	}

	private void toggleSync(SharedPreferences sharedPreferences) {
		boolean enabled = sharedPreferences.getBoolean(KEY_SYNC_ENABLE, false);
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");

		if (accountName != null && !accountName.isEmpty()) {
			if (enabled) {
				// set syncable
				ContentResolver.setIsSyncable(
						getAccount(AccountManager.get(activity), accountName),
						MyContentProvider.AUTHORITY, 1);
				// Also set sync frequency
				setSyncInterval(activity, sharedPreferences);
			}
			else {
				// set unsyncable
				// ContentResolver.setIsSyncable(
				// getAccount(AccountManager.get(activity), accountName),
				// MyContentProvider.AUTHORITY, 0);
			}
		}
		else if (enabled) {
			showAccountDialog();
		}
	}

	private void setAccountTitle(SharedPreferences sharedPreferences) {
		prefAccount.setTitle(sharedPreferences.getString(KEY_ACCOUNT, ""));
		prefAccount.setSummary(R.string.settings_account_summary);
	}

	public static class AccountDialog extends DialogFragment implements
			AccountManagerCallback<Bundle> {
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
			// Could add a clear alternative here
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
		public void accountSelected(Account account) {
			if (account != null) {
				Log.d("prefsActivity", "step one");
				this.account = account;
				// Request user's permission
				AccountManager.get(activity).getAuthToken(account,
						GoogleTaskSync.AUTH_TOKEN_TYPE, null, activity, this,
						null);
				// work continues in callback, method run()
			}
		}

		/**
		 * User wants to select an account to sync with. If we get an approval,
		 * activate sync and set periodicity also.
		 */
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

				if (token != null && !token.equals("") && account != null) {
					Log.d("prefsActivity", "step three: " + account.name);
					SharedPreferences customSharedPreference = PreferenceManager
							.getDefaultSharedPreferences(activity);
					customSharedPreference.edit()
							.putString(SyncPrefs.KEY_ACCOUNT, account.name)
							.putBoolean(KEY_SYNC_ENABLE, true).commit();

					// Set it syncable
					ContentResolver.setIsSyncable(account,
							MyContentProvider.AUTHORITY, 1);
					// Set sync frequency
					SyncPrefs.setSyncInterval(activity, customSharedPreference);
				}
			}
			catch (OperationCanceledException e) {
				// if the request was canceled for any reason
			}
			catch (AuthenticatorException e) {
				// if there was an error communicating with the authenticator or
				// if the authenticator returned an invalid response
			}
			catch (IOException e) {
				// if the authenticator returned an error response that
				// indicates that it encountered an IOException while
				// communicating with the authentication server
			}

		}
	}
}