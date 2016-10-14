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

package com.nononsenseapps.notepad.data.service.gtasks;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.ui.settings.SyncPrefs;
import com.nononsenseapps.notepad.data.remote.gtasks.GoogleTaskSync;

/**
 * This adapter syncs with GoogleTasks API. Each sync is an incremental sync
 * from our last sync. This is accomplished with a combinatinon of etags and
 * last updated time stamp. The API returns a "global" e-tag (hash-value of all
 * content). If this is the same as the etag we have, then nothing has changed
 * on server. Hence, we can know that there is nothing to download. If the etag
 * has changed, the adapter requests, for all lists, all tasks which have been
 * updated since the latest synced task in the database.
 * 
 * Before any changes are committed either way, we should have two DISJOINT
 * sets:
 * 
 * TasksFromServer and TasksToServer.
 * 
 * Due to the conflict resolution, no task should exist in both sets. We then
 * upload TasksToServer. For each upload the server will return the current
 * state of the task with some fields updated. These changes we want to save of
 * course, so we add them to TasksFromServer. Which means that after uploading
 * we have a single set:
 * 
 * TasksFromServer
 * 
 * Which now contains all tasks that were modified either locally or remotely.
 * In other words, this set is now the union of the two initially disjoint sets,
 * with some fields updated by the server.
 * 
 * These tasks are then committed to the database in a single transaction.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	public static final String SYNC_STARTED = "com.nononsenseapps.notepad.sync.SYNC_STARTED";
	public static final String SYNC_FINISHED = "com.nononsenseapps.notepad.sync.SYNC_FINISHED";

	public static final String SYNC_RESULT = "com.nononsenseapps.notepad.sync.SYNC_RESULT";
	public static final int SUCCESS = 0;
	public static final int LOGIN_FAIL = 1;
	public static final int ERROR = 2;

	private final Context mContext;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		/*
		 * Only sync if it has been enabled by the user, and account is selected
		 * Issue on reinstall where account approval is remembered by system
		 * Also only sync if APIKEY has no spaces in it. A space in the key
		 * means that the app has been built from the open source code, for
		 * which the api key is naturally not included. A space also causes the
		 * app to crash since that is entirely invalid. So don't sync if there
		 * is a space in the api key.
		 */

		Intent doneIntent = new Intent(SYNC_FINISHED);
		doneIntent.putExtra(SYNC_RESULT, ERROR);
		try {
            // Gtasks first
			// Dummy key has a space in it. Only builds using real api keys
			// should not have spaces
			if (com.nononsenseapps.build.Config.getGtasksApiKey(mContext) !=
                null && !com.nononsenseapps.build.Config
                    .getGtasksApiKey(mContext).contains(" ")) {
				if (settings.getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false)
						&& !settings.getString(SyncPrefs.KEY_ACCOUNT, "")
								.isEmpty()
						&& account.name.equals(settings.getString(
								SyncPrefs.KEY_ACCOUNT, ""))) {

					mContext.sendBroadcast(new Intent(SYNC_STARTED));

					if (GoogleTaskSync.fullSync(mContext,
							account, extras, authority, provider, syncResult)) {
						// Success
						doneIntent.putExtra(SYNC_RESULT, SUCCESS);
					}
				}
			}
		}
		finally {
			mContext.sendBroadcast(doneIntent);
		}
	}
}
