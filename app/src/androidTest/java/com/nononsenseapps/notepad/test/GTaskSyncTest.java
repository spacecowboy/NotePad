package com.nononsenseapps.notepad.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.data.model.gtasks.GoogleTask;
import com.nononsenseapps.notepad.data.model.gtasks.GoogleTaskList;
import com.nononsenseapps.notepad.data.remote.gtasks.GoogleTaskSync;

import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import android.util.Pair;

import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.resetTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setEmptyTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setFreshTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setTestDatabase;

public class GTaskSyncTest extends AndroidTestCase {
	String balle = "balle";
	String balleRemote = "balleremote";
	String account = "balleman";

	ArrayList<TaskList> localLists;
	ArrayList<GoogleTaskList> remoteListsInDB;
	ArrayList<GoogleTaskList> remoteListsSubset;

	HashMap<TaskList, ArrayList<Task>> localTasks;
	HashMap<GoogleTaskList, ArrayList<GoogleTask>> remoteTasksInDB;
	HashMap<GoogleTaskList, ArrayList<GoogleTask>> remoteTasksSubSet;

	int remoteOnlyCount = 2;
	int localOnlyCount = 1;

	int localListNewestCount = 0;
	int remoteListNewestCount = 0;
	int localTaskNewestCount = 0;
	int remoteTaskNewestCount = 0;

	@Override
	public void setUp() throws Exception {
		localListNewestCount = 0;
		remoteListNewestCount = 0;
		// Delete all existing lists
		setEmptyTestDatabase(getContext(), getClass().getName());

		// First insert some lists and remote stuff
		localLists = new ArrayList<TaskList>();
		remoteListsInDB = new ArrayList<GoogleTaskList>();
		remoteListsSubset = new ArrayList<GoogleTaskList>();
		localTasks = new HashMap<TaskList, ArrayList<Task>>();
		remoteTasksInDB = new HashMap<GoogleTaskList, ArrayList<GoogleTask>>();
		remoteTasksSubSet = new HashMap<GoogleTaskList, ArrayList<GoogleTask>>();

		int max = 8;
		for (int i = 0; i < max; i++) {
			GoogleTaskList gl = null;
			TaskList l = new TaskList();
			l.title = balle + i;
			// In the past
			l.save(mContext, 5L);
			localLists.add(l);
			localTasks.put(l, new ArrayList<Task>());
			if (i < (max - localOnlyCount)) {
				gl = new GoogleTaskList(l, account);
				// 0 2 4 6
				if (i % 2 == 0) {
					gl.updated = l.updated - 1;
					// localListNewestCount++;
				}
				else {
					gl.updated = l.updated + 1;
					// remoteListNewestCount++;
				}
				gl.remoteId = l.title;
				gl.save(mContext);
				remoteListsInDB.add(gl);
				remoteTasksInDB.put(gl, new ArrayList<GoogleTask>());
				remoteTasksSubSet.put(gl, new ArrayList<GoogleTask>());
				// 0 2
				if (i < 4) {
					remoteListsSubset.add(gl);
					if (i % 2 == 0) {
						localListNewestCount++;
					}
					else {
						remoteListNewestCount++;
					}
				}
			}
			addTasks(l, gl);
		}

		// Simulate remote only items
		GoogleTaskList gl = new GoogleTaskList(account);
		gl.title = balleRemote;
		gl.updated = Calendar.getInstance().getTimeInMillis();
		gl.remoteId = balle + "999";
		remoteListsSubset.add(gl);
		remoteListNewestCount++;

		gl = new GoogleTaskList(account);
		gl.title = balleRemote;
		gl.updated = Calendar.getInstance().getTimeInMillis();
		gl.remoteId = balle + "998";
		remoteListsSubset.add(gl);
		remoteListNewestCount++;

		remoteOnlyCount = 2;
	}

