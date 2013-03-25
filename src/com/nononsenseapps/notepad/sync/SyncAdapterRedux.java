package com.nononsenseapps.notepad.sync;

import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleDBTalker;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;

public class SyncAdapterRedux {

	private String AUTH_TOKEN_TYPE;
	private boolean NOTIFY_AUTH_FAILURE;

	@SuppressWarnings("unused")
	private Intent fullSync(final Account account, final Bundle extras,
			final String authority, final ContentProviderClient provider,
			final SyncResult syncResult, final SharedPreferences settings) {

		// Connect and shit
		// Initialize necessary stuff
		GoogleDBTalker dbTalker = new GoogleDBTalker(account.name, provider);
		GoogleAPITalker apiTalker = new GoogleAPITalker();

		AccountManager accountManager = null;
		boolean connected = apiTalker.initialize(accountManager, account,
				AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);

		if (!connected) {
			// Error notify
		} else {
			// Get list of remote lists
			
			// Get list of local lists
			
			// For each pair (one might be null)
			//   if both not null
			//     if remote newer, save to db
			//     
		}

		return null;
	}
}
