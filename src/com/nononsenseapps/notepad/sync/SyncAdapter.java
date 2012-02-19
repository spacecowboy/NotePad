/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.NotModifiedException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.PreconditionException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleDBTalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTask;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.text.format.Time;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider. This sample shows a basic 2-way sync
 * between the client and a sample server. It also contains an example of how to
 * update the contacts' status messages, which would be useful for a messaging
 * or social networking client.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";
	public final static boolean SYNC_DEBUG_PRINTS = true;

	// public static final String AUTH_TOKEN_TYPE =
	// "oauth2:https://www.googleapis.com/auth/tasks";
	public static final String AUTH_TOKEN_TYPE = "Manage your tasks"; // Alias
																		// for
																		// above
	public static final boolean NOTIFY_AUTH_FAILURE = true;
	public static final String SYNC_STARTED = "com.nononsenseapps.notepad.sync.SYNC_STARTED";
	public static final String SYNC_FINISHED = "com.nononsenseapps.notepad.sync.SYNC_FINISHED";
	private static final String PREFS_LAST_SYNC_ETAG = "lastserveretag";

	private final AccountManager accountManager;

	private final Context mContext;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		// mAccountManager = AccountManager.get(context);
		accountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		if (SYNC_DEBUG_PRINTS)
			Log.d(TAG, "onPerformSync");
		Intent i = new Intent(SYNC_STARTED);
		mContext.sendBroadcast(i);
		// Initialize necessary stuff
		GoogleDBTalker dbTalker = new GoogleDBTalker(account.name, provider);
		GoogleAPITalker apiTalker = new GoogleAPITalker();

		boolean connected = apiTalker.initialize(accountManager, account,
				AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);

		if (connected) {
			if (SYNC_DEBUG_PRINTS)
				Log.d(TAG, "We got an authToken atleast");

			try {
				// FIrst of all, we need the latest updated time later. So save
				// that for now.
				// This is the latest time we synced
				String lastUpdated = dbTalker.getLastUpdated(account.name);
				// Get the latest hash value we saw on the server
				final SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(mContext);
				String localEtag = settings.getString(PREFS_LAST_SYNC_ETAG, "");

				// Prepare lists for items
				ArrayList<GoogleTaskList> listsToSaveToDB = new ArrayList<GoogleTaskList>();
				HashMap<GoogleTaskList, ArrayList<GoogleTask>> tasksInListToSaveToDB = new HashMap<GoogleTaskList, ArrayList<GoogleTask>>();

				HashMap<Long, ArrayList<GoogleTask>> tasksInListToUpload = dbTalker.getAllModifiedTasks();
				ArrayList<GoogleTaskList> listsToUpload = new ArrayList<GoogleTaskList>();
				ArrayList<GoogleTaskList> allLocalLists = new ArrayList<GoogleTaskList>();
				dbTalker.getAllLists(allLocalLists, listsToUpload);
				
				// Get the current hash value on the server and all remote lists
				String serverEtag = apiTalker.getModifiedLists(localEtag,
						allLocalLists, listsToSaveToDB);

				// IF the tags match, then nothing has changed on server.
				if (localEtag.equals(serverEtag)) {
					if (SYNC_DEBUG_PRINTS)
						Log.d(TAG, "Etags match, nothing to download");
					// Don't have to download anything, insert empty lists
					// for (GoogleTaskList list : allLocalLists) {
					// tasksInListToSaveToDB.put(list,
					// new ArrayList<GoogleTask>());
					// }
				} else {
					if (SYNC_DEBUG_PRINTS)
						Log.d(TAG, "Etags dont match, downloading new tasks");
					// Download tasks which have been updated since last time
					for (GoogleTaskList list : listsToSaveToDB) {
						tasksInListToSaveToDB.put(list, list
								.downloadModifiedTasks(apiTalker, dbTalker,
										lastUpdated));
					}
				}

				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "Getting stuff we want to upload");
				// Get stuff we would like to upload to server
				// In case of lists, locally modified versions always wins in
				// conflict, so nothing more to do

				for (GoogleTaskList list : allLocalLists) {
					ArrayList<GoogleTask> moddedTasks = tasksInListToUpload.get(list.dbId);
					if (moddedTasks != null && !moddedTasks.isEmpty()) {
						// There are some tasks here which we want to upload
						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG, "List id " + list.dbId
									+ ", Locally modified tasks found: "
									+ moddedTasks.size());

						// Now we need to handle possible conflicts in the
						// tasks. But this has already been sorted when we
						// downloaded them
						// For any task which exists in stuffToSaveToDB, we
						// should not upload it
						for (GoogleTask moddedTask : moddedTasks) {
							ArrayList<GoogleTask> tasksToBeSaved = tasksInListToSaveToDB
									.get(list);
							if (tasksToBeSaved != null
									&& tasksToBeSaved.contains(moddedTask)) {
								if (SYNC_DEBUG_PRINTS)
									Log.d(TAG,
											"This modified task was newer on server, removing from upload list: "
													+ moddedTask.title);
								moddedTasks.remove(moddedTask);
							}
							// In the case that a task has been deleted before it was synced the first time
							// We should definitely not sync it. Only delete it later
							if (moddedTask.deleted == 1 && (moddedTask.id == null || moddedTask.id.equals(""))) {
								moddedTasks.remove(moddedTask);
							}
						}
					}
				}

				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "Uploading lists");
				// First thing we want to do is upload stuff, because some
				// values are updated then
				boolean uploadedStuff = false;
				// Start with lists
				for (GoogleTaskList list : listsToUpload) {
					GoogleTaskList result = apiTalker.uploadList(list);
					uploadedStuff = true;
					if (result != null) {
						boolean found = false;
						// Make sure that local version is the same as server's
						for (GoogleTaskList localList : listsToSaveToDB) {
							if (result.equals(localList)) {
								localList.title = result.title;
								localList.id = result.id;
								found = true;
								break;
							}
						}
						if (!found) {
							// DBID is preserved in upload method
							listsToSaveToDB.add(result);
						}
					}
				}

				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "Uploading tasks");
				// Right, now upload tasks
				for (GoogleTaskList list : allLocalLists) {
					ArrayList<GoogleTask> tasksToUpload = tasksInListToUpload
							.get(list.dbId);
					if (tasksToUpload != null) {
						for (GoogleTask task : tasksToUpload) {
							apiTalker.uploadTask(task, list);
							uploadedStuff = true;
							// Task now has relevant fields set. Add to DB-list
							if (tasksInListToSaveToDB.get(list) == null)
								tasksInListToSaveToDB.put(list,
										new ArrayList<GoogleTask>());
							tasksInListToSaveToDB.get(list).add(task);
						}
					}
				}
				
				// Finally, get the updated etag from the server and save.
				// Only worth doing if we actually uploaded anything
				String currentEtag = serverEtag;
				if (uploadedStuff) {
					currentEtag = apiTalker.getEtag();
				}
				final SharedPreferences.Editor editor = settings.edit();
				editor.putString(PREFS_LAST_SYNC_ETAG, currentEtag);
				editor.commit();

				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "Save stuff to DB");
				// At last, now just remaining stuff to DB.
				for (GoogleTaskList list : listsToSaveToDB) {
					if (SYNC_DEBUG_PRINTS)
						Log.d(TAG, "Save list to DB: " + list.dbId);
					dbTalker.SaveToDatabase(list);
				}
				
				// TODO
				// Now, set sorting values.
				for (GoogleTaskList list : tasksInListToSaveToDB.keySet()) {
					if (SYNC_DEBUG_PRINTS)
						Log.d(TAG, "Setting position values in: " + list.dbId);
					ArrayList<GoogleTask> tasks = tasksInListToSaveToDB.get(list);
					if (tasks != null) {
						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG, "Setting position values for #tasks: " + tasks.size());
						ArrayList<GoogleTask> allTasks = dbTalker.getAllTasks(list);
						list.setSortingValues(tasks, allTasks);
					}
				}
				
				for (GoogleTaskList list : tasksInListToSaveToDB.keySet()) {
					ArrayList<GoogleTask> tasksToSave = tasksInListToSaveToDB
							.get(list);
					if (tasksToSave != null) {
						for (GoogleTask task : tasksToSave) {
							if (SYNC_DEBUG_PRINTS)
								Log.d(TAG, "Save Task to DB: " + task.dbId);
							dbTalker.SaveToDatabase(task, list);
						}
					}
				}

				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "Sync Complete!");

			} catch (ClientProtocolException e) {
				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG,
							"ClientProtocolException: "
									+ e.getLocalizedMessage());
			} catch (JSONException e) {
				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "JSONException: " + e.getLocalizedMessage());
			} catch (IOException e) {
				syncResult.stats.numIoExceptions++;
				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "IOException: " + e.getLocalizedMessage());
			} catch (RemoteException e) {
				if (SYNC_DEBUG_PRINTS)
					Log.d(TAG, "RemoteException: " + e.getLocalizedMessage());
			}

		} else {
			// return real failure
			if (SYNC_DEBUG_PRINTS) Log.d(TAG, "Could not get authToken. Reporting authException");
			syncResult.stats.numAuthExceptions++;
		}

		// This must always be called or we will leak resources
		if (apiTalker != null) {
			apiTalker.closeClient();
		}
		Intent j = new Intent(SYNC_FINISHED);
		mContext.sendBroadcast(j);

		if (SYNC_DEBUG_PRINTS)
			Log.d(TAG, "SyncResult: " + syncResult.toDebugString());
	}
}
