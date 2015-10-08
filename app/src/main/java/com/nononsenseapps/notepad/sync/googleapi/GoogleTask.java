/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.sync.googleapi;

import java.text.ParseException;
import java.util.Comparator;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.database.LegacyDBHelper.NotePad;
import com.nononsenseapps.notepad.database.RemoteTask;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.util.BiMap;
import com.nononsenseapps.utils.time.RFC3339Date;

import android.content.ContentValues;
import android.database.Cursor;

import com.nononsenseapps.helpers.Log;

public class GoogleTask extends RemoteTask {

	/*
	public static class RemoteOrder implements Comparator<GoogleTask> {

		final HashMap<String, Integer> levels;

		public RemoteOrder(final HashMap<String, Integer> levels) {
			this.levels = levels;
		}

		@Override
		public int compare(GoogleTask lhs, GoogleTask rhs) {
			Log.d("remotesort", "Comparing: " + lhs.title + " and " + rhs.title);
			final Integer leftLevel = levels.get(lhs.id);
			final Integer rightLevel = levels.get(rhs.id);
			
			Log.d("remotesort", "lhs level: " + leftLevel + ", rhs level: " + rightLevel);
			
			if (leftLevel == null || rightLevel == null)
				return 0;

			if (leftLevel == rightLevel) {
				// Share parents, compare their positions
				return lhs.position.compareTo(rhs.position);
			} else if (leftLevel < rightLevel) {
				return -1;
			} else {
				return 1;
			}
		}
	}*/

	private static final String TAG = "nononsenseapps GoogleTask";
	public static final String ID = "id";
	public static final String TITLE = "title";
	public static final String UPDATED = "updated";
	public static final String NOTES = "notes";
	public static final String STATUS = "status";
	public static final String DUE = "due";
	public static final String DELETED = "deleted";
	public static final String COMPLETED = "completed";
	public static final String NEEDSACTION = "needsAction";
	public static final String PARENT = "parent";
	public static final String POSITION = "position";
	public static final String HIDDEN = "hidden";
	public String title = null;
	public String notes = null;
	public String status = null;
	public String dueDate = null;
	public String parent = null;
	public String position = null;

	public boolean remotelydeleted = false;
	//public int hidden = 0;
	//public boolean didRemoteInsert = false;

	public String possort = "";
	//public int indentLevel = 0;

//	public JSONObject json = null;
	//public boolean conflict = false;

	public GoogleTask(final String accountName) {
		super();
		account = accountName;
		this.service = GoogleTaskList.SERVICENAME;
	}

	public GoogleTask(final JSONObject jsonTask, final String accountName) throws JSONException {
		super();
		this.service = GoogleTaskList.SERVICENAME;
		account = accountName;
		remoteId = jsonTask.getString(ID);
		try {
			updated = RFC3339Date.parseRFC3339Date(jsonTask.getString(UPDATED)).getTime();
		}
		catch (Exception e) {
			updated = 0L;
		}
		//etag = jsonTask.getString("etag");
		
		if (jsonTask.has(TITLE))
			title = jsonTask.getString(TITLE);
		if (jsonTask.has(NOTES))
			notes = jsonTask.getString(NOTES);
		if (jsonTask.has(STATUS))
			status = jsonTask.getString(STATUS);
		if (jsonTask.has(PARENT))
			parent = jsonTask.getString(PARENT);
		else
			parent = null;
		if (jsonTask.has(POSITION))
			position = jsonTask.getString(POSITION);
		if (jsonTask.has(DUE))
			dueDate = jsonTask.getString(DUE);
		if (jsonTask.has(DELETED) && jsonTask.getBoolean(DELETED))
			remotelydeleted = true;
		if (jsonTask.has(HIDDEN) && jsonTask.getBoolean(HIDDEN))
			remotelydeleted = true;

//		json = jsonTask;
	}

	public GoogleTask(final Task dbTask, final String accountName) {
		super();
		this.service = GoogleTaskList.SERVICENAME;
		account = accountName;
		if (dbTask != null)
			fillFrom(dbTask);
	}
	
	public GoogleTask(final Cursor c) {
		super(c);
		this.service = GoogleTaskList.SERVICENAME;
	}

	public void fillFrom(final Task dbTask) {
		title = dbTask.title;
		notes = dbTask.note;
		dueDate = RFC3339Date.asRFC3339ZuluDate(dbTask.due);
		status = dbTask.completed != null ? GoogleTask.COMPLETED
				: GoogleTask.NEEDSACTION;
		remotelydeleted = false;
		deleted = null;
		dbid = dbTask._id;
		listdbid = dbTask.dblist;
	}

	/**
	 * Special tricks because google api actually want 'null' while JSONObject
	 * doesnt allow them. do not include read-only fields
	 * 
	 * @return
	 */
	public String toJSON() {
		String returnString = "";
		try {
			JSONObject json = new JSONObject();
			String nullAppendage = "";
			// if (id != null)
			// json.put(ID, id);

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
			returnString = jsonString.substring(0, jsonString.length() - 1)
					+ nullAppendage;

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

		if (dbid > -1)
			values.put(NotePad.Notes._ID, dbid);

		values.put(NotePad.Notes.COLUMN_NAME_LIST, listDbId);
		values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, modified);
		values.put(NotePad.Notes.COLUMN_NAME_DELETED, deleted);
		values.put(NotePad.Notes.COLUMN_NAME_POSITION, position);
		values.put(NotePad.Notes.COLUMN_NAME_PARENT, parent);
		//values.put(NotePad.Notes.COLUMN_NAME_HIDDEN, hidden);

		values.put(NotePad.Notes.COLUMN_NAME_POSSUBSORT, possort);
		//values.put(NotePad.Notes.COLUMN_NAME_INDENTLEVEL, indentLevel);

		return values;
	}

	/**
	 * The parentIndex and previousIndex can be set to valid backreference
	 * indices to indicate the id of the parent and previous of this note. If
	 * set to null, already set values will be used which might be null.
	 */
	public ContentValues toNotesBackRefContentValues(Integer listIdIndex) {
		ContentValues values = new ContentValues();
		if (listIdIndex != null)
			values.put(NotePad.Notes.COLUMN_NAME_LIST, listIdIndex);

		return values;
	}

	public ContentValues toGTasksContentValues(String accountName) {
		ContentValues values = new ContentValues();
		values.put(NotePad.GTasks.COLUMN_NAME_DB_ID, dbid);
//		if (title.contains("debug"))
//			Log.d(TAG, title + " saving id: " + id);
//		values.put(NotePad.GTasks.COLUMN_NAME_ETAG, etag);
//		values.put(NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT, accountName);
//		values.put(NotePad.GTasks.COLUMN_NAME_GTASKS_ID, id);
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
			if (dbid != -1 && dbid == task.dbid) {
				equal = true;
			}
			if (remoteId != null && remoteId.equals(task.remoteId)) {
				equal = true;
			}
		}
		return equal;
	}
}
