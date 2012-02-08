package com.nononsenseapps.notepad.sync.googleapi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotePadProvider;

/**
 * Helper class which talks to the database and converts the responses into Task
 * and List objects
 */
public class GoogleDBTalker {

	private static final String[] LIST_PROJECTION = new String[] {
			NotePad.Lists._ID, NotePad.Lists.COLUMN_NAME_TITLE,
			NotePad.Lists.COLUMN_NAME_DELETED,
			NotePad.Lists.COLUMN_NAME_MODIFIED };
	private static final String[] GTASKLIST_PROJECTION = new String[] {
			NotePad.GTaskLists._ID, NotePad.GTaskLists.COLUMN_NAME_DB_ID,
			NotePad.GTaskLists.COLUMN_NAME_ETAG,
			NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT,
			NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID,
			NotePad.GTaskLists.COLUMN_NAME_UPDATED};
	private static final String TAG = "GoogleDBTalker";

	protected String accountName;
	protected ContentProviderClient provider;
	protected SyncResult syncResult;

	/**
	 * Needs the accountName of the currently active sync account because it in
	 * essence supports several
	 */
	public GoogleDBTalker(String accountName, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.d(TAG, "constructor");
		this.accountName = accountName;
		this.provider = provider;
		this.syncResult = syncResult;
	}

	/**
	 * Gets all tasks with a modified flag in the database.
	 */
	public ArrayList<GoogleTask> getModifiedTasks() {
		return null;
		// TODO
	}

	/**
	 * Gets all lists with a modified flag set to the specified value in the
	 * database
	 * @throws RemoteException 
	 */
	public ArrayList<GoogleTaskList> getModifiedLists() throws RemoteException {
		ArrayList<GoogleTaskList> bigList = new ArrayList<GoogleTaskList>();
		// NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1"
		Cursor cursor = provider.query(NotePad.Lists.CONTENT_URI,
				LIST_PROJECTION, NotePad.Lists.COLUMN_NAME_MODIFIED + " IS 1", null, null);
		
		populateWithLists(cursor, bigList);
		cursor.close();
		
		return bigList;
	}

	public ArrayList<GoogleTaskList> getAllLists() throws RemoteException {
		ArrayList<GoogleTaskList> bigList = new ArrayList<GoogleTaskList>();
		// NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1"
		Cursor cursor = provider.query(NotePad.Lists.CONTENT_URI,
				LIST_PROJECTION, null, null, null);
		
		populateWithLists(cursor, bigList);
		cursor.close();
		
		return bigList;
	}
	
	private void populateWithLists(Cursor cursor, ArrayList<GoogleTaskList> bigList) throws RemoteException {
		if (cursor != null && !cursor.isAfterLast()) {
			while (cursor.moveToNext()) {
				GoogleTaskList list = new GoogleTaskList();
				list.dbId = cursor.getLong(cursor.getColumnIndex(NotePad.Lists._ID));
				list.title = cursor.getString(cursor.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
				list.deleted = cursor.getInt(cursor.getColumnIndex(NotePad.Lists.COLUMN_NAME_DELETED));
				
				// get etag and remote id
				Log.d(TAG, "Getting remote info: " + NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + list.dbId + " AND " +  NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT + " IS '" + accountName + "'");
				Cursor remoteCursor = provider.query(NotePad.GTaskLists.CONTENT_URI,
						GTASKLIST_PROJECTION, NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + list.dbId + " AND " +  NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT + " IS '" + accountName + "'", null, null);
				
				// Will only be one, if any
				if (remoteCursor != null && !remoteCursor.isAfterLast()) {
					remoteCursor.moveToFirst();
					
					list.etag = remoteCursor.getString(remoteCursor.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_ETAG));
					list.id = remoteCursor.getString(remoteCursor.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID));
					list.updated = remoteCursor.getString(remoteCursor.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_UPDATED));
				}
				remoteCursor.close();
				
				bigList.add(list);
			}
		}
	}

	/**
	 * Clears modified flag and saves the new fields
	 */
	public void uploaded(GoogleTask task) {

		// TODO
	}

	/**
	 * Save list to database and clear the modified flags
	 * @param result
	 * @throws RemoteException 
	 */
	public void uploaded(GoogleTaskList result) throws RemoteException {
		if (result.deleted == 1) {
			// Server is notified of the delete. Remove it from database
			provider.delete(Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE, Long.toString(result.dbId)), null, null);
			provider.delete(NotePad.GTaskLists.CONTENT_URI, NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + result.dbId, null);
		}
		else {
			provider.update(Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE, Long.toString(result.dbId)), result.toListsContentValues(0), null, null);
			if (result.didRemoteInsert)
				provider.insert(NotePad.GTaskLists.CONTENT_URI, result.toGTaskListsContentValues(accountName));
			else
				provider.update(NotePad.GTaskLists.CONTENT_URI, result.toGTaskListsContentValues(accountName), NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + result.dbId + " AND " + NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT + " IS '" + accountName + "'", null);
		}
	}

	/**
	 * Clears modified flag and saves the new fields
	 */
	public void clearModifiedFlag(GoogleTaskList list) {

		// TODO
	}

	/**
	 * Given a task, it will find the corresponding one in the database and
	 * update it. If it can not find it in the db, it will insert it.
	 * 
	 * Will clear the modified flag.
	 */
	public void SaveToDatabase(GoogleTask task) {
		// if exists
		syncResult.stats.numUpdates++;
		// else
		syncResult.stats.numInserts++;

		syncResult.stats.numEntries++;
		// TODO
	}

	/**
	 * Given a list, it will find the corresponding one in the database and
	 * update it. If it can not find it in the db, it will insert it.
	 * 
	 * Will clear the modified flag.
	 * @throws RemoteException 
	 */
	public void SaveToDatabase(GoogleTaskList list) throws RemoteException {
		// Remember to do both Lists and GTASKLists tables
		if (list.dbId > -1 && list.deleted != 1) {
			Log.d(TAG, "Updating list");
			provider.update(Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE, Long.toString(list.dbId)), list.toListsContentValues(0), null, null);
			provider.update(NotePad.GTaskLists.CONTENT_URI, list.toGTaskListsContentValues(accountName), NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + list.dbId + " AND " + NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT + " IS '" + accountName + "'", null);
			syncResult.stats.numUpdates++;
		} else if (list.dbId > -1 && list.deleted == 1) {
			provider.delete(Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE, Long.toString(list.dbId)), null, null);
			provider.delete(NotePad.GTaskLists.CONTENT_URI, NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + list.dbId, null);
			syncResult.stats.numDeletes++;
		} else {
			Log.d(TAG, "Inserting list");
			Uri newUri = provider.insert(NotePad.Lists.CONTENT_URI, list.toListsContentValues(0));
			long newId = Long.parseLong(newUri.getPathSegments().get(NotePad.Lists.ID_PATH_POSITION));
			// Set the id we just got in the other table
			list.dbId = newId;
			provider.insert(NotePad.GTaskLists.CONTENT_URI, list.toGTaskListsContentValues(accountName));
			
			syncResult.stats.numInserts++;
		}
	}

	/**
	 * Will erase items marked as deleted in the database. Make sure you synced
	 * with the server before calling this or the server will never know that
	 * they were deleted.
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
		// TODO
	}

}