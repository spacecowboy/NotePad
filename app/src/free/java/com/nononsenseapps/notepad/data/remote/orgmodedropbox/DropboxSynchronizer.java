/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.data.remote.orgmodedropbox;

import android.app.Activity;

import com.nononsenseapps.notepad.data.local.orgmode.Monitor;
import com.nononsenseapps.notepad.data.local.orgmode.SynchronizerInterface;
import com.nononsenseapps.notepad.data.service.OrgSyncService;
import com.nononsenseapps.notepad.ui.settings.SyncPrefs;

import org.cowboyprogrammer.org.OrgFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;

/**
 * Dummy class. See play flavor.
 */
public class DropboxSynchronizer implements SynchronizerInterface {
    public static final String DEFAULT_DIR = "DUMMY";

    public DropboxSynchronizer(OrgSyncService orgSyncService) {

    }

    public static void linkAccount(SyncPrefs syncPrefs, int dropboxLinkCode) {

    }

    public static void unlink(Activity activity) {

    }

    /**
     * @return A unique name for this service. Should be descriptive, like
     * DropboxOrg, SDOrg or SSHOrg.
     */
    @Override
    public String getServiceName() {
        return null;
    }

    /**
     * @return The username of the configured service. Likely an e-mail.
     */
    @Override
    public String getAccountName() {
        return null;
    }

    /**
     * Returns true if the synchronizer has been configured. This is called
     * before synchronization. It will be true if the user has selected an
     * account, folder etc...
     */
    @Override
    public boolean isConfigured() {
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
    public OrgFile getNewFile(String desiredName) throws IOException, IllegalArgumentException {
        return null;
    }

    /**
     * Replaces the file on the remote end with the given content.
     *
     * @param orgFile The file to save. Uses the filename stored in the object.
     */
    @Override
    public void putRemoteFile(OrgFile orgFile) throws IOException {

    }

    /**
     * Delete the file on the remote end.
     *
     * @param orgFile The file to delete.
     */
    @Override
    public void deleteRemoteFile(OrgFile orgFile) {

    }

    /**
     * Rename the file on the remote end.
     *
     * @param oldName The name it is currently stored as on the remote end.
     * @param orgFile
     */
    @Override
    public void renameRemoteFile(String oldName, OrgFile orgFile) {

    }

    /**
     * Returns a BufferedReader to the remote file. Null if it doesn't exist.
     *
     * @param filename Name of the file, without path
     */
    @Override
    public BufferedReader getRemoteFile(String filename) {
        return null;
    }

    /**
     * @return a set of all remote files.
     */
    @Override
    public HashSet<String> getRemoteFilenames() {
        return null;
    }

    /**
     * Do a full 2-way sync.
     */
    @Override
    public void fullSync() throws IOException, ParseException {

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
