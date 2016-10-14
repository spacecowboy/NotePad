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

package com.nononsenseapps.notepad.data.local.sql;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.util.Log;
import com.nononsenseapps.notepad.util.RFC3339Date;

/**
 * This class contains the code that has been called over the versions to
 * upgrade the database. Upgrades should be saved here as plain text to enable a
 * linear progression from 1.0 to current version without problems even if the
 * entire database is changed.
 * 
 * onUpgrade should be called first from the databaseopenhelper's onUpgrade
 * method.
 * 
 */
public class LegacyDBHelper extends SQLiteOpenHelper {

	public static final String LEGACY_DATABASE_NAME = "note_pad.db";
	public static final int LEGACY_DATABASE_FINAL_VERSION = 8;

	public LegacyDBHelper(Context context) {
		this(context, "");
	}

	public LegacyDBHelper(Context context, String testPrefix) {
		super(context.getApplicationContext(), testPrefix
				+ LEGACY_DATABASE_NAME, null, LEGACY_DATABASE_FINAL_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Don't create anything if the database doesn't exist before.
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("LegacyHelper", "onUpgrade " + "Upgrading database from version "
				+ oldVersion + " to " + newVersion);

		if (oldVersion < 3) {
			// FIrst add columns to Notes table

			String preName = "ALTER TABLE " + "notes" + " ADD COLUMN ";
			// Don't want null values. Prefer empty String
			String postText = " TEXT";
			String postNameInt = " INTEGER";
			// Add Columns to Notes DB
			db.execSQL(preName + "list" + postNameInt);
			db.execSQL(preName + "duedate" + postText);
			db.execSQL(preName + "gtaskstatus" + postText);
			db.execSQL(preName + "modifiedflag" + postNameInt);
			db.execSQL(preName + "deleted" + postNameInt);

			// Then create the 3 missing tables
			db.execSQL("CREATE TABLE " + "lists" + " (" + BaseColumns._ID
					+ " INTEGER PRIMARY KEY," + "title"
					+ " TEXT DEFAULT '' NOT NULL," + "modifiedflag"
					+ " INTEGER DEFAULT 0 NOT NULL," + "modified"
					+ " INTEGER DEFAULT 0 NOT NULL," + "deleted"
					+ " INTEGER DEFAULT 0 NOT NULL" + ");");

			db.execSQL("CREATE TABLE " + "gtasks" + " (" + BaseColumns._ID
					+ " INTEGER PRIMARY KEY," + "dbid"
					+ " INTEGER UNIQUE NOT NULL REFERENCES " + "notes" + ","
					+ "googleid" + " INTEGER NOT NULL," + "googleaccount"
					+ " INTEGER NOT NULL," + "updated" + " TEXT," + "etag"
					+ " TEXT" + ");");

			db.execSQL("CREATE TABLE " + "gtasklists" + " (" + BaseColumns._ID
					+ " INTEGER PRIMARY KEY," + "dbid"
					+ " INTEGER UNIQUE NOT NULL REFERENCES " + "lists" + ","
					+ "googleid" + " INTEGER NOT NULL," + "googleaccount"
					+ " INTEGER NOT NULL," + "updated" + " TEXT," + "etag"
					+ " TEXT" + ");");

			// Now insert a default list
			ContentValues values = new ContentValues();
			values.put("title", "Tasks");
			values.put("modifiedflag", 1);
			values.put("deleted", 0);
			long listId = db.insert("lists", null, values);

			// Place all existing notes in this list
			// And give them sensible values in the new columns
			values.clear();
			values.put("list", listId);
			values.put("modifiedflag", 1);
			values.put("deleted", 0);
			values.put("duedate", "");
			values.put("gtaskstatus", "needsAction");

			db.update("notes", values, "list" + " IS NOT ?",
					new String[] { Long.toString(listId) });
		}
		if (oldVersion < 4) {

			String preName = "ALTER TABLE " + "notes" + " ADD COLUMN ";
			String postText = " TEXT";
			String postNameInt = " INTEGER";
			// Add Columns to Notes DB
			db.execSQL(preName + "gtasks_parent" + postText);
			db.execSQL(preName + "gtasks_position" + postText);
			db.execSQL(preName + "hiddenflag" + postNameInt);

			// Give all notes sensible values
			ContentValues values = new ContentValues();
			values.put("gtasks_parent", "");
			values.put("gtasks_position", "");
			values.put("hiddenflag", 0);
			db.update("notes", values, "hiddenflag" + " IS NOT ?",
					new String[] { "0" });
		}
		if (oldVersion < 5) {

			String preName = "ALTER TABLE " + "notes" + " ADD COLUMN ";
			String postText = " TEXT DEFAULT ''";
			String postNameInt = " INTEGER DEFAULT 0";
			db.execSQL(preName + "possubsort" + postText);
			db.execSQL(preName + "localhidden" + postNameInt);
		}
		if (oldVersion < 6) {
			// Add Columns to Notes DB
			String preName = "ALTER TABLE " + "notes" + " ADD COLUMN ";
			String postNameInt = " INTEGER DEFAULT 0";
			db.execSQL(preName + "indentlevel" + postNameInt);
			db.execSQL(preName + "locked" + postNameInt);

			// Mark all notes as modified to ensure we set the indents on
			// next sync
			ContentValues values = new ContentValues();
			values.put("modifiedflag", 1);
			db.update("notes", values, null, null);
		}
		if (oldVersion < 7) {
			db.execSQL("CREATE TABLE " + "notification" + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY," + "time"
					+ " INTEGER NOT NULL DEFAULT 0," + "permanent"
					+ " INTEGER NOT NULL DEFAULT 0," + "noteid" + " INTEGER,"
					+ "FOREIGN KEY(" + "noteid" + ") REFERENCES " + "notes"
					+ "(" + BaseColumns._ID + ") ON DELETE CASCADE" + ");");
		}
		if (oldVersion < 8) {
			try {
				db.execSQL("CREATE TRIGGER post_note_markdelete AFTER UPDATE ON "
						+ "notes"
						+ " WHEN new."
						+ "deleted"
						+ " = 1"
						+ " BEGIN"
						+ "   DELETE FROM "
						+ "notification"
						+ "   WHERE "
						+ "notification"
						+ "."
						+ "noteid"
						+ "   = " + "new." + BaseColumns._ID + ";" + " END");
			}
			catch (SQLException e) {
				// Log.d(TAG,
				// "Creating trigger failed. It probably already existed:\n " +
				// e.getLocalizedMessage());
			}

			try {
				db.execSQL("CREATE TRIGGER post_note_actualdelete AFTER DELETE ON "
						+ "notes"
						+ " BEGIN"
						+ "   DELETE FROM "
						+ "notification"
						+ "   WHERE "
						+ "notification"
						+ "."
						+ "noteid"
						+ "   = "
						+ "old."
						+ BaseColumns._ID
						+ ";"
						+ " END");
			}
			catch (SQLException e) {
				// Log.d(TAG,
				// "Creating trigger failed. It probably already existed:\n " +
				// e.getLocalizedMessage());
			}
		}
	}

	public static final class NotePad {
		public static final String AUTHORITY = MyContentProvider.AUTHORITY;

		// This class cannot be instantiated
		private NotePad() {
		}

		/**
		 * Notes table contract
		 */
		public static final class Notes implements BaseColumns {

			// This class cannot be instantiated
			private Notes() {
			}

			/**
			 * The table name offered by this provider
			 */
			public static final String TABLE_NAME = "notes";
			public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

			/*
			 * URI definitions
			 */

			/**
			 * The scheme part for this provider's URI
			 */
			private static final String SCHEME = "content://";

			/**
			 * Path parts for the URIs
			 */

			/**
			 * Path part for the Notes URI
			 */
			public static final String PATH_NOTES = "/notes";
			public static final String NOTES = "notes";
			// Visible notes
			public static final String PATH_VISIBLE_NOTES = "/visiblenotes";
			public static final String VISIBLE_NOTES = "visiblenotes";
			// Complete note entry including stuff in GTasks table
			private static final String PATH_JOINED_NOTES = "/joinednotes";

			/**
			 * Path part for the Note ID URI
			 */
			public static final String PATH_NOTE_ID = "/notes/";
			public static final String PATH_VISIBLE_NOTE_ID = "/visiblenotes/";

			/**
			 * 0-relative position of a note ID segment in the path part of a
			 * note ID URI
			 */
			public static final int NOTE_ID_PATH_POSITION = 1;

			/**
			 * The content:// style URL for this table
			 */
			public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
					+ PATH_NOTES);
			public static final Uri CONTENT_VISIBLE_URI = Uri.parse(SCHEME
					+ AUTHORITY + PATH_VISIBLE_NOTES);
			public static final Uri CONTENT_JOINED_URI = Uri.parse(SCHEME
					+ AUTHORITY + PATH_JOINED_NOTES);

			/**
			 * The content URI base for a single note. Callers must append a
			 * numeric note id to this Uri to retrieve a note
			 */
			public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
					+ AUTHORITY + PATH_NOTE_ID);
			public static final Uri CONTENT_VISIBLE_ID_URI_BASE = Uri
					.parse(SCHEME + AUTHORITY + PATH_VISIBLE_NOTE_ID);

			/**
			 * The content URI match pattern for a single note, specified by its
			 * ID. Use this to match incoming URIs or to construct an Intent.
			 */
			public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
					+ AUTHORITY + PATH_NOTE_ID + "/#");
			public static final Uri CONTENT_VISIBLE_ID_URI_PATTERN = Uri
					.parse(SCHEME + AUTHORITY + PATH_VISIBLE_NOTE_ID + "/#");

