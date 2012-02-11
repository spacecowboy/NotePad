package com.nononsenseapps.notepad.sync.googleapi;

import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.NotePad;

import android.content.ContentValues;
import android.util.Log;

public class GoogleTask {

	private static final String TAG = "GoogleTask";
	public String id = null;
	public String etag = null;
	public String title = null;
	public String updated = null;
	public String notes = null;
	public String status = null;
	public String dueDate = null;

	public long dbId = -1;
	public int deleted = 0;
	public long listdbid = -1;
	public boolean didRemoteInsert = false;
	
	public JSONObject json = null;

	public GoogleTask() {
	}

	public GoogleTask(JSONObject jsonTask) throws JSONException {
		id = jsonTask.getString("id");
		title = jsonTask.getString("title");
		updated = jsonTask.getString("updated");
		etag = jsonTask.getString("etag");
		if (jsonTask.has("notes"))
			notes = jsonTask.getString("notes");
		status  = jsonTask.getString("status");
		if (jsonTask.has("dueDate"))
			dueDate = jsonTask.getString("dueDate");
		if (jsonTask.has("deleted") && jsonTask.getBoolean("deleted"))
			deleted = 1;
		else
			deleted = 0;
		
		

		json = jsonTask;
	}

	/**
	 * Special tricks because google api actually want 'null' while JSONObject doesnt allow them.
	 * @return
	 */
	public String toJSON() {
		String returnString = "";
		try {
			JSONObject json = new JSONObject();
			String nullAppendage = "";
			if (id != null)
				json.put("id", id);

			json.put("title", title);
			json.put("notes", notes);
			if (dueDate != null && !dueDate.equals(""))
				json.put("due", dueDate);
			else
				nullAppendage += ", \"due\": null";

			json.put("status", status);
			if (status != null && status.equals("needsAction")) {
				// We must reset this also in this case
				nullAppendage += ", \"completed\": null}";
			}
			
			nullAppendage += "}";
				
			
			returnString = json.toString();
			returnString = returnString.substring(0, returnString.length()-1) + nullAppendage;

			Log.d(TAG, returnString);
		} catch (JSONException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}

		return returnString;
	}

	/**
	 * Returns a ContentValues hashmap suitable for database insertion in the
	 * Lists table Includes modified flag and list id as specified in the
	 * arguments
	 * 
	 * @return
	 */
	public ContentValues toNotesContentValues(int modified, long listId) {
		ContentValues values = new ContentValues();
		values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
		values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, dueDate);
		values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS, status);
		values.put(NotePad.Notes.COLUMN_NAME_NOTE, notes);

		values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);
		values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, modified);
		return values;
	}

	public ContentValues toGTasksContentValues(String accountName) {
		ContentValues values = new ContentValues();
		values.put(NotePad.GTasks.COLUMN_NAME_DB_ID, dbId);
		values.put(NotePad.GTasks.COLUMN_NAME_ETAG, etag);
		values.put(NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT, accountName);
		values.put(NotePad.GTasks.COLUMN_NAME_GTASKS_ID, id);
		values.put(NotePad.GTasks.COLUMN_NAME_UPDATED, updated);
		return values;
	}
}
