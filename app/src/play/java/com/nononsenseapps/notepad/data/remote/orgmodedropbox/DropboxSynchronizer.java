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

package com.nononsenseapps.notepad.data.remote.orgmodedropbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.nononsenseapps.notepad.data.local.orgmode.Monitor;
import com.nononsenseapps.notepad.data.local.orgmode.Synchronizer;
import com.nononsenseapps.notepad.data.local.orgmode.SynchronizerInterface;
import com.nononsenseapps.notepad.ui.settings.SyncPrefs;

import org.cowboyprogrammer.org.OrgFile;
import org.cowboyprogrammer.org.parser.OrgParser;
import org.cowboyprogrammer.org.parser.RegexParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashSet;


public class DropboxSynchronizer extends Synchronizer implements
        SynchronizerInterface {

    // Where files are kept. User changeable in preferences.
    public static final String DEFAULT_DIR = "/NoNonsenseNotes/";
    public static final String PREF_DIR = SyncPrefs.KEY_DROPBOX_DIR;
    public static final String PREF_ENABLED = SyncPrefs.KEY_DROPBOX_ENABLE;
    public final static String SERVICENAME = "DROPBOXORG";
    protected final boolean enabled;
    private final DropboxAPI<AndroidAuthSession> mDBApi;
    private final String folderpath;
    protected DropboxAPI.Entry DIR;
    private DropboxAPI<AndroidAuthSession> dbApi = null;

    public DropboxSynchronizer(final Context context) {
        super(context);
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        enabled = prefs.getBoolean(PREF_ENABLED, false);
        mDBApi = DropboxSyncHelper.getDBApi(context);
        folderpath = prefs.getString(PREF_DIR, DEFAULT_DIR);

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
        try {
            return mDBApi.accountInfo().email;
        } catch (DropboxException e) {
            return "ERROR";
        }
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
        if (mDBApi.getSession().isLinked()) {
            // Create default dir if necessary
            try {
                DIR = mDBApi.metadata(folderpath, 1, null, false, null);
                return DIR.isDir;
            } catch (DropboxServerException e) {
                if (e.error == 404) {
                    try {
                        DIR = mDBApi.createFolder(folderpath);
                        return DIR.isDir;
                    } catch (DropboxException e1) {
                        Log.e(TAG, "isConfigured: " + e.reason);
                    }
                } else {
                    Log.e(TAG, "isConfigured: " + e.reason);
                }
            } catch (DropboxException e) {
                Log.e(TAG, "isConfigured catchall");
            }
        }
        return false;
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
        OrgParser orgParser = new RegexParser();
        String filename;
        try {
            for (int i = 0; i < 100; i++) {
                if (i == 0) {
                    filename = desiredName + ".org";
                } else {
                    filename = desiredName + i + ".org";
                }
                try {
                    DropboxAPI.Entry entry =  mDBApi.metadata(join(folderpath, filename), 1, null, false, null);
                    // If entry is returned, it exists, move to next iteration step
                } catch (DropboxServerException e) {
                    if (404 == e.error) {
                        // No such file exists, great!
                        return new OrgFile(orgParser, filename);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (DropboxServerException e) {
            throw new IOException("" + e.reason);
        } catch (DropboxException e) {
            throw new IOException();
        }
        throw new IllegalArgumentException("Filename not accessible");
    }

    private String join(String folderpath, String filename) {
        if (folderpath == null || filename == null) {
            throw new NullPointerException();
        }

        if (folderpath.endsWith("/")) {
            if (filename.startsWith("/")) {
                return folderpath + filename.substring(1);
            } else {
                return folderpath + filename;
            }
        } else {
            if (filename.startsWith("/")) {
                return folderpath + filename;
            } else {
                return folderpath + "/" + filename;
            }
        }
    }

    /**
     * Replaces the file on the remote end with the given content.
     *
     * @param orgFile The file to save. Uses the filename stored in the object.
     */
    @Override
    public void putRemoteFile(final OrgFile orgFile) throws IOException {
        try {
            byte[] bytes = orgFile.treeToString().getBytes(Charset.forName("UTF-8"));
            InputStream stream = new ByteArrayInputStream(orgFile.treeToString().getBytes(Charset.forName("UTF-8")));
            mDBApi.putFileOverwrite(join(folderpath, orgFile.getFilename()), stream, bytes.length, null);
        }  catch (DropboxServerException e) {
            throw new IOException("" + e.reason);
        } catch (DropboxException e) {
            throw new IOException("" + e.getMessage());
        }
    }

    /**
     * Delete the file on the remote end.
     *
     * @param orgFile The file to delete.
     */
    @Override
    public void deleteRemoteFile(final OrgFile orgFile) throws IOException {
        if (orgFile == null || orgFile.getFilename() == null) {
            // Nothing to do
            return;
        }
        try {
            mDBApi.delete(join(folderpath, orgFile.getFilename()));
        } catch (DropboxServerException e) {
            if (404 == e.error) {
                // This is ok, already deleted
                // ignore
            } else {
                throw new IOException("" + e.reason);
            }
        } catch (DropboxException e) {
            throw new IOException("" + e.getMessage());
        }
    }

    /**
     * Rename the file on the remote end.
     *
     * @param oldName The name it is currently stored as on the remote end.
     * @param orgFile
     */
    @Override
    public void renameRemoteFile(final String oldName, final OrgFile orgFile) throws IOException {
        if (orgFile == null || orgFile.getFilename() == null) {
            throw new NullPointerException("No new filename");
        }

        try {
            mDBApi.move(join(folderpath, oldName), join(folderpath, orgFile.getFilename()));
        }  catch (DropboxServerException e) {
            throw new IOException("" + e.reason);
        } catch (DropboxException e) {
            throw new IOException("" + e.getMessage());
        }
    }

    /**
     * Returns a BufferedReader to the remote file. Null if it doesn't exist.
     *
     * @param filename Name of the file, without path
     */
    @Override
    public BufferedReader getRemoteFile(final String filename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            mDBApi.getFile(join(folderpath, filename), null, baos, null);
            return new BufferedReader(new StringReader(baos.toString("UTF-8")));
        }  catch (DropboxServerException e) {
            throw new IOException("" + e.reason);
        } catch (DropboxException e) {
            throw new IOException("" + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new IOException("" + e.getMessage());
        }
    }

    /**
     * @return a set of all remote files.
     */
    @Override
    public HashSet<String> getRemoteFilenames() throws IOException {
        final HashSet<String> filenames = new HashSet<String>();
        try {
            DropboxAPI.Entry entry = mDBApi.metadata(folderpath, -1, null, true, null);

            for (DropboxAPI.Entry file: entry.contents) {
                if (!file.isDir && file.fileName().toLowerCase().endsWith(".org")) {
                    filenames.add(file.fileName());
                }
            }
        }  catch (DropboxServerException e) {
            throw new IOException("" + e.reason);
        } catch (DropboxException e) {
            throw new IOException("" + e.getMessage());
        }
        return filenames;
    }

    /**
     * Use this to disconnect from any services and cleanup.
     */
    @Override
    public void postSynchronize() {

    }

    /**
     * @return a Monitor for this source. May be null.
     */
    @Override
    public Monitor getMonitor() {
        return null;
    }
}