			/*
			 * MIME type definitions
			 */

			/**
			 * The MIME type of {@link #CONTENT_URI} providing a directory of
			 * notes.
			 */
			// public static final String CONTENT_TYPE =
			// "vnd.android.cursor.dir/vnd.nononsenseapps.note";

			/**
			 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
			 * note.
			 */
			public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.note";

			public static final String CONTENT_TYPE = CONTENT_ITEM_TYPE;

			/*
			 * Column definitions
			 */

			/**
			 * Column name for the title of the note
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_TITLE = "title";

			/**
			 * Column name of the note content
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_NOTE = "note";

			/**
			 * Column name for the creation timestamp
			 * <P>
			 * Type: INTEGER (long from System.curentTimeMillis())
			 * </P>
			 */
			public static final String COLUMN_NAME_CREATE_DATE = "created";

			/**
			 * Column name for the modification timestamp
			 * <P>
			 * Type: INTEGER (long from System.curentTimeMillis())
			 * </P>
			 */
			public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";

			/**
			 * Due date of the task (as an RFC 3339 timestamp) formatted as
			 * String.
			 */
			public static final String COLUMN_NAME_DUE_DATE = "duedate";

			/**
			 * Status of task, such as "completed"
			 */
			public static final String COLUMN_NAME_GTASKS_STATUS = "gtaskstatus";

