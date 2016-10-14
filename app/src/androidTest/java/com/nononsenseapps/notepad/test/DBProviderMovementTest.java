package com.nononsenseapps.notepad.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import com.nononsenseapps.notepad.data.model.sql.DAO;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

public class DBProviderMovementTest extends AndroidTestCase {

	private ContentResolver resolver;
	private Context context;

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

	/*
	 * Util methods
	 */

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

	private Cursor assertCursorGood(final Cursor c) {
		assertNotNull(c);
		assertEquals(false, c.isClosed());

		return c;
	}

	private void assertTasksCountIs(final long listId, final int count) {
		assertEquals(count, getTasks(listId).size());
	}

	private void assertTaskLeftRightAreSequential(final long listId) {
		// Get ordered
		ArrayList<Task> tasks = getTasks(listId);
		long prev = 0;
		for (Task t : tasks) {
			assertTrue("Left must be less than right! " + t.left + " !< "
					+ t.right, t.left < t.right);
			assertTrue("Previous item must have smaller left",
					prev < (long) t.left);
			if (t.right == t.left + 1) {
				prev = t.right;
			}
			else {
				prev = t.left;
			}
		}

		// Test maximum value
		Cursor c = resolver.query(Task.URI, Task.Columns.FIELDS,
				Task.Columns.DBLIST + " IS ?",
				new String[] { Long.toString(listId) }, Task.Columns.RIGHT
						+ " DESC");
		assertCursorGood(c);
		HashSet<Long> positions = new HashSet<Long>();
		if (c.getCount() > 0) {
			// Right most will be twice the number of tasks
			assertTrue(c.moveToFirst());
			final Task last = new Task(c);

			assertTrue(String.format("%d != 2 * %d", last.right, c.getCount()),
					last.right == 2 * c.getCount());

			// Make sure there are no duplicates and such
			for (long i = 1; i <= c.getCount() * 2; i++) {
				positions.add(i);
			}

			positions.remove(last.left);
			positions.remove(last.right);

			while (c.moveToNext()) {
				Task task = new Task(c);
				positions.remove(task.left);
				positions.remove(task.right);
			}
		}
		c.close();

		assertEquals("Must be duplicate positions in the list", 0,
				positions.size());
	}

	private TaskList insertList() {
		final TaskList tl = new TaskList();
		tl.title = "A test list";

		tl.setId(resolver.insert(tl.getBaseUri(), tl.getContent()));

		assertTrue(0 < tl._id);

		return tl;
	}

	private void deleteList(final TaskList tl) {
		assertTrue(0 < resolver.delete(tl.getUri(), null, null));
	}

	private ArrayList<Task> insertTasks(final long listId, final int number) {
		ArrayList<Task> results = new ArrayList<Task>(number);
		for (int i = 0; i < number; i++) {
			int count = 0;
			Task t = new Task();
			t.title = "Task" + ++count;
			t.dblist = listId;
			Uri uri = resolver.insert(Task.URI, t.getContent());
			if (uri != null) {
				t.setId(uri);
				results.add(t);
			}
			assertTaskLeftRightAreSequential(listId);
		}
		assertTaskLeftRightAreSequential(listId);
		assertTasksCountIs(listId, number);

		return results;
	}

	private ArrayList<Task> getTasks(final long listId) {
		final ArrayList<Task> results = new ArrayList<Task>();
		final Cursor c = resolver.query(Task.URI,
				Task.Columns.FIELDS, Task.Columns.DBLIST + " IS ?",
				new String[] { Long.toString(listId) }, Task.Columns.LEFT);
		assertCursorGood(c);
		while (c.moveToNext()) {
			results.add(new Task(c));
		}
		c.close();

		return results;
	}

	private Task getTask(final long id) {
		final Cursor c = resolver.query(Task.getUri(id), Task.Columns.FIELDS,
				null, null, null);
		assertCursorGood(c);
		Task t = null;
		if (c.moveToFirst()) {
			t = new Task(c);
		}
		c.close();

		return t;
	}

	private ArrayList<Task> getDeletedTask(final String title) {
		final ArrayList<Task> results = new ArrayList<Task>();
		final Cursor c = resolver.query(Task.URI_DELETED_QUERY,
				Task.Columns.FIELDS, Task.Columns.TITLE + "IS ?",
				new String[] { title }, null);
		assertCursorGood(c);
		while (c.moveToNext()) {
			results.add(new Task(c));
		}
		c.close();

		return results;
	}