	void addTasks(final TaskList l, final GoogleTaskList gl) {
		// All lists look the same so ok to overwrite these values
		if (gl != null) {
			localTaskNewestCount = 0;
			remoteTaskNewestCount = 0;
		}
		// Always add some local tasks
		int taskCount = 8;
		int remoteCount = taskCount - localOnlyCount;
		int remoteSubCount = remoteCount - 3;
		for (int i = 0; i < taskCount; i++) {
			Task t = new Task();
			t.dblist = l._id;
			t.title = balle + i;
			t.note = balle + i;
			t.save(mContext);
			localTasks.get(l).add(t);
			if (gl != null && i < remoteCount) {
				GoogleTask gt = new GoogleTask(t, account);

				gt.remoteId = l.title + balleRemote + i;

				if (i < remoteSubCount) {
					gt.updated = t.updated + 1;
					remoteTasksSubSet.get(gl).add(gt);
					remoteTaskNewestCount++;
				}
				else {
					localTaskNewestCount++;
					gt.updated = t.updated - 1;
				}

				gt.save(mContext);
				remoteTasksInDB.get(gl).add(gt);
			}
		}

		if (gl != null) {
			// add two remote only
			GoogleTask gt = new GoogleTask(account);
			gt.title = balleRemote + "991";
			gt.notes = balleRemote + "991";
			gt.remoteId = balleRemote + "991";
			// gt.listdbid = l._id;
			gt.updated = Calendar.getInstance().getTimeInMillis();
			remoteTaskNewestCount++;

			remoteTasksSubSet.get(gl).add(gt);

			gt = new GoogleTask(account);
			gt.title = balleRemote + "992";
			gt.notes = balleRemote + "992";
			gt.remoteId = balleRemote + "992";
			gt.updated = 0L;
			// gt.listdbid = l._id;
			remoteTaskNewestCount++;

			remoteTasksSubSet.get(gl).add(gt);
		}
	}

	@Override
	public void tearDown() throws Exception {
		// remote items that were inserted
		resetTestDatabase(getContext(), getClass().getName());
	}

	@SmallTest
	public void testBasic() {
		GoogleTaskList gl = new GoogleTaskList(account);
		gl.updated = 9L;
		gl.remoteId = "bobman";
		gl.dbid = 99L;
		gl.save(mContext);

		assertTrue("save did not succeed", gl._id > 0);

		final Cursor cc = mContext.getContentResolver().query(
				GoogleTaskList.URI, GoogleTaskList.Columns.FIELDS, null, null,
				null);
		try {
			assertNotNull(cc);
			assertTrue("Simple Cursor can not be empty!", cc.getCount() > 0);
			boolean found = false;
			while (cc.moveToNext()) {
				GoogleTaskList gll = new GoogleTaskList(cc);
				if (gll.remoteId.equals("bobman")) {
					found = true;
					break;
				}
			}
			assertTrue("List was not returned in simple case!", found);
		}
		finally {
			if (cc != null) cc.close();
		}

		final Cursor c = mContext.getContentResolver().query(
				GoogleTaskList.URI,
				GoogleTaskList.Columns.FIELDS,
				GoogleTaskList.Columns.ACCOUNT + " IS ? AND "
						+ GoogleTaskList.Columns.SERVICE + " IS ?",
				new String[] { account, GoogleTaskList.SERVICENAME }, null);
		try {
			assertNotNull(c);
			assertTrue("Cursor can not be empty!", c.getCount() > 0);
			boolean found = false;
			while (c.moveToNext()) {
				GoogleTaskList gll = new GoogleTaskList(c);
				if (gll.remoteId.equals("bobman")) {
					found = true;
					break;
				}
			}
			assertTrue("List was not returned!", found);
		}
		finally {
			if (c != null) c.close();
		}

		gl.delete(mContext);
	}

