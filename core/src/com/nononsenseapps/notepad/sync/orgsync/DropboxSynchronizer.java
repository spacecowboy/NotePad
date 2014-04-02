/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.sync.orgsync;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.nononsenseapps.build.Config;
import com.nononsenseapps.notepad.prefs.SyncPrefs;

import org.cowboyprogrammer.org.OrgFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;


public class DropboxSynchronizer extends Synchronizer implements
        SynchronizerInterface {

    // Where files are kept. User changeable in preferences.
    public static final String DEFAULT_DIR = "/NoNonsenseNotes/";
    public static final String PREF_DIR = SyncPrefs.KEY_DROPBOX_DIR;
    public static final String PREF_ENABLED = SyncPrefs.KEY_DROPBOX_ENABLE;
    public final static String SERVICENAME = "DROPBOXORG";
    protected final boolean enabled;
    protected DbxPath DIR;
    private DbxAccountManager accountManager = null;
    private DbxAccount account = null;
    private DbxFileSystem fs = null;
    private DbxFileSystem.PathListener pathListener = null;

    public DropboxSynchronizer(final Context context) {
        super(context);
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        enabled = prefs.getBoolean(PREF_ENABLED, false);
        DIR = new DbxPath(prefs.getString(PREF_DIR, DEFAULT_DIR));

    }

    /**
     * Link a DropboxAccount. Must be called from a GUI. Activity
     * receives result in OnActivityResult with the specific code. Returns
     * RESULT_OK if all is good.
     *
     * @param activity
     * @param requestCode
     */
    public static void linkAccount(final Activity activity,
                                   final int requestCode) {
        final String APP_KEY = Config.getKeyDropboxSyncPublic(activity);
        final String APP_SECRET = Config.getKeyDropboxSyncSecret(activity);
        DbxAccountManager.getInstance(activity.getApplicationContext(),
                APP_KEY, APP_SECRET).startLink(activity, requestCode);
    }

    /**
     * Link a DropboxAccount. Must be called from a GUI. Activity
     * receives result in OnActivityResult with the specific code. Returns
     * RESULT_OK if all is good.
     *
     * @param fragment
     * @param requestCode
     */
    public static void linkAccount(final Fragment fragment,
                                   final int requestCode) {
        final String APP_KEY = Config.getKeyDropboxSyncPublic(fragment.getActivity());
        final String APP_SECRET = Config.getKeyDropboxSyncSecret(fragment.getActivity());
        DbxAccountManager.getInstance(fragment.getActivity().getApplicationContext(),
                APP_KEY, APP_SECRET).startLink(fragment, requestCode);
    }

    /**
     * Unlink a Dropbox Connection.
     *
     * @param activity
     */
    public static void unlink(final Activity activity) {
        final String APP_KEY = Config.getKeyDropboxSyncPublic(activity);
        final String APP_SECRET = Config.getKeyDropboxSyncSecret(activity);
        DbxAccountManager.getInstance(activity.getApplicationContext(),
                APP_KEY, APP_SECRET).unlink();
    }

    /**
     * @return A unique name for this service. Should be descriptive, like
     * DropboxOrg, SDOrg or SSHOrg.
     */
    @Override
    public String getServiceName() {
        return SERVICENAME;
    }

    /**
     * @return The username of the configured service. Likely an e-mail.
     */
    @Override
    public String getAccountName() {
        return accountManager.getLinkedAccount().getUserId();
    }

    /**
     * Returns true if the synchronizer has been configured. This is called
     * before synchronization. It will be true if the user has selected an
     * account, folder etc...
     */
    @Override
    public boolean isConfigured() {
        if (!enabled) return false;

        // Need to ask dropbox if we are linked.
        if (accountManager == null) {
            final String APP_KEY = Config.getKeyDropboxSyncPublic(context);
            final String APP_SECRET = Config.getKeyDropboxSyncSecret(context);
            accountManager = DbxAccountManager.getInstance(context
                    .getApplicationContext(), APP_KEY, APP_SECRET);

            if (accountManager.hasLinkedAccount()) {
                account = accountManager.getLinkedAccount();
                try {
                    fs = DbxFileSystem.forAccount(account);
                    if (!fs.isFolder(DIR)) {
                        fs.createFolder(DIR);
                    }
                    pathListener = new DbxFileSystem.PathListener() {
                        @Override
                        public void onPathChange(DbxFileSystem fs, DbxPath registeredPath, Mode registeredMode) {
                            Log.d(TAG, "Dropbox change detected");
                        }
                    };
                    fs.addPathListener(pathListener, DIR,
                            DbxFileSystem.PathListener.Mode.PATH_OR_CHILD);

                    fs.syncNowAndWait();
                } catch (DbxException.Unauthorized unauthorized) {
                    return false;
                } catch (DbxException e) {
                    return false;
                }
            }
        }

        return accountManager.hasLinkedAccount();
    }

    /**
     * Returns an OrgFile object with a filename set that is guaranteed to
     * not already exist. Use this method to avoid having multiple objects
     * pointing to the same file.
     *
     * @param desiredName The name you'd want. If it exists,
     *                    it will be used as the base in desiredName1,
     *                    desiredName2, etc. Limited to 99.
     * @return an OrgFile guaranteed not to exist.
     * @throws java.io.IOException
     * @throws IllegalArgumentException
     */
    @Override
    public OrgFile getNewFile(final String desiredName) throws IOException, IllegalArgumentException {
        if (desiredName.contains("/")) {
            throw new IOException("Filename can't contain /");
        }
        String filename;
        try {
            for (int i = 0; i < 100; i++) {
                if (i == 0) {
                    filename = desiredName + ".org";
                } else {
                    filename = desiredName + i + ".org";
                }
                if (!fs.exists(new DbxPath(DIR, filename))) {
                    return new OrgFile(filename);
                }
            }
        } catch (DbxPath.InvalidPathException e) {
            throw new IOException(e);
        }
        throw new IllegalArgumentException("Filename not accessible");
    }

    /**
     * Replaces the file on the remote end with the given content.
     *
     * @param orgFile The file to save. Uses the filename stored in the object.
     */
    @Override
    public void putRemoteFile(final OrgFile orgFile) throws IOException {
        DbxPath path = new DbxPath(DIR, orgFile.getFilename());
        try {
            DbxFile file;
            try {
                file = fs.open(path);
            } catch (DbxException.NotFound e) {
                file = fs.create(path);
            }
            file.writeString(orgFile.treeToString());
            file.close();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    /**
     * Delete the file on the remote end.
     *
     * @param orgFile The file to delete.
     */
    @Override
    public void deleteRemoteFile(final OrgFile orgFile) {
        if (orgFile == null) {
            // Nothing to do
            return;
        }
        DbxPath path = new DbxPath(DIR, orgFile.getFilename());
        try {
            fs.delete(path);
        } catch (DbxException e) {
            //e.printStackTrace();
        }
    }

    /**
     * Rename the file on the remote end.
     *
     * @param oldName The name it is currently stored as on the remote end.
     * @param orgFile
     */
    @Override
    public void renameRemoteFile(final String oldName, final OrgFile orgFile) {
        DbxPath newPath = new DbxPath(DIR, orgFile.getFilename());
        DbxPath oldPath = new DbxPath(DIR, oldName);

        try {
            fs.move(oldPath, newPath);
        } catch (DbxException e) {
            //e.printStackTrace();
        }
    }

    /**
     * Returns a BufferedReader to the remote file. Null if it doesn't exist.
     *
     * @param filename Name of the file, without path
     */
    @Override
    public BufferedReader getRemoteFile(final String filename) {
        DbxPath path = new DbxPath(DIR, filename);
        BufferedReader br = null;
        try {
            if (fs.isFile(path)) {
                DbxFile file = fs.open(path);
                /*
                while (!file.getSyncStatus().isLatest) {
                    // TODO wait on downloads?
                    Log.d(TAG, "Waiting on download: " + file.getNewerStatus
                            ().bytesTransferred + " / " + file.getNewerStatus
                            ().bytesTotal);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                       // e.printStackTrace();
                    }
                }
                file.update();*/
                br = new BufferedReader(new StringReader(file.readString()));
                file.close();
            }
        } catch (DbxException e) {
            Log.d(TAG, e.getLocalizedMessage());
            br = null;
        } catch (IOException e) {
            Log.d(TAG, e.getLocalizedMessage());
            br = null;
        }

        return br;
    }

    /**
     * @return a set of all remote files.
     */
    @Override
    public HashSet<String> getRemoteFilenames() {
        final HashSet<String> filenames = new HashSet<String>();
        try {
            while (fs.getSyncStatus().download.inProgress) {
                Log.d(TAG, "Waiting on Dropbox sync...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            List<DbxFileInfo> fileInfos = fs.listFolder(DIR);
            for (DbxFileInfo fileInfo : fileInfos) {
                if (fileInfo.path.getName().toLowerCase().endsWith((".org"))) {
                    if (fs.isFile(fileInfo.path)) {
                        Log.d(TAG, "Adding: " + fileInfo.path.getName());
                        filenames.add(fileInfo.path.getName());
                    } else {
                        Log.d(TAG, "Caught invalid file: " + fileInfo.path
                                .getName());
                    }
                }
            }
        } catch (DbxException e) {
            Log.d(TAG, e.getLocalizedMessage());
            //e.printStackTrace();
        }
        return filenames;
    }

    /**
     * Use this to disconnect from any services and cleanup.
     */
    @Override
    public void postSynchronize() {
        fs.removePathListener(pathListener, DIR, DbxFileSystem.PathListener
                .Mode.PATH_OR_CHILD);
    }
}