	private void deleteTasks(final ArrayList<Task> tasks) {
		for (final Task t : tasks) {
			deleteTask(t);
		}
	}

	private void deleteTask(Task t) {
		assertTrue(0 < resolver.delete(t.getUri(), null, null));
		assertTaskLeftRightAreSequential(t.dblist);
	}

	private void moveTasksToList(final TaskList tl, final Task... ts) {
		long[] ids = new long[ts.length];
		for (int i = 0; i < ts.length; i++) {
			ids[i] = ts[i]._id;
		}
		
		final ContentValues val = new ContentValues();
		val.put(Task.Columns.DBLIST, tl._id);
		
		// where _ID in (1, 2, 3)
		final String whereId = new StringBuilder(Task.Columns._ID).append(" IN (")
				.append(DAO.arrayToCommaString(ids)).append(")").toString();
		
		mContext.getContentResolver().update(Task.URI, val, whereId, null);

		// Verify that task was moved
		// Check new
		assertTaskLeftRightAreSequential(tl._id);
	}

	private ArrayList<Task> moveAndAssert(final TaskList tl, final int fromPos,
			final int toPos) {
		Log.i("nononsenseapps test", "Testing move from: " + fromPos + " to "
				+ toPos);
		// Get ordered
		final ArrayList<Task> oldtasks = getTasks(tl._id);

		// Move 5 to 4
		final Task movingTask = oldtasks.get(fromPos);
		final Task targetTask = oldtasks.get(toPos);

		final int result = movingTask.moveTo(resolver, targetTask);

		// Verity that things changed or not
		if (movingTask._id != targetTask._id)
			assertTrue("Moving a task should update rows", 0 < result);
		else
			assertTrue("Moving a task to itself shouldn't change anything",
					0 == result);

		// Find new values
		final ArrayList<Task> newtasks = getTasks(tl._id);
		Task newone = null;
		Task newtarget = null;

		for (Task t : newtasks) {
			if (t._id == movingTask._id) {
				newone = t;
			}
			if (t._id == targetTask._id) {
				newtarget = t;
			}
		}
		Log.d("nononsenseapps test", "old, target, new, newtarget: "
				+ movingTask.left + "," + movingTask.right + " "
				+ targetTask.left + "," + targetTask.right + " " + newone.left
				+ "," + newone.right + " " + newtarget.left + ","
				+ newtarget.right);

		assertNotNull("Couldnt find the moved task", newone);

		if (targetTask.left < movingTask.left) {
			assertEquals("Left value does not equal target", targetTask.left,
					newone.left);
			assertEquals("Right value does not equal target left + 1",
					targetTask.left + 1, (long) newone.right);
		}
		else if (targetTask.right > movingTask.right) {
			assertEquals("Left value does not equal target right - 1",
					targetTask.right - 1, (long) newone.left);
			assertEquals("Right value does not equal target", targetTask.right,
					newone.right);
		}

		assertEquals("Width should be 1 after a move", 1, newone.right
				- newone.left);

		// assertEquals("Target should have moved 2 steps", 2,);

		assertTrue("Number of tasks should not change",
				oldtasks.size() == newtasks.size());
		assertTaskLeftRightAreSequential(tl._id);

		return getTasks(tl._id);
	}

	/*
	 * Test methods
	 */

	public void testDeleteList() {
		final TaskList tl = insertList();
		final int count = 10;
		final long listId = tl._id;

		assertTasksCountIs(listId, 0);

		insertTasks(listId, count);
		assertTasksCountIs(listId, count);

		deleteList(tl);
		// Should return nothing
		// Cursor c = resolver.query(TaskList.URI, TaskList.Columns.FIELDS,
		// TaskList.Columns._ID + " IS ?",
		// new String[] { Long.toString(listId) }, null);
		// assertCursorGood(c);

		// assertEquals("List should be gone", 0, c.getCount());
		// removing list should delete all tasks within
		assertTasksCountIs(listId, 0);
		// c.close();
	}

	public void testInsertAndRemoveTasks() {
		final TaskList tl = insertList();
		ArrayList<Task> tasks = insertTasks(tl._id, 10);
		assertTasksCountIs(tl._id, 10);
		deleteTasks(tasks);
		assertTasksCountIs(tl._id, 0);
		deleteList(tl);
	}

