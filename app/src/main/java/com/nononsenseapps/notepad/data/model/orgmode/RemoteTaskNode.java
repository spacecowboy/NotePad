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

import com.nononsenseapps.notepad.data.model.sql.RemoteTask;

public class RemoteTaskNode {

	public static String getTitle(final RemoteTask remote) {
		return remote.field2;
	}

	public static String getBody(final RemoteTask remote) {
		return remote.field3;
	}

	public static String getDueTime(final RemoteTask remote) {
		return remote.field4;
	}

	public static String getTodo(final RemoteTask remote) {
		return remote.field5;
	}

	public static void setTitle(final RemoteTask remote, final String title) {
		remote.field2 = title;
	}

	public static void setBody(final RemoteTask remote, final String body) {
		remote.field3 = body;
	}

	public static void setDueTime(final RemoteTask remote, final String s) {
		remote.field4 = s;
	}

	public static void setTodo(final RemoteTask remote, final String s) {
		remote.field5 = s;
	}
}
