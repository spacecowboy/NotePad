package com.nononsenseapps.notepad.sync.googleapi;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.NotModifiedException;

import android.content.ContentValues;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

public class GoogleTaskList {

	private static final String TAG = "GoogleTaskList";
	public String id = null;
	public long dbId = -1;
	public String etag = null;
	public String title = null;
	public int deleted = 0;
	// public String selfLink = null;
	public JSONObject json = null;
	public String updated = null;
	
	public boolean didRemoteInsert = false;

	// private GoogleAPITalker api;

	public GoogleTaskList(JSONObject jsonList) throws JSONException {
		// this.api = ;

		id = jsonList.getString("id");
		title = jsonList.getString("title");
		updated = jsonList.getString("updated");
		
		// Inital listing of lists does not contain etags
		if (jsonList.has("etag"))
			etag = jsonList.getString("etag");

		json = jsonList;
	}

	public GoogleTaskList() {
	}

	/*
	 * Tasks
	 */
	private ArrayList<GoogleTask> getTasks() {
		return null;
	}

	private GoogleTask getTask() {
		return null;
	}

	private GoogleTask insertTask(GoogleTask task) {
		return null;
	}

	private GoogleTask updateTask(GoogleTask task) {
		return null;
	}

	private void deleteTask(GoogleTask task) {

	}

	private void move(GoogleTask task, GoogleTask newParent) {

	}

	public String toString() {
		String res = "";
		JSONObject json = new JSONObject();
		try {
			json.put("title", title);
			json.put("id", id);
			json.put("etag", etag);
			json.put("dbid", dbId);
			json.put("deleted", deleted);
			json.put("updated", updated);
		
		res =  json.toString(2);
		} catch (JSONException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
		return res;
	}

	/**
	 * Returns a JSON formatted version of this list. Includes title
	 * and id if not null
	 * 
	 * @return
	 * @throws JSONException 
	 */
	public String toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("title", title);
		
		if (id != null)
			json.put("id", id);
		
		Log.d(TAG, json.toString(2));
		} catch (JSONException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
		
		return json.toString();
	}
	
	/**
	 * Returns a ContentValues hashmap suitable for database insertion in the Lists table
	 * Includes Title and modified flag as specified in the arguments
	 * @return
	 */
	public ContentValues toListsContentValues(int modified) {
		ContentValues values = new ContentValues();
		values.put(NotePad.Lists.COLUMN_NAME_TITLE, title);
		values.put(NotePad.Lists.COLUMN_NAME_MODIFIED, modified);
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

	public void uploadModifiedTasks(GoogleAPITalker apiTalker, GoogleDBTalker dbTalker) throws RemoteException, ClientProtocolException, JSONException, NotModifiedException, IOException {
		for (GoogleTask task: dbTalker.getModifiedTasks(this)) {
			// First upload the modified tasks within.
			GoogleTask result = apiTalker.uploadTask(task, this);
			if (result != null)
				dbTalker.uploaded(result, this);
			else
				handleConflict(dbTalker, apiTalker, task);
		}
	}

	public void downloadModifiedTasks(GoogleAPITalker apiTalker, GoogleDBTalker dbTalker, String lastUpdated) throws RemoteException {
		Log.d(TAG, "DownloadModifiedTasks, last date: " + lastUpdated);
		for (GoogleTask task: apiTalker.getModifiedTasks(lastUpdated, this)) {
			Log.d(TAG, "Saving modified: " + task.toJSON());
			dbTalker.SaveToDatabase(task, this);
		}
	}
	
	private void handleConflict(GoogleDBTalker dbTalker,
			GoogleAPITalker apiTalker, GoogleTask localTask) throws RemoteException, ClientProtocolException, JSONException, NotModifiedException, IOException {
		localTask.etag = null; // Set this to null so we dont do any if-none-match gets
		GoogleTask remoteTask = apiTalker.getTask(localTask, this);
		// Last updated one wins
		Time local = new Time();
		local.parse3339(localTask.updated);
		Time remote = new Time();
		remote.parse3339(remoteTask.updated);
		if (Time.compare(remote, local) >= 0) {
			Log.d(TAG, "Handling conflict: remote was newer");
			// remote is greater than local (or equal), save that to database
			remoteTask.dbId = localTask.dbId;
			dbTalker.SaveToDatabase(remoteTask, this);
		} else {
			Log.d(TAG, "Handling conflict: local was newer");
			// Local is greater than remote, upload it.
			localTask.etag = null;
			GoogleTask partialTask = apiTalker.uploadTask(localTask, this);
			localTask.id = partialTask.id;
			localTask.etag = partialTask.etag;
			// Save new etag etc to db
			dbTalker.SaveToDatabase(localTask, this);
		}
	}
}