	public void testInsertTaskInWrongList() {
		// Should not be possible to insert
		// into a non-existing list because of foreign key constraints

		final long wrongId = 92525;
		assertTasksCountIs(wrongId, 0);

		Task t = new Task();
		t.title = "Task";
		t.dblist = wrongId;

		boolean thrown = false;
		Uri uri = null;
		try {
			uri = resolver.insert(Task.URI, t.getContent());
		}
		catch (SQLException e) {
			thrown = true;
		}

		assertTrue(uri == null);
		assertTasksCountIs(wrongId, 0);
	}

	public void testInvalidPos() {
		// Positions must be greater than 0
		// or should throw constraint failed

		TaskList tl = insertList();
		ArrayList<Task> ts = insertTasks(tl._id, 1);

		Task t = ts.get(0);

		t.left = 0L;

		boolean failed = false;
		try {
			resolver.update(t.getUri(), t.getContent(), null, null);
		}
		catch (SQLiteConstraintException e) {
			failed = true;
		}

		//assertTrue("Setting left to 0 should throw exception!", failed);

		t.left = 5L;
		t.right = 0L;
		failed = false;
		try {
			resolver.update(t.getUri(), t.getContent(), null, null);
		}
		catch (SQLiteConstraintException e) {
			failed = true;
		}
		//assertTrue("Setting right to 0 should throw exception", failed);

		deleteList(tl);
	}

	public void testMoveTask() {
		final TaskList tl = insertList();
		int count = 10;
		insertTasks(tl._id, count);
		assertTaskLeftRightAreSequential(tl._id);

		// Move some tasks around
		moveAndAssert(tl, 0, count - 1);
		moveAndAssert(tl, count - 1, 0);

		moveAndAssert(tl, 1, count - 2);
		moveAndAssert(tl, count - 2, 1);

		moveAndAssert(tl, 4, 0);
		moveAndAssert(tl, 4, 9);

		for (int i = 0; i < count * 2; i++) {
			moveAndAssert(tl, 0, count - 1);
		}
		for (int i = 0; i < count * 2; i++) {
			moveAndAssert(tl, count - 2, 2);
		}

		Random rand = new Random();
		int min = 0, max = count - 1;
		for (int i = 0; i < count * 2; i++) {
			int fromPos = rand.nextInt(max - min + 1) + min;
			int toPos = fromPos;
			while (toPos == fromPos) {
				toPos = rand.nextInt(max - min + 1) + min;
			}
			// two unique positions generated, now move
			moveAndAssert(tl, fromPos, toPos);
		}

		// Clean up
		deleteList(tl);
	}

