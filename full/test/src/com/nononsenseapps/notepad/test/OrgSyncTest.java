package com.nononsenseapps.notepad.test;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
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

        File d =  new File(Environment.getExternalStorageDirectory().getPath(),
                "ORGSYNCTEST");

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
        for (File f: d.listFiles()) {
            f.delete();
        }
    }

    public ArrayList<RemoteTaskList> getRemoteTaskLists() {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor c = resolver.query(RemoteTaskList.URI, RemoteTaskList.Columns
                        .FIELDS, RemoteTaskList.Columns.ACCOUNT + " IS ?",
                new String[] {ACCOUNT}, null);

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
                new String[] {ACCOUNT}, null);

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
        for (String f: filenames) {
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

        // Check that the database is still correct
        ArrayList<RemoteTaskList> remoteLists = getRemoteTaskLists();
        assertEquals("Should only be one RemoteList!", 1, remoteLists.size());

        ArrayList<RemoteTask> remoteTasks = getRemoteTasks();
        assertEquals("Should be exactly 2 RemoteTasks", taskCount, remoteTasks.size());
    }

    class TestSynchronizer extends Synchronizer {

        private final String DIR;

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
         * Replaces the file on the remote end with the given content.
         *
         * @param orgFile The file to save. Uses the filename stored in the object.
         */
        @Override
        public void putRemoteFile(OrgFile orgFile) throws IOException {
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
    }
}
