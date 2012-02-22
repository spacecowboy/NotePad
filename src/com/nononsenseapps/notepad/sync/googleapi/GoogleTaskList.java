package com.nononsenseapps.notepad.sync.googleapi;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.NotePad;

import android.content.ContentValues;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

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

	// private GoogleAPITalker api;

	public GoogleTaskList(JSONObject jsonList) throws JSONException {
		// this.api = ;

		id = jsonList.getString("id");
		title = jsonList.getString("title");
		updated = jsonList.getString("updated");

		// Inital listing of lists does not contain etags
//		if (jsonList.has("etag"))
//			etag = jsonList.getString("etag");

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
			//json.put("etag", etag);
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

//			if (id != null)
//				json.put("id", id);

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
	 * Needs the result index, the index of the operation where the list is inserted in the database
	 * This will take precedence over the value in the other method
	 * @param accountName
	 * @param resultIndex
	 * @return
	 */
	public ContentValues toGTaskListsBackRefContentValues(String accountName, int resultIndex) {
		ContentValues values = new ContentValues();
		values.put(NotePad.GTaskLists.COLUMN_NAME_DB_ID, resultIndex);
		return values;
	}

	/*
	private void uploadModifiedTasks(GoogleAPITalker apiTalker,
			GoogleDBTalker dbTalker) throws RemoteException,
			ClientProtocolException, JSONException, IOException {
		for (GoogleTask task : dbTalker.getModifiedTasks(this)) {
			// First upload the modified tasks within.
			GoogleTask result;
			try {
				result = apiTalker.uploadTask(task, this);

				if (result != null)
					dbTalker.uploaded(result, this);
				else
					handleConflict(dbTalker, apiTalker, task);

			} catch (NotModifiedException e) {
				// Should not be possible here
				if (SyncAdapter.SYNC_DEBUG_PRINTS) Log.d(TAG, "uploadModifiedTasks: " + e.getLocalizedMessage());
			}
		}
	}*/

	public ArrayList<GoogleTask> downloadModifiedTasks(GoogleAPITalker apiTalker,
			ArrayList<GoogleTask> allTasks, String lastUpdated) throws RemoteException {
		// Compare with local tasks, if the tasks have the same remote id, then
		// they are the same. Use the existing db-id
		// to avoid creating duplicates
		// Also, we handle conflicts here directly by comparing the update timestamp on
		// both items
		ArrayList<GoogleTask> moddedTasks = new ArrayList<GoogleTask>();
		//ArrayList<GoogleTask> allTasks = dbTalker.getAllTasks(this);
		for (GoogleTask task : apiTalker.getModifiedTasks(lastUpdated, this)) {
			GoogleTask localVersion = null;
			for (GoogleTask localTask : allTasks) {
				if (task.equals(localTask)) {
						// We found it!
						task.dbId = localTask.dbId;
						// Save it until later
						localVersion = localTask;
						// Move on to next task
						break;
				}
			}
			if (localVersion == null) {
				// New item, just add to list
				moddedTasks.add(task);
			}
			else {
				// It also exists locally. Compare the timestamps and only add if it is newer
				// Last updated one wins
				Time local = new Time();
				local.parse3339(localVersion.updated);
				Time remote = new Time();
				remote.parse3339(task.updated);
				if (Time.compare(remote, local) >= 0) {
					Log.d(TAG, "Handling conflict: remote was newer: " + task.title);
					// Add to list only if remote is newer
					moddedTasks.add(task);
				}
				else {
					Log.d(TAG, "Handling conflict: local was newer: " + localVersion.title);
				}
			}
		}
		
		return moddedTasks;
	}

	/*
	private void handleConflict(GoogleDBTalker dbTalker,
			GoogleAPITalker apiTalker, GoogleTask localTask)
			throws RemoteException, ClientProtocolException, JSONException,
			NotModifiedException, IOException {
		//localTask.etag = null; // Set this to null so we dont do any
								// if-none-match gets
		GoogleTask remoteTask = apiTalker.getTask(localTask, this);
		// Last updated one wins
		Time local = new Time();
		local.parse3339(localTask.updated);
		Time remote = new Time();
		remote.parse3339(remoteTask.updated);
		if (Time.compare(remote, local) >= 0) {
			if (SyncAdapter.SYNC_DEBUG_PRINTS)
				Log.d(TAG, "Handling conflict: remote was newer");
			// remote is greater than local (or equal), save that to database
			remoteTask.dbId = localTask.dbId;
			dbTalker.SaveToDatabase(remoteTask, this);
		} else {
			if (SyncAdapter.SYNC_DEBUG_PRINTS)
				Log.d(TAG, "Handling conflict: local was newer");
			// Local is greater than remote, upload it.
			//localTask.etag = null;
			GoogleTask partialTask = apiTalker.uploadTask(localTask, this);
			localTask.id = partialTask.id;
			//localTask.etag = partialTask.etag;
			// Save new etag etc to db
			dbTalker.SaveToDatabase(localTask, this);
		}
	}*/

	/**
	 * This will set the sorting values correctly for these tasks. This must be
	 * called last when all other changes have been made to the tasks as it
	 * requires all parents etc to be present. Note that only objects in the modifiedTasks
	 * list are modified. The other is for reference only if the parent can not be found in modifiedTasks.
	 * If so, it is assumed to have a correct position.
	 */
	public void setSortingValues(ArrayList<GoogleTask> modifiedTasks,
			ArrayList<GoogleTask> allTasks) {
		// First clear all the position values as we will do a recursive
		// recalculation on these objects
		for (GoogleTask task : modifiedTasks) {
			task.abcsort = "";
			task.possort = "";
		}
		// Now, set the sorting values for these objects
		for (GoogleTask task : modifiedTasks) {
			getAbcSort(task, modifiedTasks, allTasks);
			getPosSort(task, modifiedTasks, allTasks);
		}
		// All sort values are set. It is OK to save now.
	}

	/**
	 * This will write the position value if none exist
	 */
	private String getPosSort(GoogleTask task, ArrayList<GoogleTask> modifiedTasks, ArrayList<GoogleTask> allTasks) {
		if (task.possort.isEmpty()) {
			String sortingValue = "";
			if (task.parent != null && !task.parent.isEmpty()) {
				GoogleTask parent = getTaskWithRemoteId(task.parent, modifiedTasks);
				if (parent == null) {
					// Try all tasks instead
					parent = getTaskWithRemoteId(task.parent, allTasks);
				}
				if (parent != null) {
					sortingValue += getPosSort(parent, modifiedTasks, allTasks);
				}
			}
			if (task.position != null) {
				sortingValue += task.position;
			}
			sortingValue += ".";

			task.possort = sortingValue;
		}
		return task.possort;
	}

	/**
	 * This will write the position value if none exists
	 */
	private String getAbcSort(GoogleTask task, ArrayList<GoogleTask> modifiedTasks, ArrayList<GoogleTask> allTasks) {
		if (task.abcsort.isEmpty()) {
			String sortingValue = "";
			if (task.parent != null && !task.parent.isEmpty()) {
				GoogleTask parent = getTaskWithRemoteId(task.parent, modifiedTasks);
				if (parent == null) {
					// Try all tasks instead
					parent = getTaskWithRemoteId(task.parent, allTasks);
				}
				if (parent != null) {
					sortingValue += getAbcSort(parent, modifiedTasks, allTasks);
				}
			}
			if (task.title != null) {
				sortingValue += task.title;
			}
			sortingValue += ".";

			task.abcsort = sortingValue;
		}
		return task.abcsort;
	}

	private GoogleTask getTaskWithRemoteId(String id,
			ArrayList<GoogleTask> tasks) {
		for (GoogleTask task : tasks) {
			if (task.id.equals(id)) {
				return task;
			}
		}
		return null;
	}
	
	/**
	 * Returns true if the TaskList has the same remote id or the same database id.
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