			/**
			 * INTEGER, id of entry in lists table
			 */
			public static final String COLUMN_NAME_LIST = "list";

			/**
			 * Deleted flag
			 */
			public static final String COLUMN_NAME_DELETED = "deleted";

			/**
			 * Modified flag
			 */
			public static final String COLUMN_NAME_MODIFIED = "modifiedflag";

			// parent position hidden

			public static final String COLUMN_NAME_PARENT = "gtasks_parent";

			public static final String COLUMN_NAME_POSITION = "gtasks_position";
			public static final String COLUMN_NAME_HIDDEN = "hiddenflag";

			// server side sorting and local hiding
			public static final String COLUMN_NAME_INDENTLEVEL = "indentlevel";
			public static final String COLUMN_NAME_POSSUBSORT = "possubsort";
			public static final String COLUMN_NAME_LOCALHIDDEN = "localhidden";

			/**
			 * The default sort order for this table
			 */
			// public static final String DEFAULT_SORT_TYPE = COLUMN_NAME_TITLE
			// +
			// " COLLATE NOCASE";
			public static final String ALPHABETIC_SORT_TYPE = COLUMN_NAME_TITLE
					+ " COLLATE NOCASE";
			// We want items with no due dates to be placed at the end, hence
			// the
			// sql magic
			// Coalesce returns the first non-null argument
			public static final String MODIFICATION_SORT_TYPE = COLUMN_NAME_MODIFICATION_DATE;
			public static final String DUEDATE_SORT_TYPE = "CASE WHEN "
					+ COLUMN_NAME_DUE_DATE + " IS NULL OR "
					+ COLUMN_NAME_DUE_DATE + " IS '' THEN 1 ELSE 0 END, "
					+ COLUMN_NAME_DUE_DATE;
			public static final String POSSUBSORT_SORT_TYPE = COLUMN_NAME_POSSUBSORT;

