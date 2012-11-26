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
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.PreconditionException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleDBTalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTask;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;
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

	private static final String TAG = "SyncAdapter";

	// public static final String AUTH_TOKEN_TYPE =
	// "oauth2:https://www.googleapis.com/auth/tasks";
	public static final String AUTH_TOKEN_TYPE = "Manage your tasks"; // Alias
																		// for
																		// above
	public static final boolean NOTIFY_AUTH_FAILURE = true;
	public static final String SYNC_STARTED = "com.nononsenseapps.notepad.sync.SYNC_STARTED";
	public static final String SYNC_FINISHED = "com.nononsenseapps.notepad.sync.SYNC_FINISHED";

	public static final String SYNC_RESULT = "com.nononsenseapps.notepad.sync.SYNC_RESULT";
	public static final int SUCCESS = 0;
	public static final int LOGIN_FAIL = 1;
	public static final int ERROR = 2;

	public static final String PREFS_LAST_SYNC_ETAG = "lastserveretag";

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

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		// Only sync if it has been enabled by the user, and account is selected
		// Issue on reinstall where account approval is remembered by system
		if (settings.getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false)
				&& !settings.getString(SyncPrefs.KEY_ACCOUNT, "").isEmpty()
				&& account.name.equals(settings.getString(
						SyncPrefs.KEY_ACCOUNT, ""))) {

			Log.d(TAG, "onPerformSync");
			mContext.sendBroadcast(new Intent(SYNC_STARTED));

			Intent doneIntent = new Intent(SYNC_FINISHED);
			doneIntent.putExtra(SYNC_RESULT, ERROR);

			try {
				doneIntent = fullSync(account, extras, authority, provider,
						syncResult, settings);
			} finally {
				mContext.sendBroadcast(doneIntent);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Intent fullSync(final Account account, final Bundle extras,
			final String authority, final ContentProviderClient provider,
			final SyncResult syncResult, final SharedPreferences settings) {

		Log.d(TAG, "fullSync");
		// For later
		Intent doneIntent = new Intent(SYNC_FINISHED);
		doneIntent.putExtra(SYNC_RESULT, ERROR);

		// Initialize necessary stuff
		GoogleDBTalker dbTalker = new GoogleDBTalker(account.name, provider);
		GoogleAPITalker apiTalker = new GoogleAPITalker();

		try {
			boolean connected = apiTalker.initialize(accountManager, account,
					AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);

			if (connected) {

				Log.d(TAG, "AuthToken acquired, we are connected...");

				try {
					/*
					 * First of all, we need the latest updated time later. So
					 * save that for now. This is the latest time we synced
					 */
					String lastUpdate = dbTalker.getLastUpdated(account.name);
					/*
					 * String lastUpdate =
					 * settings.getString(PREFS_LAST_SYNC_DATE, null); Get the
					 * latest hash value we saw on the server
					 */
					String localEtag = settings.getString(PREFS_LAST_SYNC_ETAG,
							"");

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

					Log.d(TAG, "Getting stuff we want to upload");

					for (GoogleTaskList list : allLocalLists) {
						ArrayList<GoogleTask> moddedTasks = tasksInListToUpload
								.get(list.dbId);
						if (moddedTasks != null && !moddedTasks.isEmpty()) {
							// There are some tasks here which we want to
							// upload

							Log.d(TAG, "List id " + list.title
									+ ", Locally modified tasks found: "
									+ moddedTasks.size());

							// Avoid concurrency problems with clone
							for (GoogleTask moddedTask : (ArrayList<GoogleTask>) moddedTasks
									.clone()) {
								/*
								 * In the case that a task has been deleted
								 * before it was synced the first time We should
								 * definitely not sync it. Only delete it later
								 */
								if (moddedTask.deleted == 1
										&& (moddedTask.id == null || moddedTask.id
												.isEmpty())) {
									Log.d(TAG,
											"Task "
													+ moddedTask.title
													+ " was deleted before sync, not uploading");
									if (moddedTask.title.contains("debug")) {
										Log.d(TAG, "SyncDupe DELETED BEFORE SYNC REMOVING " + moddedTask.title);
									}
									moddedTasks.remove(moddedTask);
									allTasks.remove(moddedTask);
									dbTalker.removeDeletedTask(moddedTask);
								}
							}
						}
					}

					Log.d(TAG, "Uploading lists");
					/*
					 * First thing we want to do is upload stuff, because some
					 * values are updated then
					 */
					boolean uploadedStuff = false;
					// Start with lists
					for (GoogleTaskList list : listsToUpload) {
						try {
							GoogleTaskList result = apiTalker.uploadList(list);
							uploadedStuff = true;
						} catch (PreconditionException e) {
							Log.d(TAG, "There was a conflict with list delete "
									+ list.dbId);
							// Tried to delete the default list or a modified
							// list. That's not allowed
							// Undelete and put in list to save to db
							if (list.deleted == 1) {
								list.deleted = 0;
								// Make the list re-download all containing
								// tasks
								list.redownload = true;
								// listsToSaveToDB.add(list);
								// Also change the etag to make sure we download
								// stuff
								localEtag = "dummytag";
							}
						}
					}

					Log.d(TAG, "Uploading tasks");
					// Right, now upload tasks
					for (GoogleTaskList list : allLocalLists) {
						ArrayList<GoogleTask> tasksToUpload = tasksInListToUpload
								.get(list.dbId);
						if (tasksToUpload != null) {
							for (GoogleTask task : tasksToUpload) {
								try {
									if (null != apiTalker
											.uploadTask(task, list))
										uploadedStuff = true;
								} catch (PreconditionException e) {
									// There was a conflict, do it again but as
									// a new note. Except if we tried to delete
									// a note
									// then just delete it locally and download the
									// server version later.
									if (task.deleted == 1) {
										if (task.title.contains("debug")) {
											Log.d(TAG, "SyncDupe Upload conflict delete " + task.title);
										}
										allTasks.remove(task);
										allTasksInList.get(task.listdbid).remove(task);
										dbTalker.removeDeletedTask(task);
									} else {
										Log.d(TAG,
												"There was task conflict. Trying as new task");
										if (task.title.contains("debug")) {
											Log.d(TAG, "SyncDupe Upload conflict doing new " + task.title);
										}
										task.id = null;
										task.etag = null;
										task.title = "sync-conflict "
												+ task.title;

										try {
											if (null != apiTalker.uploadTask(
													task, list)) {
												uploadedStuff = true;
												Log.d(TAG, "Uploaded " + task.title + ", didremoteinsert: " + task.didRemoteInsert);
											}
										} catch (PreconditionException ee) {
											Log.d(TAG,
													"Impossible conflict achieved");
											// Impossible to reach this
										}
									}
								}
							}
						}
					}

					String serverEtag = apiTalker.getModifiedLists(localEtag,
							allLocalLists, listsToSaveToDB);

					// IF the tags match, then nothing has changed on
					// server.
					if (localEtag.equals(serverEtag)) {

						Log.d(TAG, "Etags match, nothing to download");
					} else {

						Log.d(TAG, "Etags dont match, downloading new tasks");
						// Download tasks which have been updated since last
						// time
						for (GoogleTaskList list : listsToSaveToDB) {
							if (list.id != null && !list.id.isEmpty()) {

								Log.d(TAG, "Saving remote modified tasks for: "
										+ list.title);
								if (tasksInListToSaveToDB.get(list) == null)
									tasksInListToSaveToDB.put(list,
											new ArrayList<GoogleTask>());
								tasksInListToSaveToDB.get(list).addAll(
										list.downloadModifiedTasks(apiTalker,
												allTasks, lastUpdate));
							}
						}
					}

					/*
					 * Finally, get the updated etag from the server and save.
					 * Only worth doing if we actually uploaded anything
					 */
					String currentEtag = serverEtag;
					if (uploadedStuff) {
						try {
							currentEtag = apiTalker.getEtag();
						} catch (PreconditionException e) {
							// Cant happen here
							Log.e(TAG,
									"Blowtorch error: "
											+ e.getLocalizedMessage());
						}
					}

					// Now, set sorting values.
					for (GoogleTaskList list : tasksInListToSaveToDB.keySet()) {
						Log.d(TAG, "Setting position values in: " + list.id);
						ArrayList<GoogleTask> tasks = tasksInListToSaveToDB
								.get(list);

						if (tasks != null && !tasks.isEmpty()) {
							Log.d(TAG, "Setting position values for #tasks: "
									+ tasks.size());
							ArrayList<GoogleTask> allListTasks = allTasksInList
									.get(list.dbId);
							list.setSortingValues(tasks, allListTasks);
						}
					}

					// Save to database in a single transaction
					Log.d(TAG, "Save stuff to DB");
					dbTalker.SaveToDatabase(listsToSaveToDB,
							tasksInListToSaveToDB, allTasks);
					// Commit it
					ContentProviderResult[] result = dbTalker.apply();

					settings.edit()
							.putString(PREFS_LAST_SYNC_ETAG, currentEtag)
							.commit();

					Log.d(TAG, "Sync Complete!");
					doneIntent.putExtra(SYNC_RESULT, SUCCESS);

				} catch (ClientProtocolException e) {

					Log.d(TAG,
							"ClientProtocolException: "
									+ e.getLocalizedMessage());
				} catch (JSONException e) {

					Log.d(TAG, "JSONException: " + e.getLocalizedMessage());
				} catch (IOException e) {
					syncResult.stats.numIoExceptions++;

					Log.d(TAG, "IOException: " + e.getLocalizedMessage());
				} catch (RemoteException e) {

					Log.d(TAG, "RemoteException: " + e.getLocalizedMessage());
				} catch (OperationApplicationException e) {
					Log.d(TAG,
							"Joined operation failed: "
									+ e.getLocalizedMessage());
				} catch (ClassCastException e) {
					// GetListofLists will cast this if it returns a string.
					// It should not return a string
					// but it did...
					Log.d(TAG, "ClassCastException: " + e.getLocalizedMessage());
				}

			} else {
				// return real failure

				Log.d(TAG, "Could not get authToken. Reporting authException");
				syncResult.stats.numAuthExceptions++;
				doneIntent.putExtra(SYNC_RESULT, LOGIN_FAIL);
			}

		} finally {
			// This must always be called or we will leak resources
			if (apiTalker != null) {
				apiTalker.closeClient();
			}

			Log.d(TAG, "SyncResult: " + syncResult.toDebugString());
		}

		return doneIntent;
	}

	private void sortByRemoteParent(final ArrayList<GoogleTask> tasks) {
		final HashMap<String, Integer> levels = new HashMap<String, Integer>();
		levels.put(null, -1);
		final ArrayList<GoogleTask> tasksToDo = (ArrayList<GoogleTask>) tasks
				.clone();
		GoogleTask lastFailed = null;
		int current = -1;
		Log.d("remoteorder", "Doing remote sorting with size: " + tasks.size());
		while (!tasksToDo.isEmpty()) {
			current = current >= (tasksToDo.size() - 1) ? 0 : current + 1;
			Log.d("remoteorder", "current: " + current);

			if (levels.containsKey(tasksToDo.get(current).parent)) {
				Log.d("remoteorder", "parent in levelmap");
				levels.put(tasksToDo.get(current).id,
						levels.get(tasksToDo.get(current).parent) + 1);
				tasksToDo.remove(current);
				current -= 1;
				lastFailed = null;
			} else if (lastFailed == null) {
				Log.d("remoteorder", "lastFailed null, now " + current);
				lastFailed = tasksToDo.get(current);
			} else if (lastFailed.equals(tasksToDo.get(current))) {
				Log.d("remoteorder", "lastFailed == current");
				// Did full lap, parent is not new
				levels.put(tasksToDo.get(current).id, 99);
				levels.put(tasksToDo.get(current).parent, 98);
				tasksToDo.remove(current);
				current -= 1;
				lastFailed = null;
			}
		}

		// Just to make sure that new notes appear first in insertion order
		Collections.sort(tasks, new GoogleTask.RemoteOrder(levels));
	}
}
