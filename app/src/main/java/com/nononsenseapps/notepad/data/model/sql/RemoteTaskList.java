/*
 * Copyright (c) 2015. Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.data.model.sql;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.nononsenseapps.notepad.data.local.sql.MyContentProvider;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteTaskList extends DAO {

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "remotetasklist";

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps."
			+ TABLE_NAME;

	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			TABLE_NAME);

	public static final int BASEURICODE = 401;
	public static final int BASEITEMCODE = 402;

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
		// Reserved columns, depending on what different services needs
		public static final String DELETED = "field1";
		public static final String FIELD2 = "field2";
		public static final String FIELD3 = "field3";
		public static final String FIELD4 = "field4";
		public static final String FIELD5 = "field5";

		public static final String[] FIELDS = { _ID, DBID, REMOTEID, 
			UPDATED, ACCOUNT, DELETED, FIELD2,
			FIELD3, FIELD4, FIELD5, SERVICE };
	}

	/**
	 * Main table to store data
	 */
	public static final String CREATE_TABLE = new StringBuilder("CREATE TABLE ")
			.append(TABLE_NAME)
			.append("(").append(Columns._ID).append(" INTEGER PRIMARY KEY,")
			.append(Columns.ACCOUNT).append(" TEXT NOT NULL,")
			.append(Columns.SERVICE).append(" TEXT NOT NULL,")
			.append(Columns.DBID).append(" INTEGER NOT NULL,")
			.append(Columns.UPDATED).append(" INTEGER NOT NULL,")
			.append(Columns.REMOTEID).append(" TEXT NOT NULL,")
			.append(Columns.DELETED).append(" TEXT,")
			.append(Columns.FIELD2).append(" TEXT,")
			.append(Columns.FIELD3).append(" TEXT,")
			.append(Columns.FIELD4).append(" TEXT,")
			.append(Columns.FIELD5).append(" TEXT")
			// Cant delete on cascade, since then we cant remember to sync it!
			.append(")").toString();
	
	// milliseconds since 1970-01-01 UTC
	public Long updated = null;

	public Long dbid = null;
	public String account = null;
	public String remoteId = null;
	public String deleted = null;
	public String field2 = null;
	public String field3 = null;
	public String field4 = null;
	public String field5 = null;
	
	// Should be overwritten by children
	public String service = null;	
	
	public RemoteTaskList() {
		
	}

	/**
	 * None of the fields may be null!
	 * @param dbid
	 * @param remoteId
	 * @param updated
	 * @param account
	 */
	public RemoteTaskList(final Long dbid, final String remoteId, final Long updated, final String account) {
		this.dbid = dbid;
		this.remoteId = remoteId;
		this.updated = updated;
		this.account = account;
	}

	public RemoteTaskList(final Cursor c) {
		_id = c.getLong(0);
		dbid = c.getLong(1);
		remoteId = c.getString(2);
		updated = c.getLong(3);
		account  = c.getString(4);
		
		deleted  = c.isNull(5) ? null : c.getString(5);
		field2  = c.isNull(6) ? null : c.getString(6);
		field3  = c.isNull(7) ? null : c.getString(7);
		field4  = c.isNull(8) ? null : c.getString(8);
		field5  = c.isNull(9) ? null : c.getString(9);
		
		service = c.getString(10);
	}

	public RemoteTaskList(final Uri uri, final ContentValues values) {
		this(Long.parseLong(uri.getLastPathSegment()), values);
	}

	public RemoteTaskList(final long id, final ContentValues values) {
		this(values);
		_id = id;
	}
	
	public RemoteTaskList(final JSONObject json) throws JSONException {
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

	public RemoteTaskList(final ContentValues values) {
		dbid = values.getAsLong(Columns.DBID);
		remoteId = values.getAsString(Columns.REMOTEID);
		updated = values.getAsLong(Columns.UPDATED);
		account = values.getAsString(Columns.ACCOUNT);
		service = values.getAsString(Columns.SERVICE);
		
		deleted = values.getAsString(Columns.DELETED);
		field2 = values.getAsString(Columns.FIELD2);
		field3 = values.getAsString(Columns.FIELD3);
		field4 = values.getAsString(Columns.FIELD4);
		field5 = values.getAsString(Columns.FIELD5);
	}
	
	public boolean isDeleted() {
		return deleted != null && !deleted.isEmpty();
	}
	public void setDeleted(final boolean deleted) {
		this.deleted = deleted ? "deleted" : null;
	}

	@Override
	public ContentValues getContent() {
		final ContentValues values = new ContentValues();

		values.put(Columns.DBID, dbid);
		values.put(Columns.REMOTEID, remoteId);
		values.put(Columns.UPDATED,updated);
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
		}
		else {
			result += context.getContentResolver().update(getUri(),
					getContent(), null, null);
		}
		return result;
	}
	
	public int save(final Context context, final long updateTime) {
		updated = updateTime;
		return save(context);
	}
	
	/**
	 * Returns a where clause that can be used to fetch the tasklist that
	 * is associated with this remote object.
	 * As argument, use remoteid, account, service
	 * @return
	 */
	public String getTaskListWithRemoteClause() {
		return new StringBuilder(BaseColumns._ID).append(" IN (SELECT ").
		append(Columns.DBID).append(" FROM ").append(TABLE_NAME).append(" WHERE ")
		.append(Columns.REMOTEID).append(" IS ? AND ")
		.append(Columns.ACCOUNT).append(" IS ? AND ")
		.append(Columns.SERVICE).append(" IS ?)")
		.toString();
	}
	public String[] getTaskListWithRemoteArgs() {
		return new String[] {remoteId, account, service};
	}
	/**
	 * Returns a where clause that limits the tasklists to those that do not
	 * have a remote version.
	 * 
	 * Combine with account, service
	 */
	public static String getTaskListWithoutRemoteClause() {
		return new StringBuilder(BaseColumns._ID).append(" NOT IN (SELECT ").
		append(Columns.DBID).append(" FROM ").append(TABLE_NAME).append(" WHERE ")
		.append(Columns.ACCOUNT).append(" IS ? AND ")
		.append(Columns.SERVICE).append(" IS ?)").toString();
	}
	public String[] getTaskListWithoutRemoteArgs() {
		return new String[] {account, service};
	}
	
	/*
	 * Trigger to delete items when their list is deleted
	 */
	public static final String TRIGGER_REALDELETE_MARK = new StringBuilder()
	.append("CREATE TRIGGER trigger_real_deletemark_").append(TABLE_NAME)
	.append(" AFTER DELETE ON ").append(TaskList.TABLE_NAME).append(" BEGIN ")
	.append(" UPDATE ").append(TABLE_NAME).append(" SET ").append(Columns.DELETED).append(" = 'deleted' ")
	.append(" WHERE ").append(Columns.DBID).append(" IS old.").append(TaskList.Columns._ID)
	.append(";")
	.append(" END;").toString();
}