			public static final String ASCENDING_SORT_ORDERING = "ASC";
			public static final String DESCENDING_SORT_ORDERING = "DESC";
			public static final String ALPHABETIC_ASC_ORDER = COLUMN_NAME_TITLE
					+ " COLLATE NOCASE ASC";
			// public static final String POSITION_ASC_ORDER =
			// COLUMN_NAME_POSITION+
			// " ASC";

			public static final String DEFAULT_SORT_TYPE = POSSUBSORT_SORT_TYPE;
			public static final String DEFAULT_SORT_ORDERING = ASCENDING_SORT_ORDERING;

			public static String SORT_ORDER = ALPHABETIC_ASC_ORDER;
		}

		/**
		 * Lists table contract
		 */
		public static final class Lists implements BaseColumns {

			// This class cannot be instantiated
			private Lists() {
			}

			public static final String DEFAULT_LIST_NAME = "Notes";

			/**
			 * The table name offered by this provider
			 */
			public static final String TABLE_NAME = "lists";
			public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

			/*
			 * URI definitions
			 */

			/**
			 * The scheme part for this provider's URI
			 */
			private static final String SCHEME = "content://";

			/**
			 * Path parts for the URIs
			 */

			/**
			 * Path part for the Lists URI
			 */
			public static final String PATH_LISTS = "/lists";
			public static final String LISTS = "lists";
			public static final String PATH_VISIBLE_LISTS = "/visiblelists";
			public static final String VISIBLE_LISTS = "visiblelists";
			// Complete entry gotten with a join with GTasksLists table
			private static final String PATH_JOINED_LISTS = "/joinedlists";

			/**
			 * Path part for the List ID URI
			 */
			public static final String PATH_LIST_ID = "/lists/";
			public static final String PATH_VISIBLE_LIST_ID = "/visiblelists/";

			/**
			 * 0-relative position of a ID segment in the path part of a ID URI
			 */
			public static final int ID_PATH_POSITION = 1;

			/**
			 * The content:// style URL for this table
			 */
			public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
					+ PATH_LISTS);
			public static final Uri CONTENT_VISIBLE_URI = Uri.parse(SCHEME
					+ AUTHORITY + PATH_VISIBLE_LISTS);
			public static final Uri CONTENT_JOINED_URI = Uri.parse(SCHEME
					+ AUTHORITY + PATH_JOINED_LISTS);

			/**
			 * The content URI base for a single note. Callers must append a
			 * numeric note id to this Uri to retrieve a note
			 */
			public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
					+ AUTHORITY + PATH_LIST_ID);
			public static final Uri CONTENT_VISIBLE_ID_URI_BASE = Uri
					.parse(SCHEME + AUTHORITY + PATH_VISIBLE_LIST_ID);

			/**
			 * The content URI match pattern for a single note, specified by its
			 * ID. Use this to match incoming URIs or to construct an Intent.
			 */
			public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
					+ AUTHORITY + PATH_LIST_ID + "/#");
			public static final Uri CONTENT_VISIBLE_ID_URI_PATTERN = Uri
					.parse(SCHEME + AUTHORITY + PATH_VISIBLE_LIST_ID + "/#");

			/*
			 * MIME type definitions
			 */

			/**
			 * The MIME type of {@link #CONTENT_URI} providing a directory.
			 */
			// public static final String CONTENT_TYPE =
			// "vnd.android.cursor.dir/vnd.nononsenseapps.list";

			/**
			 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
			 * item.
			 */
			public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.list";

			public static final String CONTENT_TYPE = CONTENT_ITEM_TYPE;

			/*
			 * Column definitions
			 */

			/**
			 * Column name for the title of the note
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_TITLE = "title";

			/**
			 * Deleted flag
			 */
			public static final String COLUMN_NAME_DELETED = "deleted";

			/**
			 * Modified flag
			 */
			public static final String COLUMN_NAME_MODIFIED = "modifiedflag";

			/**
			 * Column name for the modification timestamp
			 * <P>
			 * Type: INTEGER (long from System.curentTimeMillis())
			 * </P>
			 */
			public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";

			/**
			 * The default sort order for this table
			 */

			public static final String DEFAULT_SORT_TYPE = COLUMN_NAME_MODIFICATION_DATE;
			public static final String DEFAULT_SORT_ORDERING = "DESC";
			public static final String MODIFIED_DESC_ORDER = COLUMN_NAME_MODIFICATION_DATE
					+ " DESC";
			public static final String ALPHABETIC_ASC_ORDER = COLUMN_NAME_TITLE
					+ " COLLATE NOCASE ASC";

			public static String SORT_ORDER = ALPHABETIC_ASC_ORDER;
		}

		/**
		 * GoogleTasks table contract
		 */
		public static final class GTasks implements BaseColumns {

			// This class cannot be instantiated
			private GTasks() {
			}

			/**
			 * The table name offered by this provider
			 */
			public static final String TABLE_NAME = "gtasks";
			public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

			/*
			 * URI definitions
			 */

			/**
			 * The scheme part for this provider's URI
			 */
			private static final String SCHEME = "content://";

			/**
			 * Path parts for the URIs
			 */

			/**
			 * Path part for the Lists URI
			 */
			private static final String PATH = "/gtasks";

			/**
			 * Path part for the List ID URI
			 */
			private static final String PATH_ID = "/gtasks/";

			/**
			 * 0-relative position of a note ID segment in the path part of a
			 * note ID URI
			 */
			public static final int ID_PATH_POSITION = 1;

			/**
			 * The content:// style URL for this table
			 */
			public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
					+ PATH);

			/**
			 * The content URI base for a single note. Callers must append a
			 * numeric note id to this Uri to retrieve a note
			 */
			public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
					+ AUTHORITY + PATH_ID);

			/**
			 * The content URI match pattern for a single note, specified by its
			 * ID. Use this to match incoming URIs or to construct an Intent.
			 */
			public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
					+ AUTHORITY + PATH_ID + "/#");

			/*
			 * MIME type definitions
			 */

			/**
			 * The MIME type of {@link #CONTENT_URI} providing a directory of
			 * notes.
			 */
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nononsenseapps.gtask";

			/**
			 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
			 * note.
			 */
			public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.gtask";

			/*
			 * Column definitions
			 */

			/**
			 * <P>
			 * Type: INTEGER, database ID
			 * </P>
			 */
			public static final String COLUMN_NAME_DB_ID = "dbid";

			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_GTASKS_ID = "googleid";

			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_GOOGLE_ACCOUNT = "googleaccount";

			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_ETAG = "etag";
			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_UPDATED = "updated";
		}

		/**
		 * GoogleTaskLists table contract
		 */
		public static final class GTaskLists implements BaseColumns {

			// This class cannot be instantiated
			private GTaskLists() {
			}

			/**
			 * The table name offered by this provider
			 */
			public static final String TABLE_NAME = "gtasklists";
			public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

			/*
			 * URI definitions
			 */

			/**
			 * The scheme part for this provider's URI
			 */
			private static final String SCHEME = "content://";

			/**
			 * Path parts for the URIs
			 */

			/**
			 * Path part for the Lists URI
			 */
			private static final String PATH = "/gtasklists";

			/**
			 * Path part for the List ID URI
			 */
			private static final String PATH_ID = "/gtasklists/";

			/**
			 * 0-relative position of a note ID segment in the path part of a
			 * note ID URI
			 */
			public static final int ID_PATH_POSITION = 1;

			/**
			 * The content:// style URL for this table
			 */
			public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
					+ PATH);

			/**
			 * The content URI base for a single note. Callers must append a
			 * numeric note id to this Uri to retrieve a note
			 */
			public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
					+ AUTHORITY + PATH_ID);

			/**
			 * The content URI match pattern for a single note, specified by its
			 * ID. Use this to match incoming URIs or to construct an Intent.
			 */
			public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
					+ AUTHORITY + PATH_ID + "/#");

			/*
			 * MIME type definitions
			 */

			/**
			 * The MIME type of {@link #CONTENT_URI} providing a directory of
			 * notes.
			 */
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nononsenseapps.gtasklist";

			/**
			 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
			 * note.
			 */
			public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.gtasklist";

			/*
			 * Column definitions
			 */

			/**
			 * <P>
			 * Type: INTEGER, database ID
			 * </P>
			 */
			public static final String COLUMN_NAME_DB_ID = "dbid";

			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_GTASKS_ID = "googleid";

			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_GOOGLE_ACCOUNT = "googleaccount";

			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_ETAG = "etag";
			/**
			 * <P>
			 * Type: TEXT
			 * </P>
			 */
			public static final String COLUMN_NAME_UPDATED = "updated";
		}

		/**
		 * Notifications table contract
		 */
		public static final class Notifications implements BaseColumns {

			// This class cannot be instantiated
			private Notifications() {
			}

			/**
			 * The table name offered by this provider
			 */
			public static final String TABLE_NAME = "notification";
			public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

			/*
			 * Column definitions
			 */
			public static final String COLUMN_NAME_TIME = "time";
			public static final String COLUMN_NAME_PERMANENT = "permanent";
			public static final String COLUMN_NAME_NOTEID = "noteid";

			public static final String JOINED_COLUMN_LIST_TITLE = NotePad.Lists.TABLE_NAME
					+ "." + NotePad.Lists.COLUMN_NAME_TITLE;

			/*
			 * URI definitions
			 */

			/**
			 * The scheme part for this provider's URI
			 */
			private static final String SCHEME = "content://";

			/**
			 * Path parts for the URIs
			 */

			/**
			 * Path part for the Lists URI
			 */
			private static final String PATH = "/" + TABLE_NAME;

			/**
			 * Path part for the List ID URI
			 */
			private static final String PATH_ID = PATH + "/";

			/**
			 * 0-relative position of a note ID segment in the path part of a
			 * note ID URI
			 */
			public static final int ID_PATH_POSITION = 1;

			private static final String PATH_JOINED_NOTIFICATIONS = "/joinednotifications";
			public static final String PATH_NOTIFICATIONS_LISTID = "/notificationlists";
			private static final String PATH_NOTIFICATIONS_LISTID_BASE = PATH_NOTIFICATIONS_LISTID
					+ "/";
			/**
			 * The content:// style URL for this table
			 */
			public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
					+ PATH);

			public static final Uri CONTENT_JOINED_URI = Uri.parse(SCHEME
					+ AUTHORITY + PATH_JOINED_NOTIFICATIONS);

			public static final Uri CONTENT_LISTID_URI_BASE = Uri.parse(SCHEME
					+ AUTHORITY + PATH_NOTIFICATIONS_LISTID);

			/**
			 * The content URI base for a single note. Callers must append a
			 * numeric note id to this Uri to retrieve a note
			 */
			public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
					+ AUTHORITY + PATH_ID);

			/**
			 * The content URI match pattern for a single note, specified by its
			 * ID. Use this to match incoming URIs or to construct an Intent.
			 */
			public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
					+ AUTHORITY + PATH_ID + "/#");

			/*
			 * MIME type definitions
			 */

			/**
			 * The MIME type of {@link #CONTENT_URI} providing a directory of
			 * notes.
			 */
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nononsenseapps."
					+ TABLE_NAME;

			/**
			 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
			 * note.
			 */
			public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps."
					+ TABLE_NAME;
		}
	}

	/**
	 * Converts the columns names from the legacy URIs. However, the data must
	 * also be returned correctly!
	 */
	public static String[] convertLegacyColumns(final String[] legacyCols) {
		String[] newCols = new String[legacyCols.length];
		for (int i = 0; i < legacyCols.length; i++) {
			String col = legacyCols[i];
			String newCol = col;
			// Lists
			if (NotePad.Lists.COLUMN_NAME_TITLE.equals(col)) {
				newCol = TaskList.Columns.TITLE;
			}
			// Tasks
			else if (NotePad.Notes.COLUMN_NAME_TITLE.equals(col)) {
				newCol = Task.Columns.TITLE;
			}
			else if (NotePad.Notes.COLUMN_NAME_NOTE.equals(col)) {
				newCol = Task.Columns.NOTE;
			}
			else if (NotePad.Notes.COLUMN_NAME_LIST.equals(col)) {
				newCol = Task.Columns.DBLIST;
			}
			else if (NotePad.Notes.COLUMN_NAME_DUE_DATE.equals(col)) {
				newCol = Task.Columns.DUE;
			}
			else if (NotePad.Notes.COLUMN_NAME_GTASKS_STATUS.equals(col)) {
				newCol = Task.Columns.COMPLETED;
			}

			//Log.d("nononsenseapps db", "legacy converted field:" + newCol);
			newCols[i] = newCol;
		}
		return newCols;
	}

	/** 
	 * Convert new values to old, but using old or new column names
	 * 
	 * TaskProjection: new String[] { "_id", "title", "note", "list", "duedate",
	 * "gtaskstatus"};
	 */
	public static Object[] convertLegacyTaskValues(final Cursor cursor) {
		Object[] retval = new Object[cursor.getColumnCount()];
		for (int i = 0; i < cursor.getColumnCount(); i++) {
			final String colName = cursor.getColumnName(i);
			final Object val;
			if (NotePad.Notes.COLUMN_NAME_DUE_DATE.equals(colName) ||
					Task.Columns.DUE.equals(colName)) {
				val = cursor.isNull(i) ? "" : RFC3339Date.asRFC3339(cursor.getLong(i));
			}
			else if (NotePad.Notes.COLUMN_NAME_GTASKS_STATUS.equals(colName) ||
					Task.Columns.COMPLETED.equals(colName)) {
				val = cursor.isNull(i) ? "needsAction" : "completed";
			}
			else {
				switch (cursor.getType(i)) {
				case Cursor.FIELD_TYPE_FLOAT:
					val = cursor.getFloat(i);
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					val = cursor.getLong(i);
					break;
				case Cursor.FIELD_TYPE_STRING:
					val = cursor.getString(i);
					break;
				case Cursor.FIELD_TYPE_NULL:
				default:
					val = null;
				}
			}
			//Log.d("nononsenseapps db", "legacy notes col: " + val);
			retval[i] = val;
		}
		return retval;
	}
}
