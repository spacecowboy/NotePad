package com.nononsenseapps.notepad.test;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.notepad.database.RemoteTask;
import com.nononsenseapps.notepad.database.RemoteTaskList;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.sync.orgsync.OrgConverter;
import com.nononsenseapps.notepad.sync.orgsync.SDSynchronizer;
import com.nononsenseapps.notepad.sync.orgsync.Synchronizer;

import org.cowboyprogrammer.org.OrgFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

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
        super.setUp();

        File d = getContext().getDir("ORGSYNCTEST", Context.MODE_PRIVATE);

        DIR = d.getPath();

        if (!d.exists()) {
            d.mkdirs();
        }
    }

    @Override
    public void tearDown() {
        ContentResolver resolver = getContext().getContentResolver();
        resolver.delete(TaskList.URI, null, null);
        resolver.delete(Task.URI, null, null);
        resolver.delete(RemoteTaskList.URI, null, null);
        resolver.delete(RemoteTask.URI, null, null);

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
    }

    /**
     * Having two lists with the same name is possible in the app,
     * but obviously impossible at the filesystem level.
     * Tested flow branches:
     * - Lists: Create file
     */
    public void testDuplicateName() {
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
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

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
    public void testRenamedList() {
        // Create first list
        TaskList list1 = new TaskList();
        list1.title = "TestList";
        list1.save(getContext());
        assertTrue(list1._id > 0);

        // Sync it
        TestSynchronizer synchronizer = new TestSynchronizer(getContext());
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

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
    public void testDeletedList() {
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
        try {
            synchronizer.fullSync();
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage(), false);
        }

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
