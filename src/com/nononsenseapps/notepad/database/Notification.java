package com.nononsenseapps.notepad.database;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class Notification extends DAO {

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "notification";
	public static final String JOINED_TASK_PATH = TABLE_NAME + "/joined_task";
	
	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps." + TABLE_NAME;

	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			TABLE_NAME);
	public static final int BASEURICODE = 301;
	public static final int BASEITEMCODE = 302;
	public static final int JOINEDTASKQUERY = 303;

	public static void addMatcherUris(UriMatcher sURIMatcher) {
		sURIMatcher
				.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#",
				BASEITEMCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, JOINED_TASK_PATH,
				JOINEDTASKQUERY);
	}

	public static class Columns implements BaseColumns {

		private Columns() {
		}

		public static final String TIME = "time";
		public static final String PERMANENT = "permanent";
		public static final String TASKID = "taskid";
		public static final String LIST_TITLE = TaskList.TABLE_NAME + "."
				+ TaskList.Columns.TITLE;

		public static final String[] FIELDS = { _ID, TIME, PERMANENT, TASKID };
		public static final String[] JOINED_FIELDS = { _ID, TIME, PERMANENT,
				TASKID, LIST_TITLE };
	}

	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ "(" + Columns._ID + " INTEGER PRIMARY KEY," + Columns.TIME
			+ " INTEGER," + Columns.PERMANENT
			+ " INTEGER NOT NULL DEFAULT 0," + Columns.TASKID + " INTEGER,"

			// Foreign key for task
			+ "FOREIGN KEY(" + Columns.TASKID + ") REFERENCES "
			+ Task.TABLE_NAME + "(" + Task.Columns._ID + ") ON DELETE CASCADE"

			+ ")";
	
	// milliseconds since 1970-01-01 UTC
	public Long time = null;
	public Long permanent = null;
	public Long taskID = null;
	
	// Read only
	public String listTitle = null;
	
	/**
	 * Must be associated with a task
	 */
	public Notification(final long taskID) {
		this.taskID = taskID;
	}
	
	public Notification(final Cursor c) {
		_id = c.getLong(0);
		time = c.getLong(1);
		permanent = c.getLong(2);
		taskID = c.getLong(3);
		if (c.getColumnCount() > 4) {
			listTitle = c.getString(4);
		}
	}
	
	public Notification(final long id, final ContentValues values) {
		this(values);
		_id = id;
	}
	
	public Notification(final ContentValues values) {
		time =values.getAsLong(Columns.TIME);
		permanent = values.getAsLong(Columns.PERMANENT);
		taskID =values.getAsLong(Columns.TASKID);
	}

	@Override
	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		
		values.put(Columns.TIME, time);
		values.put(Columns.TASKID, taskID);
		if (permanent != null)
			values.put(Columns.PERMANENT, permanent);
		
		return values;
		
	}

	@Override
	protected String getTableName() {
		return TABLE_NAME;
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

}
