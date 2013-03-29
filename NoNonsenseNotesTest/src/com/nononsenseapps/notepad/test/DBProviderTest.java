package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.database.LegacyDBHelper;
import com.nononsenseapps.notepad.database.Task;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class DBProviderTest  extends AndroidTestCase {

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
		final Cursor c = resolver.query(uri, fields, null, null, null);
		final boolean notEmpty = c.moveToFirst();
		c.close();
		assertTrue("Uri did not return a result: " + uri.getEncodedPath(), notEmpty);
	}
	
	@SmallTest
	public void testTaskURIs() {
		assertUriReturnsResult(Task.URI, Task.Columns.FIELDS);
		assertUriReturnsResult(Task.URI_DELETED_QUERY, Task.Columns.DELETEFIELDS);
		assertUriReturnsResult(Task.URI_INDENTED_QUERY, Task.Columns.FIELDS);
		// TODO remember legacy uris
		// TODO need a projection mapper
		assertUriReturnsResult(LegacyDBHelper.NotePad.Notes.CONTENT_URI, new String[] {LegacyDBHelper.NotePad.Notes.COLUMN_NAME_TITLE});
		assertUriReturnsResult(LegacyDBHelper.NotePad.Notes.CONTENT_VISIBLE_URI, new String[] {LegacyDBHelper.NotePad.Notes.COLUMN_NAME_TITLE});
	}
}
