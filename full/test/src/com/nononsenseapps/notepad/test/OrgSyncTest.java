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

    class TestSynchronizer extends Synchronizer {

        private final String DIR;
        private int putRemoteCount = 0;

        public TestSynchronizer(Context context) {
            super(context);

            DIR = OrgSyncTest.DIR;
        }

        /**
         * @return A unique name for this service. Should be descriptive, like
         * DropboxOrg, SDOrg or SSHOrg.
         */
        @Override
        public String getServiceName() {
            return "TestSync";
        }

        /**
         * @return The usename of the configured service. Likely an e-mail.
         */
        @Override
        public String getAccountName() {
            return ACCOUNT;
        }

        /**
         * Returns true if the synchronizer has been configured. This is called
         * before synchronization. It will be true if the user has selected an
         * account, folder etc...
         */
        @Override
        public boolean isConfigured() {
            return true;
        }

        /**
         * Returns an OrgFile object with a filename set that is guaranteed to
         * not already exist. Use this method to avoid having multiple objects
         * pointing to the same file.
         *
         * @param desiredName The name you'd want, minus the ".org" suffix. If
         *                    it exists, it will be used as the base in
         *                    desiredName1.org, desiredName2.org,
         *                    etc. Limited to 99.
         * @return an OrgFile guaranteed not to exist.
         * @throws java.io.IOException
         */
        @Override
        public OrgFile getNewFile(final String desiredName) throws
                IOException, IllegalArgumentException {
            String filename;
            for (int i = 0; i < 100; i++) {
                if (i == 0) {
                    filename = desiredName + ".org";
                } else {
                    filename = desiredName + i + ".org";
                }
                File f = new File(DIR, filename);
                if (!f.exists()) {
                    return new OrgFile(filename);
                }
            }
            throw new IllegalArgumentException("Filename not accessible");
        }

        /**
         * Replaces the file on the remote end with the given content.
         *
         * @param orgFile The file to save. Uses the filename stored in the object.
         */
        @Override
        public void putRemoteFile(OrgFile orgFile) throws IOException {
            putRemoteCount += 1;
            final File file = new File(DIR, orgFile.getFilename());
            final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(orgFile.treeToString());
            bw.close();
        }

        /**
         * Delete the file on the remote end.
         *
         * @param orgFile The file to delete.
         */
        @Override
        public void deleteRemoteFile(OrgFile orgFile) {
            final File file = new File(DIR, orgFile.getFilename());
            file.delete();
        }

        /**
         * Rename the file on the remote end.
         *
         * @param oldName The name it is currently stored as on the remote end.
         * @param orgFile
         */
        @Override
        public void renameRemoteFile(String oldName, OrgFile orgFile) {
            final File oldFile = new File(DIR, oldName);
            final File newFile = new File(DIR, orgFile.getFilename());
            oldFile.renameTo(newFile);
        }

        /**
         * Returns a BufferedReader to the remote file. Null if it doesn't exist.
         *
         * @param filename Name of the file, without path
         */
        @Override
        public BufferedReader getRemoteFile(String filename) {
            final File file = new File(DIR, filename);
            BufferedReader br = null;
            if (file.exists()) {
                try {
                    br = new BufferedReader(new FileReader(file));
                } catch (FileNotFoundException e) {
                    br = null;
                }
            }

            return br;
        }

        /**
         * @return a set of all remote files.
         */
        @Override
        public HashSet<String> getRemoteFilenames() {
            final HashSet<String> filenames = new HashSet<String>();
            final File dir = new File(DIR);
            final File[] files = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".org");
                }
            });

            if (files != null) {
                for (File f : files) {
                    filenames.add(f.getName());
                }
            }

            return filenames;
        }

        /**
         * Use this to disconnect from any services and cleanup.
         */
        @Override
        public void postSynchronize() {

        }

        public int getPutRemoteCount() {
            return putRemoteCount;
        }

        public void setPutRemoteCount(final int putRemoteCount) {
            this.putRemoteCount = putRemoteCount;
        }
    }
}
