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

import androidx.annotation.NonNull;

import com.mobeta.android.dslv.DragSortListView;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

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

	public static String getSECTION_DATE_VIEW_NAME(final String listId) {
		// listId CAN be null. Hence the string concat hack
		return SECTIONED_DATE_VIEW + "_" + listId;
	}

	// Used in sectioned view date
	static final String FAR_FUTURE = "strftime('%s','3999-01-01') * 1000";
	public static final String OVERDUE = "strftime('%s', '1970-01-01') * 1000";

	// Today should be from NOW...
	public static final String TODAY_START = "strftime('%s','now', 'utc') * 1000";

	/**
	 * @param offset in days
	 */
	public static String TODAY_PLUS(final int offset) {
		return "strftime('%s','now','localtime','+" + offset
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
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#", BASEITEMCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				TABLE_NAME + "/" + MOVEITEMLEFT + "/#", MOVEITEMLEFTCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				TABLE_NAME + "/" + MOVEITEMRIGHT + "/#", MOVEITEMRIGHTCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				TABLE_NAME + "/" + DELETEDQUERY, DELETEDQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				TABLE_NAME + "/" + DELETEDQUERY + "/#", DELETEDITEMCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				TABLE_NAME + "/" + SECTIONED_DATE_VIEW, SECTIONEDDATEQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				TABLE_NAME + "/" + SECTIONED_DATE_VIEW + "/#", SECTIONEDDATEITEMCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				TABLE_NAME + "/" + HISTORY_TABLE_NAME, HISTORYQUERYCODE);

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
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, FTS3_TABLE_NAME, SEARCHCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				SearchManager.SUGGEST_URI_PATH_QUERY, SEARCHSUGGESTIONSCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCHSUGGESTIONSCODE);

	}

	public static final String TARGETPOS = "targetpos";
	private static final String MOVEITEMLEFT = "moveitemleft";
	private static final String MOVEITEMRIGHT = "moveitemright";
	private static final String DELETEDQUERY = "deletedquery";

	// Special URI to look at backup table
	public static final Uri URI_DELETED_QUERY = Uri.withAppendedPath(URI, DELETEDQUERY);

	// Query the view with date section headers
	public static final Uri URI_SECTIONED_BY_DATE = Uri.withAppendedPath(URI, SECTIONED_DATE_VIEW);

	// Query for history of tasks
	public static final Uri URI_TASK_HISTORY = Uri.withAppendedPath(URI, HISTORY_TABLE_NAME);

	// Search URI
	public static final Uri URI_SEARCH = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			FTS3_TABLE_NAME);

	// Special URI to use when a move is requested
	private static final Uri URI_WRITE_MOVEITEMLEFT = Uri.withAppendedPath(URI, MOVEITEMLEFT);
	private static final Uri URI_WRITE_MOVEITEMRIGHT = Uri.withAppendedPath(URI, MOVEITEMRIGHT);

	private Uri getMoveItemLeftUri() {
		if (_id < 1) {
			throw new InvalidParameterException("_ID of this object is not valid");
		}
		return Uri.withAppendedPath(URI_WRITE_MOVEITEMLEFT, Long.toString(_id));
	}

	private Uri getMoveItemRightUri() {
		if (_id < 1) {
			throw new InvalidParameterException("_ID of this object is not valid");
		}
		return Uri.withAppendedPath(URI_WRITE_MOVEITEMRIGHT, Long.toString(_id));
	}

	/**
	 * Contains each column of the SQLite table that contains {@link Task} objects,
	 * and functions to return them as lists
	 */
	public static class Columns implements BaseColumns {

		private Columns() {}

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

	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + Columns._ID +
			" INTEGER PRIMARY KEY," + Columns.TITLE + " TEXT NOT NULL DEFAULT ''," +
			Columns.NOTE + " TEXT NOT NULL DEFAULT ''," +
			// These are all msec times
			Columns.COMPLETED + " INTEGER DEFAULT NULL," + Columns.UPDATED +
			" INTEGER DEFAULT NULL," + Columns.DUE + " INTEGER DEFAULT NULL," +
			// boolean, 1 for locked, unlocked otherwise
			Columns.LOCKED + " INTEGER NOT NULL DEFAULT 0," +

			// position stuff
			Columns.LEFT + " INTEGER NOT NULL DEFAULT 1," + Columns.RIGHT +
			" INTEGER NOT NULL DEFAULT 2," + Columns.DBLIST + " INTEGER NOT NULL," +

			// Positions must be positive and ordered!
			" CHECK(" + Columns.LEFT + " > 0), " + " CHECK(" + Columns.RIGHT + " > 1), " +

			// Each side's value should be unique in it's list
			// Handled in trigger
			// + " UNIQUE(" + Columns.LEFT + ", " + Columns.DBLIST + ")"
			// + " UNIQUE(" + Columns.RIGHT + ", " + Columns.DBLIST + ")"

			// Foreign key for list
			"FOREIGN KEY(" + Columns.DBLIST + ") REFERENCES " + TaskList.TABLE_NAME + "(" +
			TaskList.Columns._ID + ") ON DELETE CASCADE" + ")";

	/**
	 * Delete table has no constraints. In fact, list values and positions should not even be
	 * thought of as valid
	 */
	public static final String CREATE_DELETE_TABLE = "CREATE TABLE " +
			DELETE_TABLE_NAME + "(" +
			Columns._ID + " INTEGER PRIMARY KEY," +
			Columns.TITLE + " TEXT NOT NULL DEFAULT ''," +
			Columns.NOTE + " TEXT NOT NULL DEFAULT ''," +
			Columns.COMPLETED + " INTEGER DEFAULT NULL," +
			Columns.DUE + " INTEGER DEFAULT NULL," +
			Columns.DBLIST + " INTEGER DEFAULT NULL," +
			Columns.TRIG_DELETED +
			" TIMESTAMP NOT NULL DEFAULT current_timestamp" +
			")";

	/**
	 * Every change to a note gets saved here
	 */
	public static final String CREATE_HISTORY_TABLE = "CREATE TABLE " +
			HISTORY_TABLE_NAME + "(" +
			Columns._ID + " INTEGER PRIMARY KEY," +
			Columns.HIST_TASK_ID + " INTEGER NOT NULL," +
			Columns.TITLE + " TEXT NOT NULL DEFAULT ''," +
			Columns.NOTE + " TEXT NOT NULL DEFAULT ''," +
			Columns.UPDATED +
			" TIMESTAMP NOT NULL DEFAULT current_timestamp," +
			" FOREIGN KEY(" + Columns.HIST_TASK_ID +
			" ) REFERENCES " + TABLE_NAME + " ( " +
			Columns._ID + ") ON DELETE CASCADE " + " ) ";

	static final String HISTORY_TRIGGER_BODY = " INSERT INTO " + HISTORY_TABLE_NAME + " (" +
			arrayToCommaString(Columns.HISTORY_COLUMNS) + ")" + " VALUES (" +
			arrayToCommaString("new.",
					new String[] { Columns._ID, Columns.TITLE, Columns.NOTE }) + ");";

	public static final String HISTORY_UPDATE_TRIGGER_NAME = "trigger_update_" + HISTORY_TABLE_NAME;
	public static final String CREATE_HISTORY_UPDATE_TRIGGER = "CREATE TRIGGER " +
			HISTORY_UPDATE_TRIGGER_NAME + " AFTER UPDATE OF " +
			arrayToCommaString(Columns.TITLE, Columns.NOTE) + " ON " + TABLE_NAME + " WHEN old." +
			Columns.TITLE + " IS NOT new." + Columns.TITLE + " OR old." + Columns.NOTE +
			" IS NOT new." + Columns.NOTE + " BEGIN " + HISTORY_TRIGGER_BODY + " END;";

	public static final String CREATE_HISTORY_INSERT_TRIGGER = "CREATE TRIGGER trigger_insert_" +
			HISTORY_TABLE_NAME + " AFTER INSERT ON " + TABLE_NAME + " BEGIN " +
			HISTORY_TRIGGER_BODY + " END;";

	// Delete search table
	public static final String CREATE_FTS3_DELETE_TABLE = "CREATE VIRTUAL TABLE "
			+ FTS3_DELETE_TABLE_NAME + " USING FTS3(" + Columns._ID + ", "
			+ Columns.TITLE + ", " + Columns.NOTE + ");";

	public static final String CREATE_FTS3_DELETED_INSERT_TRIGGER =
			"CREATE TRIGGER deletedtask_fts3_insert AFTER INSERT ON " + DELETE_TABLE_NAME +
					" BEGIN " + " INSERT INTO " + FTS3_DELETE_TABLE_NAME + " (" +
					arrayToCommaString(Columns._ID, Columns.TITLE, Columns.NOTE) + ") VALUES (" +
					arrayToCommaString("new.",
							new String[] { Columns._ID, Columns.TITLE, Columns.NOTE }) +
					");" + " END;";

	public static final String CREATE_FTS3_DELETED_UPDATE_TRIGGER =
			"CREATE TRIGGER deletedtask_fts3_update AFTER UPDATE OF " +
					arrayToCommaString(Columns.TITLE, Columns.NOTE) +
					" ON " +
					DELETE_TABLE_NAME +
					" BEGIN " +
					" UPDATE " +
					FTS3_DELETE_TABLE_NAME +
					" SET " +
					Columns.TITLE + " = new." + Columns.TITLE +
					"," + Columns.NOTE + " = new." +
					Columns.NOTE + " WHERE " + Columns._ID +
					" IS new." + Columns._ID + ";" + " END;";

	public static final String CREATE_FTS3_DELETED_DELETE_TRIGGER =
			"CREATE TRIGGER deletedtask_fts3_delete AFTER DELETE ON " +
					DELETE_TABLE_NAME + " BEGIN " +
					" DELETE FROM " + FTS3_DELETE_TABLE_NAME +
					" WHERE " + Columns._ID + " IS old." +
					Columns._ID + ";" + " END;";

	// Search table
	public static final String CREATE_FTS3_TABLE = "CREATE VIRTUAL TABLE "
			+ FTS3_TABLE_NAME + " USING FTS3(" + Columns._ID + ", "
			+ Columns.TITLE + ", " + Columns.NOTE + ");";

	public static final String CREATE_FTS3_INSERT_TRIGGER =
			"CREATE TRIGGER task_fts3_insert AFTER INSERT ON " +
					TABLE_NAME +
					" BEGIN " +
					" INSERT INTO " +
					FTS3_TABLE_NAME +
					" (" +
					arrayToCommaString(Columns._ID, Columns.TITLE, Columns.NOTE) +
					") VALUES (" +
					arrayToCommaString("new.", new String[] { Columns._ID,
							Columns.TITLE, Columns.NOTE }) +
					");" +
					" END;";

	public static final String CREATE_FTS3_UPDATE_TRIGGER =
			"CREATE TRIGGER task_fts3_update AFTER UPDATE OF " +
					arrayToCommaString(Columns.TITLE, Columns.NOTE) + " ON " + TABLE_NAME +
					" BEGIN " + " UPDATE " + FTS3_TABLE_NAME + " SET " + Columns.TITLE + " = new." +
					Columns.TITLE + "," + Columns.NOTE + " = new." + Columns.NOTE + " WHERE " +
					Columns._ID + " IS new." + Columns._ID + ";" + " END;";

	public static final String CREATE_FTS3_DELETE_TRIGGER =
			"CREATE TRIGGER task_fts3_delete AFTER DELETE ON " + TABLE_NAME + " BEGIN " +
					" DELETE FROM " + FTS3_TABLE_NAME + " WHERE " + Columns._ID + " IS old." +
					Columns._ID + ";" + " END;";

	/**
	 * This is a view which returns the tasks in the specified list with headers
	 * suitable for dates, if any tasks would be sorted under them. Headers are used
	 * in the {@link DragSortListView}.
	 * Provider hardcodes the sort order for this.
	 *
	 * @param listId if it is null, the function will return (a query) for all lists
	 * @return a SQL query to create this view
	 */
	public static String CREATE_SECTIONED_DATE_VIEW(final String listId) {
		final String sListId = listId == null ? " NOT NULL " : "'" + listId + "'";
		// TODO this creates a SQLite view that the drag-sort-listview uses for the headers
		//  when ordering tasks by date. add here the code for HEADER_KEY_NEXT_MONTH,
		//  HEADER_KEY_NEXT_YEAR and so on
		String beginning = "CREATE TEMP VIEW IF NOT EXISTS " + getSECTION_DATE_VIEW_NAME(listId) +
				// Tasks WITH dates NOT completed, secret 0
				" AS SELECT " + arrayToCommaString(Columns.FIELDS) + ",0" + " AS " + SECRET_TYPEID +
				",1" + " AS " + SECRET_TYPEID2 + " FROM " + TABLE_NAME + " WHERE " +
				Columns.COMPLETED + " IS null " + " AND " + Columns.DUE + " IS NOT null " +
				" UNION ALL " +
				// Tasks NO dates NOT completed, secret 1
				" SELECT " + arrayToCommaString(Columns.FIELDS) + ",1" + " AS " + SECRET_TYPEID +
				",1" + " AS " + SECRET_TYPEID2 + " FROM " + TABLE_NAME + " WHERE " +
				Columns.COMPLETED + " IS null " + " AND " + Columns.DUE + " IS null " +
				" UNION ALL " +
				// Tasks completed, secret 2 + 1
				" SELECT " + arrayToCommaString(Columns.FIELDS) + ",3" + " AS " + SECRET_TYPEID +
				",1" + " AS " + SECRET_TYPEID2 + " FROM " + TABLE_NAME + " WHERE " +
				Columns.COMPLETED + " IS NOT null ";

		String TODAY = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, TODAY_START, Columns.TITLE, HEADER_KEY_TODAY, Columns.DBLIST, listId) +
				",0,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.COMPLETED +
				" IS NULL " + " AND " + Columns.DBLIST + " IS " + sListId + " AND " + Columns.DUE +
				" BETWEEN " + TODAY_START + " AND " + TODAY_PLUS(1) + ") ";

		// TOMORROW = Today + 1
		String PLUS_1 = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, TODAY_PLUS(1), Columns.TITLE, HEADER_KEY_PLUS1, Columns.DBLIST,
				listId) + ",0,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.COMPLETED +
				" IS NULL " + " AND " + Columns.DBLIST + " IS " + sListId + " AND " + Columns.DUE +
				" BETWEEN " + TODAY_PLUS(1) + " AND " + TODAY_PLUS(2) + ") ";

		// Today + 2
		String PLUS_2 = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, TODAY_PLUS(2), Columns.TITLE, HEADER_KEY_PLUS2, Columns.DBLIST,
				listId) + ",0,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.COMPLETED +
				" IS NULL " + " AND " + Columns.DBLIST + " IS " + sListId + " AND " + Columns.DUE +
				" BETWEEN " + TODAY_PLUS(2) + " AND " + TODAY_PLUS(3) + ") ";

		// Today + 3
		String PLUS_3 = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, TODAY_PLUS(3), Columns.TITLE, HEADER_KEY_PLUS3, Columns.DBLIST,
				listId) + ",0,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.COMPLETED +
				" IS NULL " + " AND " + Columns.DBLIST + " IS " + sListId + " AND " + Columns.DUE +
				" BETWEEN " + TODAY_PLUS(3) + " AND " + TODAY_PLUS(4) + ") ";

		// Today + 4
		String PLUS_4 = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, TODAY_PLUS(4), Columns.TITLE, HEADER_KEY_PLUS4, Columns.DBLIST,
				listId) + ",0,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.COMPLETED +
				" IS NULL " + " AND " + Columns.DBLIST + " IS " + sListId + " AND " + Columns.DUE +
				" BETWEEN " + TODAY_PLUS(4) + " AND " + TODAY_PLUS(5) + ") ";

		// Overdue (0)
		String overdue = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, OVERDUE, Columns.TITLE, HEADER_KEY_OVERDUE, Columns.DBLIST, listId) +
				",0,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.COMPLETED +
				" IS NULL " + " AND " + Columns.DBLIST + " IS " + sListId + " AND " + Columns.DUE +
				" BETWEEN " + OVERDUE + " AND " + TODAY_START + ") ";

		// Later
		String later = " UNION ALL " + " SELECT '-1'," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, TODAY_PLUS(5), Columns.TITLE, HEADER_KEY_LATER, Columns.DBLIST,
				listId) + ",0,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.COMPLETED +
				" IS NULL " + " AND " + Columns.DBLIST + " IS " + sListId + " AND " + Columns.DUE +
				" >= " + TODAY_PLUS(5) + ") ";

		// No date
		String noDate = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, "null", Columns.TITLE, HEADER_KEY_NODATE, Columns.DBLIST,
				listId) + ",1,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.DBLIST +
				" IS " + sListId + " AND " + Columns.DUE + " IS null " + " AND " +
				Columns.COMPLETED + " IS null " + ") ";

		// Complete, overdue to catch all
		// Set complete time to 1
		String finalSql = " UNION ALL " + " SELECT -1," + asEmptyCommaStringExcept(Columns.FIELDS_NO_ID,
				Columns.DUE, OVERDUE, Columns.COMPLETED, "1", Columns.TITLE,
				HEADER_KEY_COMPLETE, Columns.DBLIST, listId) + ",2,0" +
				// Only show header if there are tasks under it
				" WHERE EXISTS(SELECT _ID FROM " + TABLE_NAME + " WHERE " + Columns.DBLIST +
				" IS " + sListId + " AND " + Columns.COMPLETED + " IS NOT null " + ") " + ";";

		return beginning + TODAY + PLUS_1 + PLUS_2 + PLUS_3 + PLUS_4 + overdue + later + noDate
				+ finalSql;
	}

	/**
	 * Fields of this note
	 */
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

	public Task() {}

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
	 */
	public void setText(final String text) {
		int titleEnd = text.indexOf("\n");

		if (titleEnd < 0) {
			titleEnd = text.length();
		}

		title = text.substring(0, titleEnd);
		if (titleEnd + 1 < text.length()) {
			note = text.substring(titleEnd + 1);
		} else {
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
			if (result.length() > 0) {
				result += "\n";
			}
			result += note;
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
			} else {
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
		boolean result;

		if (o instanceof Task) {
			final Task other = (Task) o;
			result = true;

			result &= (title != null && title.equals(other.title));
			result &= (note != null && note.equals(other.note));
			result &= (Objects.equals(due, other.due));
			result &= ((completed != null) == (other.completed != null));

		} else {
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
			final Uri uri = context
					.getContentResolver()
					.insert(getBaseUri(), getContent());
			if (uri != null) {
				_id = Long.parseLong(uri.getLastPathSegment());
				result++;
			}
		} else {
			result += context
					.getContentResolver()
					.update(getUri(), getContent(), null, null);
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
			final AsyncTask<Long, Void, Void> task = new AsyncTask<>() {
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
	 */
	public static void setCompletedSynced(final Context context,
										  final boolean completed, final Long... ids) {
		if (ids.length < 1) {
			return;
		}

		long thisInstant = Calendar.getInstance().getTimeInMillis();
		final ContentValues values = new ContentValues();
		values.put(Columns.COMPLETED, completed ? thisInstant : null);
		values.put(Columns.UPDATED, thisInstant);

		String idStrings = "(";
		for (Long id : ids) {
			idStrings += id + ",";
		}
		idStrings = idStrings.substring(0, idStrings.length() - 1);
		idStrings += ")";
		context.getContentResolver()
				.update(URI, values, Columns._ID + " IN " + idStrings, null);
	}

	public int moveTo(final ContentResolver resolver, final Task targetTask) {
		if (targetTask.dblist.equals(dblist)) {
			if (targetTask.left < left) {
				// moving left
				return resolver.update(getMoveItemLeftUri(),
						getMoveValues(targetTask.left), null, null);
			} else if (targetTask.right > right) {
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
	static String countVals(final String col, final String ver) {
		return String.format("SELECT COUNT(DISTINCT %2$s)"
						+ " AS ColCount FROM %1$s WHERE %3$s=%4$s.%3$s", TABLE_NAME,
				col, Columns.DBLIST, ver);
	}

	// verify that left are unique
	// count number of id and compare to number of left and right
	static String posUniqueConstraint(final String ver, final String msg) {
		return String.format(
				" SELECT CASE WHEN ((%1$s) != (%2$s) OR (%1$s) != (%3$s)) THEN "
						+ " RAISE (ABORT, '" + msg + "')" + " END;",
				countVals(Columns._ID, ver), countVals(Columns.LEFT, ver),
				countVals(Columns.RIGHT, ver));
	}

	// Makes a gap in the list where the task is being inserted
	private static final String BUMP_TO_RIGHT =
			" UPDATE %1$s SET %2$s = %2$s + 2, %3$s = %3$s + 2 WHERE %3$s >= new.%3$s AND %4$s IS new.%4$s;";

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
	private static final String BUMP_TO_LEFT =
			" UPDATE %1$s SET %2$s = %2$s - 2 WHERE %2$s > old.%3$s AND %4$s IS old.%4$s;";

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
	public static final String TRIGGER_MOVE_LIST = "CREATE TRIGGER trigger_post_move_list_" +
			TABLE_NAME +
			" AFTER UPDATE OF " +
			Columns.DBLIST +
			" ON " +
			Task.TABLE_NAME +
			" WHEN old." +
			Columns.DBLIST +
			" IS NOT new." +
			Columns.DBLIST +
			" BEGIN " +
			// Bump everything to the right, except the item itself (in same
			// list)
			String.format("UPDATE %1$s SET %2$s = %2$s + 2, %3$s = %3$s + 2 WHERE %4$s IS new.%4$s AND %5$s IS NOT new.%5$s;",
					TABLE_NAME, Columns.LEFT, Columns.RIGHT, Columns.DBLIST, Columns._ID) +

			// Bump everything left in the old list, to the right of position
			String.format("UPDATE %1$s SET %2$s = %2$s - 2, %3$s = %3$s - 2 WHERE %2$s > old.%3$s AND %4$s IS old.%4$s;",
					TABLE_NAME, Columns.LEFT, Columns.RIGHT, Columns.DBLIST) +

			// Set positions to 1 and 2 for item
			String.format("UPDATE %1$s SET %2$s = 1, %3$s = 2 WHERE %4$s IS new.%4$s;",
					TABLE_NAME, Columns.LEFT, Columns.RIGHT, Columns._ID) +
			posUniqueConstraint("new", "Moving list, new positions not unique/ordered") +
			posUniqueConstraint("old", "Moving list, old positions not unique/ordered") +
			" END;";

	/**
	 * If moving left, then edgeCol is left and vice-versa. Values should come
	 * from getMoveValues
	 *
	 * 1 = table name 2 = left 3 = right 4 = edgecol 5 = old.left 6 = old.right
	 * 7 = target.pos (actually target.edgecol) 8 = dblist 9 = old.dblist
	 */
	private String getSQLMoveItem(final String edgeCol, final Long edgeVal) {
		boolean movingLeft = Columns.LEFT.equals(edgeCol);
		return String.format("UPDATE %1$s SET " +
						// Left item follows Left = Left + ...
						"%2$s = %2$s + " +
						" CASE " +
						// Moving item jumps to target pos
						" WHEN %2$s IS %5$d " +
						// ex: left = 5, target = 2, --> left = 5 + (2 - 5) == 2
						// ex left = 5, target = 9(right), --> left = 5 + (9 - 5 - 1) = 8
						" THEN " +
						" (%7$d - %5$d" +
						(movingLeft ? ") " : " -1) ") +
						// Sub items take one step opposite
						// Careful if moving inside subtree, which can only
						// happen when moving right.
						// Then only left position changes
						" WHEN %2$s BETWEEN (%5$d + 1) AND (%6$d - 1) " +
						" THEN " +
						(movingLeft ? " 1 " : " -1 ") +
						// Items in between from and to positions take two steps opposite
						" WHEN %2$s BETWEEN " +
						(movingLeft ? "%7$d" : "%6$d") +
						" AND " +
						(movingLeft ? "%5$d" : "%7$d") +
						" THEN " +
						(movingLeft ? " 2 " : " -2 ") +
						// Not in target range, no change
						" ELSE 0 END, " +
						/*
						 * Right item follows Right = Right + ...
						 */
						" %3$s = %3$s + " +
						" CASE " +
						// Moving item jumps to target pos
						" WHEN %3$s IS %6$d " +
						// ex: right = 7, target = 3(left), --> right = 7 + (3 - 7 + 1) == 4
						// ex right = 2, target = 9(right), --> right = 2 + (9 -  2) = 9
						" THEN " +
						" (%7$d - %6$d" +
						(movingLeft ? " +1) " : ") ") +
						// Sub items take one step opposite
						" WHEN %3$s BETWEEN (%5$d + 1) AND (%6$d - 1) " +
						" THEN " +
						(movingLeft ? " 1 " : " -1 ") +
						// Items in between from and to positions take two steps opposite
						" WHEN %3$s BETWEEN " +
						(movingLeft ? "%7$d" : "%6$d") + " AND " +
						(movingLeft ? "%5$d" : "%7$d") + " THEN " +
						(movingLeft ? " 2 " : " -2 ") +
						// Not in target range, no change
						" ELSE 0 END " +
						// And limit to the list in question
						" WHERE %8$s IS %9$d;", TABLE_NAME,
				Columns.LEFT, Columns.RIGHT, edgeCol, left, right, edgeVal, Columns.DBLIST, dblist);
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

	/*
	 * commodity functions added after 2022
	 */

	/**
	 * @param input         The {@link String} received from the {@link Cursor} which, I think,
	 *                      comes from the query on the view returned by
	 *                      {@link #CREATE_SECTIONED_DATE_VIEW}
	 * @param dueDateMillis the value of {@link Task.Columns#DUE} from the same {@link Cursor}
	 *                      that gave you the "input" parameter
	 * @param formatterObj  an object that specifies how days are shown. Usually it comes from
	 *                      {@link TimeFormatter#getLocalFormatterWeekday}
	 * @return the name to show on a header of the {@link DragSortListView} when the notes are
	 * ordered by date. For example, "Tomorrow", or "Later", depending on when the note
	 * is due.
	 */
	public static String getHeaderNameForListSortedByDate(String input, long dueDateMillis,
														  @NonNull SimpleDateFormat formatterObj,
														  @NonNull Context context) {
		String sTemp;
		if (Task.HEADER_KEY_OVERDUE.equals(input)) {
			sTemp = context.getString(R.string.date_header_overdue);
		} else if (Task.HEADER_KEY_TODAY.equals(input)) {
			sTemp = context.getString(R.string.date_header_today);
		} else if (Task.HEADER_KEY_PLUS1.equals(input)) {
			sTemp = context.getString(R.string.date_header_tomorrow);
		} else if (Task.HEADER_KEY_PLUS2.equals(input)
				|| Task.HEADER_KEY_PLUS3.equals(input)
				|| Task.HEADER_KEY_PLUS4.equals(input)) {
			// TODO if you want to show text like "next month" in the
			//  drag-sort-listview, you would add your code here.
			//  As of now, it divides taks by next 2, 3 or 4 days, and "later"
			//  for everything else. I think I'll add:
			//  * HEADER_KEY_NEXT_WEEK or HEADER_KEY_PLUS7
			//  * HEADER_KEY_NEXT_MONTH or HEADER_KEY_PLUS30
			//  * HEADER_KEY_NEXT_YEAR or HEADER_KEY_PLUS365
			//  see also CREATE_SECTIONED_DATE_VIEW, which is where these are made in the database
			sTemp = formatterObj.format(new Date(dueDateMillis));
		} else if (Task.HEADER_KEY_LATER.equals(input)) {
			sTemp = context.getString(R.string.date_header_future);
		} else if (Task.HEADER_KEY_NODATE.equals(input)) {
			sTemp = context.getString(R.string.date_header_none);
		} else if (Task.HEADER_KEY_COMPLETE.equals(input)) {
			sTemp = context.getString(R.string.date_header_completed);
		} else {
			// for compatibility with the old version, but i don't think it happens...
			sTemp = input;
		}
		return sTemp;
	}
}
