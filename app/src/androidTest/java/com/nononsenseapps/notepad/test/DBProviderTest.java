package com.nononsenseapps.notepad.test;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.database.DatabaseHandler;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DBProviderTest extends TestCase {

	private Context mContext;
	private ContentResolver mResolver;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		mResolver = mContext.getContentResolver();

		// clear app data
		PreferenceManager.getDefaultSharedPreferences(mContext).edit().clear().commit();
		DatabaseHandler.resetDatabase(mContext);
	}

	private void assertUriReturnsResult(final Uri uri, final String[] fields) {
		assertUriReturnsResult(uri, fields, null, null, -1);
	}

	private void assertUriReturnsResult(final Uri uri, final String[] fields, final String where,
										final String[] whereArgs, final int count) {
		final Cursor c = mResolver.query(uri, fields, where, whereArgs, null);
		NnnLogger.warning(DBProviderTest.class,
				"'parameter' count = " + count + ", cursorCount = " + c.getCount());
		if (count != c.getCount()) {
			// will crash. Let's get more info
			try {
				NnnLogger.debug(DBProviderTest.class, "cursor info:");
				c.moveToFirst();
				while (c.moveToNext()) {

					NnnLogger.debug(DBProviderTest.class, c);
					NnnLogger.debug(DBProviderTest.class, c.getCount());
					NnnLogger.debug(DBProviderTest.class, c.getColumnNames());
					NnnLogger.debug(DBProviderTest.class, c.getLong(0));
				}
			} catch (Exception e) {
				NnnLogger.exception(e);
			}
		}
		final int cursorCount = c.getCount();
		c.close();
		if (count < 0) {
			assertTrue("Uri did not return a result: " + uri.getEncodedPath(),
					cursorCount > 0);
		} else {
			// I don't know, sometimes it happens...
			assertEquals("Uri did not return expected number of results!",
					count, cursorCount);
		}
	}

	private TaskList getNewList() {
		TaskList result = new TaskList();
		result.title = "111aaTestingList";
		result.save(mContext);
		return result;
	}

	private ArrayList<Task> insertSomeTasks(final TaskList list, final int count) {
		ArrayList<Task> tasks = new ArrayList<Task>();
		for (int i = 0; i < count; i++) {
			Task t = new Task();
			t.title = "testTask" + i;
			t.note = "testNote" + i;
			t.due = Calendar.getInstance().getTimeInMillis();
			t.dblist = list._id;
			t.save(mContext);
			tasks.add(t);
		}
		return tasks;
	}

	@MediumTest
	public void testTaskListURIs() {
		final TaskList list = getNewList();
		assertUriReturnsResult(TaskList.URI, TaskList.Columns.FIELDS);
		assertUriReturnsResult(TaskList.URI_WITH_COUNT, TaskList.Columns.FIELDS);
		list.delete(mContext);
	}

	@MediumTest
	public void testTaskURIs() {
		final TaskList list = getNewList();
		final int taskCount = 5;
		final List<Task> tasks = insertSomeTasks(list, taskCount);

		assertUriReturnsResult(Task.URI, Task.Columns.FIELDS);

		// maybe the next line call to assertUriReturnsResult() fails due to timing issues ?
		SystemClock.sleep(500);

		// Sectioned Date query
		assertUriReturnsResult(Task.URI_SECTIONED_BY_DATE, Task.Columns.FIELDS,
				Task.Columns.DBLIST + " IS ?",
				new String[] { Long.toString(list._id) }, taskCount + 1);

		// History query
		Task t = tasks.get(0);
		final int histCount = 22;
		for (int i = 0; i < 22; i++) {
			// edit the note & save it
			t.title += " hist" + i;
			t.save(mContext);
		}
		// Should return insert (1) + update count (histCount)
		assertUriReturnsResult(Task.URI_TASK_HISTORY,
				Task.Columns.HISTORY_COLUMNS, Task.Columns.HIST_TASK_ID
						+ " IS ?", new String[] { Long.toString(t._id) },
				histCount + 1);

		// TODO remember legacy uris
		// TODO need a projection mapper
		// assertUriReturnsResult(LegacyDBHelper.NotePad.Notes.CONTENT_URI,
		// new String[] { LegacyDBHelper.NotePad.Notes.COLUMN_NAME_TITLE });
		// assertUriReturnsResult(
		// LegacyDBHelper.NotePad.Notes.CONTENT_VISIBLE_URI,
		// new String[] { LegacyDBHelper.NotePad.Notes.COLUMN_NAME_TITLE });

		list.delete(mContext);

		// Should return insert NOTHING since it should have been deleted
		assertUriReturnsResult(Task.URI_TASK_HISTORY,
				Task.Columns.HISTORY_COLUMNS, Task.Columns.HIST_TASK_ID
						+ " IS ?", new String[] { Long.toString(t._id) }, 0);
	}
}
