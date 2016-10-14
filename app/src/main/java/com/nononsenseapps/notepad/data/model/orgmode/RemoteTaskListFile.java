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

package com.nononsenseapps.notepad.data.model.orgmode;

import com.nononsenseapps.notepad.data.model.sql.RemoteTaskList;

public class RemoteTaskListFile {

	public static String getSorting(final RemoteTaskList remote) {
		return remote.field2;
	}

	public static String getListType(final RemoteTaskList remote) {
		return remote.field3;
	}

	public static void setSorting(final RemoteTaskList remote, final String s) {
		remote.field2 = s;
	}

	public static void setListType(final RemoteTaskList remote, final String s) {
		remote.field3 = s;
	}
}
