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

package com.nononsenseapps.notepad.sync.googleapi;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import com.nononsenseapps.helpers.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.util.BiMap;

/**
 * Helper class which talks to the database and converts the responses into Task
 * and List objects
 */
public class GoogleDBTalker {
	// Joined notes query
	private static final String[] JOINED_NOTES_PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_DELETED,
			NotePad.Notes.COLUMN_NAME_MODIFIED,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
			NotePad.Notes.COLUMN_NAME_LIST,
			NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
			NotePad.Notes.COLUMN_NAME_PARENT,
			NotePad.Notes.COLUMN_NAME_POSITION,
			NotePad.Notes.COLUMN_NAME_HIDDEN, NotePad.GTasks.COLUMN_NAME_DB_ID,
			NotePad.Notes.COLUMN_NAME_POSSUBSORT,
			NotePad.Notes.COLUMN_NAME_INDENTLEVEL,
			NotePad.GTasks.COLUMN_NAME_ETAG,
			NotePad.GTasks.COLUMN_NAME_GTASKS_ID,
			NotePad.GTasks.COLUMN_NAME_UPDATED };

	// Joined lists query
	private static final String[] JOINED_LIST_PROJECTION = new String[] {
			NotePad.Lists._ID, NotePad.Lists.COLUMN_NAME_TITLE,
			NotePad.Lists.COLUMN_NAME_DELETED,
			NotePad.Lists.COLUMN_NAME_MODIFIED,
			NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE,
			NotePad.GTaskLists.COLUMN_NAME_DB_ID,
			NotePad.GTaskLists.COLUMN_NAME_ETAG,
			NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID,
			NotePad.GTaskLists.COLUMN_NAME_UPDATED };

	private static final String TAG = "GoogleDBTalker";

	protected String accountName;
	protected ContentProviderClient provider;

	private ArrayList<ContentProviderOperation> operations = null;

	/**
	 * Needs the accountName of the currently active sync account because it in
	 * essence supports several
	 */
	public GoogleDBTalker(String accountName, ContentProviderClient provider) {

		Log.d(TAG, "constructor");
		this.accountName = accountName;
		this.provider = provider;

		operations = new ArrayList<ContentProviderOperation>();
	}

	/**
	 * Gets all tasks with a modified flag in the database.
	 * 
	 * @param list
	 * @throws RemoteException
	 */
	/*
	 * public ArrayList<GoogleTask> getModifiedTasks(GoogleTaskList list) throws
	 * RemoteException { ArrayList<GoogleTask> modifiedTasks = new
	 * ArrayList<GoogleTask>();
	 * 
	 * Cursor cursor = provider.query(NotePad.Notes.CONTENT_URI,
	 * NOTES_PROJECTION, NotePad.Notes.COLUMN_NAME_MODIFIED + " IS ? AND " +
	 * NotePad.Notes.COLUMN_NAME_LIST + " IS ?", new String[] { "1",
	 * Long.toString(list.dbId) }, null);
	 * 
	 * populateWithTasks(cursor, modifiedTasks); cursor.close();
	 * 
	 * return modifiedTasks; }
	 */

	/**
	 * Gets all lists with a modified flag set to the specified value in the
	 * database
	 * 
	 * @throws RemoteException
	 */
	// public ArrayList<GoogleTaskList> getModifiedLists() throws
	// RemoteException {
	// ArrayList<GoogleTaskList> bigList = new ArrayList<GoogleTaskList>();
	// Cursor cursor = provider.query(NotePad.Lists.CONTENT_URI,
	// LIST_PROJECTION, NotePad.Lists.COLUMN_NAME_MODIFIED + " IS 1",
	// null, null);
	//
	// populateWithLists(cursor, bigList);
	// cursor.close();
	//
	// return bigList;
	// }

	/**
	 * Gets all lists from the database using a single joined query
	 * 
	 * @param allLists
	 * @param modifiedLists
	 * @throws RemoteException
	 */
	public void getAllLists(ArrayList<GoogleTaskList> allLists,
			ArrayList<GoogleTaskList> modifiedLists) throws RemoteException {
		// NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1"
		Cursor cursor = provider.query(NotePad.Lists.CONTENT_JOINED_URI,
				JOINED_LIST_PROJECTION, null, new String[] { accountName },
				null);

		populateWithLists(cursor, allLists, modifiedLists);
		cursor.close();
	}

	private void populateWithLists(Cursor cursor,
			ArrayList<GoogleTaskList> allLists,
			ArrayList<GoogleTaskList> modifiedLists) throws RemoteException {
		if (cursor != null && !cursor.isAfterLast()) {
			while (cursor.moveToNext()) {
				GoogleTaskList list = new GoogleTaskList();
				list.dbId = cursor.getLong(cursor
						.getColumnIndex(NotePad.Lists._ID));
				list.title = cursor.getString(cursor
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
				list.deleted = cursor.getInt(cursor
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_DELETED));

				list.modified = cursor.getInt(cursor
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_MODIFIED));

				// convert modification time to timestamp
				long modTime = cursor
						.getInt(cursor
								.getColumnIndex(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE));

				Time localTime = new Time(Time.getCurrentTimezone());
				localTime.set(modTime);

				list.updated = localTime.format3339(false);

				list.id = cursor
						.getString(cursor
								.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID));
				// Compare with the modification time in the object, select
				// the newest timestamp
				String updTime = cursor
						.getString(cursor
								.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_UPDATED));
				Time remoteTime = new Time(Time.getCurrentTimezone());
				if (updTime != null && !updTime.isEmpty()) {
					remoteTime.parse3339(updTime);

					if (Time.compare(localTime, remoteTime) < 0) {
						// remoteTime is greater
						list.updated = updTime;
					} // else we already did that before
				}

				allLists.add(list);
				if (list.modified == 1)
					modifiedLists.add(list);
			}
		}
	}

	/**
	 * Returns a 3339-formatted timestamp which is the latest time that we
	 * synced. If no sync has been done before, null will be returned Finds the
	 * latest update-stamp in the GTasks table
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public String getLastUpdated(String accountName) throws RemoteException {
		Time lastDate = null;
		Cursor cursor = provider.query(NotePad.GTasks.CONTENT_URI,
				new String[] { NotePad.GTasks.COLUMN_NAME_UPDATED },
				NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT + " IS ?",
				new String[] { accountName }, null);
		if (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
			while (cursor.moveToNext()) {
				String updated = cursor.getString(cursor
						.getColumnIndex(NotePad.GTasks.COLUMN_NAME_UPDATED));
				if ("".equals(updated))
					continue;
				Time thisDate = new Time();
				thisDate.parse3339(updated);
				if (lastDate == null || Time.compare(thisDate, lastDate) >= 0) {
					lastDate = thisDate;
				}
			}
		}
		cursor.close();

		if (lastDate == null)
			return null;
		else
			return lastDate.format3339(false);
	}

	/**
	 * Fetches all tasks with a single query from the database and populates the
	 * arguments with all tasks and only modified tasks respectively. Keys are
	 * the list database ids
	 * 
	 * Will clear both hashmaps initially.
	 * 
	 * @param allTasks
	 * @param modifiedTasks
	 * @throws RemoteException
	 */
	public ArrayList<GoogleTask> getAllTasks(
			HashMap<Long, ArrayList<GoogleTask>> allTasks,
			HashMap<Long, ArrayList<GoogleTask>> modifiedTasks,
			HashMap<Long, String> idMap) throws RemoteException {
		allTasks.clear();
		modifiedTasks.clear();

		ArrayList<GoogleTask> listOfAllTasks = new ArrayList<GoogleTask>();

		// Sort by position so the id map is updated correctly before being used
		Cursor cursor = provider.query(NotePad.Notes.CONTENT_JOINED_URI,
				JOINED_NOTES_PROJECTION, null, new String[] { accountName },
				NotePad.Notes.COLUMN_NAME_POSSUBSORT);

		populateWithTasks(cursor, allTasks, idMap);
		cursor.close();
		// Now populate the modified ones
		for (long listId : allTasks.keySet()) {
			ArrayList<GoogleTask> modList = new ArrayList<GoogleTask>();
			for (GoogleTask task : allTasks.get(listId)) {
				if (task.modified == 1) {
					Log.d(TAG, "Task modified status: 1");
					modList.add(task);
				}
				listOfAllTasks.add(task); // add all to this
			}
			modifiedTasks.put(listId, modList);
		}

		return listOfAllTasks;
	}

	private void populateWithTasks(Cursor cursor,
			HashMap<Long, ArrayList<GoogleTask>> map,
			HashMap<Long, String> idMap) throws RemoteException {
		if (cursor != null && !cursor.isAfterLast()) {
			while (cursor.moveToNext()) {
				GoogleTask task = new GoogleTask();
				task.dbId = cursor.getLong(cursor
						.getColumnIndex(NotePad.Notes._ID));
				task.title = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE));
				task.deleted = cursor.getInt(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_DELETED));
				task.notes = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));
				task.dueDate = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE));
				task.status = cursor
						.getString(cursor
								.getColumnIndex(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS));
				task.listdbid = cursor.getLong(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_LIST));

				task.parent = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_PARENT));
				task.position = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_POSITION));

				task.hidden = cursor.getInt(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_HIDDEN));
				// Task is assembled, move on
				task.modified = cursor.getInt(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFIED));

				task.indentLevel = cursor.getInt(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_INDENTLEVEL));
				task.possort = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_POSSUBSORT));

				// convert modification time to timestamp
				long modTime = cursor
						.getLong(cursor
								.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));

				Time localTime = new Time(Time.getCurrentTimezone());
				localTime.set(modTime);

				task.updated = localTime.format3339(false);

				task.id = cursor.getString(cursor
						.getColumnIndex(NotePad.GTasks.COLUMN_NAME_GTASKS_ID));
				
				task.etag = cursor.getString(cursor.getColumnIndex(NotePad.GTasks.COLUMN_NAME_ETAG));

				// We need to be able to easily convert ids for position reasons
				if (task.id != null && !task.id.isEmpty())
					idMap.put(task.dbId, task.id);

				// Compare with the modification time in the object, select
				// the newest timestamp
				String updTime = cursor.getString(cursor
						.getColumnIndex(NotePad.GTasks.COLUMN_NAME_UPDATED));
				Time remoteTime = new Time(Time.getCurrentTimezone());
				if (updTime != null && !updTime.isEmpty()) {
					remoteTime.parse3339(updTime);

					if (Time.compare(localTime, remoteTime) < 0) {
						// remoteTime is greater
						task.updated = updTime;
					} // else we already did that before
				}

				if (null == map.get(task.listdbid))
					map.put(task.listdbid, new ArrayList<GoogleTask>());
				map.get(task.listdbid).add(task);
			}
		}
	}

	public void SaveToDatabase(
			ArrayList<GoogleTaskList> listsToSaveToDB,
			HashMap<GoogleTaskList, ArrayList<GoogleTask>> tasksInListToSaveToDB,
			BiMap<Long, String> idMap, ArrayList<GoogleTask> allTasks) {
		int listIdIndex = -1;

		Set<GoogleTaskList> listIdsWithTasks = tasksInListToSaveToDB.keySet();
		for (GoogleTaskList list : listsToSaveToDB) {
			if (list.dbId > -1) {
				// Exists in database, update that record
				if (list.deleted == 1) {
					// Delete from database
					operations.add(ContentProviderOperation.newDelete(
							Uri.withAppendedPath(
									NotePad.Lists.CONTENT_ID_URI_BASE,
									Long.toString(list.dbId))).build());

					// GTaskslists table handled by provider
					// As well as any notes contained in list

				} else {
					// Update record
					operations.add(ContentProviderOperation
							.newUpdate(
									Uri.withAppendedPath(
											NotePad.Lists.CONTENT_ID_URI_BASE,
											Long.toString(list.dbId)))
							.withValues(list.toListsContentValues()).build());
					// Then gtasks table
					if (list.didRemoteInsert) {
						// New on server
						operations
								.add(ContentProviderOperation
										.newInsert(
												NotePad.GTaskLists.CONTENT_URI)
										.withValues(
												list.toGTaskListsContentValues(accountName))
										.build());
					} else {
						// Already existed on server
						operations
								.add(ContentProviderOperation
										.newUpdate(
												NotePad.GTaskLists.CONTENT_URI)
										.withValues(
												list.toGTaskListsContentValues(accountName))
										.withSelection(
												NotePad.GTaskLists.COLUMN_NAME_DB_ID
														+ " IS ?"
														+ " AND "
														+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT
														+ " IS ?",
												new String[] {
														Long.toString(list.dbId),
														accountName }).build());
					}
				}
			} else {
				// Do insertion
				operations.add(ContentProviderOperation
						.newInsert(NotePad.Lists.CONTENT_URI)
						.withValues(list.toListsContentValues()).build());
				listIdIndex = operations.size() - 1;
				// Then gtasks table with back reference
				operations
						.add(ContentProviderOperation
								.newInsert(NotePad.GTaskLists.CONTENT_URI)
								.withValues(
										list.toGTaskListsContentValues(accountName))
								.withValueBackReferences(
										list.toGTaskListsBackRefContentValues(
												accountName, listIdIndex))
								.build());
			}
			// Now do any possible tasks
			ArrayList<GoogleTask> tasks = tasksInListToSaveToDB.get(list);
			if (tasks != null && !tasks.isEmpty()) {

				Log.d(TAG, "Found some tasks to save in: " + list.id);
				SaveNoteToDatabase(tasks, listIdIndex, list.dbId, idMap,
						allTasks);
			}
			// Remove it from the keyset, will affect the hashmap as well
			if (list.id != null) {
				listIdsWithTasks.remove(list);
			}
		}
		// If any lists are left, that means only the tasks and not the lists
		// were modified.
		// Save potential items here as well.
		for (GoogleTaskList list : listIdsWithTasks) {
			ArrayList<GoogleTask> tasks = tasksInListToSaveToDB.get(list);
			if (tasks != null && !tasks.isEmpty()) {
				long listDbId = tasks.get(0).listdbid;

				Log.d(TAG, "Saving tasks for: " + listDbId);
				SaveNoteToDatabase(tasks, -1, listDbId, idMap, allTasks);
			}
		}
	}

	private void SaveNoteToDatabase(ArrayList<GoogleTask> tasks,
			int listIdIndex, long listDbId, BiMap<Long, String> idMap,
			ArrayList<GoogleTask> allTasks) {
		int lastNoteIdIndex;
		// Need to remember in what operation a note was created for position
		// reasons
		// BiMap<String, Integer> remoteToIndex = new BiMap<String, Integer>();
		for (GoogleTask task : tasks) {
			if (task.dbId > -1 && task.deleted != 1) {

				Log.d(TAG, "Updating task");

				operations.add(ContentProviderOperation
						.newUpdate(
								Uri.withAppendedPath(
										NotePad.Notes.CONTENT_ID_URI_BASE,
										Long.toString(task.dbId)))
						.withValues(
								task.toNotesContentValues(0, listDbId, idMap))
						.build());
				if (task.didRemoteInsert) {
					operations
							.add(ContentProviderOperation
									.newInsert(NotePad.GTasks.CONTENT_URI)
									.withValues(
											task.toGTasksContentValues(accountName))
									.build());
				} else {
					operations
							.add(ContentProviderOperation
									.newUpdate(NotePad.GTasks.CONTENT_URI)
									.withValues(
											task.toGTasksContentValues(accountName))
									.withSelection(
											NotePad.GTasks.COLUMN_NAME_DB_ID
													+ " IS ?"
													+ " AND "
													+ NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT
													+ " IS ?",
											new String[] {
													Long.toString(task.dbId),
													accountName }).build());
				}

			} else if (task.dbId > -1 && task.deleted == 1) {

				Log.d(TAG, "Deleting task");

				operations.add(ContentProviderOperation.newDelete(
						Uri.withAppendedPath(NotePad.Notes.CONTENT_ID_URI_BASE,
								Long.toString(task.dbId))).build());

				// GTasks table Handled by the provider

			} else if (task.deleted == 1) {

				Log.d(TAG, "Delete task with no dbId? Madness!");
				// This must be a task which we ourselves deleted earlier.
				// Ignore them as they do not exist in the DB
			} else {
				// Tasks which have been created on the server

				Log.d(TAG, "Inserting task");

				// Either the list exists, or the list is new as well
				if (listDbId > -1) {
					// List exists, use existing id
					operations.add(ContentProviderOperation
							.newInsert(NotePad.Notes.CONTENT_URI)
							.withValues(
									task.toNotesContentValues(0, listDbId,
											idMap)).build());
				} else {
					// Use back reference to that insert operation
					// back reference will get precedence over the invalid value
					operations
							.add(ContentProviderOperation
									.newInsert(NotePad.Notes.CONTENT_URI)
									.withValues(
											task.toNotesContentValues(0,
													listDbId, idMap))
									.withValueBackReferences(
											task.toNotesBackRefContentValues(listIdIndex))
									.build());
				}
				// Now the other table, use back reference to the id the note
				// received
				lastNoteIdIndex = operations.size() - 1;
				// And store for positions
				// remoteToIndex.put(task.id, lastNoteIdIndex);

				operations
						.add(ContentProviderOperation
								.newInsert(NotePad.GTasks.CONTENT_URI)
								.withValues(
										task.toGTasksContentValues(accountName))
								.withValueBackReferences(
										task.toGTasksBackRefContentValues(lastNoteIdIndex))
								.build());

			}
		}

	}

	public ContentProviderResult[] apply() throws RemoteException,
			OperationApplicationException {
		return provider.applyBatch(operations);
	}
}