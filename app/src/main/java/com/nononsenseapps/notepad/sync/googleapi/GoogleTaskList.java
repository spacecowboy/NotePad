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

import com.nononsenseapps.notepad.database.RemoteTaskList;
import com.nononsenseapps.notepad.database.TaskList;

public class GoogleTaskList extends RemoteTaskList {

	public static final String SERVICENAME = "googletasks";
	public String title = null;

	public GoogleTaskList(final TaskList dbList, final String accountName) {
		super();
		this.title = dbList.title;
		this.dbid = dbList._id;
		this.account = accountName;
		this.service = SERVICENAME;
	}

	public GoogleTaskList(final Long dbid, final String remoteId, final Long updated, final String account) {
		super(dbid, remoteId, updated, account);
		this.service = SERVICENAME;
	}

	/**
	 * Returns true if the TaskList has the same remote id or the same database
	 * id.
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		if (o instanceof GoogleTaskList list) {
			// It's a list!
			if (dbid != -1 && dbid.equals(list.dbid)) {
				equal = true;
			}
			if (remoteId != null && remoteId.equals(list.remoteId)) {
				equal = true;
			}
		}
		return equal;
	}
}
