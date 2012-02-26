package com.nononsenseapps.notepad.widget;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotePadProvider;
import com.nononsenseapps.notepad.NotePadProvider.DatabaseHelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * This is a limited version of the real provider. Widgets do NOT have read
 * permission, hence the code duplication... To not get a SecurityException on a
 * Widget, you must have exported=true and not require any read permissions.
 * 
 * It only supports query, and only query for visible notes.
 * 
 * @author Jonas
 * 
 */
public class ListDBProvider extends ContentProvider {

	public static final Uri CONTENT_VISIBLE_URI = Uri
			.parse("content://com.nononsenseapps.NotePad.widget/visiblenotes");

	private Context mContext;

	private DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = new NotePadProvider.DatabaseHelper(getContext());
		return true;
	}

	@Override
	public int delete(Uri fakeUri, String where, String[] selectionArgs) {
		// Not supported
		return 0;
	}

	@Override
	public String getType(Uri widgetUri) {
		return mContext.getContentResolver().getType(
				NotePad.Notes.CONTENT_VISIBLE_URI);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// Not supported
		return null;
	}

	@Override
	public Cursor query(Uri widgetUri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Constructs a new query builder and sets its table name
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String orderBy = null;

		// Add a selection criteria, but then fall through for normal note
		// handling.
		qb.appendWhere(NotePad.Notes.COLUMN_NAME_HIDDEN + " IS 0 AND ");
		qb.appendWhere(NotePad.Notes.COLUMN_NAME_LOCALHIDDEN + " IS 0 AND ");
		qb.appendWhere(NotePad.Notes.COLUMN_NAME_DELETED + " IS 0");

		qb.setTables(NotePad.Notes.TABLE_NAME);
		qb.setProjectionMap(NotePadProvider.sNotesProjectionMap);
		if (selectionArgs != null
				&& (selection == null || selection.equals(""))) {
			selection = NotePad.Notes.COLUMN_NAME_NOTE + " MATCH ?";
		}

		// If no sort order is specified, uses the default
		if (TextUtils.isEmpty(sortOrder)) {
			// hanlded in switch
		} else {
			// otherwise, uses the incoming sort order
			orderBy = sortOrder;
		}

		// Opens the database object in "read" mode, since no writes need to be
		// done.
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		/*
		 * Performs the query. If no problems occur trying to read the database,
		 * then a Cursor object is returned; otherwise, the cursor variable
		 * contains null. If no records were selected, then the Cursor object is
		 * empty, and Cursor.getCount() returns 0.
		 */
		Cursor c;

		c = qb.query(db, // The database to query
				projection, // The columns to return from the query
				selection, // The columns for the where clause
				selectionArgs, // The values for the where clause
				null, // don't group the rows
				null, // don't filter by row groups
				orderBy // The sort order
		);

		// Tells the Cursor what URI to watch, so it knows when its source data
		// changes
		if (c != null)
			c.setNotificationUri(getContext().getContentResolver(),
					NotePad.Notes.CONTENT_URI);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// Not supported
		return 0;
	}

}
