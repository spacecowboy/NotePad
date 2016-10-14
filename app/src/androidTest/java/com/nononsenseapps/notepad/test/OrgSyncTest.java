package com.nononsenseapps.notepad.test;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.notepad.data.model.sql.RemoteTask;
import com.nononsenseapps.notepad.data.model.sql.RemoteTaskList;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.data.local.orgmode.OrgConverter;
import com.nononsenseapps.notepad.data.local.orgmode.SDSynchronizer;

import org.cowboyprogrammer.org.OrgFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;

import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.resetTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setEmptyTestDatabase;
import static com.nononsenseapps.notepad.data.local.sql.DatabaseHandler.setTestDatabase;

/**
 * Test the synchronizer code.
 * Methods starting with 'testFresh' are meant to be reused in higher-order
 * tests.
 */
public class OrgSyncTest extends AndroidTestCase {

    private static final String ACCOUNT = "bobtester";
    private static String DIR;

    @Override
    public void setUp() throws Exception {
        File d = getContext().getDir("ORGSYNCTEST", Context.MODE_PRIVATE);
        DIR = d.getPath();

        if (!d.exists()) {
            d.mkdirs();
        }

        reset();
        setEmptyTestDatabase(getContext(), getClass().getName());
    }

    @Override
    public void tearDown() {
        reset();
    }

    private void reset() {
        resetTestDatabase(getContext(), getClass().getName());

        File d = new File(DIR);
        for (File f : d.listFiles()) {
            f.delete();
        }
    }

    public ArrayList<TaskList> getTaskLists() {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor c = resolver.query(TaskList.URI, TaskList.Columns
                .FIELDS, null, null, null);

        ArrayList<TaskList> result = new ArrayList<TaskList>();
        while (c.moveToNext()) {
            result.add(new TaskList(c));
        }
        c.close();

        return result;
    }

    public ArrayList<Task> getTasks(final long listid) {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor c = resolver.query(Task.URI, Task.Columns
                        .FIELDS, Task.Columns.DBLIST + " IS ?",
                new String[]{Long.toString(listid)}, null
        );

        ArrayList<Task> result = new ArrayList<Task>();
        while (c.moveToNext()) {
            result.add(new Task(c));
        }
        c.close();

        return result;
    }

    public ArrayList<RemoteTaskList> getRemoteTaskLists() {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor c = resolver.query(RemoteTaskList.URI, RemoteTaskList.Columns
                        .FIELDS, RemoteTaskList.Columns.ACCOUNT + " IS ?",
                new String[]{ACCOUNT}, null
        );

        ArrayList<RemoteTaskList> result = new ArrayList<RemoteTaskList>();
        while (c.moveToNext()) {
            result.add(new RemoteTaskList(c));
        }
        c.close();

        return result;
    }

    public ArrayList<RemoteTask> getRemoteTasks() {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor c = resolver.query(RemoteTask.URI, RemoteTask.Columns
                        .FIELDS, RemoteTask.Columns.ACCOUNT + " IS ?",
                new String[]{ACCOUNT}, null
        );

        ArrayList<RemoteTask> result = new ArrayList<RemoteTask>();
        while (c.moveToNext()) {
            result.add(new RemoteTask(c));
        }
        c.close();

        return result;
    }

    @SmallTest
    public void testPass() {
        // This always passes
        assertTrue(true);
    }

    @SmallTest
    public void testTester() {
        TestSynchronizer tester = new TestSynchronizer(getContext());
        assertTrue(tester.isConfigured());
    }

