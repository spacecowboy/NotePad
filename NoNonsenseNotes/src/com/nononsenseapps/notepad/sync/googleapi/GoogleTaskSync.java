package com.nononsenseapps.notepad.sync.googleapi;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Pair;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.PreconditionException;
import com.nononsenseapps.utils.time.RFC3339Date;

public class GoogleTaskSync {
	static final String TAG = "nononsenseapps gtasksync";
	public static final String AUTH_TOKEN_TYPE = "Manage your tasks";
	public static final boolean NOTIFY_AUTH_FAILURE = true;
	public static final String PREFS_LAST_SYNC_ETAG = "lastserveretag";
	public static final String PREFS_GTASK_LAST_SYNC_TIME = "gtasklastsync";

	/**
	 * Returns true if sync was successful, false otherwise
	 */
	public static boolean fullSync(final Context context,
			final Account account, final Bundle extras, final String authority,
			final ContentProviderClient provider, final SyncResult syncResult) {

		Log.d(TAG, "fullSync");
		// Is saved at a successful sync
		final long currentTime = Calendar.getInstance().getTimeInMillis();

		boolean success = false;
		// Initialize necessary stuff
		final AccountManager accountManager = AccountManager.get(context);
		final GoogleDBTalker dbTalker = new GoogleDBTalker(account.name,
				provider);
		final GoogleAPITalker apiTalker = new GoogleAPITalker();

		try {
			boolean connected = apiTalker.initialize(accountManager, account,
					AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);

			if (connected) {

				Log.d(TAG, "AuthToken acquired, we are connected...");

				try {
					// final String localEtag;

					// If full sync, then assume no local information exists.
					// if (settings.getBoolean(SyncPrefs.KEY_FULLSYNC, false)) {
					// localEtag = "dummytag";
					// }
					// else {
					// localEtag = settings
					// .getString(PREFS_LAST_SYNC_ETAG, "");
					// }

					// First the lists
					// Download lists from server
					final List<GoogleTaskList> remoteLists = downloadLists(apiTalker);

					// Synchronize lists locally
					final List<Pair<TaskList, GoogleTaskList>> listPairs = synchronizeListsLocally(
							context, remoteLists);

					// Synchronize lists remotely
					final List<Pair<TaskList, GoogleTaskList>> syncedPairs = synchronizeListsRemotely(
							listPairs, apiTalker);

					// For each list
					for (Pair<TaskList, GoogleTaskList> syncedPair : syncedPairs) {
						// Download tasks from server
						final List<GoogleTask> remoteTasks = downloadChangedTasks(
								context, apiTalker, syncedPair.second);

						// Synchronize tasks locally
						// TODO
						final List<Pair<Task, GoogleTask>> taskPairs = synchronizeTasksLocally(
								context, remoteTasks);

						// Synchronize tasks remotely
						// TODO
					}

					/*
					 * Tasks Step 1: Download changes from the server Step 2:
					 * Iterate and compare with local content Step 2a: If both
					 * versions changed, choose the latest Step 2b: If remote is
					 * newer, put info in local task, save Step 2c: If local is
					 * newer, upload it (in background) Step 3: For remote items
					 * that do not exist locally, save Step 4: For local items
					 * that do not exist remotely, upload
					 */

					// Prepare lists for items
					// ArrayList<GoogleTaskList> listsToSaveToDB = new
					// ArrayList<GoogleTaskList>();
					// HashMap<GoogleTaskList, ArrayList<GoogleTask>>
					// tasksInListToSaveToDB = new HashMap<GoogleTaskList,
					// ArrayList<GoogleTask>>();
					//
					// HashMap<Long, ArrayList<GoogleTask>> tasksInListToUpload
					// = new HashMap<Long, ArrayList<GoogleTask>>();
					// HashMap<Long, ArrayList<GoogleTask>> allTasksInList = new
					// HashMap<Long, ArrayList<GoogleTask>>();
					//
					// // gets all tasks in one query
					// ArrayList<GoogleTask> allTasks = dbTalker.getAllTasks(
					// allTasksInList, tasksInListToUpload);
					//
					// ArrayList<GoogleTaskList> listsToUpload = new
					// ArrayList<GoogleTaskList>();
					// ArrayList<GoogleTaskList> allLocalLists = new
					// ArrayList<GoogleTaskList>();

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
						}
						catch (PreconditionException e) {
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
								}
								catch (PreconditionException e) {
									// There was a conflict, do it again but as
									// a new note. Except if we tried to delete
									// a note
									// then just delete it locally and download
									// the
									// server version later.
									if (task.deleted == 1) {
										if (task.title.contains("debug")) {
											Log.d(TAG,
													"SyncDupe Upload conflict delete "
															+ task.title);
										}
										allTasks.remove(task);
										allTasksInList.get(task.listdbid)
												.remove(task);
										dbTalker.removeDeletedTask(task);
									}
									else {
										Log.d(TAG,
												"There was task conflict. Trying as new task");
										if (task.title.contains("debug")) {
											Log.d(TAG,
													"SyncDupe Upload conflict doing new "
															+ task.title);
										}
										task.id = null;
										task.etag = null;
										task.title = "sync-conflict "
												+ task.title;
										task.conflict = true;

										try {
											if (null != apiTalker.uploadTask(
													task, list)) {
												uploadedStuff = true;
												Log.d(TAG, "Uploaded "
														+ task.title
														+ ", didremoteinsert: "
														+ task.didRemoteInsert);
											}
										}
										catch (PreconditionException ee) {
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
					}
					else {

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

						// Now, set sorting values.
						for (GoogleTaskList list : tasksInListToSaveToDB
								.keySet()) {
							Log.d(TAG, "Setting position values in: " + list.id);
							ArrayList<GoogleTask> tasks = tasksInListToSaveToDB
									.get(list);

							if (tasks != null && !tasks.isEmpty()) {
								Log.d(TAG,
										"Setting position values for #tasks: "
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
								.putString(PREFS_LAST_SYNC_ETAG, serverEtag)
								.putBoolean(SyncPrefs.KEY_FULLSYNC, false)
								.commit();
					}

					Log.d(TAG, "Sync Complete!");
					success = true;

				}
				catch (ClientProtocolException e) {

					Log.d(TAG,
							"ClientProtocolException: "
									+ e.getLocalizedMessage());
				}
				// catch (JSONException e) {

				// Log.d(TAG, "JSONException: " + e.getLocalizedMessage());
				// }
				catch (IOException e) {
					syncResult.stats.numIoExceptions++;

					Log.d(TAG, "IOException: " + e.getLocalizedMessage());
				}
				catch (RemoteException e) {

					Log.d(TAG, "RemoteException: " + e.getLocalizedMessage());
				}
				catch (OperationApplicationException e) {
					Log.d(TAG,
							"Joined operation failed: "
									+ e.getLocalizedMessage());
				}
				catch (ClassCastException e) {
					// GetListofLists will cast this if it returns a string.
					// It should not return a string
					// but it did...
					Log.d(TAG, "ClassCastException: " + e.getLocalizedMessage());
				}

			}
			else {
				// return real failure

				Log.d(TAG, "Could not get authToken. Reporting authException");
				syncResult.stats.numAuthExceptions++;
				// doneIntent.putExtra(SYNC_RESULT, LOGIN_FAIL);
			}

		}
		catch (Exception e) {
			// Something went wrong, don't punish the user
		}
		finally {
			// This must always be called or we will leak resources
			if (apiTalker != null) {
				apiTalker.closeClient();
			}

			Log.d(TAG, "SyncResult: " + syncResult.toDebugString());
		}

		return success;
	}

	/**
	 * Downloads all lists in GTasks and returns them
	 * 
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	static List<GoogleTaskList> downloadLists(final GoogleAPITalker apiTalker)
			throws ClientProtocolException, IOException {
		// Do the actual download
		final ArrayList<GoogleTaskList> remoteLists = new ArrayList<GoogleTaskList>();
		apiTalker.getListOfLists(remoteLists);

		// Return list of TaskLists
		return remoteLists;
	}

	/**
	 * Given a list of remote GTaskLists, iterates through it and their versions
	 * (if any) in the local database. If the remote version is newer, the local
	 * version is updated.
	 * 
	 * If local list has a remote id, but it does not exist in the list of
	 * remote lists, then it has been deleted remotely and is deleted locally as
	 * well.
	 * 
	 * Returns a list of pairs (local, remote).
	 */
	static List<Pair<TaskList, GoogleTaskList>> synchronizeListsLocally(
			final Context context, final List<GoogleTaskList> remoteLists) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final ArrayList<Pair<TaskList, GoogleTaskList>> listPairs = new ArrayList<Pair<TaskList, GoogleTaskList>>();
		// For every list
		for (final GoogleTaskList remoteList : remoteLists) {
			// Compare with local
			TaskList localList = loadRemoteListFromDB(context, remoteList);

			if (localList == null) {
				if (remoteList.updated > settings.getLong(
						PREFS_GTASK_LAST_SYNC_TIME, 0)) {
					// If no local, and updated is higher than latestupdate,
					// create
					// new
					localList = new TaskList();
					localList.title = remoteList.title;
					localList.updated = remoteList.updated;
					localList.save(context);
					// Save id in remote also
					remoteList.dbid = localList._id;
					remoteList.save(context);
				}
				else {
					// If no local, updated is not higher, leaving local means
				}
			}
			else {
				// If local is newer, update remote object
				if (localList.updated > remoteList.updated) {
					remoteList.title = localList.title;
					// Updated is set by Google
				}
				else {
					// If remote is newer, update local and save to db
					localList.title = remoteList.title;
					localList.updated = remoteList.updated;
					localList.save(context);
				}
			}
			listPairs.add(new Pair<TaskList, GoogleTaskList>(localList,
					remoteList));
		}

		// Add local lists without a remote version to pairs
		for (final TaskList tl : loadNewListsFromDB(context, remoteLists.get(0))) {
			listPairs.add(new Pair<TaskList, GoogleTaskList>(tl, null));
		}

		// return pairs
		return listPairs;
	}

	static List<Pair<TaskList, GoogleTaskList>> synchronizeListsRemotely(
			final List<Pair<TaskList, GoogleTaskList>> listPairs,
			final GoogleAPITalker apiTalker) throws ClientProtocolException,
			IOException, PreconditionException {
		final List<Pair<TaskList, GoogleTaskList>> syncedPairs = new ArrayList<Pair<TaskList, GoogleTaskList>>();
		// For every list
		for (final Pair<TaskList, GoogleTaskList> pair : listPairs) {
			Pair<TaskList, GoogleTaskList> syncedPair = pair;
			if (pair.first == null) {
				// Deleted locally, delete remotely also
				pair.second.deletedLocally = true;
				apiTalker.uploadList(pair.second);
				syncedPair = null;
			}
			else if (pair.second == null) {
				// New list to create
				final GoogleTaskList newList = new GoogleTaskList(pair.first);
				apiTalker.uploadList(newList);
				syncedPair = new Pair<TaskList, GoogleTaskList>(pair.first,
						newList);
			}
			else if (pair.first.updated > pair.second.updated) {
				// If local update is different than remote, that means we
				// should update
				apiTalker.uploadList(pair.second);

			}
			// else remote has already been saved locally, nothing to upload
			if (syncedPair != null) {
				syncedPairs.add(syncedPair);
			}
		}
		// return (updated) pairs
		return syncedPairs;
	}

	static TaskList loadRemoteListFromDB(final Context context,
			final GoogleTaskList remoteList) {
		final Cursor c = context.getContentResolver().query(TaskList.URI,
				TaskList.Columns.FIELDS,
				remoteList.getTaskListWithRemoteClause(),
				remoteList.getTaskListWithRemoteArgs(), null);
		TaskList tl = null;
		try {
			if (c.moveToFirst()) {
				tl = new TaskList(c);
			}
		}
		finally {
			if (c != null) c.close();
		}

		return tl;
	}

	static List<TaskList> loadNewListsFromDB(final Context context,
			final GoogleTaskList remoteList) {
		final Cursor c = context.getContentResolver().query(TaskList.URI,
				TaskList.Columns.FIELDS,
				remoteList.getTaskListWithoutRemoteClause(),
				remoteList.getTaskListWithoutRemoteArgs(), null);
		final ArrayList<TaskList> lists = new ArrayList<TaskList>();
		try {
			while (c.moveToNext()) {
				lists.add(new TaskList(c));
			}
		}
		finally {
			if (c != null) c.close();
		}

		return lists;
	}
	
	static List<Task> loadNewTasksFromDB(final Context context,
			final GoogleTask remoteTask) {
		final Cursor c = context.getContentResolver().query(Task.URI,
				Task.Columns.FIELDS,
				remoteTask.getTaskWithoutRemoteClause(),
				remoteTask.getTaskWithoutRemoteArgs(), null);
		final ArrayList<Task> tasks = new ArrayList<Task>();
		try {
			while (c.moveToNext()) {
				tasks.add(new Task(c));
			}
		}
		finally {
			if (c != null) c.close();
		}

		return tasks;
	}

	static List<GoogleTask> downloadChangedTasks(final Context context,
			final GoogleAPITalker apiTalker, final GoogleTaskList remoteList)
			throws ClientProtocolException, IOException {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		final List<GoogleTask> remoteTasks = apiTalker.getModifiedTasks(
				RFC3339Date.asRFC3339(settings.getLong(
						PREFS_GTASK_LAST_SYNC_TIME, 0)), remoteList);

		return remoteTasks;
	}

	static Task loadRemoteTaskFromDB(final Context context,
			final GoogleTask remoteTask) {
		final Cursor c = context.getContentResolver().query(Task.URI,
				Task.Columns.FIELDS, remoteTask.getTaskWithRemoteClause(),
				remoteTask.getTaskWithRemoteArgs(), null);
		Task t = null;
		try {
			if (c.moveToFirst()) {
				t = new Task(c);
			}
		}
		finally {
			if (c != null) c.close();
		}

		return t;
	}

	static List<Pair<Task, GoogleTask>> synchronizeTasksLocally(
			final Context context, final List<GoogleTask> remoteTasks) {
		// TODO Auto-generated method stub
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		final ArrayList<Pair<Task, GoogleTask>> taskPairs = new ArrayList<Pair<Task, GoogleTask>>();
		// For every list
		for (final GoogleTask remoteTask : remoteTasks) {
			// Compare with local
			Task localTask = loadRemoteTaskFromDB(context, remoteTask);

			if (localTask == null) {
				if (remoteTask.updated > settings.getLong(
						PREFS_GTASK_LAST_SYNC_TIME, 0)) {
					// If no local, and updated is higher than latestupdate,
					// create
					// new
					localTask = new Task();
					localTask.title = remoteTask.title;
					localTask.note = remoteTask.notes;
					localTask.updated = remoteTask.updated;
					localTask.dblist = remoteTask.listdbid;
					if (remoteTask.dueDate != null
							&& !remoteTask.dueDate.isEmpty()) {
						try {
							localTask.due = RFC3339Date.parseRFC3339Date(
									remoteTask.dueDate).getTime();
						}
						catch (Exception e) {
						}
					}
					if (remoteTask.status != null
							&& remoteTask.status.equals(GoogleTask.COMPLETED)) {
						localTask.completed = remoteTask.updated;
					}

					localTask.save(context);
					// Save id in remote also
					remoteTask.dbid = localTask._id;
					remoteTask.save(context);
				}
				else {
					// If no local, updated is not higher, leaving local means
				}
			}
			else {
				// If local is newer, update remote object
				if (localTask.updated > remoteTask.updated) {
					// TODO
				}
				else {
					// If remote is newer, update local and save to db
					localTask.title = remoteTask.title;
					localTask.note = remoteTask.notes;
					localTask.updated = remoteTask.updated;
					localTask.dblist = remoteTask.listdbid;
					if (remoteTask.dueDate != null
							&& !remoteTask.dueDate.isEmpty()) {
						try {
							localTask.due = RFC3339Date.parseRFC3339Date(
									remoteTask.dueDate).getTime();
						}
						catch (Exception e) {
						}
					}
					if (localTask.completed == null
							&& remoteTask.status != null
							&& remoteTask.status.equals(GoogleTask.COMPLETED)) {
						localTask.completed = remoteTask.updated;
					}

					localTask.save(context);
				}
			}
			taskPairs.add(new Pair<Task, GoogleTask>(localTask, remoteTask));
		}

		// Add local lists without a remote version to pairs
		for (final Task t : loadNewTasksFromDB(context, remoteTasks.get(0))) {
			taskPairs.add(new Pair<Task, GoogleTask>(t, null));
		}

		// return pairs
		return taskPairs;
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
			}
			else if (lastFailed == null) {
				Log.d("remoteorder", "lastFailed null, now " + current);
				lastFailed = tasksToDo.get(current);
			}
			else if (lastFailed.equals(tasksToDo.get(current))) {
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
