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

package com.nononsenseapps.notepad.data.model.sql;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.text.format.Time;

import com.nononsenseapps.notepad.data.local.sql.LegacyDBHelper;
import com.nononsenseapps.notepad.data.local.sql.MyContentProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.Calendar;

/**
 * An object that represents the task information contained in the database.
 * Provides convenience methods for moving items.
 */
public class Task extends DAO {

	// Used to separate tasks with due dates from completed and from tasks with
	// no date
	public static final String SECRET_TYPEID = "secret_typeid";
	public static final String SECRET_TYPEID2 = "secret_typeid2";

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "task";
	public static final String DELETE_TABLE_NAME = "deleted_task";
	public static final String FTS3_DELETE_TABLE_NAME = "fts3_deleted_task";
	public static final String HISTORY_TABLE_NAME = "history";
	private static final String SECTIONED_DATE_VIEW = "sectioned_date_view";
	public static final String FTS3_TABLE_NAME = "fts3_task";
  private static final String TAG = "Task";

  public static String getSECTION_DATE_VIEW_NAME(final String listId) {
		// listId CAN be null. Hence the string concat hack
		return new StringBuilder().append(SECTIONED_DATE_VIEW).append("_")
				.append("" + listId).toString();
	}

	// Used in sectioned view date
	static final String FAR_FUTURE = "strftime('%s','3999-01-01') * 1000";
	public static final String OVERDUE = "strftime('%s', '1970-01-01') * 1000";
	// Today should be from NOW...
	public static final String TODAY_START = "strftime('%s','now', 'utc') * 1000";

	// static final String TODAY_START =
	// "strftime('%s','now','localtime','start of day', 'utc') * 1000";

	public static final String TODAY_PLUS(final int offset) {
		return "strftime('%s','now','localtime','+" + Integer.toString(offset)
				+ " days','start of day', 'utc') * 1000";
	}

