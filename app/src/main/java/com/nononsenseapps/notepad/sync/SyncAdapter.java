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

package com.nononsenseapps.notepad.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

/**
 * this used to do google tasks sync. It may be useful in the future
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	public static final String SYNC_STARTED = "com.nononsenseapps.notepad.sync.SYNC_STARTED";
	public static final String SYNC_FINISHED = "com.nononsenseapps.notepad.sync.SYNC_FINISHED";

	public static final String SYNC_RESULT = "com.nononsenseapps.notepad.sync.SYNC_RESULT";
	public static final int SUCCESS = 0;
	public static final int LOGIN_FAIL = 1;
	public static final int ERROR = 2;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
							  ContentProviderClient provider, SyncResult syncResult) {

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this.getContext());

		Intent doneIntent = new Intent(SYNC_FINISHED)
				.putExtra(SYNC_RESULT, ERROR);

		this.getContext().sendBroadcast(doneIntent);
	}
}