	@SmallTest
	public void testLogic() {
		final Cursor c = mContext.getContentResolver().query(
				GoogleTaskList.URI,
				GoogleTaskList.Columns.FIELDS,
				GoogleTaskList.Columns.ACCOUNT + " IS ? AND "
						+ GoogleTaskList.Columns.SERVICE + " IS ?",
				new String[] { account, GoogleTaskList.SERVICENAME }, null);
		try {
			assertNotNull(c);
			assertEquals("Cursor of remote lists not expected size",
					remoteListsInDB.size(), c.getCount());
		}
		finally {
			if (c != null) c.close();
		}
	}

	@SmallTest
	public void testMergeLists() {
		// Verify that merge works
		GoogleTaskSync.mergeListsWithLocalDB(mContext, account,
				remoteListsSubset);

		assertEquals("mergeLists result does not equal real result!",
				remoteListsInDB.size() + remoteOnlyCount,
				remoteListsSubset.size());
	}

	@SmallTest
	public void testMergeTasks() {
		final TaskList tl = localLists.get(0);
		final GoogleTaskList gl = remoteListsInDB.get(0);
		List<GoogleTask> remoteTasks = remoteTasksSubSet.get(gl);
		GoogleTaskSync.mergeTasksWithLocalDB(mContext, account, remoteTasks,
				tl._id);

		Log.d("nononsenseapps gtasktest",
				"remoteTasks size: " + remoteTasks.size());
		assertEquals("mergeTasks did not equal expected size!", remoteTasksInDB
				.get(gl).size() + remoteOnlyCount, remoteTasks.size());
		int remoteCount = 0;
		for (GoogleTask gt : remoteTasks) {
			// See that all has local id, except the two remote only
			if (gt.remoteId.contains("99")) {
				remoteCount++;
			}
			else {
				assertNotNull("Local id was not set", gt.dbid);
				assertTrue("Local id was not set", gt.dbid > 0);
			}
			// And that list is set!
			assertTrue("List id was not set", gt.listdbid > 0);

			Log.d("nononsenseapps gtasktest", gt.remoteId);
		}

		assertEquals("Only two tasks were remote only!", remoteOnlyCount,
				remoteCount);
	}

	@SmallTest
	public void testSyncListsLocally() {
		int initialSize = remoteListsSubset.size();
		// Two is remote only, one old and one new
		GoogleTaskSync.mergeListsWithLocalDB(mContext, account,
				remoteListsSubset);

		List<Pair<TaskList, GoogleTaskList>> pairs = GoogleTaskSync
				.synchronizeListsLocally(mContext, remoteListsSubset);

		assertNotNull(pairs);
		// for (Pair<TaskList, GoogleTaskList> pair : pairs) {
		// if (pair.first == null) {
		// Log.d("nononsenseapps gtasktest", "r: " + pair.second.title);
		// }
		// else if (pair.second == null) {
		// Log.d("nononsenseapps gtasktest", "l: " + pair.first.title);
		// }
		// else
		// Log.d("nononsenseapps gtasktest", "l: " + pair.first.title
		// + ", r: " + pair.second.title);
		// }
		assertEquals("Resulting list seems to have wrong size", initialSize
				+ localOnlyCount, pairs.size());

		int remoteNewestCount = 0;
		int localNewestCount = 0;
		int remoteNullCount = 0;
		int localNullCount = 0;

		for (Pair<TaskList, GoogleTaskList> pair : pairs) {
			if (pair.first == null) {
				assertNotNull(pair.second);
				localNullCount++;
			}
			else if (pair.second == null) {
				assertNotNull(pair.first);
				remoteNullCount++;
			}
			else {
				assertEquals("Titles should be the same", pair.first.title,
						pair.second.title);
				assertEquals("local id should be set", (Long) pair.first._id,
						pair.second.dbid);
				// REverse check to avoid .equals fuckup
				if (pair.first.updated > pair.second.updated) {
					localNewestCount++;
				}
				else {
					remoteNewestCount++;
					assertEquals("Update time should be the same",
							pair.second.updated, pair.first.updated);
				}
			}
		}

//		assertTrue("Only one item was supposed to be remote and deleted locally",
//				localNullCount == remoteOnlyCount);
		assertTrue("Expected only one 'new local list'", remoteNullCount == 1);

		assertTrue(localNewestCount > 0);
		assertTrue(remoteNewestCount > 0);
		assertEquals("lUpdate time incorrect", localListNewestCount,
				localNewestCount);
		// Plus one for remote only object which creates new list
		assertEquals("rUpdate time incorrect", remoteListNewestCount,
				remoteNewestCount);
	}

