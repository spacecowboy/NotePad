package com.nononsenseapps.notepad.sync.orgsync;

import com.nononsenseapps.notepad.database.RemoteTaskList;

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