	// Code used to decode title of date header
	public static final String HEADER_KEY_TODAY = "today+0";
	public static final String HEADER_KEY_PLUS1 = "today+1";
	public static final String HEADER_KEY_PLUS2 = "today+2";
	public static final String HEADER_KEY_PLUS3 = "today+3";
	public static final String HEADER_KEY_PLUS4 = "today+4";
	public static final String HEADER_KEY_OVERDUE = "overdue";
	public static final String HEADER_KEY_LATER = "later";
	public static final String HEADER_KEY_NODATE = "nodate";
	public static final String HEADER_KEY_COMPLETE = "complete";

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.note";

	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			TABLE_NAME);

	public static Uri getUri(final long id) {
		return Uri.withAppendedPath(URI, Long.toString(id));
	}

	public static final int BASEURICODE = 201;
	public static final int BASEITEMCODE = 202;
	public static final int DELETEDQUERYCODE = 209;
	public static final int DELETEDITEMCODE = 210;
	public static final int SECTIONEDDATEQUERYCODE = 211;
	public static final int SECTIONEDDATEITEMCODE = 212;
	public static final int HISTORYQUERYCODE = 213;
	public static final int MOVEITEMLEFTCODE = 214;
	public static final int MOVEITEMRIGHTCODE = 215;
	// Legacy support, these also need to use legacy projections
	public static final int LEGACYBASEURICODE = 221;
	public static final int LEGACYBASEITEMCODE = 222;
	public static final int LEGACYVISIBLEURICODE = 223;
	public static final int LEGACYVISIBLEITEMCODE = 224;
	// Search URI
	public static final int SEARCHCODE = 299;
	public static final int SEARCHSUGGESTIONSCODE = 298;

	public static void addMatcherUris(UriMatcher sURIMatcher) {
		sURIMatcher
				.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#",
				BASEITEMCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ MOVEITEMLEFT + "/#", MOVEITEMLEFTCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ MOVEITEMRIGHT + "/#", MOVEITEMRIGHTCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ DELETEDQUERY, DELETEDQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ DELETEDQUERY + "/#", DELETEDITEMCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ SECTIONED_DATE_VIEW, SECTIONEDDATEQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ SECTIONED_DATE_VIEW + "/#", SECTIONEDDATEITEMCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ HISTORY_TABLE_NAME, HISTORYQUERYCODE);

		// Legacy URIs
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.NOTES, LEGACYBASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.NOTES + "/#", LEGACYBASEITEMCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.VISIBLE_NOTES,
				LEGACYVISIBLEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.VISIBLE_NOTES + "/#",
				LEGACYVISIBLEITEMCODE);

		// Search URI
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, FTS3_TABLE_NAME,
				SEARCHCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				SearchManager.SUGGEST_URI_PATH_QUERY, SEARCHSUGGESTIONSCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
				SEARCHSUGGESTIONSCODE);

	}

	public static final String TARGETPOS = "targetpos";
	private static final String MOVEITEMLEFT = "moveitemleft";
	private static final String MOVEITEMRIGHT = "moveitemright";
	private static final String DELETEDQUERY = "deletedquery";

	// Special URI to look at backup table
	public static final Uri URI_DELETED_QUERY = Uri.withAppendedPath(URI,
			DELETEDQUERY);

	// Query the view with date section headers
	public static final Uri URI_SECTIONED_BY_DATE = Uri.withAppendedPath(URI,
			SECTIONED_DATE_VIEW);

	// Query for history of tasks
	public static final Uri URI_TASK_HISTORY = Uri.withAppendedPath(URI,
			HISTORY_TABLE_NAME);

	// Search URI
	public static final Uri URI_SEARCH = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			FTS3_TABLE_NAME);

	// Special URI to use when a move is requested
	// public static final Uri URI_WRITE_MOVESUBTREE = Uri.withAppendedPath(URI,
	// MOVESUBTREE);
	private static final Uri URI_WRITE_MOVEITEMLEFT = Uri.withAppendedPath(URI,
			MOVEITEMLEFT);
	private static final Uri URI_WRITE_MOVEITEMRIGHT = Uri.withAppendedPath(
			URI, MOVEITEMRIGHT);

	// public Uri getMoveSubTreeUri() {
	// if (_id < 1) {
	// throw new InvalidParameterException(
	// "_ID of this object is not valid");
	// }
	// return Uri.withAppendedPath(URI_WRITE_MOVESUBTREE, Long.toString(_id));
	// }
	private Uri getMoveItemLeftUri() {
		if (_id < 1) {
			throw new InvalidParameterException(
					"_ID of this object is not valid");
		}
		return Uri.withAppendedPath(URI_WRITE_MOVEITEMLEFT, Long.toString(_id));
	}

	private Uri getMoveItemRightUri() {
		if (_id < 1) {
			throw new InvalidParameterException(
					"_ID of this object is not valid");
		}
		return Uri
				.withAppendedPath(URI_WRITE_MOVEITEMRIGHT, Long.toString(_id));
	}

    public static int delete(final long id, Context context) {
        if (id > 0) {
            return context.getContentResolver()
                    .delete(Uri.withAppendedPath(Uri.withAppendedPath(
                            Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
                            TABLE_NAME), Long.toString(id)),
                            null,
                            null);
        }
        throw new IllegalArgumentException("Can only delete positive ids");
    }

	public static class Columns implements BaseColumns {

		private Columns() {
		}

		public static final String TITLE = "title";
		public static final String NOTE = "note";
		public static final String DBLIST = "dblist";
		public static final String COMPLETED = "completed";
		public static final String DUE = "due";
		public static final String UPDATED = "updated";
		public static final String LOCKED = "locked";

		public static final String LEFT = "lft";
		public static final String RIGHT = "rgt";

		public static final String[] FIELDS = { _ID, TITLE, NOTE, COMPLETED,
				DUE, UPDATED, LEFT, RIGHT, DBLIST, LOCKED };
		public static final String[] FIELDS_NO_ID = { TITLE, NOTE, COMPLETED,
				DUE, UPDATED, LEFT, RIGHT, DBLIST, LOCKED };
		public static final String[] SHALLOWFIELDS = { _ID, TITLE, NOTE,
				DBLIST, COMPLETED, DUE, UPDATED, LOCKED };
		public static final String TRIG_DELETED = "deletedtime";
		public static final String HIST_TASK_ID = "taskid";
		// Used to read the table. Deleted field set by database
		public static final String[] DELETEFIELDS = { _ID, TITLE, NOTE,
				COMPLETED, DUE, DBLIST, TRIG_DELETED };
		// Used in trigger creation
		private static final String[] DELETEFIELDS_TRIGGER = { TITLE, NOTE,
				COMPLETED, DUE, DBLIST };

		// accessible fields in history table
		public static final String[] HISTORY_COLUMNS = { Columns.HIST_TASK_ID,
				Columns.TITLE, Columns.NOTE };
		public static final String[] HISTORY_COLUMNS_UPDATED = { Columns.HIST_TASK_ID,
			Columns.TITLE, Columns.NOTE, Columns.UPDATED };

	}

	public static final String CREATE_TABLE = new StringBuilder("CREATE TABLE ")
			.append(TABLE_NAME)
			.append("(")
			.append(Columns._ID)
			.append(" INTEGER PRIMARY KEY,")
			.append(Columns.TITLE)
			.append(" TEXT NOT NULL DEFAULT '',")
			.append(Columns.NOTE)
			.append(" TEXT NOT NULL DEFAULT '',")
			// These are all msec times
			.append(Columns.COMPLETED)
			.append(" INTEGER DEFAULT NULL,")
			.append(Columns.UPDATED)
			.append(" INTEGER DEFAULT NULL,")
			.append(Columns.DUE)
			.append(" INTEGER DEFAULT NULL,")
			// boolean, 1 for locked, unlocked otherwise
			.append(Columns.LOCKED)
			.append(" INTEGER NOT NULL DEFAULT 0,")

			// position stuff
			.append(Columns.LEFT)
			.append(" INTEGER NOT NULL DEFAULT 1,")
			.append(Columns.RIGHT)
			.append(" INTEGER NOT NULL DEFAULT 2,")
			.append(Columns.DBLIST)
			.append(" INTEGER NOT NULL,")

			// Positions must be positive and ordered!
			.append(" CHECK(")
			.append(Columns.LEFT)
			.append(" > 0), ")
			.append(" CHECK(")
			.append(Columns.RIGHT)
			.append(" > 1), ")
			// Each side's value should be unique in it's list
			// Handled in trigger
			// ).append(" UNIQUE(").append(Columns.LEFT).append(", ").append(Columns.DBLIST).append(")"
			// ).append(" UNIQUE(").append(Columns.RIGHT).append(", ").append(Columns.DBLIST).append(")"

			// Foreign key for list
			.append("FOREIGN KEY(").append(Columns.DBLIST)
			.append(") REFERENCES ").append(TaskList.TABLE_NAME).append("(")
			.append(TaskList.Columns._ID).append(") ON DELETE CASCADE")
			.append(")")

			.toString();

	// Delete table has no constraints. In fact, list values and positions
	// should not even be thought of as valid.
	public static final String CREATE_DELETE_TABLE = new StringBuilder(
			"CREATE TABLE ").append(DELETE_TABLE_NAME).append("(")
			.append(Columns._ID).append(" INTEGER PRIMARY KEY,")
			.append(Columns.TITLE).append(" TEXT NOT NULL DEFAULT '',")
			.append(Columns.NOTE).append(" TEXT NOT NULL DEFAULT '',")
			.append(Columns.COMPLETED).append(" INTEGER DEFAULT NULL,")
			.append(Columns.DUE).append(" INTEGER DEFAULT NULL,")
			.append(Columns.DBLIST).append(" INTEGER DEFAULT NULL,")
			.append(Columns.TRIG_DELETED)
			.append(" TIMESTAMP NOT NULL DEFAULT current_timestamp")
			.append(")").toString();

	// Every change to a note gets saved here
	public static final String CREATE_HISTORY_TABLE = new StringBuilder(
			"CREATE TABLE ").append(HISTORY_TABLE_NAME).append("(")
			.append(Columns._ID).append(" INTEGER PRIMARY KEY,")
			.append(Columns.HIST_TASK_ID).append(" INTEGER NOT NULL,")
			.append(Columns.TITLE).append(" TEXT NOT NULL DEFAULT '',")
			.append(Columns.NOTE).append(" TEXT NOT NULL DEFAULT '',")
			.append(Columns.UPDATED)
			.append(" TIMESTAMP NOT NULL DEFAULT current_timestamp,")
			.append(" FOREIGN KEY(").append(Columns.HIST_TASK_ID)
			.append(" ) REFERENCES ").append(TABLE_NAME).append(" ( ")
			.append(Columns._ID).append(") ON DELETE CASCADE ").append(" ) ")
			.toString();
	static final String HISTORY_TRIGGER_BODY = new StringBuilder(
			" INSERT INTO ")
			.append(HISTORY_TABLE_NAME)
			.append(" (")
			.append(arrayToCommaString(Columns.HISTORY_COLUMNS))
			.append(")")
			.append(" VALUES (")
			.append(arrayToCommaString("new.", new String[] { Columns._ID,
					Columns.TITLE, Columns.NOTE })).append(");").toString();
	public static final String HISTORY_UPDATE_TRIGGER_NAME = "trigger_update_" + HISTORY_TABLE_NAME;
	public static final String CREATE_HISTORY_UPDATE_TRIGGER = new StringBuilder(
			"CREATE TRIGGER ").append(HISTORY_UPDATE_TRIGGER_NAME)
			.append(" AFTER UPDATE OF ")
			.append(arrayToCommaString(new String[] { Columns.TITLE,
					Columns.NOTE })).append(" ON ").append(TABLE_NAME)

			.append(" WHEN old.")
			.append(Columns.TITLE).append(" IS NOT new.")
			.append(Columns.TITLE).append(" OR old.")
			.append(Columns.NOTE).append(" IS NOT new.")
			.append(Columns.NOTE)

			.append(" BEGIN ").append(HISTORY_TRIGGER_BODY).append(" END;")
			.toString();
	public static final String CREATE_HISTORY_INSERT_TRIGGER = new StringBuilder(
			"CREATE TRIGGER trigger_insert_").append(HISTORY_TABLE_NAME)
			.append(" AFTER INSERT ON ").append(TABLE_NAME).append(" BEGIN ")
			.append(HISTORY_TRIGGER_BODY).append(" END;").toString();

	// Delete search table
	public static final String CREATE_FTS3_DELETE_TABLE = "CREATE VIRTUAL TABLE "
			+ FTS3_DELETE_TABLE_NAME
			+ " USING FTS3("
			+ Columns._ID
			+ ", "
			+ Columns.TITLE + ", " + Columns.NOTE + ");";
	public static final String CREATE_FTS3_DELETED_INSERT_TRIGGER = new StringBuilder()
			.append("CREATE TRIGGER deletedtask_fts3_insert AFTER INSERT ON ")
			.append(DELETE_TABLE_NAME)
			.append(" BEGIN ")
			.append(" INSERT INTO ")
			.append(FTS3_DELETE_TABLE_NAME)
			.append(" (")
			.append(arrayToCommaString(Columns._ID, Columns.TITLE, Columns.NOTE))
			.append(") VALUES (")
			.append(arrayToCommaString("new.", new String[] { Columns._ID,
					Columns.TITLE, Columns.NOTE })).append(");")
			.append(" END;").toString();

	public static final String CREATE_FTS3_DELETED_UPDATE_TRIGGER = new StringBuilder()
			.append("CREATE TRIGGER deletedtask_fts3_update AFTER UPDATE OF ")
			.append(arrayToCommaString(new String[] { Columns.TITLE,
					Columns.NOTE })).append(" ON ").append(DELETE_TABLE_NAME)
			.append(" BEGIN ").append(" UPDATE ")
			.append(FTS3_DELETE_TABLE_NAME).append(" SET ")
			.append(Columns.TITLE).append(" = new.").append(Columns.TITLE)
			.append(",").append(Columns.NOTE).append(" = new.")
			.append(Columns.NOTE).append(" WHERE ").append(Columns._ID)
			.append(" IS new.").append(Columns._ID).append(";").append(" END;")
			.toString();
	public static final String CREATE_FTS3_DELETED_DELETE_TRIGGER = new StringBuilder()
			.append("CREATE TRIGGER deletedtask_fts3_delete AFTER DELETE ON ")
			.append(DELETE_TABLE_NAME).append(" BEGIN ")
			.append(" DELETE FROM ").append(FTS3_DELETE_TABLE_NAME)
			.append(" WHERE ").append(Columns._ID).append(" IS old.")
			.append(Columns._ID).append(";").append(" END;").toString();

	// Search table
	public static final String CREATE_FTS3_TABLE = "CREATE VIRTUAL TABLE "
			+ FTS3_TABLE_NAME + " USING FTS3(" + Columns._ID + ", "
			+ Columns.TITLE + ", " + Columns.NOTE + ");";

	public static final String CREATE_FTS3_INSERT_TRIGGER = new StringBuilder()
			.append("CREATE TRIGGER task_fts3_insert AFTER INSERT ON ")
			.append(TABLE_NAME)
			.append(" BEGIN ")
			.append(" INSERT INTO ")
			.append(FTS3_TABLE_NAME)
			.append(" (")
			.append(arrayToCommaString(Columns._ID, Columns.TITLE, Columns.NOTE))
			.append(") VALUES (")
			.append(arrayToCommaString("new.", new String[] { Columns._ID,
					Columns.TITLE, Columns.NOTE })).append(");")
			.append(" END;").toString();

	public static final String CREATE_FTS3_UPDATE_TRIGGER = new StringBuilder()
			.append("CREATE TRIGGER task_fts3_update AFTER UPDATE OF ")
			.append(arrayToCommaString(new String[] { Columns.TITLE,
					Columns.NOTE })).append(" ON ").append(TABLE_NAME)
			.append(" BEGIN ").append(" UPDATE ").append(FTS3_TABLE_NAME)
			.append(" SET ").append(Columns.TITLE).append(" = new.")
			.append(Columns.TITLE).append(",").append(Columns.NOTE)
			.append(" = new.").append(Columns.NOTE).append(" WHERE ")
			.append(Columns._ID).append(" IS new.").append(Columns._ID)
			.append(";").append(" END;").toString();

	public static final String CREATE_FTS3_DELETE_TRIGGER = new StringBuilder()
			.append("CREATE TRIGGER task_fts3_delete AFTER DELETE ON ")
			.append(TABLE_NAME).append(" BEGIN ").append(" DELETE FROM ")
			.append(FTS3_TABLE_NAME).append(" WHERE ").append(Columns._ID)
			.append(" IS old.").append(Columns._ID).append(";").append(" END;")
			.toString();

	/**
	 * This is a view which returns the tasks in the specified list with headers
	 * suitable for dates, if any tasks would be sorted under them. Provider
	 * hardcodes the sort order for this.
	 *
	 * if listId is null, will return for all lists
	 */
	public static final String CREATE_SECTIONED_DATE_VIEW(final String listId) {
		final String sListId = listId == null ? " NOT NULL " : "'" + listId
				+ "'";
		return new StringBuilder()
				.append("CREATE TEMP VIEW IF NOT EXISTS ")
				.append(getSECTION_DATE_VIEW_NAME(listId))
				// Tasks WITH dates NOT completed, secret 0
				.append(" AS SELECT ")
				.append(arrayToCommaString(Columns.FIELDS))
				.append(",0")
				.append(" AS ")
				.append(SECRET_TYPEID)
				.append(",1")
				.append(" AS ")
				.append(SECRET_TYPEID2)
				.append(" FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS null ")
				.append(" AND ")
				.append(Columns.DUE)
				.append(" IS NOT null ")
				.append(" UNION ALL ")
				// Tasks NO dates NOT completed, secret 1
				.append(" SELECT ")
				.append(arrayToCommaString(Columns.FIELDS))
				.append(",1")
				.append(" AS ")
				.append(SECRET_TYPEID)
				.append(",1")
				.append(" AS ")
				.append(SECRET_TYPEID2)
				.append(" FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS null ")
				.append(" AND ")
				.append(Columns.DUE)
				.append(" IS null ")
				.append(" UNION ALL ")
				// Tasks completed, secret 2 + 1
				.append(" SELECT ")
				.append(arrayToCommaString(Columns.FIELDS))
				.append(",3")
				.append(" AS ")
				.append(SECRET_TYPEID)
				.append(",1")
				.append(" AS ")
				.append(SECRET_TYPEID2)
				.append(" FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NOT null ")
				// TODAY
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, TODAY_START, Columns.TITLE,
						HEADER_KEY_TODAY, Columns.DBLIST, listId))
				.append(",0,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NULL ")
				.append(" AND ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" BETWEEN ")
				.append(TODAY_START)
				.append(" AND ")
				.append(TODAY_PLUS(1))
				.append(") ")
				// TOMORROW (Today + 1)
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, TODAY_PLUS(1), Columns.TITLE,
						HEADER_KEY_PLUS1, Columns.DBLIST, listId))
				.append(",0,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NULL ")
				.append(" AND ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" BETWEEN ")
				.append(TODAY_PLUS(1))
				.append(" AND ")
				.append(TODAY_PLUS(2))
				.append(") ")
				// Today + 2
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, TODAY_PLUS(2), Columns.TITLE,
						HEADER_KEY_PLUS2, Columns.DBLIST, listId))
				.append(",0,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NULL ")
				.append(" AND ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" BETWEEN ")
				.append(TODAY_PLUS(2))
				.append(" AND ")
				.append(TODAY_PLUS(3))
				.append(") ")
				// Today + 3
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, TODAY_PLUS(3), Columns.TITLE,
						HEADER_KEY_PLUS3, Columns.DBLIST, listId))
				.append(",0,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NULL ")
				.append(" AND ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" BETWEEN ")
				.append(TODAY_PLUS(3))
				.append(" AND ")
				.append(TODAY_PLUS(4))
				.append(") ")
				// Today + 4
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, TODAY_PLUS(4), Columns.TITLE,
						HEADER_KEY_PLUS4, Columns.DBLIST, listId))
				.append(",0,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NULL ")
				.append(" AND ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" BETWEEN ")
				.append(TODAY_PLUS(4))
				.append(" AND ")
				.append(TODAY_PLUS(5))
				.append(") ")
				// Overdue (0)
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, OVERDUE, Columns.TITLE,
						HEADER_KEY_OVERDUE, Columns.DBLIST, listId))
				.append(",0,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NULL ")
				.append(" AND ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" BETWEEN ")
				.append(OVERDUE)
				.append(" AND ")
				.append(TODAY_START)
				.append(") ")
				// Later
				.append(" UNION ALL ")
				.append(" SELECT '-1',")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, TODAY_PLUS(5), Columns.TITLE,
						HEADER_KEY_LATER, Columns.DBLIST, listId))
				.append(",0,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.COMPLETED)
				.append(" IS NULL ")
				.append(" AND ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" >= ")
				.append(TODAY_PLUS(5))
				.append(") ")
				// No date
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, "null", Columns.TITLE, HEADER_KEY_NODATE,
						Columns.DBLIST, listId))
				.append(",1,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ")
				.append(TABLE_NAME)
				.append(" WHERE ")
				.append(Columns.DBLIST)
				.append(" IS ")
				.append(sListId)
				.append(" AND ")
				.append(Columns.DUE)
				.append(" IS null ")
				.append(" AND ")
				.append(Columns.COMPLETED)
				.append(" IS null ")
				.append(") ")
				// Complete, overdue to catch all
				// Set complete time to 1
				.append(" UNION ALL ")
				.append(" SELECT -1,")
				.append(asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
						Columns.DUE, OVERDUE, Columns.COMPLETED, "1",
						Columns.TITLE, HEADER_KEY_COMPLETE, Columns.DBLIST,
						listId))
				.append(",2,0")
				// Only show header if there are tasks under it
				.append(" WHERE EXISTS(SELECT _ID FROM ").append(TABLE_NAME)
				.append(" WHERE ").append(Columns.DBLIST).append(" IS ")
				.append(sListId).append(" AND ").append(Columns.COMPLETED)
				.append(" IS NOT null ").append(") ")

				.append(";").toString();
	}

	public String title = null;
	public String note = null;
	// All milliseconds since 1970-01-01 UTC
	public Long completed = null;
	public Long due = null;
	public Long updated = null;
	// converted from integer
	public boolean locked = false;

	// position stuff
	public Long left = null;
	public Long right = null;
	public Long dblist = null;

	public Task() {

	}

	/**
	 * Resets id and position values
	 */
	public void resetForInsertion() {
		_id = -1;
		left = null;
		right = null;
	}

	/**
	 * Set task as completed. Returns the time stamp that is set.
	 */
	public Long setAsCompleted() {

		final Time time = new Time(Time.TIMEZONE_UTC);
		time.setToNow();
		completed = new Time().toMillis(false);
		return completed;
	}

	/**
	 * Set first line as title, rest as note.
	 *
	 * @param text
	 */
	public void setText(final String text) {
		int titleEnd = text.indexOf("\n");

		if (titleEnd < 0) {
			titleEnd = text.length();
		}

		title = text.substring(0, titleEnd);
		if (titleEnd + 1 < text.length()) {
			note = text.substring(titleEnd + 1, text.length());
		}
		else {
			note = "";
		}
	}

	/**
	 * Returns a text where first line is title, rest is note
	 */
	public String getText() {
		String result = "";
		if (title != null) {
			result += title;
		}
		if (note != null && !note.isEmpty()) {
            // If title is empty, it should remain so
			result += "\n" + note;
		}
		return result;
	}

	public Task(final Cursor c) {
		this._id = c.getLong(0);
		this.title = c.getString(1);
		note = c.getString(2);
		// msec times which can be null
		if (!c.isNull(3)) completed = c.getLong(3);
		if (!c.isNull(4)) due = c.getLong(4);
		if (!c.isNull(5)) updated = c.getLong(5);

		// enforced not to be null
		left = c.getLong(6);
		right = c.getLong(7);
		dblist = c.getLong(8);
		locked = c.getInt(9) == 1;
	}

	public Task(final long id, final ContentValues values) {
		this(values);
		this._id = id;
	}

	public Task(final Uri uri, final ContentValues values) {
		this(Long.parseLong(uri.getLastPathSegment()), values);
	}

	public Task(final ContentValues values) {
		if (values != null) {
			if (values.containsKey(TARGETPOS)) {
				// Content form getMoveValues
				this.left = values.getAsLong(Columns.LEFT);
				this.right = values.getAsLong(Columns.RIGHT);
				this.dblist = values.getAsLong(Columns.DBLIST);
			}
			else {
				this.title = values.getAsString(Columns.TITLE);
				this.note = values.getAsString(Columns.NOTE);
				this.completed = values.getAsLong(Columns.COMPLETED);
				this.due = values.getAsLong(Columns.DUE);
				this.updated = values.getAsLong(Columns.UPDATED);
				this.locked = values.getAsLong(Columns.LOCKED) == 1;

				this.dblist = values.getAsLong(Columns.DBLIST);
				this.left = values.getAsLong(Columns.LEFT);
				this.right = values.getAsLong(Columns.RIGHT);
			}
		}
	}

	public Task(final JSONObject json) throws JSONException {
		if (json.has(Columns.TITLE))
			this.title = json.getString(Columns.TITLE);
		if (json.has(Columns.NOTE))
			this.note = json.getString(Columns.NOTE);
		if (json.has(Columns.COMPLETED))
			this.completed = json.getLong(Columns.COMPLETED);
		if (json.has(Columns.DUE))
			this.due = json.getLong(Columns.DUE);
		if (json.has(Columns.UPDATED))
			this.updated = json.getLong(Columns.UPDATED);
		if (json.has(Columns.LOCKED))
			this.locked = json.getLong(Columns.LOCKED) == 1;
		if (json.has(Columns.DBLIST))
			this.dblist = json.getLong(Columns.DBLIST);
		if (json.has(Columns.LEFT))
			this.left = json.getLong(Columns.LEFT);
		if (json.has(Columns.RIGHT))
			this.right = json.getLong(Columns.RIGHT);
	}

	/**
	 * A move operation should be performed alone. No other information should
	 * accompany such an update.
	 */
	public ContentValues getMoveValues(final long targetPos) {
		final ContentValues values = new ContentValues();
		values.put(TARGETPOS, targetPos);
		values.put(Columns.LEFT, left);
		values.put(Columns.RIGHT, right);
		values.put(Columns.DBLIST, dblist);
		return values;
	}

	/**
	 * Use this for regular updates of the task.
	 */
	@Override
	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		// Note that ID is NOT included here
		if (title != null) values.put(Columns.TITLE, title);
		if (note != null) values.put(Columns.NOTE, note);

		if (dblist != null) values.put(Columns.DBLIST, dblist);
		// Never write position values unless you are 110% sure that they are correct
		//if (left != null) values.put(Columns.LEFT, left);
		//if (right != null) values.put(Columns.RIGHT, right);

		values.put(Columns.UPDATED, updated);
		values.put(Columns.DUE, due);
		values.put(Columns.COMPLETED, completed);
		values.put(Columns.LOCKED, locked ? 1 : 0);

		return values;
	}

	/**
	 * Compares this task to another and returns true if their contents are the
	 * same. Content is defined as: title, note, duedate, completed != null
	 * Returns false if title or note are null.
	 *
	 * The intended usage is the editor where content and not id's or position
	 * are of importance.
	 */
	@Override
	public boolean equals(Object o) {
		boolean result = false;

		if (o instanceof Task) {
			final Task other = (Task) o;
			result = true;

			result &= (title != null && title.equals(other.title));
			result &= (note != null && note.equals(other.note));
			result &= (due == other.due);
			result &= ((completed != null) == (other.completed != null));

		}
		else {
			result = super.equals(o);
		}

		return result;
	}

	/**
	 * Convenience method for normal operations. Updates "updated" field to
	 * specified Returns number of db-rows affected. Fail if < 1
	 */
	public int save(final Context context, final long updated) {
		int result = 0;
		this.updated = updated;
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

	/**
	 * Convenience method for normal operations. Updates "updated" field.
	 * Returns number of db-rows affected. Fail if < 1
	 */
	@Override
	public int save(final Context context) {
		return save(context, Calendar.getInstance().getTimeInMillis());
	}

	/**
	 * Convenience method to complete tasks in list view for example. Starts an
	 * asynctask to do the operation in the background.
	 */
	public static void setCompleted(final Context context,
			final boolean completed, final Long... ids) {
		if (ids.length > 0) {
			final AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {
				@Override
				protected Void doInBackground(final Long... ids) {
					setCompletedSynced(context, completed, ids);
					return null;
				}
			};
			task.execute(ids);
		}
	}

    /**
     * Convenience method to complete tasks. Runs on the thread that called it.
     * @param context
     * @param completed
     * @param ids
     */
    public static void setCompletedSynced(final Context context,
            final boolean completed, final Long... ids) {
        if (ids.length < 1) {
            return;
        }

        final ContentValues values = new ContentValues();
        values.put(Columns.COMPLETED, completed ? Calendar
                .getInstance().getTimeInMillis() : null);
        values.put(Columns.UPDATED, Calendar.getInstance()
                .getTimeInMillis());
        String idStrings = "(";
        for (Long id : ids) {
            idStrings += id + ",";
        }
        idStrings = idStrings.substring(0, idStrings.length() - 1);
        idStrings += ")";
        context.getContentResolver().update(URI, values,
                Columns._ID + " IN " + idStrings, null);
    }

	public int moveTo(final ContentResolver resolver, final Task targetTask) {
		if (targetTask.dblist.longValue() == dblist.longValue()) {
			if (targetTask.left < left) {
				// moving left
				return resolver.update(getMoveItemLeftUri(),
						getMoveValues(targetTask.left), null, null);
			}
			else if (targetTask.right > right) {
				// moving right
				return resolver.update(getMoveItemRightUri(),
						getMoveValues(targetTask.right), null, null);
			}
		}
		return 0;
	}

	@Override
	protected String getTableName() {
		return TABLE_NAME;
	}

	/**
	 * Can't use unique constraint on positions because SQLite checks
	 * constraints after every row is updated an not after each statement like
	 * it should. So have to do the check in a trigger instead.
	 */
	static final String countVals(final String col, final String ver) {
		return String.format("SELECT COUNT(DISTINCT %2$s)"
				+ " AS ColCount FROM %1$s WHERE %3$s=%4$s.%3$s", TABLE_NAME,
				col, Columns.DBLIST, ver);
	}

	// verify that left are unique
	// count number of id and compare to number of left and right
	static final String posUniqueConstraint(final String ver, final String msg) {
		return String.format(
				" SELECT CASE WHEN ((%1$s) != (%2$s) OR (%1$s) != (%3$s)) THEN "
						+ " RAISE (ABORT, '" + msg + "')" + " END;",
				countVals(Columns._ID, ver), countVals(Columns.LEFT, ver),
				countVals(Columns.RIGHT, ver));
	};

	// public static final String TRIGGER_POST_UPDATE = String.format(
	// "CREATE TRIGGER task_post_update AFTER UPDATE ON %1$s BEGIN "
	// + posUniqueConstraint("new", "pos not unique post update")
	// + posUniqueConstraint("old", "pos not unique post update")
	// + " END;", TABLE_NAME);

	// Makes a gap in the list where the task is being inserted
	private static final String BUMP_TO_RIGHT = " UPDATE %1$s SET %2$s = %2$s + 2, %3$s = %3$s + 2 WHERE %3$s >= new.%3$s AND %4$s IS new.%4$s;";
	public static final String TRIGGER_PRE_INSERT = String.format(
			"CREATE TRIGGER task_pre_insert BEFORE INSERT ON %s BEGIN ",
			TABLE_NAME)
			+ String.format(BUMP_TO_RIGHT, TABLE_NAME, Columns.RIGHT,
					Columns.LEFT, Columns.DBLIST) + " END;";

	public static final String TRIGGER_POST_INSERT = String.format(
			"CREATE TRIGGER task_post_insert AFTER INSERT ON %s BEGIN ",
			TABLE_NAME)
			// Enforce integrity
			+ posUniqueConstraint("new", "pos not unique post insert")

			+ " END;";

	// Upgrades children and closes the gap made from the delete
	private static final String BUMP_TO_LEFT = " UPDATE %1$s SET %2$s = %2$s - 2 WHERE %2$s > old.%3$s AND %4$s IS old.%4$s;";
	// private static final String UPGRADE_CHILDREN =
	// " UPDATE %1$s SET %2$s = %2$s - 1, %3$s = %3$s - 1 WHERE %4$s IS old.%4$s AND %2$s BETWEEN old.%2$s AND old.%3$s;";
	public static final String TRIGGER_POST_DELETE = String.format(
			"CREATE TRIGGER task_post_delete AFTER DELETE ON %s BEGIN ",
			TABLE_NAME)
			// + String.format(UPGRADE_CHILDREN, TABLE_NAME, Columns.LEFT,
			// Columns.RIGHT, Columns.DBLIST)
			+ String.format(BUMP_TO_LEFT, TABLE_NAME, Columns.LEFT,
					Columns.RIGHT, Columns.DBLIST)
			+ String.format(BUMP_TO_LEFT, TABLE_NAME, Columns.RIGHT,
					Columns.RIGHT, Columns.DBLIST)

			// Enforce integrity
			+ posUniqueConstraint("old", "pos not unique post delete")

			+ " END;";

	public static final String TRIGGER_PRE_DELETE = String.format(
			"CREATE TRIGGER task_pre_delete BEFORE DELETE ON %1$s BEGIN "
					+ " INSERT INTO %2$s ("
					+ arrayToCommaString("", Columns.DELETEFIELDS_TRIGGER, "")
					+ ") "
					+ " VALUES("
					+ arrayToCommaString("old.", Columns.DELETEFIELDS_TRIGGER,
							"") + "); "

					+ " END;", TABLE_NAME, DELETE_TABLE_NAME);

	public String getSQLMoveItemLeft(final ContentValues values) {
		if (!values.containsKey(TARGETPOS)
				|| values.getAsLong(TARGETPOS) >= left) {
			return null;
		}
		return getSQLMoveItem(Columns.LEFT, values.getAsLong(TARGETPOS));
	}

	public String getSQLMoveItemRight(final ContentValues values) {
		if (!values.containsKey(TARGETPOS)
				|| values.getAsLong(TARGETPOS) <= right) {
			return null;
		}
		return getSQLMoveItem(Columns.RIGHT, values.getAsLong(TARGETPOS));
	}

	/*
	 * Trigger to move between lists
	 */
	public static final String TRIGGER_MOVE_LIST = new StringBuilder()
			.append("CREATE TRIGGER trigger_post_move_list_")
			.append(TABLE_NAME)
			.append(" AFTER UPDATE OF ")
			.append(Task.Columns.DBLIST)
			.append(" ON ")
			.append(Task.TABLE_NAME)
			.append(" WHEN old.")
			.append(Task.Columns.DBLIST)
			.append(" IS NOT new.")
			.append(Task.Columns.DBLIST)
			.append(" BEGIN ")
			// Bump everything to the right, except the item itself (in same
			// list)
			.append(String
					.format("UPDATE %1$s SET %2$s = %2$s + 2, %3$s = %3$s + 2 WHERE %4$s IS new.%4$s AND %5$s IS NOT new.%5$s;",
							TABLE_NAME, Columns.LEFT, Columns.RIGHT,
							Columns.DBLIST, Columns._ID))

			// Bump everything left in the old list, to the right of position
			.append(String
					.format("UPDATE %1$s SET %2$s = %2$s - 2, %3$s = %3$s - 2 WHERE %2$s > old.%3$s AND %4$s IS old.%4$s;",
							TABLE_NAME, Columns.LEFT, Columns.RIGHT,
							Columns.DBLIST))

			// Set positions to 1 and 2 for item
			.append(String
					.format("UPDATE %1$s SET %2$s = 1, %3$s = 2 WHERE %4$s IS new.%4$s;",
							TABLE_NAME, Columns.LEFT, Columns.RIGHT,
							Columns._ID))

			.append(posUniqueConstraint("new", "Moving list, new positions not unique/ordered"))
			.append(posUniqueConstraint("old", "Moving list, old positions not unique/ordered"))

			.append(" END;").toString();

	/**
	 * If moving left, then edgeCol is left and vice-versa. Values should come
	 * from getMoveValues
	 *
	 * 1 = table name 2 = left 3 = right 4 = edgecol 5 = old.left 6 = old.right
	 * 7 = target.pos (actually target.edgecol) 8 = dblist 9 = old.dblist
	 */
	private String getSQLMoveItem(final String edgeCol, final Long edgeVal) {
		boolean movingLeft = Columns.LEFT.equals(edgeCol);
		return String
				.format(new StringBuilder("UPDATE %1$s SET ")
						/*
						 * Left item follows Left = Left + ...
						 */
						.append("%2$s = %2$s + ")
						.append(" CASE ")
						// Moving item jumps to target pos
						.append(" WHEN %2$s IS %5$d ")
						// ex: left = 5, target = 2, --> left = 5 + (2 - 5) == 2
						// ex left = 5, target = 9(right), --> left = 5 + (9 - 5
						// - 1) = 8
						.append(" THEN ")
						.append(" (%7$d - %5$d")
						.append(movingLeft ? ") " : " -1) ")
						// Sub items take one step opposite
						// Careful if moving inside subtree, which can only
						// happen when moving right.
						// Then only left position changes
						.append(" WHEN %2$s BETWEEN (%5$d + 1) AND (%6$d - 1) ")
						.append(" THEN ")
						.append(movingLeft ? " 1 " : " -1 ")
						// Items in between from and to positions take two steps
						// opposite
						.append(" WHEN %2$s BETWEEN ")
						.append(movingLeft ? "%7$d" : "%6$d")
						.append(" AND ")
						.append(movingLeft ? "%5$d" : "%7$d")
						.append(" THEN ")
						.append(movingLeft ? " 2 " : " -2 ")
						// Not in target range, no change
						.append(" ELSE 0 END, ")
						/*
						 * Right item follows Right = Right + ...
						 */
						.append(" %3$s = %3$s + ")
						.append(" CASE ")
						// Moving item jumps to target pos
						.append(" WHEN %3$s IS %6$d ")
						// ex: right = 7, target = 3(left), --> right = 7 + (3 -
						// 7 + 1) == 4
						// ex right = 2, target = 9(right), --> right = 2 + (9 -
						// 2) = 9
						.append(" THEN ")
						.append(" (%7$d - %6$d")
						.append(movingLeft ? " +1) " : ") ")
						// Sub items take one step opposite
						.append(" WHEN %3$s BETWEEN (%5$d + 1) AND (%6$d - 1) ")
						.append(" THEN ")
						.append(movingLeft ? " 1 " : " -1 ")
						// Items in between from and to positions take two steps
						// opposite
						.append(" WHEN %3$s BETWEEN ")
						.append(movingLeft ? "%7$d" : "%6$d").append(" AND ")
						.append(movingLeft ? "%5$d" : "%7$d").append(" THEN ")
						.append(movingLeft ? " 2 " : " -2 ")
						// Not in target range, no change
						.append(" ELSE 0 END ")
						// And limit to the list in question
						.append(" WHERE %8$s IS %9$d;").toString(), TABLE_NAME,
						Columns.LEFT, Columns.RIGHT, edgeCol, left, right,
						edgeVal, Columns.DBLIST, dblist);
	}

	/*
	 * @SuppressLint("DefaultLocale") public String getSQLMoveSubTree(final
	 * ContentValues values) { return
	 * String.format("UPDATE %1$s SET %2$s = %2$s + " +
	 * 
	 * " CASE " + // Tasks are moving left " WHEN (%4$d < %6$d) " +
	 * 
	 * " THEN CASE " + " WHEN %2$s BETWEEN %4$d AND (%6$d - 1) " + // Then they
	 * must flow [width] to the right " THEN %7$d - %6$d + 1 " +
	 * " WHEN %2$s BETWEEN %6$d AND %7$d " + // Tasks in subtree jump to the
	 * left // targetleft - left " THEN %4$d - %6$d " + // Do nothing otherwise
	 * " ELSE 0 END " + // Tasks are moving right " WHEN (%4$d > %6$d) " +
	 * " THEN CASE " + " WHEN %2$s BETWEEN (%7$d + 1) AND %4$d " + // Then move
	 * them [width] to the left " THEN %6$d - %7$d - 1" +
	 * " WHEN %2$s BETWEEN %6$d AND %7$d " + // Tasks in subtree jump to the
	 * right // targetleft - left
	 * 
	 * // Depends on if we are moving inside a task or // moving an entire one
	 * " THEN CASE WHEN %5$d > (%4$d + 1) " + " THEN %4$d - %7$d " +
	 * " ELSE %4$d - %7$d + 1 END " + // Do nothing otherwise " ELSE 0 END " +
	 * // No move actually performed. comma to do right next " ELSE 0 END, " +
	 * 
	 * " %3$s = %3$s + " + " CASE " + // Tasks are moving left
	 * " WHEN (%4$d < %6$d) " +
	 * 
	 * " THEN CASE " + // but only if right is left of originleft
	 * " WHEN %3$s BETWEEN %4$d AND (%6$d - 1)" + // Then they must flow [width]
	 * to the right " THEN %7$d - %6$d + 1" +
	 * " WHEN %2$s BETWEEN %6$d AND %7$d " + // Tasks in subtree jump to the
	 * left // targetleft - left " THEN %4$d - %6$d " + // Do nothing otherwise
	 * " ELSE 0 END " + // Tasks are moving right " WHEN (%4$d > %6$d) " +
	 * " THEN CASE " + // when right is between myright + 1 and targetleft + 1
	 * " WHEN %3$s BETWEEN (%7$d + 1) AND (%4$d + 1) " + // Then move them
	 * [width] to the left " THEN %6$d - %7$d - 1" +
	 * " WHEN %2$s BETWEEN %6$d AND %7$d " + // targetleft - left // Depends on
	 * if we are moving inside a task or // moving an entire one
	 * " THEN CASE WHEN %5$d > (%4$d + 1) " + " THEN %4$d - %7$d  " +
	 * " ELSE %4$d - %7$d + 1 END " + // Do nothing otherwise " ELSE 0 END " +
	 * // No move actually performed. End update with semicolon " ELSE 0 END " +
	 * " WHERE %8$s IS %9$d; "
	 * 
	 * //Enforce integrity + posUniqueConstraint("new",
	 * "pos not unique move sub tree") ,
	 * 
	 * TABLE_NAME, Columns.LEFT, Columns.RIGHT, values.getAsLong(TARGETLEFT),
	 * values.getAsLong(TARGETRIGHT), left, right, Columns.DBLIST, dblist
	 * 
	 * ); }
	 */

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}
}
