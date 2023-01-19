package com.nononsenseapps.notepad.test;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.nononsenseapps.notepad.database.DatabaseHandler;
import com.nononsenseapps.notepad.database.LegacyDBHelper;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import junit.framework.TestCase;

public class DBFreshTest extends TestCase {

	static final String PREFIX = "fresh_test_";

	private Context context;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
		Cursor tlc = db.query(TaskList.TABLE_NAME, TaskList.Columns.FIELDS,
				null, null, null, null, null);

		assertEquals("Should be ONE list present on fresh installs",
				1, tlc.getCount());
		tlc.close();

		Cursor tc = db.query(Task.TABLE_NAME, Task.Columns.FIELDS,
				null, null, null, null, null);
		assertEquals("Should be 1 task present on fresh installs, the 'welcome' task",
				1, tc.getCount());
		tc.close();

		db.close();
		assertTrue("Could not delete database",
				context.deleteDatabase(PREFIX + LegacyDBHelper.LEGACY_DATABASE_NAME));
		assertTrue("Could not delete database",
				context.deleteDatabase(PREFIX + DatabaseHandler.DATABASE_NAME));
	}
}
