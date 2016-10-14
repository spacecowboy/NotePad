package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.data.local.sql.DatabaseHandler;
import com.nononsenseapps.notepad.data.local.sql.LegacyDBHelper;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class DBFreshTest extends AndroidTestCase {
	
	static final String PREFIX = "fresh_test_";

	private Context context;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		context = getContext();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	@SmallTest
	public void testFreshInstall() {
		context.deleteDatabase(PREFIX + LegacyDBHelper.LEGACY_DATABASE_NAME);
		context.deleteDatabase(PREFIX + DatabaseHandler.DATABASE_NAME);
		final SQLiteDatabase db = new DatabaseHandler(context, PREFIX).getReadableDatabase();
		// Just open the database, there should be one list and one task present
		Cursor tlc = db.query(TaskList.TABLE_NAME, TaskList.Columns.FIELDS, null, null, null, null, null);
		
		assertEquals("Should be ONE list present on fresh installs", 1, tlc.getCount());
		tlc.close();
		
		Cursor tc = db.query(Task.TABLE_NAME, Task.Columns.FIELDS, null, null, null, null, null);
		assertEquals("Should be NO task present on fresh installs", 0, tc.getCount());
		tc.close();
		
		db.close();
		assertTrue("Could not delete database", context.deleteDatabase(PREFIX + LegacyDBHelper.LEGACY_DATABASE_NAME));
		assertTrue("Could not delete database", context.deleteDatabase(PREFIX + DatabaseHandler.DATABASE_NAME));
	}
}
