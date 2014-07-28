package com.nononsenseapps.notepad.sync.orgsync;

import com.nononsenseapps.notepad.database.RemoteTask;

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
