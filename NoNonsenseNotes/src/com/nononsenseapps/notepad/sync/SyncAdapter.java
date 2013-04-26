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

package com.nononsenseapps.notepad.sync;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.PreconditionException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleDBTalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTask;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskSync;
import com.nononsenseapps.util.BiMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import com.nononsenseapps.helpers.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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

	private static final String TAG = MainActivity.TAG + ".SyncAdapter";

	// public static final String AUTH_TOKEN_TYPE =
	// "oauth2:https://www.googleapis.com/auth/tasks";

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
			// Dummy key has a space in it. Only builds using real api keys
			// should
			// not have spaces
			Log.d("nononsensenotes",
					com.nononsenseapps.build.Config.GTASKS_API_KEY);
			if (!com.nononsenseapps.build.Config.GTASKS_API_KEY.contains(" ")) {

				if (settings.getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false)
						&& !settings.getString(SyncPrefs.KEY_ACCOUNT, "")
								.isEmpty()
						&& account.name.equals(settings.getString(
								SyncPrefs.KEY_ACCOUNT, ""))) {

					Log.d(TAG, "onPerformSync");
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