    /**
     * End result: synced state of one tasklist with two tasks.
     * Tested flow branches:
     * - Lists: Create file
     * - Tasks: Create node
     */
    public void testFreshSimple() {
        // First create a list with 2 tasks
        TaskList list = new TaskList();
        list.title = "TestList";
        list.save(getContext());
        assertTrue(list._id > 0);

        final int taskCount = 2;
        for (int i = 0; i < taskCount; i++) {
            Task t = new Task();
            t.dblist = list._id;
            t.title = "Task" + i;
            t.note = "A body for the task";
            t.save(getContext());
            assertTrue(t._id > 0);
        }

        TestSynchronizer synchronizer = new TestSynchronizer(getContext());

        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // See the result
        HashSet<String> filenames = synchronizer.getRemoteFilenames();

        assertEquals("Only one list was created.", 1, filenames.size());

        String filename = null;
        for (String f : filenames) {
            filename = f;
        }

        assertEquals("Wrong filename", list.title + ".org", filename);

        // Check that the database is correct
        ArrayList<RemoteTaskList> remoteLists = getRemoteTaskLists();
        assertEquals("Should only be one RemoteList!", 1, remoteLists.size());

        ArrayList<RemoteTask> remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly 2 RemoteTasks", taskCount, remoteTasks.size());
        long lastDbid = -1;
        for (int i = 1; i < remoteTasks.size() + 1; i++) {
            RemoteTask r = remoteTasks.remove(i - 1);
            // Check for duplicates
            assertEquals("Id is not correct", i, r._id);
            assertTrue(lastDbid != r.dbid);
            lastDbid = r.dbid;
        }
    }

    public void syncAndAssertNothingChanged(final int taskCount) {
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // It should NOT have written to disk at all
        assertEquals("No changes should not be written!", 0,
                synchronizer.getPutRemoteCount());

        // Check that the database is still correct
        ArrayList<TaskList> lists = getTaskLists();
        assertEquals("Should only be one list", 1, lists.size());

        ArrayList<Task> tasks = getTasks(lists.get(0)._id);
        assertEquals("Should be only 2 tasks in list", taskCount, tasks.size());

        ArrayList<RemoteTaskList> remoteLists = getRemoteTaskLists();
        assertEquals("Should only be one RemoteList!", 1, remoteLists.size());

        ArrayList<RemoteTask> remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly 2 RemoteTasks", taskCount, remoteTasks.size());
        long lastDbid = -1;
        for (int i = 1; i < remoteTasks.size() + 1; i++) {
            RemoteTask r = remoteTasks.get(i - 1);
            // Check for duplicates
            assertEquals("Id is not correct", i, r._id);
            assertTrue(lastDbid != r.dbid);
            lastDbid = r.dbid;
        }
    }

    /**
     * Nothing has changed here.
     * Tested flow branches:
     * - Lists: Update Merge
     * - Tasks: Update Merge
     */
    public void testNothingNew() {
        final int taskCount = 2;
        testFreshSimple();

        syncAndAssertNothingChanged(taskCount);
        syncAndAssertNothingChanged(taskCount);
        syncAndAssertNothingChanged(taskCount);
        syncAndAssertNothingChanged(taskCount);
    }

    /**
     * Having two lists with the same name is possible in the app,
     * but obviously impossible at the filesystem level.
     * Tested flow branches:
     * - Lists: Create file
     */
    public void testDuplicateName() throws IOException, ParseException {
        // Create first list
        TaskList list1 = new TaskList();
        list1.title = "TestList";
        list1.save(getContext());
        assertTrue(list1._id > 0);

        // Create second list
        TaskList list2 = new TaskList();
        list2.title = "TestList";
        list2.save(getContext());
        assertTrue(list2._id > 0);

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());
        synchronizer.fullSync();

        // Make sure the second one was renamed!
        for (TaskList tl : getTaskLists()) {
            if (tl._id == list1._id) {
                assertEquals(list1.title, tl.title);
            } else if (tl._id == list2._id) {
                assertEquals("List should have been renamed",
                        list2.title + 1, tl.title);
            }
        }

