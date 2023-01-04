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

import com.nononsenseapps.helpers.RFC3339Date;
import com.nononsenseapps.notepad.database.RemoteTask;
import com.nononsenseapps.notepad.database.Task;

public class GoogleTask extends RemoteTask {

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

	// all of these should be changed to methods like getTitle() { return this.title; }
	// and setTitle(String new) { this.title = new; } but as of now google task is
	// not even used by the app...
	public String title = null;
	public String notes = null;
	public String status = null;
	public String dueDate = null;
	public String parent = null;
	public String position = null;

	public boolean remotelydeleted = false;

	public final String possort = "";

	public GoogleTask(final Task dbTask, final String accountName) {
		super();
		this.service = GoogleTaskList.SERVICENAME;
		account = accountName;
		if (dbTask != null)
			fillFrom(dbTask);
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
	 * Returns true if the task has the same remote id or same database id.
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		if (o instanceof GoogleTask) {
			// It's a list!
			GoogleTask task = (GoogleTask) o;
			if (dbid != -1 && dbid.equals(task.dbid)) {
				equal = true;
			}
			if (remoteId != null && remoteId.equals(task.remoteId)) {
				equal = true;
			}
		}
		return equal;
	}
}
