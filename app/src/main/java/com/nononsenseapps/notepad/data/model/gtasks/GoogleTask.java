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

package com.nononsenseapps.notepad.data.model.gtasks;

import android.content.ContentValues;
import android.database.Cursor;

import com.nononsenseapps.notepad.data.local.sql.LegacyDBHelper.NotePad;
import com.nononsenseapps.notepad.data.model.sql.RemoteTask;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.remote.gtasks.GoogleTasksAPI;
import com.nononsenseapps.notepad.util.RFC3339Date;

public class GoogleTask extends RemoteTask {

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

	public GoogleTask(final GoogleTasksAPI.TaskResource taskResource, final String accountName) {
		super();
		this.service = GoogleTaskList.SERVICENAME;
		account = accountName;

		updateFromTaskResource(taskResource);
	}

    /**
     * Fill in fields from taskresource
     */
    public void updateFromTaskResource(GoogleTasksAPI.TaskResource taskResource) {
        remoteId = taskResource.id;
        try {
            updated = RFC3339Date.parseRFC3339Date(taskResource.updated).getTime();
        }
        catch (Exception e) {
            updated = 0L;
        }
        //etag = jsonTask.getString("etag");

        if (taskResource.title != null)
            title = taskResource.title;
        if (taskResource.notes != null)
            notes = taskResource.notes;
        if (taskResource.status != null)
            status = taskResource.status;
        if (taskResource.parent != null)
            parent = taskResource.parent;
        else
            parent = null;
        if (taskResource.position != null)
            position = taskResource.position;
        if (taskResource.due != null)
            dueDate = taskResource.due;
        if (taskResource.deleted != null && taskResource.deleted)
            remotelydeleted = true;
        if (taskResource.hidden != null && taskResource.hidden)
            remotelydeleted = true;
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
	 * Return a taskresource version of this task. Does not include id.
	 */
	public GoogleTasksAPI.TaskResource toTaskResource() {
        GoogleTasksAPI.TaskResource result = new GoogleTasksAPI.TaskResource();

        result.title = title;
        result.notes = notes;
        result.due = dueDate;
        result.status = status;

		return result;
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
