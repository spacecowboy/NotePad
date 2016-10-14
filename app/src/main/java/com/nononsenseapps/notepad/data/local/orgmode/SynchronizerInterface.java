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

package com.nononsenseapps.notepad.data.local.orgmode;

import org.cowboyprogrammer.org.OrgFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;

/**
 * This interface defines an Org-Mode synchronizer.
 * 
 */
public interface SynchronizerInterface {

	/**
	 * 
	 * @return A unique name for this service. Should be descriptive, like
	 *         DropboxOrg, SDOrg or SSHOrg.
	 */
	String getServiceName();
	
	/**
	 * 
	 * @return The username of the configured service. Likely an e-mail.
	 */
	String getAccountName();

	/**
	 * Returns true if the synchronizer has been configured. This is called
	 * before synchronization. It will be true if the user has selected an
	 * account, folder etc...
	 */
	boolean isConfigured();

    /**
     * Returns an OrgFile object with a filename set that is guaranteed to
     * not already exist. Use this method to avoid having multiple objects
     * pointing to the same file.
     *
     * @param desiredName The name you'd want. If it exists,
     *                    it will be used as the base in desiredName1,
     *                    desiredName2, etc. Limited to 99.
     * @return an OrgFile guaranteed not to exist.
     * @throws IOException
     * @throws IllegalArgumentException
     */
	OrgFile getNewFile(final String desiredName) throws IOException,
            IllegalArgumentException;

	/**
	 * Replaces the file on the remote end with the given content.
	 * 
	 * @param orgFile
	 *            The file to save. Uses the filename stored in the object.
	 */
	void putRemoteFile(final OrgFile orgFile) throws IOException;

	/**
	 * Delete the file on the remote end.
	 * 
	 * @param orgFile
	 *            The file to delete.
	 */
	void deleteRemoteFile(final OrgFile orgFile) throws IOException;

	/**
	 * Rename the file on the remote end.
	 * 
	 * @param oldName
	 *            The name it is currently stored as on the remote end.
	 * @param orgFile
	 *            This contains the new name.
	 */
	void renameRemoteFile(final String oldName, final OrgFile orgFile) throws IOException;

	/**
	 * Returns a BufferedReader to the remote file. Null if it doesn't exist.
	 * 
	 * @param filename
	 *            Name of the file, without path
	 */
	BufferedReader getRemoteFile(final String filename) throws IOException;
	
	/**
	 * 
	 * @return a set of all remote files.
	 */
	HashSet<String> getRemoteFilenames() throws IOException;
	
	/**
	 * Do a full 2-way sync.
	 */
	void fullSync() throws IOException, ParseException;
	
	/**
	 * Use this to disconnect from any services and cleanup.
	 */
	void postSynchronize();

    /**
     *
     * @return a Monitor for this source. May be null.
     */
	Monitor getMonitor();
}
