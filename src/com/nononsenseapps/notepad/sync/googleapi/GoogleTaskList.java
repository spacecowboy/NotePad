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

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.util.BiMap;

import android.content.ContentValues;
import android.os.RemoteException;
import com.nononsenseapps.helpers.Log;

public class GoogleTaskList {

	private static final String TAG = "GoogleTaskList";
	public String id = null;
	public long dbId = -1;
	public String etag = "";
	public String title = null;
	public int deleted = 0;
	// public String selfLink = null;
	public JSONObject json = null;
	public String updated = null;

	public boolean didRemoteInsert = false;
	public int modified = 0;
	// Intended for when default list is deleted. When that fails, redownload it and its contents
	public boolean redownload = false;

	// private GoogleAPITalker api;

	public GoogleTaskList(JSONObject jsonList) throws JSONException {
		// this.api = ;

		id = jsonList.getString("id");
		title = jsonList.getString("title");
		updated = jsonList.getString("updated");

		// Inital listing of lists does not contain etags
		if (jsonList.has("etag")) {
			etag = jsonList.getString("etag");
		}

		json = jsonList;
	}

	public GoogleTaskList() {
	}

	public String toString() {
		String res = "";
		JSONObject json = new JSONObject();
		try {
			json.put("title", title);
			json.put("id", id);
			// json.put("etag", etag);
			json.put("dbid", dbId);
			json.put("deleted", deleted);
			json.put("updated", updated);

			res = json.toString(2);
		} catch (JSONException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
		return res;
	}

	/**
	 * Returns a JSON formatted version of this list. Includes title and not id
	 * 
	 * @return
	 * @throws JSONException
	 */
	public String toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("title", title);

			// if (id != null)
			// json.put("id", id);

		} catch (JSONException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}

		return json.toString();
	}

	/**
	 * Returns a ContentValues hashmap suitable for database insertion in the
	 * Lists table Includes Title and modified flag as specified in the
	 * arguments
	 * 
	 * @return
	 */
	public ContentValues toListsContentValues() {
		ContentValues values = new ContentValues();
		values.put(NotePad.Lists.COLUMN_NAME_TITLE, title);
		values.put(NotePad.Lists.COLUMN_NAME_MODIFIED, 0);
		values.put(NotePad.Lists.COLUMN_NAME_DELETED, 0);
		return values;
	}

	public ContentValues toGTaskListsContentValues(String accountName) {
		ContentValues values = new ContentValues();
		values.put(NotePad.GTaskLists.COLUMN_NAME_DB_ID, dbId);
		values.put(NotePad.GTaskLists.COLUMN_NAME_ETAG, etag);
		values.put(NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT, accountName);
		values.put(NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID, id);
		values.put(NotePad.GTaskLists.COLUMN_NAME_UPDATED, updated);
		return values;
	}

	/**
	 * Needs the result index, the index of the operation where the list is
	 * inserted in the database This will take precedence over the value in the
	 * other method
	 * 
	 * @param accountName
	 * @param resultIndex
	 * @return
	 */
	public ContentValues toGTaskListsBackRefContentValues(String accountName,
			int resultIndex) {
		ContentValues values = new ContentValues();
		values.put(NotePad.GTaskLists.COLUMN_NAME_DB_ID, resultIndex);
		return values;
	}

	public ArrayList<GoogleTask> downloadModifiedTasks(
			GoogleAPITalker apiTalker, ArrayList<GoogleTask> allTasks,
			String lastUpdated)
			throws RemoteException {
		// Compare with local tasks, if the tasks have the same remote id, then
		// they are the same. Use the existing db-id
		// to avoid creating duplicates
		ArrayList<GoogleTask> moddedTasks = new ArrayList<GoogleTask>();
		String timestamp = lastUpdated;
		if (redownload) {
			// Force download of everything
			Log.d(TAG, "Redownloading items in list " + this.title);
			timestamp = null;
		}
		for (GoogleTask task : apiTalker.getModifiedTasks(timestamp, this)) {
			// GoogleTask localVersion = null;
			for (GoogleTask localTask : allTasks) {
				if (task.equals(localTask)) {
					// We found it!
					Log.d(TAG, "Found local version for remote task " + task.title);
					task.dbId = localTask.dbId;
					// Move on to next task
					break;
				}
			}
			Log.d(TAG, "DBID for " + task.title + " is " + task.dbId + " deleted: " + task.deleted);
			moddedTasks.add(task);
		}

		return moddedTasks;
	}

	/**
	 * This will set the sorting values correctly for these tasks. This must be
	 * called last when all other changes have been made to the tasks as it
	 * requires all parents etc to be present. Note that only objects in the
	 * modifiedTasks list are modified. The other is for reference only if the
	 * parent can not be found in modifiedTasks. If so, it is assumed to have a
	 * correct position.
	 * 
	 * Will also set indent levels on objects
	 */
	public void setSortingValues(ArrayList<GoogleTask> modifiedTasks,
			ArrayList<GoogleTask> allTasks) {
		// First clear all the position values as we will do a recursive
		// recalculation on these objects
		for (GoogleTask task : modifiedTasks) {
			task.possort = "";
			task.indentLevel = 0;
		}
		// Now, set the sorting values for these objects
		for (GoogleTask task : modifiedTasks) {
			getPosSort(task, modifiedTasks, allTasks);
		}
		// All sort values are set. It is OK to save now.
	}

	/**
	 * This will write the position value if none exist
	 */
	private String getPosSort(GoogleTask task,
			ArrayList<GoogleTask> modifiedTasks, ArrayList<GoogleTask> allTasks) {
		if (task.possort.isEmpty()) {
			String sortingValue = "";
			if (task.parent != null && !task.parent.isEmpty()) {
				GoogleTask parent = getTaskWithRemoteId(task.parent,
						modifiedTasks);
				if (parent == null) {
					// Try all tasks instead
					parent = getTaskWithRemoteId(task.parent, allTasks);
				}
				if (parent != null) {
					sortingValue += getPosSort(parent, modifiedTasks, allTasks);
					task.indentLevel = parent.indentLevel + 1;
				}
			}
			if (task.position != null) {
				sortingValue += task.position;
			}
			sortingValue += ".";

			task.possort = sortingValue;
		}
		Log.d(TAG, "indent: " + task.indentLevel);
		return task.possort;
	}

	private GoogleTask getTaskWithRemoteId(String id,
			ArrayList<GoogleTask> tasks) {
		if (id == null || tasks == null)
			return null;

		for (GoogleTask task : tasks) {
			if (id.equals(task.id)) {
				return task;
			}
		}
		return null;
	}

	/**
	 * Returns true if the TaskList has the same remote id or the same database
	 * id.
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		if (GoogleTaskList.class.isInstance(o)) {
			// It's a list!
			GoogleTaskList list = (GoogleTaskList) o;
			if (dbId != -1 && dbId == list.dbId) {
				equal = true;
			}
			if (id != null && id.equals(list.id)) {
				equal = true;
			}
		}
		return equal;
	}
}
