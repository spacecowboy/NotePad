package com.nononsenseapps.notepad.sync.orgsync;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;

import org.cowboyprogrammer.org.OrgFile;

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
	public String getServiceName();
	
	/**
	 * 
	 * @return The usename of the configured service. Likely an e-mail.
	 */
	public String getAccountName();

	/**
	 * Returns true if the synchronizer has been configured. This is called
	 * before synchronization. It will be true if the user has selected an
	 * account, folder etc...
	 */
	public boolean isConfigured();

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
	public void deleteRemoteFile(final OrgFile orgFile);

	/**
	 * Rename the file on the remote end.
	 * 
	 * @param oldName
	 *            The name it is currently stored as on the remote end.
	 * @param orgFile
	 *            This contains the new name.
	 */
	public void renameRemoteFile(final String oldName, final OrgFile orgFile);

	/**
	 * Returns a BufferedReader to the remote file. Null if it doesn't exist.
	 * 
	 * @param filename
	 *            Name of the file, without path
	 */
	public BufferedReader getRemoteFile(final String filename);
	
	/**
	 * 
	 * @return a set of all remote files.
	 */
	public HashSet<String> getRemoteFilenames();
	
	/**
	 * Do a full 2-way sync.
	 */
	public void fullSync() throws IOException, ParseException;
	
	/**
	 * Use this to disconnect from any services and cleanup.
	 */
	public void postSynchronize();
}