	public void testMoveTaskToList() {
		final TaskList tl = insertList();
		final TaskList tl2 = insertList();
		int count = 10;
		ArrayList<Task> tasks1 = insertTasks(tl._id, count);
		ArrayList<Task> tasks2 = insertTasks(tl2._id, count);
		assertTaskLeftRightAreSequential(tl._id);
		assertTaskLeftRightAreSequential(tl2._id);

		// Move some tasks around


		Random rand = new Random();
		int min = 0, max = count - 1;
		for (int i = 0; i < 100; i++) {
			if (rand.nextBoolean() && tasks1.size() > 1) {
				int taskIndex = rand.nextInt(tasks1.size());
				Task t1 = tasks1.remove(taskIndex);
				taskIndex = rand.nextInt(tasks1.size());
				Task t2 = tasks1.remove(taskIndex);
				moveTasksToList(tl2, t1, t2);
				tasks2.add(t1);
				tasks2.add(t2);
			}
			else if (tasks2.size() > 1) {
				int taskIndex = rand.nextInt(tasks2.size());
				Task t1 = tasks2.remove(taskIndex);
				taskIndex = rand.nextInt(tasks2.size());
				Task t2 = tasks2.remove(taskIndex);
				moveTasksToList(tl, t1, t2);
				tasks1.add(t1);
				tasks1.add(t2);
			}
		}

		// Clean up
		deleteList(tl);
		deleteList(tl2);
	}

//	public void testIndents() {
//		final TaskList tl = insertList();
//		int count = 7;
//		insertTasks(tl._id, count);
//		ArrayList<Task> orgTasks = getTasks(tl._id);
//
//		// Indenting the first item should fail (not change anything)
//		// as it's impossible to do
//
//		indentAndAssert(orgTasks.get(0), false);
//
//		// Test a successful one
//		/*
//		 * a0 b1 c0 d0 e0
//		 */
//		orgTasks = indentAndAssert(orgTasks.get(1), true);
//		assertEquals("Task level should be two", 2, orgTasks.get(1).level);
//
//		// Indenting it again should fail though
//		/*
//		 * a0 b1 c0 d0 e0 f0 g0
//		 */
//		orgTasks = indentAndAssert(orgTasks.get(1), false);
//		assertEquals("Task level should still be two", 2, orgTasks.get(1).level);
//
//		// Try last item
//		/*
//		 * a0 b1 c0 d0 e0 f0 g1
//		 */
//		orgTasks = indentAndAssert(orgTasks.get(count - 1), true);
//		assertEquals("Task level should be two", 2,
//				orgTasks.get(count - 1).level);
//
//		/*
//		 * a0 b1 c2 d1 e0 f1 g2
//		 */
//		orgTasks = indentAndAssert(orgTasks.get(2), true);
//		orgTasks = indentAndAssert(orgTasks.get(2), true);
//		orgTasks = indentAndAssert(orgTasks.get(2), false);
//		assertEquals("Task level should be three", 3, orgTasks.get(2).level);
//		orgTasks = indentAndAssert(orgTasks.get(3), true);
//		assertEquals("Task level incorrect", 2, orgTasks.get(3).level);
//		orgTasks = indentAndAssert(orgTasks.get(5), true);
//		orgTasks = indentAndAssert(orgTasks.get(5), false);
//		assertEquals("Task level incorrect", 2, orgTasks.get(5).level);
//		orgTasks = indentAndAssert(orgTasks.get(6), true);
//		orgTasks = indentAndAssert(orgTasks.get(6), false);
//		assertEquals("Task level incorrect", 3, orgTasks.get(6).level);
//
//		/*
//		 * a0 b1 c2 d3 e4 f5 g6
//		 */
//		orgTasks = indentAndAssert(orgTasks.get(0), false);
//		orgTasks = indentAndAssert(orgTasks.get(1), false);
//		orgTasks = indentAndAssert(orgTasks.get(2), false);
//		orgTasks = indentAndAssert(orgTasks.get(3), true);
//		orgTasks = indentAndAssert(orgTasks.get(3), true);
//		orgTasks = indentAndAssert(orgTasks.get(3), false);
//		orgTasks = indentAndAssert(orgTasks.get(4), true);
//		orgTasks = indentAndAssert(orgTasks.get(4), true);
//		orgTasks = indentAndAssert(orgTasks.get(4), true);
//		orgTasks = indentAndAssert(orgTasks.get(4), true);
//		orgTasks = indentAndAssert(orgTasks.get(4), false);
//		orgTasks = indentAndAssert(orgTasks.get(5), true);
//		orgTasks = indentAndAssert(orgTasks.get(5), true);
//		orgTasks = indentAndAssert(orgTasks.get(5), true);
//		orgTasks = indentAndAssert(orgTasks.get(5), true);
//		orgTasks = indentAndAssert(orgTasks.get(5), false);
//		orgTasks = indentAndAssert(orgTasks.get(6), true);
//		orgTasks = indentAndAssert(orgTasks.get(6), true);
//		orgTasks = indentAndAssert(orgTasks.get(6), true);
//		orgTasks = indentAndAssert(orgTasks.get(6), true);
//		orgTasks = indentAndAssert(orgTasks.get(6), false);
//
//		for (int i = 0; i < orgTasks.size(); i++) {
//			assertEquals("Task level incorrect", 1 + i, orgTasks.get(i).level);
//		}
//
//		// Let's start unindenting stuff!
//
//		// Unindent root should fail
//		orgTasks = unIndentAndAssert(orgTasks.get(0), false);
//		// Last one should succeed many times
//		for (int i = orgTasks.size(); i > 1; i--) {
//			assertEquals("Level incorrect (i = " + i + ")", i,
//					orgTasks.get(orgTasks.size() - 1).level);
//			orgTasks = unIndentAndAssert(orgTasks.get(orgTasks.size() - 1),
//					true);
//		}
//		// Should now be a root
//		assertEquals("Level incorrect", 1,
//				orgTasks.get(orgTasks.size() - 1).level);
//
//		// Let's do the rest, top to bottom
//		// All preceeding items are affected by this amount
//		int cum = 0;
//		for (int j = 1; j < orgTasks.size() - 1; j++) {
//			for (int i = j + 1; i - cum > 1; i--) {
//				assertEquals("Level incorrect (i = " + i + ")", i - cum,
//						orgTasks.get(j).level);
//				orgTasks = unIndentAndAssert(orgTasks.get(j), true);
//			}
//			cum += 1;
//			// Should now be a root
//			assertEquals("Level incorrect", 1, orgTasks.get(j).level);
//		}
//
//		deleteList(tl);
//	}
//
//	public void testIndentsHarder() {
//		// Was something I actually did and noticed a crash
//		final TaskList tl = insertList();
//		int count = 3;
//		insertTasks(tl._id, count);
//		ArrayList<Task> orgTasks = getTasks(tl._id);
//
//		// The issue is the order things are moved in the statement
//		// Moved in order of their IDs!
//
//		// Now at 2,1,0
//		// Want 0,2,1 (where 2 is indented)
//		orgTasks = moveAndAssert(tl, 2, 0);
//		orgTasks = indentAndAssert(orgTasks.get(1), true);
//
//		// This crashed by trying to set right to null
//		// Since the parent was re-assigned before the child
//		orgTasks = unIndentAndAssert(orgTasks.get(1), true);
//
//		deleteList(tl);
//	}
//
//	public void testMoveIndentedTrees() {
//		// important to test moving tasks in a tree structure
//		final TaskList tl = insertList();
//		int count = 9;
//		insertTasks(tl._id, count);
//		ArrayList<Task> orgTasks = getTasks(tl._id);
//
//		// Roots at 0, 2 and 6
//		// 0
//		indentAndAssert(orgTasks.get(1), true);
//		// 2
//		indentAndAssert(orgTasks.get(3), true);
//		indentAndAssert(orgTasks.get(4), true);
//		indentAndAssert(orgTasks.get(4), true);
//		indentAndAssert(orgTasks.get(5), true);
//		indentAndAssert(orgTasks.get(5), true);
//		indentAndAssert(orgTasks.get(5), true);
//		// 6
//		indentAndAssert(orgTasks.get(7), true);
//		indentAndAssert(orgTasks.get(8), true);
//		indentAndAssert(orgTasks.get(8), true);
//
//		// move 2 tree to bottom
//		// now part of six tree
//		moveAndAssert(tl, 2, 8);
//
//		// Move 6 tree to top
//		moveAndAssert(tl, 3, 0);
//
//		// Move 2 tree to top
//		moveAndAssert(tl, 6, 0);
//
//		deleteList(tl);
//	}