        HashSet<String> filenames = synchronizer.getRemoteFilenames();
        assertEquals(2, filenames.size());
        assertTrue(filenames.contains(list1.title + ".org"));
        assertTrue(filenames.contains(list2.title + 1 + ".org"));
    }

    /**
     * Renaming a list in the app should rename the file.
     * Tested branches:
     * - Update list, renamed
     */
    public void testRenamedList() throws IOException, ParseException {
        // Create first list
        TaskList list1 = new TaskList();
        list1.title = "TestList";
        list1.save(getContext());
        assertTrue(list1._id > 0);

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());
        synchronizer.fullSync();

        File org = new File(DIR, OrgConverter.getTitleAsFilename
                (list1));
        // Make sure original file is there
        assertTrue(org.exists());

        // Rename the list
        list1.title = "RenamedList";
        list1.save(getContext());

        // Sync it
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // Make sure rename was successful
        assertFalse(org.exists());

        File renamed = new File(DIR, OrgConverter.getTitleAsFilename
                (list1));
        assertTrue(renamed.exists());
    }

    /**
     * Deleting a list should delete the corresponding file and all tasks.
     * Tested branches:
     * - Delete File Db
     */
    public void testDeletedList() throws IOException, ParseException {
        // Setup simple DB
        final int taskCount = 2;
        testFreshSimple();

        // Delete list(s)
        File file = null;
        ArrayList<TaskList> lists = getTaskLists();
        for (TaskList list: lists) {
            file = new File(DIR, OrgConverter.getTitleAsFilename(list));

            list.delete(getContext());
        }

        assertNotNull(file);
        // Make sure it exists at this point
        assertTrue(file.exists());
        // And that the database still has a record of it
        ArrayList<RemoteTaskList> remoteLists = getRemoteTaskLists();
        assertEquals("Should be one RemoteList!", 1, remoteLists.size());

        ArrayList<RemoteTask> remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly 2 RemoteTasks", taskCount, remoteTasks.size());

        // Sync it again
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());
        synchronizer.fullSync();

        // Check that the database has removed it
        lists = getTaskLists();
        assertTrue("Should be no list", lists.isEmpty());

        remoteLists = getRemoteTaskLists();
        assertTrue("Should be no RemoteList!", remoteLists.isEmpty());

        remoteTasks = getRemoteTasks();
        assertTrue("Should be no RemoteTasks", remoteTasks.isEmpty());

        // Make sure no file exists anymore
        assertFalse(file.exists());
    }

    /** Test moving 1 task from List A to List B
     *
     */
    public void testMoveOne() throws IOException, ParseException {
        // First create Two lists
        TaskList listA = new TaskList();
        listA.title = "TestListA";
        listA.save(getContext());
        assertTrue(listA._id > 0);

        TaskList listB = new TaskList();
        listB.title = "TestListB";
        listB.save(getContext());
        assertTrue(listB._id > 0);

        // Add one task in ListA
        Task t = new Task();
           t.dblist = listA._id;
            t.title = "Task";
            t.note = "A body for the task";
            t.save(getContext());
            assertTrue(t._id > 0);

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());

        synchronizer.fullSync();

        // Check state of sync
        ArrayList<RemoteTaskList> remoteLists = getRemoteTaskLists();
        assertEquals("Should be two RemoteLists!", 2, remoteLists.size());

        ArrayList<RemoteTask> remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly 1 RemoteTask", 1, remoteTasks.size());

        assertEquals("RemoteTask is in wrong list!", listA._id,
                (long) remoteTasks.get(0).listdbid);

        // Move the task
        t.dblist = listB._id;
        t.save(getContext());

        // Trigger should have deleted remotes now
        remoteTasks = getRemoteTasks();
        for (RemoteTask rt: remoteTasks) {
            assertEquals("RemoteTask should be deleted after move before sync", "deleted", rt.deleted);
        }

        // Sync it
        synchronizer.fullSync();

        // Check state of sync
        remoteLists = getRemoteTaskLists();
        assertEquals("Should be two RemoteLists after move!", 2, remoteLists.size());

        remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly 1 RemoteTask after move", 1, remoteTasks.size());

        assertEquals("RemoteTask is in wrong list after move!", listB._id,
                (long) remoteTasks.get(0).listdbid);
    }

    /** Test moving 20 tasks from List A to List B
     *
     */
    public void testMoveMany() throws IOException, ParseException {
        // First create Two lists
        TaskList listA = new TaskList();
        listA.title = "TestListA";
        listA.save(getContext());
        assertTrue(listA._id > 0);

        TaskList listB = new TaskList();
        listB.title = "TestListB";
        listB.save(getContext());
        assertTrue(listB._id > 0);

        final int taskCount = 20;
        ArrayList<Task> tasks = new ArrayList<Task>();
        for (int i = 0; i < taskCount; i++) {
            Task t = new Task();
            t.dblist = listA._id;
            t.title = "Task" + i;
            t.note = "A body for the task";
            t.save(getContext());
            assertTrue(t._id > 0);

            tasks.add(t);
        }

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());

        synchronizer.fullSync();

        // Check state of sync
        ArrayList<RemoteTaskList> remoteLists = getRemoteTaskLists();
        assertEquals("Should be two RemoteLists!", 2, remoteLists.size());

        ArrayList<RemoteTask> remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly x RemoteTask", taskCount, remoteTasks.size());

        for (RemoteTask remoteTask: remoteTasks) {
            assertEquals("RemoteTask is in wrong list!", listA._id,
                    (long) remoteTask.listdbid);
        }

        // Move the tasks
        for (Task t: tasks) {
            t.dblist = listB._id;
            t.save(getContext());
        }

        // Trigger should have deleted remotes now
        remoteTasks = getRemoteTasks();
        for (RemoteTask rt: remoteTasks) {
            assertEquals("RemoteTask should be deleted after move before sync", "deleted", rt.deleted);
        }

        // Sync it
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // Check state of sync
        remoteLists = getRemoteTaskLists();
        assertEquals("Should be two RemoteLists after move!", 2, remoteLists.size());

        remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly x RemoteTask after move and sync", taskCount, remoteTasks.size());

        for (RemoteTask remoteTask: remoteTasks) {
            assertEquals("RemoteTask is in wrong list after move!", listB._id,
                    (long) remoteTask.listdbid);
        }
    }

    /** Test moving 12 tasks from List A to List B where there are 12 lists each with 20 tasks
     *
     */
    public void testMoveManyAmongMany() throws IOException, ParseException {
        final int listCount = 12;
        final int taskCount = 20;
        final int movedTaskCount = 12;
        ArrayList<Task> tasksToMove = new ArrayList<Task>();
        TaskList listA = null, listB = null;
        // First create Lists
        for (int listIndex = 0; listIndex < listCount; listIndex++) {
            TaskList list = new TaskList();
            list.title = "TestList" + listIndex;
            list.save(getContext());
            assertTrue(list._id > 0);

            if (listA == null)
                listA = list;
            else if (listB == null)
                listB = list;

            for (int i = 0; i < taskCount; i++) {
                Task t = new Task();
                t.dblist = list._id;
                t.title = "Task" + listIndex + "." + i;
                t.note = "A body for the task";
                t.save(getContext());
                assertTrue(t._id > 0);

                if (tasksToMove.size() < movedTaskCount) {
                    tasksToMove.add(t);
                }
            }
        }

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());

        synchronizer.fullSync();

        // Check state of sync
        ArrayList<RemoteTaskList> remoteLists = getRemoteTaskLists();
        assertEquals("Should be X RemoteLists!", listCount, remoteLists.size());

        ArrayList<RemoteTask> remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly x RemoteTask", taskCount*listCount, remoteTasks.size());

        // Move the tasks
        assertNotNull(listA);
        assertNotNull(listB);
        assertTrue("List A and B should be different!", listA._id != listB._id);
        assertEquals("Expected something to move", movedTaskCount, tasksToMove.size());
        for (Task t: tasksToMove) {
            assertEquals("Expected task to be in list A!", listA._id, (long) t.dblist);
            t.dblist = listB._id;
            t.save(getContext());
        }

        // Trigger should have deleted remotes now
        remoteTasks = getRemoteTasks();
        int deletecount = 0;
        int realcount = 0;
        for (RemoteTask rt: remoteTasks) {
            if ("deleted".equals(rt.deleted)) {
                deletecount += 1;
            } else {
                realcount += 1;
            }
        }

        assertEquals("Deleted remotetasks did not match", movedTaskCount, deletecount);
        assertEquals("Remaining remotetasks did not match", taskCount*listCount - movedTaskCount, realcount);

        // Sync it
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // Check state of sync
        ArrayList<RemoteTaskList> remoteTaskLists = getRemoteTaskLists();
        remoteTasks = getRemoteTasks();
        deletecount = 0;
        realcount = 0;
        for (RemoteTask rt: remoteTasks) {
            if ("deleted".equals(rt.deleted)) {
                deletecount += 1;
            } else {
                realcount += 1;
            }
        }

        assertEquals("Number of remote lits did not match", listCount, remoteTaskLists.size());
        assertEquals("Deleted remotetasks did not match", 0, deletecount);
        assertEquals("Remaining remotetasks did not match", taskCount*listCount, realcount);

        int nowInB = 0;
        for (RemoteTask remoteTask: remoteTasks) {
            assertTrue(!"deleted".equals(remoteTask.deleted));
            if (remoteTask.listdbid == listB._id) {
                nowInB += 1;
            }
        }
        assertEquals("RemoteTasks in b not expected count", taskCount + movedTaskCount,
                nowInB);

        // Check same things for local tasks
        ArrayList<TaskList> taskLists = getTaskLists();
        assertEquals("Number of lists did not match", listCount, taskLists.size());
        for (TaskList list: taskLists) {
            ArrayList<Task> tasks = getTasks(list._id);
            if (listA._id == list._id) {
                assertEquals("Not expected count in A", taskCount - movedTaskCount, tasks.size());
            } else if (listB._id == list._id) {
                assertEquals("Not expected count in B", taskCount + movedTaskCount, tasks.size());
            } else {
                assertEquals("Not expected count in C->", taskCount, tasks.size());
            }
        }
    }

    public void testFilenameWithSlash() {
        // Filenames with slashes are not permitted
        final TaskList lista = new TaskList();
        lista.title = "Test/List/Slash/Name";
        lista.save(getContext());
        assertTrue(lista._id > 0);

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());

        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // Check contents after sync
        final TaskList listb = getTaskLists().get(0);

        // Should no longer have slashes in name
        assertTrue(!listb.title.contains("/"));
        assertEquals("Test_List_Slash_Name", listb.title);
    }

    public void testContentStability() {
        // Make sure content is not changed
        // Create list
        final TaskList lista = new TaskList();
        lista.title = "TestList";
        lista.save(getContext());
        assertTrue(lista._id > 0);

        final Task task1a = new Task();
        task1a.title = "The title1";
        task1a.note = "A note without newline";
        task1a.dblist = lista._id;
        task1a.save(getContext());
        assertTrue(task1a._id > 0);

        final Task task2a = new Task();
        task2a.title = "The title2";
        task2a.note = "Another note\non two lines";
        task2a.dblist = lista._id;
        task2a.save(getContext());
        assertTrue(task2a._id > 0);

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());

        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // Check contents after sync
        final TaskList listb = getTaskLists().get(0);

        assertEquals(lista.title, listb.title);


        for (Task taskb: getTasks(listb._id)) {
            Task org;
            if (taskb._id == task1a._id) {
                org = task1a;
            } else {
                org = task2a;
            }
            // Compare title and note
            assertEquals(org.title, taskb.title);
            assertEquals(org.note, taskb.note);
        }

        // Sync it again
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

        // Check contents after sync
        final TaskList listc = getTaskLists().get(0);

        assertEquals(lista.title, listc.title);

        for (Task taskc: getTasks(listb._id)) {
            Task org;
            if (taskc._id == task1a._id) {
                org = task1a;
            } else {
                org = task2a;
            }
            // Compare title and note
            assertEquals(org.title, taskc.title);
            assertEquals(org.note, taskc.note);
        }
    }

    class TestSynchronizer extends SDSynchronizer {

        private int putRemoteCount = 0;

        public TestSynchronizer(Context context) {
            super(context);

            ORG_DIR = OrgSyncTest.DIR;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        /**
         * @return A unique name for this service. Should be descriptive, like
         * DropboxOrg, SDOrg or SSHOrg.
         */
        @Override
        public String getServiceName() {
            return ACCOUNT;
        }

        @Override
        public String getAccountName() {
            return ACCOUNT;
        }

        /**
         * Replaces the file on the remote end with the given content.
         *
         * @param orgFile The file to save. Uses the filename stored in the object.
         */
        @Override
        public void putRemoteFile(OrgFile orgFile) throws IOException {
            putRemoteCount += 1;
            super.putRemoteFile(orgFile);
        }

        public int getPutRemoteCount() {
            return putRemoteCount;
        }

        public void setPutRemoteCount(final int putRemoteCount) {
            this.putRemoteCount = putRemoteCount;
        }
    }
}
