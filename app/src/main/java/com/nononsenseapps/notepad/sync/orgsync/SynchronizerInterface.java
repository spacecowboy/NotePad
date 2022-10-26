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
	 *         SDOrg or SSHOrg.
	 */
	public String getServiceName();
	
	/**
	 * 
	 * @return The username of the configured service. Likely an e-mail.
	 */
	public String getAccountName();

	/**
	 * Returns true if the synchronizer has been configured. This is called
	 * before synchronization. It will be true if the user has selected an
	 * account, folder etc...
	 */
	public boolean isConfigured();

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
    public OrgFile getNewFile(final String desiredName) throws IOException,
            IllegalArgumentException;

	/**
	 * Replaces the file on the remote end with the given content.
	 * 
	 * @param orgFile
	 *            The file to save. Uses the filename stored in the object.
	 */
	public void putRemoteFile(final OrgFile orgFile) throws IOException;

	/**
	 * Delete the file on the remote end.
	 * 
	 * @param orgFile
	 *            The file to delete.
	 */
	public void deleteRemoteFile(final OrgFile orgFile) throws IOException;

	/**
	 * Rename the file on the remote end.
	 * 
	 * @param oldName
	 *            The name it is currently stored as on the remote end.
	 * @param orgFile
	 *            This contains the new name.
	 */
	public void renameRemoteFile(final String oldName, final OrgFile orgFile) throws IOException;

	/**
	 * Returns a BufferedReader to the remote file. Null if it doesn't exist.
	 * 
	 * @param filename
	 *            Name of the file, without path
	 */
	public BufferedReader getRemoteFile(final String filename) throws IOException;
	
	/**
	 * 
	 * @return a set of all remote files.
	 */
	public HashSet<String> getRemoteFilenames() throws IOException;
	
	/**
	 * Do a full 2-way sync.
	 */
	public void fullSync() throws IOException, ParseException;
	
	/**
	 * Use this to disconnect from any services and cleanup.
	 */
	public void postSynchronize();

    /**
     *
     * @return a Monitor for this source. May be null.
     */
    public Monitor getMonitor();
}
