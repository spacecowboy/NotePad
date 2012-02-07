package com.nononsenseapps.notepad.sync.googleapi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Helper class which talks to the database and converts the responses into Task and List objects
 */
public class GoogleDBTalker {
	
	private static final String[] NOTE_PROJECTION = new String[] {
		NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
		NotePad.Notes.COLUMN_NAME_NOTE,
		NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
	
	
	
	protected String accountName;
	protected ContentProviderClient provider;
	protected SyncResult syncResult;
	
	/**
	 * Needs the accountName of the currently active sync account because it in essence supports several
	 */
	public GoogleDBTalker(String accountName, ContentProviderClient provider, SyncResult syncResult) {
		this.accountName = accountName;
		this.provider = provider;
		this.syncResult = syncResult;
	}
	
	/**
	 * Gets all tasks with a modified flag in the database.
	 */
	public ArrayList<GoogleTask> getModifiedTasks() {
		return null;
	}
	
	/**
	 * Gets all lists with a modified flag set to the specified value in the database
	 */
	public ArrayList<GoogleTaskList> getModifiedLists(boolean modified) {
		return null;
	}
	
	/**
	 * Clears modified flag and saves the new fields
	 */
	public void uploaded(GoogleTask task) {
		
	}
	
	/**
	 * Clears modified flag and saves the new fields
	 */
	public void clearModifiedFlag(GoogleTaskList list) {
		
	}
	
	/**
	 * Given a task, it will find the corresponding one in the database and update it.
	 * If it can not find it in the db, it will insert it.
	 * 
	 * Will clear the modified flag.
	 */
	public void SaveToDatabase(GoogleTask task) {
		// if exists
		syncResult.stats.numUpdates++;
		// else
		syncResult.stats.numInserts++;
		
		
		syncResult.stats.numEntries++;
	}
	
	/**
	 * Given a list, it will find the corresponding one in the database and update it.
	 * If it can not find it in the db, it will insert it.
	 * 
	 * Will clear the modified flag.
	 */
	public void SaveToDatabase(GoogleTaskList list) {
		// if exists
		syncResult.stats.numUpdates++;
		// else
		syncResult.stats.numInserts++;
		
		
		syncResult.stats.numEntries++;
	}
	
	/**
	 * Will erase items marked as deleted in the database. Make sure you synced with the
	 * server before calling this or the server will never know that they were deleted.
	 * 
	 * Erases both tasks and lists.
	 */
	public void clearDeleted() {
		// For all tasks
			// Erase Task
			// Erase account entries
		// For all lists
			// Erase List
			// Erase account entries
	}
}