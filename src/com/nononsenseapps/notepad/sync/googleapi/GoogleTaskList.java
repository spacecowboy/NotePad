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

	// private GoogleAPITalker api;

	public GoogleTaskList(JSONObject jsonList) throws JSONException {
		// this.api = ;

		id = jsonList.getString("id");
		etag = jsonList.getString("etag");
		title = jsonList.getString("title");
		updated = jsonList.getString("updated");

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
		String res = "{\n" + "etag: " + etag + ",\n" + "id: " + id + ",\n"
				+ "title: " + title + ",\n" + "dbId: " + dbId  + ",\n" + "updated: " + updated + ",\ndeleted: " + deleted + "}";
		return res;
	}

	/**
	 * Returns a JSON formatted version of this list. Includes title
	 * and id if not null
	 * 
	 * @return
	 */
	public String toJSON() {
		String jsonBody = "{ \"title\": \"" + title + "\"";
		if (id != null)
			jsonBody += ", \"id\": \"" + id + "\"";
		jsonBody += "}";
		Log.d(TAG, jsonBody);
		return jsonBody;
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