	public void testInvalidMoves() {
		// TODO
		// fail("TODO");
	}

	public void testMultipleDeletes() {
		// TODO
		// fail("TODO");
	}

	public void testTaskContent() {
		Task t = new Task();
		t.title = "Hej";
		t.dblist = 1L;
		ContentValues values = t.getContent();

		assertFalse(values.containsKey(Task.Columns.LEFT));
		assertFalse(values.containsKey(Task.Columns.RIGHT));
	}

	public void testDeleteTrigger() {
		// Deleting an item should place a copy of it in the delete-table
		final TaskList tl = insertList();
		insertTasks(tl._id, 1);
		ArrayList<Task> orgTasks = getTasks(tl._id);

		final Task orgTask = orgTasks.get(0);

		final String t = "HABANA MAMAMANA";
		orgTask.title = t;

		resolver.update(orgTask.getUri(), orgTask.getContent(), null, null);
		resolver.delete(orgTask.getUri(), null, null);

		orgTasks = getTasks(tl._id);

		assertEquals("List should be empty now", 0, orgTasks.size());

		// Get the item from backup table instead
		Cursor c = resolver.query(Task.URI_DELETED_QUERY,
				Task.Columns.DELETEFIELDS, Task.Columns.TITLE + " IS ?",
				new String[] { t }, null);

		assertTrue("Task should be found in delete table after delete!",
				c.moveToFirst());

		deleteList(tl);
	}
}
