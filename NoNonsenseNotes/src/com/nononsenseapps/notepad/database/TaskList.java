package com.nononsenseapps.notepad.database;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class TaskList extends DAO {

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "tasklist";
	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			TABLE_NAME);
	
	public static Uri getUri(final long id) {
		return Uri.withAppendedPath(URI, Long.toString(id));
	}
	
	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.list";
	
	public static final int BASEURICODE = 101;
	public static final int BASEITEMCODE = 102;
	
	/**
	 * TaskList URIs start at 101, up to 199
	 */
	public static void addMatcherUris(UriMatcher sURIMatcher) {
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#", BASEITEMCODE);
	}

	public static class Columns implements BaseColumns {

		private Columns() {
		}
		
		public static final String TITLE = "title";
		public static final String UPDATED = "updated";
		
		public static final String GTASKACCOUNT = "gtaskaccount";
		public static final String GTASKID = "gtaskid";
		
		// Future proofing
		public static final String DROPBOXACCOUNT = "dropboxaccount";
		public static final String DROPBOXID = "dropboxid";

		public static final String[] FIELDS = { _ID, TITLE, UPDATED, 
			GTASKACCOUNT, GTASKID,
			DROPBOXACCOUNT, DROPBOXID};
	}

	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ "(" + Columns._ID + " INTEGER PRIMARY KEY," + Columns.TITLE
			+ " TEXT NOT NULL DEFAULT '',"
			+ Columns.UPDATED + " INTEGER,"
			// GTask fields
			+ Columns.GTASKACCOUNT + " TEXT," 
			+ Columns.GTASKID + " TEXT," 
			// Dropbox fields
			+ Columns.DROPBOXACCOUNT + " TEXT," 
			+ Columns.DROPBOXID + " TEXT" 
			
			+ ")";

	public String title = "";
	
	// milliseconds since 1970-01-01 UTC
	public Long updated = null;
	
	// Sync stuff
	public String gtaskaccount = null;
	public String gtaskid = null;
	public String dropboxaccount = null;
	public String dropboxid = null;

	public TaskList() {
	}

	public TaskList(final Cursor c) {
		this._id = c.getLong(0);
		this.title = c.getString(1);
		this.updated = c.getLong(2);
		// sync stuff
		gtaskaccount = c.getString(3);
		gtaskid = c.getString(4);
		dropboxaccount = c.getString(5);
		dropboxid = c.getString(6);
	}
	
	public TaskList(final long id, final ContentValues values) {
		this(values);
		this._id = id;
	}
	
	public TaskList(final ContentValues values) {
		this.title = values.getAsString(Columns.TITLE);
		updated = values.getAsLong(Columns.UPDATED);
		gtaskaccount =values.getAsString(Columns.GTASKACCOUNT);
		gtaskid = values.getAsString(Columns.GTASKID);
		dropboxaccount =values.getAsString(Columns.DROPBOXACCOUNT);
		dropboxid = values.getAsString(Columns.DROPBOXID);
	}

	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		// Note that ID is NOT included here
		values.put(Columns.TITLE, title);
		
		values.put(Columns.UPDATED, updated);
		values.put(Columns.GTASKACCOUNT, gtaskaccount);
		values.put(Columns.GTASKID, gtaskid);
		values.put(Columns.DROPBOXACCOUNT, dropboxaccount);
		values.put(Columns.DROPBOXID, dropboxid);

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
