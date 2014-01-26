package com.nononsenseapps.notepad.sync.orgmode;

import java.util.Calendar;
import java.util.Random;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.nononsenseapps.notepad.database.RemoteTask;
import com.nononsenseapps.notepad.database.Task;

public class OrgEntry extends RemoteTask {
	public static final String ORGMODE = "orgmode";

	private Task task = null;
	
	private static Random rnd = null;
	private static Random getRandom() {
		if (rnd == null) {
			rnd = new Random();
		}
		return rnd;
	}

	public OrgEntry() {
		super();
		this.service = ORGMODE;
		this.account = ORGMODE;
	}

	public OrgEntry(final Cursor c) {
		super(c);
		this.service = ORGMODE;
		this.account = ORGMODE;
	}

	public Task getTask() {
		return task;
	}
	public void setTask(final Task task) {
		this.task = task;
	}

	public void fetchTask(final Context context) {
		final Cursor c = context.getContentResolver().query(Task.URI,
				Task.Columns.FIELDS, getTaskWithRemoteClause(),
				getTaskWithRemoteArgs(), null);

		if (c != null) {
			if (c.moveToFirst()) {
				this.task = new Task(c);
			}
			c.close();
		}
	}

	/**
	 * Fetches an orgEntry corresponding to the specified task. If none existed,
	 * a new one is created.
	 */
	public static OrgEntry fetchFromTask(final Context context, final Task task) {
		OrgEntry result = null;
		final Cursor c = context.getContentResolver().query(
				OrgEntry.URI,
				OrgEntry.Columns.FIELDS,
				OrgEntry.Columns.DBID + " IS ? AND " + OrgEntry.Columns.SERVICE
						+ " IS ?",
				new String[] { Long.toString(task._id), ORGMODE }, null);

		if (c != null) {
			if (c.moveToFirst()) {
				result = new OrgEntry(c);
			}
			c.close();
		}
		
		if (result == null) {
			// Create a new one
			result = new OrgEntry();
			result.remoteId = generateId();
			result.dbid = task._id;
			result.listdbid = task.dblist;
			result.save(context);
		}

		return result;
	}
	
	/**
	 * Return a random 8 char hexadecimal string
	 */
	private static String generateId() {
		final Random r = getRandom();
        final StringBuffer sb = new StringBuffer();
        final int numchars = 8;
        while(sb.length() < numchars){
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars);
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
		}
		else {
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
