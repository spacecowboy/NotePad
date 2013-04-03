package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.database.DatabaseHandler;
import com.nononsenseapps.notepad.database.LegacyDBHelper;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class DBProviderTest extends AndroidTestCase {

	private Context context;
	private ContentResolver resolver;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		context = getContext();
		resolver = context.getContentResolver();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	private void assertUriReturnsResult(final Uri uri, final String[] fields) {
		assertUriReturnsResult(uri, fields, null, null);
	}

	private void assertUriReturnsResult(final Uri uri, final String[] fields,
			final String where, final String[] whereArgs) {
		final Cursor c = resolver.query(uri, fields, where, whereArgs, null);
		final boolean notEmpty = c.moveToFirst();
		c.close();
		assertTrue("Uri did not return a result: " + uri.getEncodedPath(),
				notEmpty);
	}

	private TaskList getList() {
		final Cursor c = resolver.query(TaskList.URI, TaskList.Columns.FIELDS,
				null, null, null);
		c.moveToFirst();
		final TaskList result = new TaskList(c);
		c.close();
		return result;
	}

	@SmallTest
	public void testTaskListURIs() {
		assertUriReturnsResult(TaskList.URI, TaskList.Columns.FIELDS);
		// TODO legacy
	}

	@SmallTest
	public void testTaskURIs() {
		assertUriReturnsResult(Task.URI, Task.Columns.FIELDS);
		final TaskList list = getList();
		assertUriReturnsResult(Task.URI_INDENTED_QUERY, Task.Columns.FIELDS,
				Task.Columns.DBLIST + " IS ?",
				new String[] { Long.toString(list._id) });
		// TODO remember legacy uris
		// TODO need a projection mapper
		// assertUriReturnsResult(LegacyDBHelper.NotePad.Notes.CONTENT_URI,
		// new String[] { LegacyDBHelper.NotePad.Notes.COLUMN_NAME_TITLE });
		// assertUriReturnsResult(
		// LegacyDBHelper.NotePad.Notes.CONTENT_VISIBLE_URI,
		// new String[] { LegacyDBHelper.NotePad.Notes.COLUMN_NAME_TITLE });
	}

	@SmallTest
	public void testTaskDeletedURIs() {
		// Manually insert an object first
		final SQLiteDatabase db = DatabaseHandler.getInstance(mContext)
				.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(Task.Columns.TITLE, "title");
		long id = db.insert(Task.DELETE_TABLE_NAME, null, values);
		resolver.notifyChange(Task.URI_DELETED_QUERY, null, false);

		// TODO
		assertUriReturnsResult(Task.URI_DELETED_QUERY,
				Task.Columns.DELETEFIELDS);

		// Remove item again
		db.delete(Task.DELETE_TABLE_NAME, Task.Columns._ID + " IS ?",
				new String[] { Long.toString(id) });
	}
}
