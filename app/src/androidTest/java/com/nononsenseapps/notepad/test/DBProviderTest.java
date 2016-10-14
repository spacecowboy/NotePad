package com.nononsenseapps.notepad.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.resetTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setEmptyTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setFreshTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setTestDatabase;

public class DBProviderTest extends AndroidTestCase {

	private Context context;
	private ContentResolver resolver;

	@Override
	public void setUp() throws Exception {
		context = getContext();
		setEmptyTestDatabase(context, getClass().getName());
		resolver = context.getContentResolver();
	}

	@Override
	public void tearDown() throws Exception {
        resetTestDatabase(context, getClass().getName());
	}

	private void assertUriReturnsResult(final Uri uri, final String[] fields) {
		assertUriReturnsResult(uri, fields, null, null, -1);
	}

	private void assertUriReturnsResult(final Uri uri, final String[] fields,
			final String where, final String[] whereArgs, final int count) {
		final Cursor c = resolver.query(uri, fields, where, whereArgs, null);
		final int cursorCount = c.getCount();
		c.close();
		if (count < 0) {
			assertTrue("Uri did not return a result: " + uri.getEncodedPath(),
					cursorCount > 0);
		}
		else {
			assertEquals("Uri did not return expected number of results",
					count, cursorCount);
		}
	}

	private TaskList getNewList() {
		TaskList result = new TaskList();
		result.title = "111aaTestingList";
		result.save(context);
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
			t.save(context);
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

		// Sectioned Date query
		assertUriReturnsResult(Task.URI_SECTIONED_BY_DATE, Task.Columns.FIELDS,
				Task.Columns.DBLIST + " IS ?",
				new String[] { Long.toString(list._id) }, taskCount + 1);

		// History query
		Task t = tasks.get(0);
		final int histCount = 22;
		for (int i = 0; i < 22; i++) {
			t.title += " hist" + i;
			t.save(getContext());
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

		list.delete(context);

		// Should return insert NOTHING since it should have been deleted
		assertUriReturnsResult(Task.URI_TASK_HISTORY,
				Task.Columns.HISTORY_COLUMNS, Task.Columns.HIST_TASK_ID
						+ " IS ?", new String[] { Long.toString(t._id) }, 0);
	}
}
