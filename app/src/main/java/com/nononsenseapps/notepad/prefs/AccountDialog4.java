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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTasksClient;
import com.nononsenseapps.util.SyncGtaskHelper;

import java.io.IOException;

/**
 * A copy of AccountDialog in SyncPrefs, but extending from support library
 * fragment.
 *
 * In addition, a successful account choice will trigger an immediate sync.
 */
public class AccountDialog4 extends DialogFragment implements
		AccountManagerCallback<Bundle> {
	private Activity activity;
	private Account account;

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	@NonNull
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
		// TODO Could add a clear alternative here
		builder.setItems(names, (dialog, which) -> {
			// Stuff to do when the account is selected by the user
			accountSelected(accounts[which]);
		});
		return builder.create();
	}

	/**
	 * Called from the activity, since that one builds the dialog
	 */
	public void accountSelected(final Account account) {
		if (account != null) {
			NnnLogger.debug(AccountDialog4.class, "step one");
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
	@Override
	public void run(AccountManagerFuture<Bundle> future) {
		try {
			NnnLogger.debug(AccountDialog4.class, "step two");
			// If the user has authorized
			// your application to use the
			// tasks API
			// a token is available.
			String token = future.getResult().getString(
					AccountManager.KEY_AUTHTOKEN);
			// Now we are authorized by the user.
			NnnLogger.debug(AccountDialog4.class, "step two-b: " + token);

			if (token != null && !token.equals("") && account != null) {
				NnnLogger.debug(AccountDialog4.class, "step three: " + account.name);
				SharedPreferences customSharedPreference = PreferenceManager
						.getDefaultSharedPreferences(activity);
				customSharedPreference.edit()
						.putString(SyncPrefs.KEY_ACCOUNT, account.name)
						.putBoolean(SyncPrefs.KEY_SYNC_ENABLE, true).commit();

				// Set it syncable
				ContentResolver.setSyncAutomatically(account,
						MyContentProvider.AUTHORITY, true);
				ContentResolver.setIsSyncable(account,
						MyContentProvider.AUTHORITY, 1);
				// Set sync frequency
				SyncPrefs.setSyncInterval(activity, customSharedPreference);

				// And trigger an immediate sync
				SyncGtaskHelper.requestSyncIf(activity, SyncGtaskHelper.MANUAL);
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