	@SmallTest
	public void testSyncTasksLocally() {
		// TODO
		final TaskList tl = localLists.get(0);
		final GoogleTaskList gl = remoteListsInDB.get(0);

		List<GoogleTask> remoteTasks = remoteTasksSubSet.get(gl);
		GoogleTaskSync.mergeTasksWithLocalDB(mContext, account, remoteTasks,
				tl._id);

		List<Pair<Task, GoogleTask>> pairs = GoogleTaskSync
				.synchronizeTasksLocally(mContext, remoteTasks,
						new Pair<TaskList, GoogleTaskList>(tl, gl));

		assertNotNull(pairs);
		assertEquals("Synced pairs have wrong length", localOnlyCount + localTaskNewestCount, pairs.size());

		int remoteNewestCount = 0;
		int localNewestCount = 0;
		int remoteNullCount = 0;
		int localNullCount = 0;

		for (Pair<Task, GoogleTask> pair : pairs) {
			if (pair.first == null) {
				assertNotNull(pair.second);
				localNullCount++;
			}
			else if (pair.second == null) {
				assertNotNull(pair.first);
				remoteNullCount++;
			}
			else {
				assertEquals("Titles should be the same", pair.first.title,
						pair.second.title);
				assertEquals("local id should be set", (Long) pair.first._id,
						pair.second.dbid);
				assertEquals("list id should be set", (Long) pair.first.dblist,
						pair.second.listdbid);
				
				if (pair.first.updated > pair.second.updated){
					localNewestCount++;

					Log.d("nononsenseapps gtasksync", "local newest: "
							+ pair.first.title + " : " + pair.second.title);
				}
				else {
					remoteNewestCount++;
					assertEquals("Update time should be the same",
							pair.second.updated, pair.first.updated);
					assertEquals("Title should be the same", pair.second.title,
							pair.first.title);
					assertEquals("Note should be the same", pair.second.notes,
							pair.first.note);
					assertNotNull("Title is not allowed to be null",
							pair.first.title);

					Log.d("nononsenseapps gtasksync", "remote newest: "
							+ pair.first.title + " : " + pair.second.title);
				}
			}
		}

//		assertTrue("Only one item was supposed to be remote and deletedlocally: "
//				+ localNullCount, localNullCount == localOnlyCount);
		assertEquals("Not expected 'new local task' count: " + remoteNullCount,
				localTasks.get(tl).size() - remoteTasksInDB.get(gl).size(),
				remoteNullCount);

		Log.d("nononsenseapps gtasksync", "localnewest: "
				+ localTaskNewestCount + " vs " + localNewestCount);
		Log.d("nononsenseapps gtasksync", "remotenewest: "
				+ remoteTaskNewestCount + " vs " + remoteNewestCount);

		assertTrue("local count", localNewestCount > 0);
		assertEquals("remote count", 0, remoteNewestCount);
		assertEquals("lUpdate time incorrect", localTaskNewestCount, localNewestCount);
		assertEquals("rUpdate time incorrect", 0, remoteNewestCount);
	}
	
	@SmallTest
	public void testSyncTasksLocallyCompleteBug() {
		// TODO
//		Complete status should be transferred correclty,
//		localcomplete, remote not
//		localcomplete, remote complete
//		local not, remote not
//		localcomplete, remote complete
	}
}
