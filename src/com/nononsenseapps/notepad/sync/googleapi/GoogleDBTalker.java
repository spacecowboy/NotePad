package com.nononsenseapps.notepad.sync.googleapi;


import android.content.ContentProviderClient;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.sync.SyncAdapter;

/**
 * Helper class which talks to the database and converts the responses into Task
 * and List objects
 */
public class GoogleDBTalker {

	private static final String[] LIST_PROJECTION = new String[] {
			NotePad.Lists._ID, NotePad.Lists.COLUMN_NAME_TITLE,
			NotePad.Lists.COLUMN_NAME_DELETED,
			NotePad.Lists.COLUMN_NAME_MODIFIED,
			NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE };
	private static final String[] GTASKLIST_PROJECTION = new String[] {
			NotePad.GTaskLists._ID, NotePad.GTaskLists.COLUMN_NAME_DB_ID,
			NotePad.GTaskLists.COLUMN_NAME_ETAG,
			NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT,
			NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID,
			NotePad.GTaskLists.COLUMN_NAME_UPDATED };

	private static final String[] NOTES_PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_DELETED,
			NotePad.Notes.COLUMN_NAME_MODIFIED,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
			NotePad.Notes.COLUMN_NAME_LIST,
			NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
	private static final String[] GTASK_PROJECTION = new String[] {
			NotePad.GTasks._ID, NotePad.GTasks.COLUMN_NAME_DB_ID,
			NotePad.GTasks.COLUMN_NAME_ETAG,
			NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT,
			NotePad.GTasks.COLUMN_NAME_GTASKS_ID,
			NotePad.GTasks.COLUMN_NAME_UPDATED };

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
		if (SyncAdapter.SYNC_DEBUG_PRINTS) Log.d(TAG, "constructor");
		this.accountName = accountName;
		this.provider = provider;
		this.syncResult = syncResult;
	}

	/**
	 * Gets all tasks with a modified flag in the database.
	 * 
	 * @param list
	 * @throws RemoteException
	 */
	public ArrayList<GoogleTask> getModifiedTasks(GoogleTaskList list)
			throws RemoteException {
		ArrayList<GoogleTask> modifiedTasks = new ArrayList<GoogleTask>();

		Cursor cursor = provider.query(NotePad.Notes.CONTENT_URI,
				NOTES_PROJECTION, NotePad.Notes.COLUMN_NAME_MODIFIED
						+ " IS ? AND " + NotePad.Notes.COLUMN_NAME_LIST
						+ " IS ?", new String[] {"1", Long.toString(list.dbId)}, null);

		populateWithTasks(cursor, modifiedTasks);
		cursor.close();

		return modifiedTasks;
	}

	/**
	 * Gets all lists with a modified flag set to the specified value in the
	 * database
	 * 
	 * @throws RemoteException
	 */
	public ArrayList<GoogleTaskList> getModifiedLists() throws RemoteException {
		ArrayList<GoogleTaskList> bigList = new ArrayList<GoogleTaskList>();
		// NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1"
		Cursor cursor = provider.query(NotePad.Lists.CONTENT_URI,
				LIST_PROJECTION, NotePad.Lists.COLUMN_NAME_MODIFIED + " IS 1",
				null, null);

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

	private void populateWithTasks(Cursor cursor, ArrayList<GoogleTask> bigList)
			throws RemoteException {
		if (cursor != null && !cursor.isAfterLast()) {
			while (cursor.moveToNext()) {
				GoogleTask task = new GoogleTask();
				task.dbId = cursor.getLong(cursor
						.getColumnIndex(NotePad.Notes._ID));
				task.title = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE));
				task.deleted = cursor.getInt(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_DELETED));
				task.notes = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));
				task.dueDate = cursor.getString(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE));
				task.status = cursor
						.getString(cursor
								.getColumnIndex(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS));
				task.listdbid = cursor.getLong(cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_LIST));
				// Task is assembled, move on

				// convert modification time to timestamp
				long modTime = cursor
						.getLong(cursor
								.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));

				Time localTime = new Time(Time.getCurrentTimezone());
				localTime.set(modTime);

				task.updated = localTime.format3339(false);

				// get etag and remote id
				if (SyncAdapter.SYNC_DEBUG_PRINTS) Log.d(TAG, "Getting remote info: "
						+ NotePad.GTasks.COLUMN_NAME_DB_ID + " IS " + task.dbId
						+ " AND " + NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT
						+ " IS '" + accountName + "'");
				Cursor remoteCursor = provider.query(
						NotePad.GTasks.CONTENT_URI, GTASK_PROJECTION,
						NotePad.GTasks.COLUMN_NAME_DB_ID + " IS " + task.dbId
								+ " AND "
								+ NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT
								+ " IS '" + accountName + "'", null, null);

				// Will only be one, if any
				if (remoteCursor != null && !remoteCursor.isAfterLast()) {
					remoteCursor.moveToFirst();

					task.etag = remoteCursor.getString(remoteCursor
							.getColumnIndex(NotePad.GTasks.COLUMN_NAME_ETAG));
					task.id = remoteCursor
							.getString(remoteCursor
									.getColumnIndex(NotePad.GTasks.COLUMN_NAME_GTASKS_ID));
					// Compare with the modification time in the object, select
					// the newest timestamp
					String updTime = remoteCursor
							.getString(remoteCursor
									.getColumnIndex(NotePad.GTasks.COLUMN_NAME_UPDATED));
					Time remoteTime = new Time(Time.getCurrentTimezone());
					remoteTime.parse3339(updTime);

					if (Time.compare(localTime, remoteTime) < 0) {
						// remoteTime is greater
						task.updated = updTime;
					} // else we already did that before
				}
				remoteCursor.close();

				bigList.add(task);
			}
		}
	}

	private void populateWithLists(Cursor cursor,
			ArrayList<GoogleTaskList> bigList) throws RemoteException {
		if (cursor != null && !cursor.isAfterLast()) {
			while (cursor.moveToNext()) {
				GoogleTaskList list = new GoogleTaskList();
				list.dbId = cursor.getLong(cursor
						.getColumnIndex(NotePad.Lists._ID));
				list.title = cursor.getString(cursor
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
				list.deleted = cursor.getInt(cursor
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_DELETED));

				// convert modification time to timestamp
				long modTime = cursor
						.getInt(cursor
								.getColumnIndex(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE));

				Time localTime = new Time(Time.getCurrentTimezone());
				localTime.set(modTime);

				list.updated = localTime.format3339(false);

				// get etag and remote id
				Log.d(TAG, "Getting remote info: "
						+ NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS "
						+ list.dbId + " AND "
						+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT
						+ " IS '" + accountName + "'");
				Cursor remoteCursor = provider.query(
						NotePad.GTaskLists.CONTENT_URI, GTASKLIST_PROJECTION,
						NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS "
								+ list.dbId + " AND "
								+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT
								+ " IS '" + accountName + "'", null, null);

				// Will only be one, if any
				if (remoteCursor != null && !remoteCursor.isAfterLast()) {
					remoteCursor.moveToFirst();

					list.etag = remoteCursor
							.getString(remoteCursor
									.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_ETAG));
					list.id = remoteCursor
							.getString(remoteCursor
									.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID));
					// Compare with the modification time in the object, select
					// the newest timestamp
					String updTime = remoteCursor
							.getString(remoteCursor
									.getColumnIndex(NotePad.GTaskLists.COLUMN_NAME_UPDATED));
					Time remoteTime = new Time(Time.getCurrentTimezone());
					remoteTime.parse3339(updTime);

					if (Time.compare(localTime, remoteTime) < 0) {
						// remoteTime is greater
						list.updated = updTime;
					} // else we already did that before

				}
				remoteCursor.close();

				bigList.add(list);
			}
		}
	}

	/**
	 * Clears modified flag and saves the new fields
	 * 
	 * @throws RemoteException
	 */
	public void uploaded(GoogleTask result, GoogleTaskList list)
			throws RemoteException {
		if (result.deleted == 1) {
			// Server is notified of the delete. Remove it from database
			provider.delete(Uri.withAppendedPath(
					NotePad.Notes.CONTENT_ID_URI_BASE,
					Long.toString(result.dbId)), null, null);
			provider.delete(NotePad.GTasks.CONTENT_URI,
					NotePad.GTasks.COLUMN_NAME_DB_ID + " IS " + result.dbId,
					null);
		} else {
			provider.update(Uri.withAppendedPath(
					NotePad.Notes.CONTENT_ID_URI_BASE,
					Long.toString(result.dbId)), result.toNotesContentValues(0,
					list.dbId), null, null);
			if (result.didRemoteInsert)
				provider.insert(NotePad.GTasks.CONTENT_URI,
						result.toGTasksContentValues(accountName));
			else
				provider.update(NotePad.GTasks.CONTENT_URI,
						result.toGTasksContentValues(accountName),
						NotePad.GTasks.COLUMN_NAME_DB_ID + " IS " + result.dbId
								+ " AND "
								+ NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT
								+ " IS '" + accountName + "'", null);
		}
	}

	/**
	 * Save list to database and clear the modified flags
	 * 
	 * @param result
	 * @throws RemoteException
	 */
	public void uploaded(GoogleTaskList result) throws RemoteException {
		if (result.deleted == 1) {
			// Server is notified of the delete. Remove it from database
			provider.delete(Uri.withAppendedPath(
					NotePad.Lists.CONTENT_ID_URI_BASE,
					Long.toString(result.dbId)), null, null);
			provider.delete(
					NotePad.GTaskLists.CONTENT_URI,
					NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + result.dbId,
					null);
		} else {
			provider.update(Uri.withAppendedPath(
					NotePad.Lists.CONTENT_ID_URI_BASE,
					Long.toString(result.dbId)),
					result.toListsContentValues(0), null, null);
			if (result.didRemoteInsert)
				provider.insert(NotePad.GTaskLists.CONTENT_URI,
						result.toGTaskListsContentValues(accountName));
			else
				provider.update(NotePad.GTaskLists.CONTENT_URI,
						result.toGTaskListsContentValues(accountName),
						NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS "
								+ result.dbId + " AND "
								+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT
								+ " IS '" + accountName + "'", null);
		}
	}

	/**
	 * Given a task, it will find the corresponding one in the database and
	 * update it. If it can not find it in the db, it will insert it.
	 * 
	 * Will clear the modified flag.
	 * 
	 * @throws RemoteException
	 */
	public void SaveToDatabase(GoogleTask task, GoogleTaskList plist)
			throws RemoteException {
		// Only if list id is valid!
		if (plist.dbId > -1) {
			// Remember to do both Lists and GTASKLists tables
			if (task.dbId > -1 && task.deleted != 1) {
				if (SyncAdapter.SYNC_DEBUG_PRINTS) Log.d(TAG, "Updating task");
				provider.update(Uri.withAppendedPath(
						NotePad.Notes.CONTENT_ID_URI_BASE,
						Long.toString(task.dbId)), task.toNotesContentValues(0,
						plist.dbId), null, null);
				provider.update(NotePad.GTasks.CONTENT_URI,
						task.toGTasksContentValues(accountName),
						NotePad.GTasks.COLUMN_NAME_DB_ID + " IS " + task.dbId
								+ " AND "
								+ NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT
								+ " IS '" + accountName + "'", null);
				syncResult.stats.numUpdates++;
			} else if (task.dbId > -1 && task.deleted == 1) {
				provider.delete(Uri.withAppendedPath(
						NotePad.Notes.CONTENT_ID_URI_BASE,
						Long.toString(task.dbId)), null, null);
				provider.delete(NotePad.GTasks.CONTENT_URI,
						NotePad.GTasks.COLUMN_NAME_DB_ID + " IS " + task.dbId,
						null);
				syncResult.stats.numDeletes++;
			} else if (task.deleted == 1) {
				// This must be a task which we ourselves deleted earlier. Ignore them
			}
			else {
				if (SyncAdapter.SYNC_DEBUG_PRINTS) Log.d(TAG, "Inserting task");
				Uri newUri = provider.insert(NotePad.Notes.CONTENT_URI,
						task.toNotesContentValues(0, plist.dbId));
				long newId = Long.parseLong(newUri.getPathSegments().get(
						NotePad.Notes.NOTE_ID_PATH_POSITION));
				// Set the id we just got in the other table
				task.dbId = newId;
				provider.insert(NotePad.GTasks.CONTENT_URI,
						task.toGTasksContentValues(accountName));

				syncResult.stats.numInserts++;
			}
		} else {
			Log.d(TAG,
					"ListID was not valid. Make sure you are saving tasks only for lists that exist in the database");
		}
	}

	/**
	 * Given a list, it will find the corresponding one in the database and
	 * update it. If it can not find it in the db, it will insert it.
	 * 
	 * Will clear the modified flag.
	 * 
	 * The list will have a database id after this function has returned.
	 * 
	 * @throws RemoteException
	 */
	public void SaveToDatabase(GoogleTaskList list) throws RemoteException {
		// Remember to do both Lists and GTASKLists tables
		if (list.dbId > -1 && list.deleted != 1) {
			if (SyncAdapter.SYNC_DEBUG_PRINTS) Log.d(TAG, "Updating list");
			provider.update(
					Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE,
							Long.toString(list.dbId)), list
							.toListsContentValues(0), null, null);
			provider.update(NotePad.GTaskLists.CONTENT_URI,
					list.toGTaskListsContentValues(accountName),
					NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + list.dbId
							+ " AND "
							+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT
							+ " IS '" + accountName + "'", null);
			syncResult.stats.numUpdates++;
		} else if (list.dbId > -1 && list.deleted == 1) {
			provider.delete(
					Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE,
							Long.toString(list.dbId)), null, null);
			provider.delete(NotePad.GTaskLists.CONTENT_URI,
					NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS " + list.dbId,
					null);
			syncResult.stats.numDeletes++;
		} else {
			if (SyncAdapter.SYNC_DEBUG_PRINTS) Log.d(TAG, "Inserting list");
			Uri newUri = provider.insert(NotePad.Lists.CONTENT_URI,
					list.toListsContentValues(0));
			long newId = Long.parseLong(newUri.getPathSegments().get(
					NotePad.Lists.ID_PATH_POSITION));
			// Set the id we just got in the other table
			list.dbId = newId;
			provider.insert(NotePad.GTaskLists.CONTENT_URI,
					list.toGTaskListsContentValues(accountName));

			syncResult.stats.numInserts++;
		}
	}

	/**
	 * Returns a 3339-formatted timestamp which is the latest time that we
	 * synced. If no sync has been done before, null will be returned Finds the
	 * latest update-stamp in the GTasks table
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public String getLastUpdated(String accountName) throws RemoteException {
		Time lastDate = null;
		Cursor cursor = provider.query(NotePad.GTasks.CONTENT_URI,
				new String[] { NotePad.GTasks.COLUMN_NAME_UPDATED },
				NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT + " IS ?",
				new String[] { accountName }, null);
		if (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
			while (cursor.moveToNext()) {
				String updated = cursor.getString(cursor
						.getColumnIndex(NotePad.GTasks.COLUMN_NAME_UPDATED));
				Time thisDate = new Time(Time.getCurrentTimezone());
				thisDate.parse3339(updated);
				if (lastDate == null || Time.compare(thisDate, lastDate) >= 0) {
					lastDate = thisDate;
				}
			}
		}
		cursor.close();

		if (lastDate == null)
			return null;
		else
			return lastDate.format3339(false);
	}

	public ArrayList<GoogleTask> getAllTasks(GoogleTaskList list) throws RemoteException {
		ArrayList<GoogleTask> tasks = new ArrayList<GoogleTask>();
		
		Cursor cursor = provider.query(NotePad.Notes.CONTENT_URI,
				NOTES_PROJECTION, NotePad.Notes.COLUMN_NAME_LIST
						+ " IS ?", new String[] {Long.toString(list.dbId)}, null);

		populateWithTasks(cursor, tasks);
		cursor.close();
		
		return tasks;
	}
}