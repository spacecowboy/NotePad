package com.nononsenseapps.notepad.sync.googleapi;

import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.NotePad;

import android.content.ContentValues;
import android.util.Log;

public class GoogleTask {

	private static final String TAG = "GoogleTask";
	private static final String ID = "id";
	private static final String TITLE = "title";
	private static final String UPDATED = "updated";
	private static final String NOTES = "notes";
	private static final String STATUS = "status";
	private static final String DUE = "due";
	private static final String DELETED = "deleted";
	private static final String COMPLETED = "completed";
	private static final String NEEDSACTION = "needsAction";
	private static final String PARENT = "parent";
	private static final String POSITION = "position";
	private static final String HIDDEN = "hidden";
	public String id = null;
	public String etag = "";
	public String title = null;
	public String updated = null;
	public String notes = null;
	public String status = null;
	public String dueDate = null;
	public String parent = null;
	public String position = null;
	
	public int modified = 0;

	public long dbId = -1;
	public int deleted = 0;
	public int hidden = 0;
	public long listdbid = -1;
	public boolean didRemoteInsert = false;
	
	// These are local values and not synced
	public String abcsort = "";
	public String possort = "";
	
	public JSONObject json = null;

	public GoogleTask() {
	}

	public GoogleTask(JSONObject jsonTask) throws JSONException {
		id = jsonTask.getString(ID);
		title = jsonTask.getString(TITLE);
		updated = jsonTask.getString(UPDATED);
		//etag = jsonTask.getString("etag");
		if (jsonTask.has(NOTES))
			notes = jsonTask.getString(NOTES);
		status  = jsonTask.getString(STATUS);
		if (jsonTask.has(PARENT))
			parent  = jsonTask.getString(PARENT);
		position  = jsonTask.getString(POSITION);
		if (jsonTask.has(DUE))
			dueDate = jsonTask.getString(DUE);
		if (jsonTask.has(DELETED) && jsonTask.getBoolean(DELETED))
			deleted = 1;
		else
			deleted = 0;
		if (jsonTask.has(HIDDEN) && jsonTask.getBoolean(HIDDEN))
			hidden = 1;
		else
			hidden = 0;
		
		

		json = jsonTask;
	}

	/**
	 * Special tricks because google api actually want 'null' while JSONObject doesnt allow them.
	 * do not include read-only fields
	 * @return
	 */
	public String toJSON() {
		String returnString = "";
		try {
			JSONObject json = new JSONObject();
			String nullAppendage = "";
//			if (id != null)
//				json.put(ID, id);

			json.put(TITLE, title);
			json.put(NOTES, notes);
			if (dueDate != null && !dueDate.equals(""))
				json.put(DUE, dueDate);
			else
				nullAppendage += ", \"" + DUE + "\": null";

			json.put(STATUS, status);
			if (status != null && status.equals(NEEDSACTION)) {
				// We must reset this also in this case
				nullAppendage += ", \"" + COMPLETED + "\": null";
			}
			
			nullAppendage += "}";
			
			String jsonString = json.toString();
			returnString = jsonString.substring(0, jsonString.length()-1) + nullAppendage;

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
	public ContentValues toNotesContentValues(int modified, long listDbId) {
		ContentValues values = new ContentValues();
		if (title != null)
			values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
		if (dueDate != null)
			values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, dueDate);
		if (status != null)
			values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS, status);
		if (notes != null)
			values.put(NotePad.Notes.COLUMN_NAME_NOTE, notes);
		
		if (dbId > -1)
			values.put(NotePad.Notes._ID, dbId);

		values.put(NotePad.Notes.COLUMN_NAME_LIST, listDbId);
		values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, modified);
		values.put(NotePad.Notes.COLUMN_NAME_DELETED, deleted);
		values.put(NotePad.Notes.COLUMN_NAME_POSITION, position);
		values.put(NotePad.Notes.COLUMN_NAME_PARENT, parent);
		values.put(NotePad.Notes.COLUMN_NAME_HIDDEN, hidden);
		
		values.put(NotePad.Notes.COLUMN_NAME_POSSUBSORT, possort);
		values.put(NotePad.Notes.COLUMN_NAME_ABCSUBSORT, abcsort);
		
		return values;
	}
	
	public ContentValues toNotesBackRefContentValues(int listIdIndex) {
		ContentValues values = new ContentValues();
		values.put(NotePad.Notes.COLUMN_NAME_LIST, listIdIndex);
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
	
	public ContentValues toGTasksBackRefContentValues(int pos) {
		ContentValues values = new ContentValues();
		values.put(NotePad.GTasks.COLUMN_NAME_DB_ID, pos);
		return values;
	}
	
	/**
	 * Returns true if the task has the same remote id or same database id.
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		if (GoogleTask.class.isInstance(o)) {
			// It's a list!
			GoogleTask task = (GoogleTask) o;
			if (dbId != -1 && dbId == task.dbId) {
				equal = true;
			}
			if (id != null && id.equals(task.id)) {
				equal = true;
			}
		}
		return equal;
	}

	
}
