package com.nononsenseapps.notepad.sync.googleapi;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.NotePad;

import android.content.ContentValues;
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
	public ArrayList<GoogleTask> getTasks() {
		return null;
	}

	public GoogleTask getTask() {
		return null;
	}

	public GoogleTask insertTask(GoogleTask task) {
		return null;
	}

	public GoogleTask updateTask(GoogleTask task) {
		return null;
	}

	public void deleteTask(GoogleTask task) {

	}

	public void move(GoogleTask task, GoogleTask newParent) {

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
}
