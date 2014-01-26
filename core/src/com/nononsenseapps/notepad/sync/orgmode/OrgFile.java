package com.nononsenseapps.notepad.sync.orgmode;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.RemoteTaskList;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

public class OrgFile extends RemoteTaskList {
	// Org files end with .org
	public static final String ORG_EXTENSION = ".org";
	public static final String ORGMODE = "orgmode";

	public static final String ASTASKS = "astasks";

	private TaskList taskList = null;

	static Random rnd = null;

	static Random getRandom() {
		if (rnd == null) {
			rnd = new Random();
		}
		return rnd;
	}

	public OrgFile() {
		super();
		this.service = ORGMODE;
		this.account = ORGMODE;
	}

	public OrgFile(final Cursor c) {
		super(c);
		this.service = ORGMODE;
		this.account = ORGMODE;
	}

	public void setTaskList(final TaskList taskList) {
		this.taskList = taskList;
	}

	/**
	 * Return the tasklist associated with this object
	 */
	public TaskList getTaskList(final Context context) {
		if (isDeleted()) {
			Log.d(OrgSyncer.TAG, "List is deleted, returning null");
			return null;
		}

		if (taskList == null) {
			Log.d(OrgSyncer.TAG, "Fetching tasklist from database: " + dbid);
			fetchTaskListFromDB(context);
		}
		if (taskList == null)
			throw new NullPointerException("tasklist was null");
		return taskList;
	}

	private void fetchTaskListFromDB(final Context context) {
		final Cursor c = context.getContentResolver().query(
				TaskList.getUri(dbid), TaskList.Columns.FIELDS, null, null,
				null);

		if (c != null) {
			if (c.moveToFirst()) {
				Log.d(OrgSyncer.TAG, "Building tasklist object");
				this.taskList = new TaskList(c);
			}
			c.close();
		}
	}

	/**
	 * Returns the filename this object expects to write to. Typically the title
	 * of the list.
	 */
	public String getFileName(final Context context) {
		if (remoteId == null) {
			remoteId = getTaskList(context).title + ORG_EXTENSION;
		}
		Log.d(OrgSyncer.TAG, "Expected filename: " + remoteId);

		return remoteId;
	}

	public String getUniqueFileName(final Context context) {
		final Random rnd = getRandom();
		final String filename = getTaskList(context).title + "_"
				+ Integer.toString(rnd.nextInt(1000)) + ORG_EXTENSION;
		remoteId = filename;
		Log.d(OrgSyncer.TAG, "Unique filename: " + remoteId);
		return filename;
	}

	/**
	 * Returns the list this object represents and all its tasks formatted in
	 * orgmode.
	 */
	public String getOrgString(final Context context) {
		final OrgParser orgParser = new OrgParser();
		final StringBuilder sb = new StringBuilder();
		final String spacer = "\n\n";
		// Append bits with an empty line inbetween
		Log.d(OrgSyncer.TAG, "Getting tasklist from database");
		final TaskList taskList = getTaskList(context);
		if (taskList == null)
			throw new NullPointerException("tasklist was null");
		Log.d(OrgSyncer.TAG, "Appending tasklist as string");
		sb.append(orgParser.toOrgMode(taskList));

		// Append every task
		Log.d(OrgSyncer.TAG, "Handling every task");
		for (final OrgEntry orgEntry : getOrgEntries(context, taskList)) {
			// Then add to string
			sb.append(spacer).append(
					orgParser.toOrgMode(orgEntry,
							ASTASKS.equals(taskList.listtype),
							getNotificationsFor(context, orgEntry.getTask())));
		}

		return sb.toString();
	}

	/**
	 * Returns notifications associated with task
	 */
	private List<Notification> getNotificationsFor(final Context context,
			Task task) {
		final ArrayList<Notification> reminders = new ArrayList<Notification>();

		final Cursor c = context.getContentResolver().query(Notification.URI,
				Notification.Columns.FIELDS,
				Notification.Columns.TASKID + " IS ?",
				new String[] { Long.toString(task._id) },
				Notification.Columns.TIME);

		while (c != null && c.moveToNext()) {
			reminders.add(new Notification(c));
		}

		if (c != null)
			c.close();

		return reminders;
	}

	/**
	 * Returns a list of orgentries. Tasks which do not have entries yet get
	 * them created here.
	 */
	private List<OrgEntry> getOrgEntries(final Context context,
			final TaskList taskList) {
		final ArrayList<OrgEntry> entries = new ArrayList<OrgEntry>();

		final Cursor c = context.getContentResolver()
				.query(Task.URI, Task.Columns.FIELDS,
						Task.Columns.DBLIST + " IS ?",
						new String[] { Long.toString(taskList._id) },
						Task.Columns.LEFT);
		try {
			while (c.moveToNext()) {
				final Task task = new Task(c);
				final OrgEntry orgEntry = OrgEntry.fetchFromTask(context, task);
				orgEntry.setTask(task);
				entries.add(orgEntry);
			}
		} finally {
			if (c != null)
				c.close();
		}

		return entries;
	}

	public boolean isNewFile() {
		return (_id < 1);
	}

	public static String[] getTaskListWithoutOrgArgs() {
		return new String[] { ORGMODE, ORGMODE };
	}

	public static String getOnlyUpdatedClause() {
		return new StringBuilder("EXISTS ( SELECT ")
				.append(TaskList.Columns.UPDATED).append(" FROM ")
				.append(TaskList.TABLE_NAME).append(" WHERE ")
				.append(OrgFile.TABLE_NAME).append(".")
				.append(OrgFile.Columns.UPDATED).append(" < ")
				.append(TaskList.TABLE_NAME).append(".")
				.append(TaskList.Columns.UPDATED).append(")").toString();
	}

	public static String[] getOnlyUpdatedArgs() {
		return null;
	}

	public static String fetchOrgFileForTaskListClause() {
		return OrgEntry.Columns.DBID + " IS ? AND " + OrgEntry.Columns.SERVICE
				+ " IS ? AND " + OrgEntry.Columns.ACCOUNT + " IS ?";
	}

	public static String[] fetchOrgFileForTaskListArgs(final TaskList taskList) {
		return new String[] { Long.toString(taskList._id), ORGMODE,
				ORGMODE };
	}

	/**
	 * Convenience method for normal operations. Updates "updated" field to
	 * specified Returns number of db-rows affected. Fail if < 1
	 */
	public int save(final Context context, final long updated) {
		int result = 0;
		this.updated = updated;
		if (_id < 1) {
			final Uri uri = context.getContentResolver().insert(getBaseUri(),
					getContent());
			if (uri != null) {
				_id = Long.parseLong(uri.getLastPathSegment());
				result++;
			}
		} else {
			result += context.getContentResolver().update(getUri(),
					getContent(), null, null);
		}
		return result;
	}

	/**
	 * Convenience method for normal operations. Updates "updated" field.
	 * Returns number of db-rows affected. Fail if < 1
	 */
	@Override
	public int save(final Context context) {
		return save(context, Calendar.getInstance().getTimeInMillis());
	}
}
