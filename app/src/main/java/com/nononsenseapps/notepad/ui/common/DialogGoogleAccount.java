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

package com.nononsenseapps.notepad.ui.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.remote.gtasks.GoogleTasksClient;

/**
 * Dialog which allows the user to select a Google account. Requests permission to handle tasks
 * on selected.
 */
public class DialogGoogleAccount extends DialogFragment {
    private AccountManagerCallback<Bundle> callbacks;
    private boolean accountSelected = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Crash if not possible to cast
        callbacks = (AccountManagerCallback<Bundle>) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle args) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_account);
        final Account[] accounts = AccountManager.get(getActivity()).getAccountsByType("com.google");
        final int size = accounts.length;
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = accounts[i].name;
        }
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Stuff to do when the account is selected by the user
                accountSelected(accounts[which]);
            }
        });
        return builder.create();
    }

    /**
     * Called when the fragment is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    @Override
    public void onDestroy() {
        // If no account was selected, disable the toggle switch in preferences possibly
        if (!accountSelected) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                    (getActivity());
            if (!sharedPreferences.contains(getString(R.string
                    .const_preference_gtask_account_key))) {
                sharedPreferences.edit().putBoolean(getString(R.string
                        .const_preference_gtask_enabled_key), false).apply();
            }
        }
        super.onDestroy();
    }

    @SuppressLint("CommitPrefEdits")
    public void accountSelected(Account account) {
        if (account != null) {
            this.accountSelected = true;
            // Store name for future use in callback and later
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                    (getActivity());
            sharedPreferences.edit().putString(getString(R.string
                    .const_preference_gtask_account_key), account.name).commit();
            // Request user's permission
            GoogleTasksClient.getAuthTokenAsync(getActivity(), account, callbacks);
            // work continues in callback, method run()
        }
    }
}
