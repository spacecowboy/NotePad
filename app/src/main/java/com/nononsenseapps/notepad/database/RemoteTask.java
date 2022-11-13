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

package com.nononsenseapps.notepad.database;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class RemoteTask extends DAO {

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "remotetask";

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps."
			+ TABLE_NAME;

	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			TABLE_NAME);

	public static final int BASEURICODE = 501;
	public static final int BASEITEMCODE = 502;

	public static void addMatcherUris(UriMatcher sURIMatcher) {
		sURIMatcher
				.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#",
				BASEITEMCODE);
	}

	public static Uri getUri(final long id) {
		return Uri.withAppendedPath(URI, Long.toString(id));
	}

	public static class Columns implements BaseColumns {

		private Columns() {
		}

		public static final String SERVICE = "service";
		public static final String ACCOUNT = "account";
		public static final String REMOTEID = "remoteid";
		public static final String UPDATED = "updated";
		public static final String DBID = "dbid";
		public static final String LISTDBID = "listdbid";
		// Reserved columns, depending on what different services needs
		public static final String DELETED = "field1";
		public static final String FIELD2 = "field2";
		public static final String FIELD3 = "field3";
		public static final String FIELD4 = "field4";
		public static final String FIELD5 = "field5";

		public static final String[] FIELDS = { _ID, DBID, REMOTEID, UPDATED,
				ACCOUNT, LISTDBID, DELETED, FIELD2, FIELD3, FIELD4, FIELD5, SERVICE };
	}

	/**
	 * Main table to store data
	 */
	public static final String CREATE_TABLE = "CREATE TABLE " +
			TABLE_NAME + "(" + Columns._ID +
			" INTEGER PRIMARY KEY," + Columns.ACCOUNT +
			" TEXT NOT NULL," + Columns.SERVICE +
			" TEXT NOT NULL," + Columns.DBID +
			" INTEGER NOT NULL," + Columns.UPDATED +
			" INTEGER NOT NULL," + Columns.REMOTEID +
			" TEXT NOT NULL," + Columns.LISTDBID +
			" INTEGER NOT NULL," + Columns.DELETED +
			" TEXT," + Columns.FIELD2 + " TEXT," +
			Columns.FIELD3 + " TEXT," + Columns.FIELD4 +
			" TEXT," + Columns.FIELD5 + " TEXT" +
			// Cant delete on cascade because we must sync before!
			")";

	/*
	 * Trigger to delete items when their list is deleted
	 */
	public static final String TRIGGER_LISTDELETE_CASCADE = "CREATE TRIGGER cascade_trigger_delete_" +
			TABLE_NAME + " AFTER DELETE ON " +
			RemoteTaskList.TABLE_NAME + " BEGIN " +
			" DELETE FROM " + TABLE_NAME + " WHERE " +
			Columns.LISTDBID + " IS old." +
			RemoteTaskList.Columns.DBID + " AND " +
			Columns.ACCOUNT + " IS old." +
			RemoteTaskList.Columns.ACCOUNT + " AND " +
			Columns.SERVICE + " IS old." +
			RemoteTaskList.Columns.SERVICE + ";" + " END;";

	/*
	 * Trigger to delete items when their real items are deleted
	 */
	public static final String TRIGGER_REALDELETE_MARK = "CREATE TRIGGER trigger_real_deletemark_" +
			TABLE_NAME + " AFTER DELETE ON " +
			Task.TABLE_NAME + " BEGIN " + " UPDATE " +
			TABLE_NAME + " SET " + Columns.DELETED +
			" = 'deleted' " + " WHERE " + Columns.DBID +
			" IS old." + Task.Columns._ID + ";" +
			" END;";

	/*
	 * Trigger to move between lists
	 */
	public static final String TRIGGER_MOVE_LIST = "CREATE TRIGGER trigger_move_list_" + TABLE_NAME +
			" AFTER UPDATE OF " + Task.Columns.DBLIST +
			" ON " + Task.TABLE_NAME + " WHEN old." +
			Task.Columns.DBLIST + " IS NOT new." +
			Task.Columns.DBLIST + " BEGIN " + " UPDATE " +
			TABLE_NAME + " SET " + Columns.DELETED +
			" = 'deleted', " + Columns.DBID + " = -99 " +
			" WHERE " + Columns.DBID + " IS old." +
			Task.Columns._ID + ";" + " END;";

	// milliseconds since 1970-01-01 UTC
	public Long updated = null;

	public Long dbid = null;
	public Long listdbid = null;
	public String account = null;
	public String remoteId = null;
	public String deleted = null;
	public String field2 = null;
	public String field3 = null;
	public String field4 = null;
	public String field5 = null;

	public boolean isDeleted() {
		return deleted != null && deleted.equals("deleted");
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted ? "deleted" : null;
	}

	// Should be overwritten by children
	public String service = null;

	public RemoteTask() {

	}

	/**
	 * None of the fields may be null!
	 *
	 * @param dbid
	 * @param remoteId
	 * @param updated
	 * @param account
	 */
	public RemoteTask(final Long dbid, final Long listdbid,
					  final String remoteId, final Long updated, final String account) {
		this.dbid = dbid;
		this.listdbid = listdbid;
		this.remoteId = remoteId;
		this.updated = updated;
		this.account = account;
	}

	public RemoteTask(final Cursor c) {
		_id = c.getLong(0);
		dbid = c.getLong(1);
		remoteId = c.getString(2);
		updated = c.getLong(3);
		account = c.getString(4);
		listdbid = c.getLong(5);

		deleted = c.isNull(6) ? null : c.getString(6);
		field2 = c.isNull(7) ? null : c.getString(7);
		field3 = c.isNull(8) ? null : c.getString(8);
		field4 = c.isNull(9) ? null : c.getString(9);
		field5 = c.isNull(10) ? null : c.getString(10);

		service = c.getString(11);
	}

	public RemoteTask(final Uri uri, final ContentValues values) {
		this(Long.parseLong(uri.getLastPathSegment()), values);
	}

	public RemoteTask(final long id, final ContentValues values) {
		this(values);
		_id = id;
	}

	public RemoteTask(final JSONObject json) throws JSONException {
		if (json.has(Columns.DBID))
			dbid = json.getLong(Columns.DBID);
		if (json.has(Columns.REMOTEID))
			remoteId = json.getString(Columns.REMOTEID);
		if (json.has(Columns.UPDATED))
			updated = json.getLong(Columns.UPDATED);
		if (json.has(Columns.ACCOUNT))
			account = json.getString(Columns.ACCOUNT);
		if (json.has(Columns.SERVICE))
			service = json.getString(Columns.SERVICE);
		if (json.has(Columns.LISTDBID))
			listdbid = json.getLong(Columns.LISTDBID);
		if (json.has(Columns.DELETED))
			deleted = json.getString(Columns.DELETED);
		if (json.has(Columns.FIELD2))
			field2 = json.getString(Columns.FIELD2);
		if (json.has(Columns.FIELD3))
			field3 = json.getString(Columns.FIELD3);
		if (json.has(Columns.FIELD4))
			field4 = json.getString(Columns.FIELD4);
		if (json.has(Columns.FIELD5))
			field5 = json.getString(Columns.FIELD5);
	}

	public RemoteTask(final ContentValues values) {
		dbid = values.getAsLong(Columns.DBID);
		remoteId = values.getAsString(Columns.REMOTEID);
		updated = values.getAsLong(Columns.UPDATED);
		account = values.getAsString(Columns.ACCOUNT);
		service = values.getAsString(Columns.SERVICE);
		listdbid = values.getAsLong(Columns.LISTDBID);

		deleted = values.getAsString(Columns.DELETED);
		field2 = values.getAsString(Columns.FIELD2);
		field3 = values.getAsString(Columns.FIELD3);
		field4 = values.getAsString(Columns.FIELD4);
		field5 = values.getAsString(Columns.FIELD5);
	}

	@Override
	public ContentValues getContent() {
		final ContentValues values = new ContentValues();

		values.put(Columns.DBID, dbid);
		values.put(Columns.LISTDBID, listdbid);
		values.put(Columns.REMOTEID, remoteId);
		values.put(Columns.UPDATED, updated);
		values.put(Columns.ACCOUNT, account);
		values.put(Columns.SERVICE, service);
		values.put(Columns.DELETED, deleted);
		values.put(Columns.FIELD2, field2);
		values.put(Columns.FIELD3, field3);
		values.put(Columns.FIELD4, field4);
		values.put(Columns.FIELD5, field5);

		return values;

	}

	@Override
	protected String getTableName() {
		return TABLE_NAME;
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	public int save(final Context context) {
		int result = 0;
		if (_id < 1) {
			final Uri uri = context.getContentResolver().insert(getBaseUri(),
					getContent());
			if (uri != null) {
				_id = Long.parseLong(uri.getLastPathSegment());
				result++;
			}
		} else {
			result += context.getContentResolver().update(getUri(),
					getContent(), null, null);
		}
		return result;
	}

	/**
	 * Returns a where clause that can be used to fetch the task that is
	 * associated with this remote object. As argument, use remoteid, account
	 *
	 * @return
	 */
	public String getTaskWithRemoteClause() {
		return Task.Columns.DBLIST + " IS ? AND " +
				BaseColumns._ID + " IN (SELECT " +
				Columns.DBID + " FROM " + TABLE_NAME +
				" WHERE " + Columns.REMOTEID +
				" IS ? AND " + Columns.ACCOUNT + " IS ?)";
	}

	public String[] getTaskWithRemoteArgs() {
		return new String[] { Long.toString(listdbid), remoteId, account };
	}

	/**
	 * Returns a where clause that limits the tasklists to those that do not
	 * have a remote version.
	 *
	 * Combine with account
	 */
	public static String getTaskWithoutRemoteClause() {
		return Task.Columns.DBLIST + " IS ? AND " +
				BaseColumns._ID + " NOT IN (SELECT " +
				Columns.DBID + " FROM " + TABLE_NAME +
				" WHERE " + Columns.ACCOUNT + " IS ? AND " +
				Columns.SERVICE + " IS ?)";
	}

	public static String[] getTaskWithoutRemoteArgs(final long listdbid,
													final String account, final String service) {
		return new String[] { Long.toString(listdbid), account, service };
	}

}
