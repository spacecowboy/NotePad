/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.sync;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleDBTalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTask;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;

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
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This adapter syncs with GoogleTasks API. Each sync is an incremental sync
 * from our last sync. This is accomplished with a combinatinon of etags and
 * last updated time stamp. The API returns a "global" e-tag (hash-value of all
 * content). If this is the same as the etag we have, then nothing has changed
 * on server. Hence, we can know that there is nothing to download. If the etag
 * has changed, the adapter requests, for all lists, all tasks which have been
 * updated since the latest synced task in the database. Possible conflicts with
 * locally modified tasks is resolved by always choosing the latests modified
 * task as the winner.
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

	private static final String TAG = "SyncAdapter";
	public final static boolean SYNC_DEBUG_PRINTS = false;

	// public static final String AUTH_TOKEN_TYPE =
	// "oauth2:https://www.googleapis.com/auth/tasks";
	public static final String AUTH_TOKEN_TYPE = "Manage your tasks"; // Alias
																		// for
																		// above
	public static final boolean NOTIFY_AUTH_FAILURE = true;
	public static final String SYNC_STARTED = "com.nononsenseapps.notepad.sync.SYNC_STARTED";
	public static final String SYNC_FINISHED = "com.nononsenseapps.notepad.sync.SYNC_FINISHED";
	private static final String PREFS_LAST_SYNC_ETAG = "lastserveretag";
	private static final String PREFS_LAST_SYNC_DATE = "lastsyncdate";

	private final AccountManager accountManager;

	private final Context mContext;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		// mAccountManager = AccountManager.get(context);
		accountManager = AccountManager.get(context);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		// Only sync if it has been enabled by the user, and account is selected
		// Issue on reinstall where account approval is remembered by system
		if (settings.getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false)
				&& !settings.getString(SyncPrefs.KEY_ACCOUNT, "").isEmpty()
				&& account.name.equals(settings.getString(
						SyncPrefs.KEY_ACCOUNT, ""))) {

			if (SYNC_DEBUG_PRINTS)
				Log.d(TAG, "onPerformSync");
			Intent i = new Intent(SYNC_STARTED);
			mContext.sendBroadcast(i);
			// Initialize necessary stuff
			GoogleDBTalker dbTalker = new GoogleDBTalker(account.name, provider);
			GoogleAPITalker apiTalker = new GoogleAPITalker();

			try {
				boolean connected = apiTalker.initialize(accountManager,
						account, AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);

				if (connected) {
					if (SYNC_DEBUG_PRINTS)
						Log.d(TAG, "We got an authToken atleast");

					try {
						// FIrst of all, we need the latest updated time later.
						// So
						// save
						// that for now.
						// This is the latest time we synced
						//String lastUpdated = dbTalker.getLastUpdated(account.name);
						String lastUpdate = settings.getString(PREFS_LAST_SYNC_DATE, null);
						// Get the latest hash value we saw on the server
						String localEtag = settings.getString(
								PREFS_LAST_SYNC_ETAG, "");

						// Prepare lists for items
						ArrayList<GoogleTaskList> listsToSaveToDB = new ArrayList<GoogleTaskList>();
						HashMap<GoogleTaskList, ArrayList<GoogleTask>> tasksInListToSaveToDB = new HashMap<GoogleTaskList, ArrayList<GoogleTask>>();

						HashMap<Long, ArrayList<GoogleTask>> tasksInListToUpload = new HashMap<Long, ArrayList<GoogleTask>>();
						HashMap<Long, ArrayList<GoogleTask>> allTasksInList = new HashMap<Long, ArrayList<GoogleTask>>();

						// gets all tasks in one query
						ArrayList<GoogleTask> allTasks = dbTalker.getAllTasks(
								allTasksInList, tasksInListToUpload);

						ArrayList<GoogleTaskList> listsToUpload = new ArrayList<GoogleTaskList>();
						ArrayList<GoogleTaskList> allLocalLists = new ArrayList<GoogleTaskList>();

						// gets all lists in one query
						dbTalker.getAllLists(allLocalLists, listsToUpload);

						// Get the current hash value on the server and all
						// remote
						// lists if upload is not true

						String serverEtag;
						if (extras.getBoolean(
								ContentResolver.SYNC_EXTRAS_UPLOAD, false)) {
							serverEtag = localEtag;
						} else {
							serverEtag = apiTalker.getModifiedLists(localEtag,
									allLocalLists, listsToSaveToDB);
						}

						// IF the tags match, then nothing has changed on
						// server.
						if (localEtag.equals(serverEtag)) {
							if (SYNC_DEBUG_PRINTS)
								Log.d(TAG, "Etags match, nothing to download");
						} else {
							if (SYNC_DEBUG_PRINTS)
								Log.d(TAG,
										"Etags dont match, downloading new tasks");
							// Download tasks which have been updated since last
							// time
							for (GoogleTaskList list : listsToSaveToDB) {
								if (list.id != null && !list.id.isEmpty()) {
									if (SYNC_DEBUG_PRINTS)
										Log.d(TAG,
												"Saving remote modified tasks for: "
														+ list.id);
									tasksInListToSaveToDB.put(list, list
											.downloadModifiedTasks(apiTalker,
													allTasks, lastUpdate));
								}
							}
						}

						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG, "Getting stuff we want to upload");
						// Get stuff we would like to upload to server
						// In case of lists, locally modified versions always
						// wins
						// in
						// conflict, so nothing more to do

						for (GoogleTaskList list : allLocalLists) {
							ArrayList<GoogleTask> moddedTasks = tasksInListToUpload
									.get(list.dbId);
							if (moddedTasks != null && !moddedTasks.isEmpty()) {
								// There are some tasks here which we want to
								// upload
								if (SYNC_DEBUG_PRINTS)
									Log.d(TAG,
											"List id "
													+ list.dbId
													+ ", Locally modified tasks found: "
													+ moddedTasks.size());

								// Now we need to handle possible conflicts in
								// the
								// tasks. But this has already been sorted when
								// we
								// downloaded them
								// For any task which exists in stuffToSaveToDB,
								// we
								// should not upload it
								// Iterate over a clone to avoid concurrency
								// problems since we will be modifying
								// the list during iteration
								for (GoogleTask moddedTask : (ArrayList<GoogleTask>) moddedTasks
										.clone()) {
									ArrayList<GoogleTask> tasksToBeSaved = tasksInListToSaveToDB
											.get(list);
									if (tasksToBeSaved != null
											&& tasksToBeSaved
													.contains(moddedTask)) {
										if (SYNC_DEBUG_PRINTS)
											Log.d(TAG,
													"This modified task was newer on server, removing from upload list: "
															+ moddedTask.title);
										moddedTasks.remove(moddedTask);
									}
									// In the case that a task has been deleted
									// before it was synced the first time
									// We should definitely not sync it. Only
									// delete
									// it later
									if (moddedTask.deleted == 1
											&& (moddedTask.id == null || moddedTask.id
													.isEmpty())) {
										moddedTasks.remove(moddedTask);
									}
								}
							}
						}

						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG, "Uploading lists");
						// First thing we want to do is upload stuff, because
						// some
						// values are updated then
						boolean uploadedStuff = false;
						// Start with lists
						for (GoogleTaskList list : listsToUpload) {
							GoogleTaskList result = apiTalker.uploadList(list);
							uploadedStuff = true;
							if (result != null) {
								// Make sure that local version is the same as
								// server's
								for (GoogleTaskList localList : allLocalLists) {
									if (result.equals(localList)) {
										localList.title = result.title;
										localList.id = result.id;
										result.dbId = localList.dbId;
										break;
									}
								}
								listsToSaveToDB.add(result);
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
									GoogleTask result = apiTalker.uploadTask(
											task, list);
									uploadedStuff = true;
									// Task now has relevant fields set. Add to
									// DB-list
									if (tasksInListToSaveToDB.get(list) == null)
										tasksInListToSaveToDB.put(list,
												new ArrayList<GoogleTask>());
									tasksInListToSaveToDB.get(list).add(result);
								}
							}
						}

						// Finally, get the updated etag from the server and
						// save.
						// Only worth doing if we actually uploaded anything
						// Also, only do this if we are doing a full sync
						String currentEtag = serverEtag;
						if (uploadedStuff
								&& !extras.getBoolean(
										ContentResolver.SYNC_EXTRAS_UPLOAD,
										false)) {
							currentEtag = apiTalker.getEtag();
							lastUpdate = dbTalker.getLastUpdated(account.name);
						}

						settings.edit()
								.putString(PREFS_LAST_SYNC_ETAG, currentEtag)
								.putString(PREFS_LAST_SYNC_DATE, lastUpdate)
								.commit();

						// Now, set sorting values.
						for (GoogleTaskList list : tasksInListToSaveToDB
								.keySet()) {
							if (SYNC_DEBUG_PRINTS)
								Log.d(TAG, "Setting position values in: "
										+ list.id);
							ArrayList<GoogleTask> tasks = tasksInListToSaveToDB
									.get(list);
							if (tasks != null) {
								if (SYNC_DEBUG_PRINTS)
									Log.d(TAG,
											"Setting position values for #tasks: "
													+ tasks.size());
								ArrayList<GoogleTask> allListTasks = allTasksInList
										.get(list.dbId);
								list.setSortingValues(tasks, allListTasks);
							}
						}

						// Save to database in a single transaction
						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG, "Save stuff to DB");
						dbTalker.SaveToDatabase(listsToSaveToDB,
								tasksInListToSaveToDB);
						// Commit it
						ContentProviderResult[] result = dbTalker.apply();
						Log.d(TAG, "Batched items: " + result.length);

						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG, "Sync Complete!");

					} catch (ClientProtocolException e) {
						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG,
									"ClientProtocolException: "
											+ e.getLocalizedMessage());
					} catch (JSONException e) {
						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG,
									"JSONException: " + e.getLocalizedMessage());
					} catch (IOException e) {
						syncResult.stats.numIoExceptions++;
						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG,
									"IOException: " + e.getLocalizedMessage());
					} catch (RemoteException e) {
						if (SYNC_DEBUG_PRINTS)
							Log.d(TAG,
									"RemoteException: "
											+ e.getLocalizedMessage());
					} catch (OperationApplicationException e) {
						Log.d(TAG,
								"Joined operation failed: "
										+ e.getLocalizedMessage());
					}

				} else {
					// return real failure
					if (SYNC_DEBUG_PRINTS)
						Log.d(TAG,
								"Could not get authToken. Reporting authException");
					syncResult.stats.numAuthExceptions++;
				}

			} finally {
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
	}
}
