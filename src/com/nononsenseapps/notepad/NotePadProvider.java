/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotePad.GTasks;
import com.nononsenseapps.notepad.NotePad.Notes;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.SyncAdapter;
import com.nononsenseapps.notepad.widget.ListWidgetProvider;

import android.appwidget.AppWidgetManager;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class NotePadProvider extends ContentProvider implements
		PipeDataWriter<Cursor> {

	public static boolean SyncAuto(Context context) {
		String setting = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(SyncPrefs.KEY_SYNC_FREQ, "0");
		int syncAuto = Integer.parseInt(setting);

		boolean syncEnabled = PreferenceManager.getDefaultSharedPreferences(
				context).getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false);

		return syncEnabled && (syncAuto > 0);
	}

	// Used for debugging and logging
	private static final String TAG = "NotePadProvider";

	/**
	 * The database that the provider uses as its underlying data store
	 */
	private static final String DATABASE_NAME = "note_pad.db";

	private static final int DATABASE_VERSION = 6;

	/**
	 * A projection map used to select columns from the database
	 */
	public static HashMap<String, String> sNotesProjectionMap;

	// THis projection map is intended for list displays only
	// A substring of the title is returned and a substring of the
	// note text is only returned if note is not locked.
	public static HashMap<String, String> sFastVisibleNotesProjectionMap;

	private static HashMap<String, String> sListsProjectionMap;
	private static HashMap<String, String> sGTasksProjectionMap;
	private static HashMap<String, String> sGTaskListsProjectionMap;

	private static HashMap<String, String> sJoinedNotesProjectionMap;
	private static HashMap<String, String> sJoinedListsProjectionMap;

	/**
	 * Standard projection for the interesting columns of a normal note.
	 */
	private static final String[] READ_NOTE_PROJECTION = new String[] {
			NotePad.Notes._ID, // Projection position 0, the note's id
			NotePad.Notes.COLUMN_NAME_NOTE, // Projection position 1, the note's
											// content
			NotePad.Notes.COLUMN_NAME_TITLE, // Projection position 2, the
												// note's title
	};
	private static final int READ_NOTE_NOTE_INDEX = 1;
	private static final int READ_NOTE_TITLE_INDEX = 2;

	/*
	 * Constants used by the Uri matcher to choose an action based on the
	 * pattern of the incoming URI
	 */
	// The incoming URI matches the Notes URI pattern
	private static final int NOTES = 1;

	// The incoming URI matches the Note ID URI pattern
	private static final int NOTE_ID = 2;

	private static final int LISTS = 3;
	private static final int LISTS_ID = 4;

	private static final int GTASKS = 5;
	private static final int GTASKS_ID = 6;

	private static final int GTASKLISTS = 7;
	private static final int GTASKLISTS_ID = 8;

	// Convenience URIs
	private static final int VISIBLE_NOTES = 9;
	private static final int VISIBLE_NOTE_ID = 10;
	private static final int VISIBLE_LISTS = 11;
	private static final int VISIBLE_LIST_ID = 12;

	// For joined queries
	private static final int JOINED_NOTES = 13;
	private static final int JOINED_LISTS = 14;

	// private static final int SEARCH = 4;

	/**
	 * A UriMatcher instance
	 */
	private static final UriMatcher sUriMatcher;

	// Handle to a new DatabaseHelper.
	private DatabaseHelper mOpenHelper;

	/**
	 * A block that instantiates and sets static objects
	 */
	static {

		/*
		 * Creates and initializes the URI matcher
		 */
		// Create a new instance
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		// Add a pattern that routes URIs terminated with "notes" to a NOTES
		// operation
		sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);

		// Add a pattern that routes URIs terminated with "notes" plus an
		// integer
		// to a note ID operation
		sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

		sUriMatcher.addURI(NotePad.AUTHORITY, "lists", LISTS);
		sUriMatcher.addURI(NotePad.AUTHORITY, "lists/#", LISTS_ID);

		sUriMatcher.addURI(NotePad.AUTHORITY, "gtasks", GTASKS);
		sUriMatcher.addURI(NotePad.AUTHORITY, "gtasks/#", GTASKS_ID);

		sUriMatcher.addURI(NotePad.AUTHORITY, "gtasklists", GTASKLISTS);
		sUriMatcher.addURI(NotePad.AUTHORITY, "gtasklists/#", GTASKLISTS_ID);

		// Convenience URIs
		sUriMatcher.addURI(NotePad.AUTHORITY, "visiblenotes", VISIBLE_NOTES);
		sUriMatcher
				.addURI(NotePad.AUTHORITY, "visiblenotes/#", VISIBLE_NOTE_ID);
		sUriMatcher.addURI(NotePad.AUTHORITY, "visiblelists", VISIBLE_LISTS);
		sUriMatcher
				.addURI(NotePad.AUTHORITY, "visiblelists/#", VISIBLE_LIST_ID);

		// Joined queries
		sUriMatcher.addURI(NotePad.AUTHORITY, "joinedlists", JOINED_LISTS);
		sUriMatcher.addURI(NotePad.AUTHORITY, "joinednotes", JOINED_NOTES);

		/*
		 * Creates and initializes a projection map that returns all columns
		 */

		// Creates a new projection map instance. The map returns a column name
		// given a string. The two are usually equal.
		sNotesProjectionMap = new HashMap<String, String>();

		// Maps the string "_ID" to the column name "_ID"
		sNotesProjectionMap.put(BaseColumns._ID, BaseColumns._ID);

		// Maps "title" to "title"
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE,
				NotePad.Notes.COLUMN_NAME_TITLE);

		// Maps "note" to "note"
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE,
				NotePad.Notes.COLUMN_NAME_NOTE);

		// Maps "created" to "created"
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
				NotePad.Notes.COLUMN_NAME_CREATE_DATE);

		// Maps "modified" to "modified"
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
				NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_DUE_DATE,
				NotePad.Notes.COLUMN_NAME_DUE_DATE);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_MODIFIED,
				NotePad.Notes.COLUMN_NAME_MODIFIED);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
				NotePad.Notes.COLUMN_NAME_GTASKS_STATUS);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_DELETED,
				NotePad.Notes.COLUMN_NAME_DELETED);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_LIST,
				NotePad.Notes.COLUMN_NAME_LIST);

		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_PARENT,
				NotePad.Notes.COLUMN_NAME_PARENT);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_POSITION,
				NotePad.Notes.COLUMN_NAME_POSITION);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_HIDDEN,
				NotePad.Notes.COLUMN_NAME_HIDDEN);

		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_INDENTLEVEL,
				NotePad.Notes.COLUMN_NAME_INDENTLEVEL);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_POSSUBSORT,
				NotePad.Notes.COLUMN_NAME_POSSUBSORT);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_LOCALHIDDEN,
				NotePad.Notes.COLUMN_NAME_LOCALHIDDEN);

		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_LOCKED,
				NotePad.Notes.COLUMN_NAME_LOCKED);

		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_PREVIOUS,
				NotePad.Notes.COLUMN_NAME_PREVIOUS);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_PREVTRUEPOS,
				NotePad.Notes.COLUMN_NAME_PREVTRUEPOS);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TRUEPOS,
				NotePad.Notes.COLUMN_NAME_TRUEPOS);
		sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NEXTTRUEPOS,
				NotePad.Notes.COLUMN_NAME_NEXTTRUEPOS);

		// This is a special map. A locked note will not return its text in
		// this projection.
		// Useful for list displays, places that are not supposed to ask for
		// passwords
		sFastVisibleNotesProjectionMap = new HashMap<String, String>();
		// Set all stuffs as in notes projection first
		for (Entry<String, String> notesEntry : sNotesProjectionMap.entrySet()) {
			sFastVisibleNotesProjectionMap.put(notesEntry.getKey(),
					notesEntry.getValue());
		}
		// Now replace the title text with a substring
		sFastVisibleNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE,
				substrOf(NotePad.Notes.COLUMN_NAME_TITLE, "150"));
		// Now replace the note text with a case statement to check the lock and
		// do substr as well
		Log.d("lockfix", caseWhenLocked(NotePad.Notes.COLUMN_NAME_NOTE));
		sFastVisibleNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE,
				caseWhenLocked(NotePad.Notes.COLUMN_NAME_NOTE));

		/*
		 * Creates an initializes a projection map for handling Lists
		 */

		// Creates a new projection map instance
		sListsProjectionMap = new HashMap<String, String>();

		sListsProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
		sListsProjectionMap.put(NotePad.Lists.COLUMN_NAME_TITLE,
				NotePad.Lists.COLUMN_NAME_TITLE);
		sListsProjectionMap.put(NotePad.Lists.COLUMN_NAME_DELETED,
				NotePad.Lists.COLUMN_NAME_DELETED);
		sListsProjectionMap.put(NotePad.Lists.COLUMN_NAME_MODIFIED,
				NotePad.Lists.COLUMN_NAME_MODIFIED);
		sListsProjectionMap.put(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE,
				NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE);

		/*
		 * Gtasks stuff
		 */
		sGTasksProjectionMap = new HashMap<String, String>();
		sGTasksProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
		sGTasksProjectionMap.put(NotePad.GTasks.COLUMN_NAME_GTASKS_ID,
				NotePad.GTasks.COLUMN_NAME_GTASKS_ID);
		sGTasksProjectionMap.put(NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT,
				NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT);
		sGTasksProjectionMap.put(NotePad.GTasks.COLUMN_NAME_ETAG,
				NotePad.GTasks.COLUMN_NAME_ETAG);
		sGTasksProjectionMap.put(NotePad.GTasks.COLUMN_NAME_DB_ID,
				NotePad.GTasks.COLUMN_NAME_DB_ID);
		sGTasksProjectionMap.put(NotePad.GTasks.COLUMN_NAME_UPDATED,
				NotePad.GTasks.COLUMN_NAME_UPDATED);

		sGTaskListsProjectionMap = new HashMap<String, String>();
		sGTaskListsProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
		sGTaskListsProjectionMap.put(NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID,
				NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID);
		sGTaskListsProjectionMap.put(
				NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT,
				NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT);
		sGTaskListsProjectionMap.put(NotePad.GTaskLists.COLUMN_NAME_ETAG,
				NotePad.GTaskLists.COLUMN_NAME_ETAG);
		sGTaskListsProjectionMap.put(NotePad.GTaskLists.COLUMN_NAME_DB_ID,
				NotePad.GTaskLists.COLUMN_NAME_DB_ID);
		sGTaskListsProjectionMap.put(NotePad.GTaskLists.COLUMN_NAME_UPDATED,
				NotePad.GTaskLists.COLUMN_NAME_UPDATED);

		// Joined projection maps
		// Prepend the table name here
		// Order is important since _ID column is the same
		sJoinedNotesProjectionMap = new HashMap<String, String>();
		for (Entry<String, String> gtasksEntry : sGTasksProjectionMap
				.entrySet()) {
			sJoinedNotesProjectionMap.put(gtasksEntry.getKey(),
					NotePad.GTasks.TABLE_NAME + "." + gtasksEntry.getValue());
		}
		for (Entry<String, String> notesEntry : sNotesProjectionMap.entrySet()) {
			sJoinedNotesProjectionMap.put(notesEntry.getKey(),
					NotePad.Notes.TABLE_NAME + "." + notesEntry.getValue());
		}

		sJoinedListsProjectionMap = new HashMap<String, String>();
		// Order is important. _ID column will be the same, so I want that to be
		// the list, hence it must be last
		for (Entry<String, String> gtasklistsEntry : sGTaskListsProjectionMap
				.entrySet()) {
			sJoinedListsProjectionMap.put(
					gtasklistsEntry.getKey(),
					NotePad.GTaskLists.TABLE_NAME + "."
							+ gtasklistsEntry.getValue());
		}
		for (Entry<String, String> listsEntry : sListsProjectionMap.entrySet()) {
			sJoinedListsProjectionMap.put(listsEntry.getKey(),
					NotePad.Lists.TABLE_NAME + "." + listsEntry.getValue());
		}

	}

	/**
	 * substr('this is the string', 0, length)
	 */
	private static String substrOf(String name, String length) {
		return substrOf(name, length, name);
	}

	private static String substrOf(String name, String length, String target) {
		return "substr(" + name + ",0," + length + ") as " + target;
	}

	private static String caseWhenLocked(String note) {
		// We only do the search on the last chars to increase speed
		String endnote = "substr(" + note + ",-10" + ")";
		String clause = endnote + " LIKE '%" + NoteAttributes.LOCKED + "%'";
		String that = "substr(" + note + ",0," + 190 + ")";
		return caseWhen(clause, "''", that) + " as " + note;
	}

	/**
	 * case when CLAUSE then THIS else THAT end
	 */
	private static String caseWhen(String clause, String cThis, String cThat) {
		return "case when " + clause + " then " + cThis + " else " + cThat
				+ " end";
	}

	/**
	 * 
	 * This class helps open, create, and upgrade the database file. Set to
	 * package visibility for testing purposes.
	 */
	public static class DatabaseHelper extends SQLiteOpenHelper {
		Context context;

		public DatabaseHelper(Context context) {

			// calls the super constructor, requesting the default cursor
			// factory.
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			if (SyncAdapter.SYNC_DEBUG_PRINTS)
				Log.d("DataBaseHelper", "Constructor");
			this.context = context;
		}

		/**
		 * 
		 * Creates the underlying database with table name and column names
		 * taken from the NotePad class.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			if (SyncAdapter.SYNC_DEBUG_PRINTS)
				Log.d("DataBaseHelper", "onCreate");
			createNotesTable(db);
			createListsTable(db);
			createGTasksTable(db);
			createGTaskListsTable(db);

			long listId = insertDefaultList(db);
			insertDefaultNote(db, listId);
		}

		private long insertDefaultList(SQLiteDatabase db) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Lists.COLUMN_NAME_TITLE,
					context.getString(R.string.app_name));
			values.put(NotePad.Lists.COLUMN_NAME_MODIFIED, 1);
			values.put(NotePad.Lists.COLUMN_NAME_DELETED, 0);

			return db.insert(NotePad.Lists.TABLE_NAME, null, values);
		}

		private long insertDefaultNote(SQLiteDatabase db, long listId) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Notes.COLUMN_NAME_TITLE,
					context.getString(R.string.default_notetitle));
			values.put(NotePad.Notes.COLUMN_NAME_NOTE,
					context.getString(R.string.default_notetext));
			values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, 1);
			values.put(NotePad.Notes.COLUMN_NAME_DELETED, 0);
			values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);

			// Gets the current system time in milliseconds
			Long now = Long.valueOf(System.currentTimeMillis());
			values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
			values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
			values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, "");
			values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS, "needsAction");

			values.put(NotePad.Notes.COLUMN_NAME_POSSUBSORT, "");
			values.put(NotePad.Notes.COLUMN_NAME_INDENTLEVEL, 0);

			final ContentValues posValues = getNewPositionValuesFor(null, null,
					listId, db);

			values.putAll(posValues);

			// values.put(Notes.COLUMN_NAME_PREVTRUEPOS, Notes.HEAD);
			// values.put(Notes.COLUMN_NAME_NEXTTRUEPOS, Notes.TAIL);
			// values.put(Notes.COLUMN_NAME_TRUEPOS,
			// Notes.between(Notes.HEAD, Notes.TAIL));
			// values.putNull(Notes.COLUMN_NAME_PARENT);
			// values.putNull(Notes.COLUMN_NAME_PREVIOUS);

			return db.insert(NotePad.Notes.TABLE_NAME, null, values);
		}

		/**
		 * This is where notes are stored. Links to List_table and GTasks_table.
		 */
		private void createNotesTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
					+ NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
					+ NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
					+ NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
					+ NotePad.Notes.COLUMN_NAME_DUE_DATE + " TEXT,"
					+ NotePad.Notes.COLUMN_NAME_LIST + " INTEGER,"
					+ NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " TEXT,"
					+ "gtasks_parent" + " TEXT,"
					+ NotePad.Notes.COLUMN_NAME_POSITION + " TEXT,"
					+ NotePad.Notes.COLUMN_NAME_HIDDEN + " INTEGER,"
					+ NotePad.Notes.COLUMN_NAME_MODIFIED + " INTEGER,"

					+ NotePad.Notes.COLUMN_NAME_INDENTLEVEL
					+ " INTEGER DEFAULT 0,"
					+ NotePad.Notes.COLUMN_NAME_POSSUBSORT
					+ " TEXT DEFAULT '',"
					+ NotePad.Notes.COLUMN_NAME_LOCALHIDDEN
					+ " INTEGER DEFAULT 0,"

					+ NotePad.Notes.COLUMN_NAME_TRUEPOS
					+ " TEXT DEFAULT '' NOT NULL,"
					+ NotePad.Notes.COLUMN_NAME_PREVTRUEPOS
					+ " TEXT DEFAULT '' NOT NULL,"
					+ NotePad.Notes.COLUMN_NAME_NEXTTRUEPOS
					+ " TEXT DEFAULT '' NOT NULL,"
					+ NotePad.Notes.COLUMN_NAME_PREVIOUS
					+ " TEXT DEFAULT NULL," + NotePad.Notes.COLUMN_NAME_PARENT
					+ " TEXT DEFAULT NULL,"

					+ NotePad.Notes.COLUMN_NAME_LOCKED + " INTEGER DEFAULT 0,"

					+ NotePad.Notes.COLUMN_NAME_DELETED + " INTEGER" + ");");
		}

		private void createListsTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + NotePad.Lists.TABLE_NAME + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ NotePad.Lists.COLUMN_NAME_TITLE + " TEXT,"
					+ NotePad.Lists.COLUMN_NAME_MODIFIED + " INTEGER,"
					+ NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
					+ NotePad.Lists.COLUMN_NAME_DELETED + " INTEGER" + ");");
		}

		private void createGTasksTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + NotePad.GTasks.TABLE_NAME + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ NotePad.GTasks.COLUMN_NAME_DB_ID + " INTEGER,"
					+ "FOREIGN KEY("+NotePad.GTasks.COLUMN_NAME_DB_ID + ") REFERENCES " + Notes.TABLE_NAME + "(" + Notes._ID + ")"
					+ NotePad.GTasks.COLUMN_NAME_GTASKS_ID + " INTEGER,"
					+ NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT + " INTEGER,"
					+ NotePad.GTasks.COLUMN_NAME_UPDATED + " TEXT,"
					+ NotePad.GTasks.COLUMN_NAME_ETAG + " TEXT" + ");");
		}

		private void createGTaskListsTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + NotePad.GTaskLists.TABLE_NAME + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ NotePad.GTaskLists.COLUMN_NAME_DB_ID + " INTEGER,"
					+ "FOREIGN KEY("+NotePad.GTaskLists.COLUMN_NAME_DB_ID + ") REFERENCES " + NotePad.Lists.TABLE_NAME + "(" + NotePad.Lists._ID + ")"
					+ NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID + " INTEGER,"
					+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT
					+ " INTEGER," + NotePad.GTaskLists.COLUMN_NAME_UPDATED
					+ " TEXT," + NotePad.GTaskLists.COLUMN_NAME_ETAG + " TEXT"
					+ ");");
		}

		/**
		 * 
		 * Demonstrates that the provider must consider what happens when the
		 * underlying datastore is changed. In this sample, the database is
		 * upgraded the database by destroying the existing data. A real
		 * application should upgrade the database in place.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("DataBaseHelper", "onUpgrade "
					+ "Upgrading database from version " + oldVersion + " to "
					+ newVersion);

			// These lines must never be active in market versions!
			// if (oldVersion == 4) {
			// // Kills the table and existing data
			// db.execSQL("DROP TABLE IF EXISTS " + NotePad.Notes.TABLE_NAME);
			// db.execSQL("DROP TABLE IF EXISTS " + NotePad.Lists.TABLE_NAME);
			// db.execSQL("DROP TABLE IF EXISTS "
			// + NotePad.GTaskLists.TABLE_NAME);
			// db.execSQL("DROP TABLE IF EXISTS " + NotePad.GTasks.TABLE_NAME);
			//
			// // Recreates the database with a new version
			// onCreate(db);
			// } else {
			if (oldVersion < 3) {
				// FIrst add columns to Notes table

				String preName = "ALTER TABLE " + NotePad.Notes.TABLE_NAME
						+ " ADD COLUMN ";
				// Don't want null values. Prefer empty String
				String postText = " TEXT";
				String postNameInt = " INTEGER";
				// Add Columns to Notes DB
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_LIST
						+ postNameInt);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_DUE_DATE
						+ postText);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_GTASKS_STATUS
						+ postText);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_MODIFIED
						+ postNameInt);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_DELETED
						+ postNameInt);

				// Then create the 3 missing tables
				createListsTable(db);
				createGTasksTable(db);
				createGTaskListsTable(db);

				// Now insert a default list
				long listId = insertDefaultList(db);

				// Place all existing notes in this list
				// And give them sensible values in the new columns
				ContentValues values = new ContentValues();
				values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);
				values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, 1);
				values.put(NotePad.Notes.COLUMN_NAME_DELETED, 0);
				values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, "");
				values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
						"needsAction");

				db.update(NotePad.Notes.TABLE_NAME, values,
						NotePad.Notes.COLUMN_NAME_LIST + " IS NOT ?",
						new String[] { Long.toString(listId) });
			}
			if (oldVersion < 4) {

				String preName = "ALTER TABLE " + NotePad.Notes.TABLE_NAME
						+ " ADD COLUMN ";
				String postText = " TEXT";
				String postNameInt = " INTEGER";
				// Add Columns to Notes DB
				db.execSQL(preName + "gtasks_parent" + postText);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_POSITION
						+ postText);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_HIDDEN
						+ postNameInt);

				// Give all notes sensible values
				ContentValues values = new ContentValues();
				values.put("gtasks_parent", "");
				values.put(NotePad.Notes.COLUMN_NAME_POSITION, "");
				values.put(NotePad.Notes.COLUMN_NAME_HIDDEN, 0);
				db.update(NotePad.Notes.TABLE_NAME, values,
						NotePad.Notes.COLUMN_NAME_HIDDEN + " IS NOT ?",
						new String[] { "0" });
			}
			if (oldVersion < 5) {

				String preName = "ALTER TABLE " + NotePad.Notes.TABLE_NAME
						+ " ADD COLUMN ";
				String postText = " TEXT DEFAULT ''";
				String postNameInt = " INTEGER DEFAULT 0";
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_POSSUBSORT
						+ postText);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_LOCALHIDDEN
						+ postNameInt);
			}
			if (oldVersion < 6) {
				// Add Columns to Notes DB
				String preName = "ALTER TABLE " + NotePad.Notes.TABLE_NAME
						+ " ADD COLUMN ";
				String postNameInt = " INTEGER DEFAULT 0";
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_INDENTLEVEL
						+ postNameInt);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_LOCKED
						+ postNameInt);

				// Mark all notes as modified to ensure we set the indents on
				// next sync
				ContentValues values = new ContentValues();
				values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, 1);
				db.update(NotePad.Notes.TABLE_NAME, values, null, null);
			}
			if (oldVersion < 7) {
				// Add columns
				String preName = "ALTER TABLE " + NotePad.Notes.TABLE_NAME
						+ " ADD COLUMN ";
				String postNameInt = " INTEGER";
				String postNameText = "  TEXT DEFAULT '' NOT NULL";
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_TRUEPOS
						+ postNameText);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_PREVTRUEPOS
						+ postNameText);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_NEXTTRUEPOS
						+ postNameText);

				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_PREVIOUS
						+ postNameInt);
				db.execSQL(preName + NotePad.Notes.COLUMN_NAME_PARENT
						+ postNameInt);

				// Give all tasks correct position values
			}
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
			// This should be allright to do. Can only see this happening in
			// testing
		}
	}

	/**
	 * 
	 * Initializes the provider by creating a new DatabaseHelper. onCreate() is
	 * called automatically when Android creates the provider in response to a
	 * resolver request from a client.
	 */
	@Override
	public boolean onCreate() {
		// Creates a new helper object. Note that the database itself isn't
		// opened until
		// something tries to access it, and it's only created if it doesn't
		// already exist.
		mOpenHelper = new DatabaseHelper(getContext());

		// Assumes that any failures will be reported by a thrown exception.
		return true;
	}

	/**
	 * This method is called when a client calls
	 * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)}
	 * . Queries the database and returns a cursor containing the results.
	 * 
	 * @return A cursor containing the results of the query. The cursor exists
	 *         but is empty if the query returns no results or an exception
	 *         occurs.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	synchronized public Cursor query(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "query");

		// Only allow ascending order for truepos
		if (sortOrder != null
				&& sortOrder.startsWith(Notes.POSSUBSORT_SORT_TYPE))
			sortOrder = Notes.POSSUBSORT_SORT_TYPE;

		// Constructs a new query builder and sets its table name
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String orderBy = null;

		/**
		 * Choose the projection and adjust the "where" clause based on URI
		 * pattern-matching.
		 */
		switch (sUriMatcher.match(uri)) {
		// If the incoming URI is for notes, chooses the Notes projection
		case VISIBLE_NOTES:
			// Add a selection criteria, but then fall through for normal note
			// handling.
			qb.appendWhere(NotePad.Notes.COLUMN_NAME_HIDDEN + " IS NOT 1 AND ");
			qb.appendWhere(NotePad.Notes.COLUMN_NAME_LOCALHIDDEN
					+ " IS NOT 1 AND ");
			qb.appendWhere(NotePad.Notes.COLUMN_NAME_DELETED + " IS NOT 1");
			qb.setTables(NotePad.Notes.TABLE_NAME);
			qb.setProjectionMap(sFastVisibleNotesProjectionMap);
			if (selectionArgs != null
					&& (selection == null || selection.equals(""))) {
				selection = NotePad.Notes.COLUMN_NAME_NOTE + " MATCH ?";
			}
			break;
		case NOTES:
			qb.setTables(NotePad.Notes.TABLE_NAME);
			qb.setProjectionMap(sNotesProjectionMap);
			if (selectionArgs != null
					&& (selection == null || selection.equals(""))) {
				selection = NotePad.Notes.COLUMN_NAME_NOTE + " MATCH ?";
			}
			break;
		/*
		 * Retrieve the complete note entry join with Gtasks table through join
		 * statement.
		 */
		case JOINED_NOTES:
			// Can not use where because null values in right table is not
			// returned then
			// Must bake it into the join statement
			qb.setTables(NotePad.Notes.TABLE_NAME + " LEFT OUTER JOIN "
					+ NotePad.GTasks.TABLE_NAME + " ON ("
					+ NotePad.Notes.TABLE_NAME + "." + NotePad.Notes._ID
					+ " = " + NotePad.GTasks.TABLE_NAME + "."
					+ NotePad.GTasks.COLUMN_NAME_DB_ID + " AND "
					+ NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT + " IS '"
					+ selectionArgs[0] + "')");
			selection = null;
			selectionArgs = null;

			// set projection map
			qb.setProjectionMap(sJoinedNotesProjectionMap);
			break;

		/*
		 * If the incoming URI is for a single note identified by its ID,
		 * chooses the note ID projection, and appends "_ID = <noteID>" to the
		 * where clause, so that it selects that single note
		 */
		case VISIBLE_NOTE_ID:
			// Add a selection criteria, but then fall through for normal note
			// handling.
			qb.appendWhere(NotePad.Notes.COLUMN_NAME_HIDDEN + " IS 0 AND ");
			qb.appendWhere(NotePad.Notes.COLUMN_NAME_LOCALHIDDEN + " IS 0 AND ");
			qb.appendWhere(NotePad.Notes.COLUMN_NAME_DELETED + " IS 0 AND ");
		case NOTE_ID:
			qb.setTables(NotePad.Notes.TABLE_NAME);
			qb.setProjectionMap(sNotesProjectionMap);
			qb.appendWhere(BaseColumns._ID + // the name of the ID column
					"=" +
					// the position of the note ID itself in the incoming URI
					uri.getPathSegments().get(
							NotePad.Notes.NOTE_ID_PATH_POSITION));
			break;
		case VISIBLE_LIST_ID:
			// Add a selection criteria, but then fall through for normal
			// handling.
			qb.appendWhere(NotePad.Lists.COLUMN_NAME_DELETED + " IS 0 AND ");
		case LISTS_ID:
			qb.appendWhere(BaseColumns._ID + // the name of the ID column
					"=" +
					// the position of the note ID itself in the incoming URI
					uri.getPathSegments().get(NotePad.Lists.ID_PATH_POSITION));
			orderBy = NotePad.Lists.SORT_ORDER;
			qb.setTables(NotePad.Lists.TABLE_NAME);
			qb.setProjectionMap(sListsProjectionMap);
			break;
		case VISIBLE_LISTS:
			// Add a selection criteria, but then fall through for normal
			// handling.
			qb.appendWhere(NotePad.Lists.COLUMN_NAME_DELETED + " IS 0");
		case LISTS:
			orderBy = NotePad.Lists.SORT_ORDER;
			qb.setTables(NotePad.Lists.TABLE_NAME);
			qb.setProjectionMap(sListsProjectionMap);
			break;
		case JOINED_LISTS:
			// Can not use where because null values in right table is not
			// returned then
			// Must bake it into the join statement
			orderBy = NotePad.Lists.SORT_ORDER;
			qb.setTables(NotePad.Lists.TABLE_NAME + " LEFT OUTER JOIN "
					+ NotePad.GTaskLists.TABLE_NAME + " ON ("
					+ NotePad.Lists.TABLE_NAME + "." + NotePad.Lists._ID
					+ " = " + NotePad.GTaskLists.TABLE_NAME + "."
					+ NotePad.GTaskLists.COLUMN_NAME_DB_ID + " AND "
					+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT + " IS '"
					+ selectionArgs[0] + "')");
			selection = null;
			selectionArgs = null;

			// set projection map
			qb.setProjectionMap(sJoinedListsProjectionMap);
			break;
		case GTASKS_ID:
			qb.appendWhere(BaseColumns._ID + // the name of the ID column
					"=" +
					// the position of the note ID itself in the incoming URI
					uri.getPathSegments().get(NotePad.GTasks.ID_PATH_POSITION));
		case GTASKS:
			qb.setTables(NotePad.GTasks.TABLE_NAME);
			qb.setProjectionMap(sGTasksProjectionMap);
			break;
		case GTASKLISTS_ID:
			qb.appendWhere(BaseColumns._ID + // the name of the ID column
					"=" +
					// the position of the note ID itself in the incoming URI
					uri.getPathSegments().get(
							NotePad.GTaskLists.ID_PATH_POSITION));
		case GTASKLISTS:
			qb.setTables(NotePad.GTaskLists.TABLE_NAME);
			qb.setProjectionMap(sGTaskListsProjectionMap);
			break;
		default:
			// If the URI doesn't match any of the known patterns, throw an
			// exception.
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified, uses the default
		if (TextUtils.isEmpty(sortOrder)) {
			// hanlded in switch
		} else {
			// otherwise, uses the incoming sort order
			orderBy = sortOrder;
		}

		// Opens the database object in "read" mode, since no writes need to be
		// done.
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		/*
		 * Performs the query. If no problems occur trying to read the database,
		 * then a Cursor object is returned; otherwise, the cursor variable
		 * contains null. If no records were selected, then the Cursor object is
		 * empty, and Cursor.getCount() returns 0.
		 */
		Cursor c = qb.query(db, // The database to query
				projection, // The columns to return from the query
				selection, // The columns for the where clause
				selectionArgs, // The values for the where clause
				null, // don't group the rows
				null, // don't filter by row groups
				orderBy // The sort order
				);

		// Tells the Cursor what URI to watch, so it knows when its source data
		// changes
		if (c != null)
			c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#getType(Uri)}. Returns the MIME
	 * data type of the URI given as a parameter.
	 * 
	 * @param uri
	 *            The URI whose MIME type is desired.
	 * @return The MIME type of the URI.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	synchronized public String getType(Uri uri) {

		/**
		 * Chooses the MIME type based on the incoming URI pattern
		 */
		switch (sUriMatcher.match(uri)) {

		// If the pattern is for notes or live folders, returns the general
		// content type.
		case NOTES:
		case VISIBLE_NOTES:
			return NotePad.Notes.CONTENT_TYPE;
			// If the pattern is for note IDs, returns the note ID content type.
		case NOTE_ID:
		case VISIBLE_NOTE_ID:
			return NotePad.Notes.CONTENT_ITEM_TYPE;

		case LISTS:
		case VISIBLE_LISTS:
			return NotePad.Lists.CONTENT_TYPE;
		case LISTS_ID:
		case VISIBLE_LIST_ID:
			return NotePad.Lists.CONTENT_ITEM_TYPE;

		case GTASKS:
			return NotePad.GTasks.CONTENT_TYPE;
		case GTASKS_ID:
			return NotePad.GTasks.CONTENT_ITEM_TYPE;

		case GTASKLISTS:
			return NotePad.GTaskLists.CONTENT_TYPE;
		case GTASKLISTS_ID:
			return NotePad.GTaskLists.CONTENT_ITEM_TYPE;

			// If the URI pattern doesn't match any permitted patterns, throws
			// an exception.
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * This describes the MIME types that are supported for opening a note URI
	 * as a stream.
	 */
	static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
			new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

	/**
	 * Returns the types of available data streams. URIs to specific notes are
	 * supported. The application can convert such a note to a plain text
	 * stream.
	 * 
	 * @param uri
	 *            the URI to analyze
	 * @param mimeTypeFilter
	 *            The MIME type to check for. This method only returns a data
	 *            stream type for MIME types that match the filter. Currently,
	 *            only text/plain MIME types match.
	 * @return a data stream MIME type. Currently, only text/plan is returned.
	 * @throws IllegalArgumentException
	 *             if the URI pattern doesn't match any supported patterns.
	 */
	@Override
	synchronized public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
		/**
		 * Chooses the data stream type based on the incoming URI pattern.
		 */
		switch (sUriMatcher.match(uri)) {

		// If the pattern is for notes or live folders, return null. Data
		// streams are not
		// supported for this type of URI.
		case NOTES:
		case VISIBLE_NOTES:
		case VISIBLE_LISTS:
		case VISIBLE_LIST_ID:
		case LISTS:
		case LISTS_ID:
		case GTASKLISTS:
		case GTASKLISTS_ID:
		case GTASKS:
		case GTASKS_ID:
			return null;

			// If the pattern is for note IDs and the MIME filter is text/plain,
			// then return
			// text/plain
		case VISIBLE_NOTE_ID:
		case NOTE_ID:
			return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);

			// If the URI pattern doesn't match any permitted patterns, throws
			// an exception.
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * Returns a stream of data for each supported stream type. This method does
	 * a query on the incoming URI, then uses
	 * {@link android.content.ContentProvider#openPipeHelper(Uri, String, Bundle, Object, PipeDataWriter)}
	 * to start another thread in which to convert the data into a stream.
	 * 
	 * @param uri
	 *            The URI pattern that points to the data stream
	 * @param mimeTypeFilter
	 *            A String containing a MIME type. This method tries to get a
	 *            stream of data with this MIME type.
	 * @param opts
	 *            Additional options supplied by the caller. Can be interpreted
	 *            as desired by the content provider.
	 * @return AssetFileDescriptor A handle to the file.
	 * @throws FileNotFoundException
	 *             if there is no file associated with the incoming URI.
	 */
	@Override
	synchronized public AssetFileDescriptor openTypedAssetFile(Uri uri,
			String mimeTypeFilter, Bundle opts) throws FileNotFoundException {

		// Checks to see if the MIME type filter matches a supported MIME type.
		String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);

		// If the MIME type is supported
		if (mimeTypes != null) {

			// Retrieves the note for this URI. Uses the query method defined
			// for this provider,
			// rather than using the database query method.
			Cursor c = query(uri, // The URI of a note
					READ_NOTE_PROJECTION, // Gets a projection containing the
											// note's ID, title,
											// and contents
					null, // No WHERE clause, get all matching records
					null, // Since there is no WHERE clause, no selection
							// criteria
					null // Use the default sort order (modification date,
							// descending
			);

			// If the query fails or the cursor is empty, stop
			if (c == null || !c.moveToFirst()) {

				// If the cursor is empty, simply close the cursor and return
				if (c != null) {
					c.close();
				}

				// If the cursor is null, throw an exception
				throw new FileNotFoundException("Unable to query " + uri);
			}

			// Start a new thread that pipes the stream data back to the caller.
			return new AssetFileDescriptor(openPipeHelper(uri, mimeTypes[0],
					opts, c, this), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
		}

		// If the MIME type is not supported, return a read-only handle to the
		// file.
		return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
	}

	/**
	 * Implementation of {@link android.content.ContentProvider.PipeDataWriter}
	 * to perform the actual work of converting the data in one of cursors to a
	 * stream of data for the client to read.
	 */
	// @Override
	synchronized public void writeDataToPipe(ParcelFileDescriptor output,
			Uri uri, String mimeType, Bundle opts, Cursor c) {
		// We currently only support conversion-to-text from a single note
		// entry,
		// so no need for cursor data type checking here.
		FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
			pw.println(c.getString(READ_NOTE_TITLE_INDEX));
			pw.println("");
			pw.println(c.getString(READ_NOTE_NOTE_INDEX));
		} catch (UnsupportedEncodingException e) {
			// Log.w(TAG, "Ooops", e);
		} finally {
			c.close();
			if (pw != null) {
				pw.flush();
			}
			try {
				fout.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#insert(Uri, ContentValues)}.
	 * Inserts a new row into the database. This method sets up default values
	 * for any columns that are not included in the incoming map. If rows were
	 * inserted, then listeners are notified of the change.
	 * 
	 * @return The row ID of the inserted row.
	 * @throws SQLException
	 *             if the insertion fails.
	 */
	@Override
	synchronized public Uri insert(Uri uri, ContentValues initialValues) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "insert");

		// Validates the incoming URI. Only the full provider URI is allowed for
		// inserts.
		switch (sUriMatcher.match(uri)) {
		case VISIBLE_NOTES:
		case NOTES:
			return insertNote(uri, initialValues);
		case VISIBLE_LISTS:
		case LISTS:
			return insertList(uri, initialValues);
		case GTASKS:
			return insertGTask(uri, initialValues);
		case GTASKLISTS:
			return insertGTaskList(uri, initialValues);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	synchronized private Uri insertNote(Uri uri, ContentValues initialValues) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "insertNote");
		// A map to hold the new record's values.
		ContentValues values;

		// If the incoming values map is not null, uses it for the new values.
		if (initialValues != null) {
			values = new ContentValues(initialValues);

		} else {
			// Otherwise, create a new value map
			values = new ContentValues();
		}

		if (values.containsKey(NotePad.Notes.COLUMN_NAME_LIST) == false
				|| values.getAsLong(NotePad.Notes.COLUMN_NAME_LIST) < 0) {
			if (SyncAdapter.SYNC_DEBUG_PRINTS)
				Log.d(TAG, "Forgot to include note in a list");
			throw new SQLException("A note must always belong to a list!");
		}

		final Long listId = values.getAsLong(Notes.COLUMN_NAME_LIST);

		// Gets the current system time in milliseconds
		Long now = Long.valueOf(System.currentTimeMillis());

		// If the values map doesn't contain the creation date, sets the value
		// to the current time.
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
		}

		// If the values map doesn't contain the modification date, sets the
		// value to the current
		// time.
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
		}

		// If the values map doesn't contain a title, sets the value to the
		// empty string.
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_TITLE, "");
		}

		// If the values map doesn't contain note text, sets the value to an
		// empty string.
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
		}

		// If the values map doesn't contain, sets the value to an
		// empty string.
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_DUE_DATE) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, "");
		}

		// If the values map doesn't contain, sets the value to an
		// empty string.
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS, "needsAction");
		}

		if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFIED) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, 1);
		}

		if (values.containsKey(NotePad.Notes.COLUMN_NAME_DELETED) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_DELETED, 0);
		}

		if (values.containsKey(NotePad.Notes.COLUMN_NAME_INDENTLEVEL) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_INDENTLEVEL, 0);
		}
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_LOCKED) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_LOCKED, 0);
		}

		// Set a default sort position so it matches where new tasks are added
		// in gmail
		if (values.containsKey(NotePad.Notes.COLUMN_NAME_POSSUBSORT) == false) {
			values.put(NotePad.Notes.COLUMN_NAME_POSSUBSORT, "0.");
		}

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		/*
		 * A new note is inserted at the top hence the following is true of it
		 * (head is the current task at the top):
		 * 
		 * parent is null previous is null prevtruepos = 0 nexttruepos = head !=
		 * null ? head.truepos : 2 truepos = between(prevtruepos, nexttruepos)
		 * 
		 * In addition the previous first task, if one exists, must be updated,
		 * e.g.:
		 * 
		 * head.prevtruepos = newTask.truepos head.previous = newTask Now all
		 * fields are correct.
		 */
		ContentValues posValues = getNewPositionValuesFor(
				values.getAsLong(Notes.COLUMN_NAME_PARENT),
				values.getAsLong(Notes.COLUMN_NAME_PREVIOUS), listId, db);

		values.putAll(posValues);

		// Need to remember the task at that position

		// Performs the insert and returns the ID of the new note.
		long rowId = db.insert(NotePad.Notes.TABLE_NAME, // The table to insert
															// into.
				NotePad.Notes.COLUMN_NAME_NOTE, // A hack, SQLite sets this
												// column value to null
												// if values is empty.
				values // A map of column names, and the values to insert
						// into the columns.
				);

		// If the insert succeeded, the row ID exists.
		if (rowId > 0) {
			// Bump previously surrounding tasks
			bumpTaskAt(values.getAsString(Notes.COLUMN_NAME_PREVTRUEPOS), null,
					posValues.getAsString(Notes.COLUMN_NAME_TRUEPOS), listId,
					db);
			bumpTaskAt(values.getAsString(Notes.COLUMN_NAME_NEXTTRUEPOS),
					posValues.getAsString(Notes.COLUMN_NAME_TRUEPOS), null,
					listId, db);
			bumpGTaskAt(values.getAsLong(Notes.COLUMN_NAME_PARENT),
					values.getAsLong(Notes.COLUMN_NAME_PREVIOUS), rowId,
					listId, db);

			// Creates a URI with the note ID pattern and the new row ID
			// appended to it.
			Uri noteUri = ContentUris.withAppendedId(
					NotePad.Notes.CONTENT_ID_URI_BASE, rowId);

			// Notifies observers registered against this provider that the data
			// changed.
			getContext().getContentResolver()
					.notifyChange(noteUri, null, false);
			// Also tell lists watching the other URI
			getContext().getContentResolver().notifyChange(
					NotePad.Notes.CONTENT_VISIBLE_URI, null, false);
			// And update widgets
			updateAllWidgets();
			return noteUri;
		}

		// If the insert didn't succeed, then the rowID is <= 0. Throws an
		// exception.
		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * Creates new valid positions values at the specified location
	 * 
	 * @param db
	 */
	private static ContentValues getNewPositionValuesFor(
			final Long gTaskParent, final Long gTaskPrevious,
			final Long listId, SQLiteDatabase db) {
		Log.d("posnew", "parent: " + gTaskParent + " , previous: "
				+ gTaskPrevious + " , list: " + listId);
		final ContentValues posValues = new ContentValues();
		final ContentValues currentValues = getPrevAndNextPositionValuesFor(
				gTaskParent, gTaskPrevious, listId, db);

		final String prevTruePos = currentValues
				.getAsString(Notes.COLUMN_NAME_PREVTRUEPOS);
		final String nextTruePos = currentValues
				.getAsString(Notes.COLUMN_NAME_NEXTTRUEPOS);

		final int indent = currentValues
				.getAsInteger(Notes.COLUMN_NAME_INDENTLEVEL);

		posValues.put(Notes.COLUMN_NAME_PREVTRUEPOS, prevTruePos);
		posValues.put(Notes.COLUMN_NAME_NEXTTRUEPOS, nextTruePos);
		posValues.put(Notes.COLUMN_NAME_TRUEPOS,
				Notes.between(prevTruePos, nextTruePos));

		posValues.put(Notes.COLUMN_NAME_INDENTLEVEL, indent);

		if (gTaskParent == null)
			posValues.putNull(Notes.COLUMN_NAME_PARENT);
		if (gTaskPrevious == null)
			posValues.putNull(Notes.COLUMN_NAME_PREVIOUS);

		return posValues;
	}

	/**
	 * Returns the prev and next position values at a certain location. Note
	 * that no Task have to exist at the specified location. Valid values are
	 * always returned.
	 * 
	 * 
	 * @param db
	 */
	private static ContentValues getPrevAndNextPositionValuesFor(
			final Long gTaskParent, final Long gTaskPrevious,
			final Long listId, SQLiteDatabase db) {
		final ContentValues posValues = new ContentValues();
		Cursor c;
		final int indent;
		boolean isPrevious = false;
		if (gTaskPrevious != null) {
			// There exists a task there
			// The previous value should be 1) the note if no children exist
			// or 2) the last recursive child.
			Log.d("posGetVals", "we have previous");
			isPrevious = true;

			c = db.query(NotePad.Notes.TABLE_NAME, new String[] { Notes._ID,
					Notes.COLUMN_NAME_TRUEPOS, Notes.COLUMN_NAME_NEXTTRUEPOS,
					Notes.COLUMN_NAME_INDENTLEVEL }, NotePad.Notes._ID
					+ " IS ?", new String[] { gTaskPrevious.toString() }, null,
					null, Notes.COLUMN_NAME_TRUEPOS);

			if (c.moveToFirst()) {
				indent = c.getInt(c
						.getColumnIndex(Notes.COLUMN_NAME_INDENTLEVEL));
			} else {
				indent = 0;
			}

			c = getLastRecursivePosition(db, c, gTaskPrevious);

		} else if (gTaskParent != null) {
			// Parent is be the previous task
			Log.d("posGetVals", "we have parent");
			isPrevious = true;

			c = db.query(NotePad.Notes.TABLE_NAME, new String[] { Notes._ID,
					Notes.COLUMN_NAME_TRUEPOS, Notes.COLUMN_NAME_NEXTTRUEPOS,
					Notes.COLUMN_NAME_INDENTLEVEL }, NotePad.Notes._ID
					+ " IS ?", new String[] { gTaskParent.toString() }, null,
					null, Notes.COLUMN_NAME_TRUEPOS);

			if (c.moveToFirst()) {
				indent = 1 + c.getInt(c
						.getColumnIndex(Notes.COLUMN_NAME_INDENTLEVEL));
			} else {
				indent = 1;
			}

		} else {
			Log.d("posGetVals", "we have note at top");

			indent = 0;

			c = db.query(NotePad.Notes.TABLE_NAME, new String[] { Notes._ID,
					Notes.COLUMN_NAME_PREVTRUEPOS, Notes.COLUMN_NAME_TRUEPOS,
					Notes.COLUMN_NAME_INDENTLEVEL },
					NotePad.Notes.COLUMN_NAME_PREVTRUEPOS + " IS ? AND "
							+ Notes.COLUMN_NAME_LIST + " IS ?",
					new String[] { Notes.HEAD, listId.toString() }, null, null,
					Notes.COLUMN_NAME_TRUEPOS);
		}
		// If we found a note, just pluck the values
		// If no note was found, prev is HEAD and next is TAIL

		if (c.moveToLast()) {
			Log.d("posGetVals", "calculating from note we found");

			if (isPrevious) {
				// Previous is this
				posValues.put(Notes.COLUMN_NAME_PREVTRUEPOS, c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_TRUEPOS)));
				// Next is the next
				posValues.put(Notes.COLUMN_NAME_NEXTTRUEPOS, c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_NEXTTRUEPOS)));
			} else {
				// Previous is the previous of this note
				posValues.put(Notes.COLUMN_NAME_PREVTRUEPOS, c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_PREVTRUEPOS)));
				// Next is this note
				posValues.put(Notes.COLUMN_NAME_NEXTTRUEPOS, c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_TRUEPOS)));
			}

		} else {
			Log.d("posGetVals", "we must have an empty list");
			posValues.put(Notes.COLUMN_NAME_PREVTRUEPOS, Notes.HEAD);
			posValues.put(Notes.COLUMN_NAME_NEXTTRUEPOS, Notes.TAIL);
		}

		c.close();

		posValues.put(Notes.COLUMN_NAME_INDENTLEVEL, indent);

		return posValues;
	}

	/**
	 * Assigns the prevtruepos and nexttruepos fields of the task at truepos.
	 * Ignores null arguments
	 * 
	 * Returns number of database rows updated
	 * 
	 * @param db
	 */
	private static int bumpTaskAt(final String truePos,
			final String prevTruePos, final String nextTruePos,
			final Long listId, SQLiteDatabase db) {
		final ContentValues values = new ContentValues();
		if (prevTruePos != null)
			values.put(Notes.COLUMN_NAME_PREVTRUEPOS, prevTruePos);
		if (nextTruePos != null)
			values.put(Notes.COLUMN_NAME_NEXTTRUEPOS, nextTruePos);

		return db.update(Notes.TABLE_NAME, values, Notes.COLUMN_NAME_TRUEPOS
				+ " IS ? AND " + Notes.COLUMN_NAME_LIST + " IS ?",
				new String[] { truePos, listId.toString() });
	}

	/**
	 * Assigns the previous field of the task at the specified location with a
	 * new previous field = newTask
	 * 
	 * Returns number of database rows updated
	 */
	private static int bumpGTaskAt(final Long parent, final Long previous,
			final Long newTaskId, final Long newList, SQLiteDatabase db) {
		final ContentValues values = new ContentValues();
		values.put(Notes.COLUMN_NAME_PREVIOUS, newTaskId);

		String par = parent != null ? parent.toString() : null;
		String pre = previous != null ? previous.toString() : null;

		return db.update(Notes.TABLE_NAME, values, Notes.COLUMN_NAME_PARENT
				+ " IS ? AND " + Notes.COLUMN_NAME_PREVIOUS + " IS ? AND "
				+ Notes._ID + " IS NOT ? AND " + Notes.COLUMN_NAME_LIST
				+ " IS ?", new String[] { par, pre, Long.toString(newTaskId),
				Long.toString(newList) });
	}

	synchronized private Uri insertList(Uri uri, ContentValues initialValues) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "insertList");
		// A map to hold the new record's values.
		ContentValues values;

		// If the incoming values map is not null, uses it for the new values.
		if (initialValues != null) {
			values = new ContentValues(initialValues);

		} else {
			// Otherwise, create a new value map
			values = new ContentValues();
		}

		// If the values map doesn't contain a title, sets the value to the
		// default title.
		if (values.containsKey(NotePad.Lists.COLUMN_NAME_TITLE) == false) {
			Resources r = Resources.getSystem();
			values.put(NotePad.Lists.COLUMN_NAME_TITLE,
					r.getString(android.R.string.untitled));
		}

		if (values.containsKey(NotePad.Lists.COLUMN_NAME_MODIFIED) == false) {
			values.put(NotePad.Lists.COLUMN_NAME_MODIFIED, 1);
		}

		if (values.containsKey(NotePad.Lists.COLUMN_NAME_DELETED) == false) {
			values.put(NotePad.Lists.COLUMN_NAME_DELETED, 0);
		}

		// Gets the current system time in milliseconds
		Long now = Long.valueOf(System.currentTimeMillis());

		// If the values map doesn't contain the modification date, sets the
		// value to the current
		// time.
		if (values.containsKey(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE) == false) {
			values.put(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE, now);
		}

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		// Performs the insert and returns the ID of the new note.
		long rowId = db.insert(NotePad.Lists.TABLE_NAME, // The table to insert
															// into.
				NotePad.Lists.COLUMN_NAME_TITLE, // A hack, SQLite sets this
													// column value to null
													// if values is empty.
				values // A map of column names, and the values to insert
						// into the columns.
				);

		// If the insert succeeded, the row ID exists.
		if (rowId > 0) {
			// Creates a URI with the note ID pattern and the new row ID
			// appended to it.
			Uri noteUri = ContentUris.withAppendedId(
					NotePad.Lists.CONTENT_ID_URI_BASE, rowId);

			// Notifies observers registered against this provider that the data
			// changed.
			getContext().getContentResolver()
					.notifyChange(noteUri, null, false);
			// Also tell lists watching the other URI
			getContext().getContentResolver().notifyChange(
					NotePad.Notes.CONTENT_VISIBLE_URI, null, false);
			// And update widgets
			updateAllWidgets();
			return noteUri;
		}

		// If the insert didn't succeed, then the rowID is <= 0. Throws an
		// exception.
		throw new SQLException("Failed to insert row into " + uri);
	}

	synchronized private Uri insertGTask(Uri uri, ContentValues initialValues) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "insertGTask");
		// A map to hold the new record's values.
		ContentValues values;

		// If the incoming values map is not null, uses it for the new values.
		if (initialValues != null) {
			values = new ContentValues(initialValues);

		} else {
			// Otherwise, create a new value map
			values = new ContentValues();
		}

		// If the values map doesn't contain a title, sets the value to the
		// default title.
		if (values.containsKey(NotePad.GTasks.COLUMN_NAME_DB_ID) == false
				|| values
						.containsKey(NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT) == false
				|| values.containsKey(NotePad.GTasks.COLUMN_NAME_GTASKS_ID) == false
				|| values.containsKey(NotePad.GTasks.COLUMN_NAME_ETAG) == false) {
			throw new SQLException(
					"Must always include a valid values when creating a GTask. They are provided by the server");
		}

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		// Performs the insert and returns the ID of the new note.
		long rowId = db.insert(NotePad.GTasks.TABLE_NAME, // The table to insert
															// into.
				NotePad.GTasks.COLUMN_NAME_DB_ID, // A hack, SQLite sets this
													// column value to null
													// if values is empty.
				values // A map of column names, and the values to insert
						// into the columns.
				);

		// If the insert succeeded, the row ID exists.
		if (rowId > 0) {
			// Creates a URI with the note ID pattern and the new row ID
			// appended to it.
			Uri noteUri = ContentUris.withAppendedId(
					NotePad.GTasks.CONTENT_ID_URI_BASE, rowId);

			// Notifies observers registered against this provider that the data
			// changed.
			getContext().getContentResolver()
					.notifyChange(noteUri, null, false);
			// Also tell lists watching the other URI
			getContext().getContentResolver().notifyChange(
					NotePad.Notes.CONTENT_VISIBLE_URI, null, false);
			// And update widgets
			updateAllWidgets();
			return noteUri;
		}

		// If the insert didn't succeed, then the rowID is <= 0. Throws an
		// exception.
		throw new SQLException("Failed to insert row into " + uri);
	}

	synchronized private Uri insertGTaskList(Uri uri,
			ContentValues initialValues) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "insertGTaskList");
		// A map to hold the new record's values.
		ContentValues values;

		// If the incoming values map is not null, uses it for the new values.
		if (initialValues != null) {
			values = new ContentValues(initialValues);

		} else {
			// Otherwise, create a new value map
			values = new ContentValues();
		}

		// If the values map doesn't contain a title, sets the value to the
		// default title.
		if (values.containsKey(NotePad.GTaskLists.COLUMN_NAME_DB_ID) == false
				|| values
						.containsKey(NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT) == false
				|| values.containsKey(NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID) == false
				|| values.containsKey(NotePad.GTaskLists.COLUMN_NAME_ETAG) == false) {
			throw new SQLException(
					"Must always include a valid values when creating a GTaskList. They are provided by the server");
		}

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		// Performs the insert and returns the ID of the new note.
		long rowId = db.insert(NotePad.GTaskLists.TABLE_NAME, // The table to
																// insert
																// into.
				NotePad.GTaskLists.COLUMN_NAME_DB_ID, // A hack, SQLite sets
														// this
				// column value to null
				// if values is empty.
				values // A map of column names, and the values to insert
						// into the columns.
				);

		// If the insert succeeded, the row ID exists.
		if (rowId > 0) {
			// Creates a URI with the note ID pattern and the new row ID
			// appended to it.
			Uri noteUri = ContentUris.withAppendedId(
					NotePad.GTaskLists.CONTENT_ID_URI_BASE, rowId);

			// Notifies observers registered against this provider that the data
			// changed.
			getContext().getContentResolver()
					.notifyChange(noteUri, null, false);
			// Also tell lists watching the other URI
			getContext().getContentResolver().notifyChange(
					NotePad.Notes.CONTENT_VISIBLE_URI, null, false);
			// And update widgets
			updateAllWidgets();
			return noteUri;
		}

		// If the insert didn't succeed, then the rowID is <= 0. Throws an
		// exception.
		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * Calls deleteListFromDb on all ids fetched from cursor.
	 * 
	 * @param db
	 * @param cursor
	 */
	synchronized private static int deleteListsFromDb(SQLiteDatabase db,
			Cursor cursor) {
		int count = 0;
		while (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
			if (!cursor.moveToNext())
				break;

			long id = cursor
					.getLong(cursor.getColumnIndex((NotePad.Lists._ID)));

			count += deleteListFromDb(db, Long.toString(id), null, null);
		}

		return count;
	}

	/**
	 * Performs a query: WHERE listId IS ?, listId then does deleteNotesFromDb
	 * on the cursor
	 * 
	 * @param db
	 * @param listId
	 */
	synchronized private static int deleteNotesInListFromDb(SQLiteDatabase db,
			String listId) {
		int count = 0;
		Cursor cursor = db.query(NotePad.Notes.TABLE_NAME,
				new String[] { NotePad.Notes._ID },
				NotePad.Notes.COLUMN_NAME_LIST + " IS ?",
				new String[] { listId }, null, null, null);

		count = deleteNotesFromDb(db, cursor);

		cursor.close();

		return count;
	}

	/**
	 * Also deletes from gtasklists table and calls deleteNotesInListFromDb
	 * 
	 * @param db
	 * @param id
	 * @param where
	 * @param whereArgs
	 * @return
	 */
	synchronized private static int deleteListFromDb(SQLiteDatabase db,
			String id, String where, String[] whereArgs) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "Deleting list from DB: " + id);

		String finalWhere = BaseColumns._ID + " = " + id;

		// If there were additional selection criteria, append them to the
		// final
		// WHERE clause
		if (where != null) {
			finalWhere = finalWhere + " AND " + where;
		}

		int count = db.delete(NotePad.Lists.TABLE_NAME, finalWhere, whereArgs);

		// Also delete from GTaskLists table
		if (count > 0) {
			db.delete(NotePad.GTaskLists.TABLE_NAME,
					NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS ?",
					new String[] { id });
		}

		// And notes in this list
		deleteNotesInListFromDb(db, id);

		return count;
	}

	/**
	 * Expects cursor to contain _ID columns of notes. Will then call
	 * deleteNoteFromDB for every one.
	 * 
	 * @param db
	 * @param cursor
	 * @param where
	 * @param whereArgs
	 * @return
	 */
	synchronized private static int deleteNotesFromDb(SQLiteDatabase db,
			Cursor cursor) {
		int count = 0;
		while (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
			if (!cursor.moveToNext())
				break;

			long id = cursor
					.getLong(cursor.getColumnIndex((NotePad.Notes._ID)));

			count += deleteNoteFromDb(db, Long.toString(id), null, null);
		}
		return count;
	}

	/**
	 * Deletes a single note and its accompanying gtasks entry Assumes
	 * surrounding notes already have been resorted. Notes should be marked as
	 * deleted before they are deleted for good. Update function will reorder
	 * correctly.
	 * 
	 * @param db
	 * @param id
	 * @param where
	 * @param whereArgs
	 * @return
	 */
	synchronized private static int deleteNoteFromDb(SQLiteDatabase db,
			String id, String where, String[] whereArgs) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "Deleting note from DB: " + id);

		String finalWhere = BaseColumns._ID + " = " + id;

		// If there were additional selection criteria, append them to the
		// final
		// WHERE clause
		if (where != null) {
			finalWhere = finalWhere + " AND " + where;
		}

		Log.d("posDEL", "reordering before real delete");
		// Make sure surrounding notes have proper values first
		final ContentValues values = new ContentValues();
		values.put(Notes.COLUMN_NAME_DELETED, "1");
		int count = reorderNotes(db, values, finalWhere, whereArgs);

		count += db.delete(NotePad.Notes.TABLE_NAME, // The database table
				// name
				finalWhere, // The incoming where clause column names
				whereArgs // The incoming where clause values
				);

		// Also delete from GTasks table
		if (count > 0) {
			db.delete(NotePad.GTasks.TABLE_NAME,
					NotePad.GTasks.COLUMN_NAME_DB_ID + " IS ?",
					new String[] { id });
		}

		return count;
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#delete(Uri, String, String[])}.
	 * Deletes records from the database. If the incoming URI matches the note
	 * ID URI pattern, this method deletes the one record specified by the ID in
	 * the URI. Otherwise, it deletes a a set of records. The record or records
	 * must also match the input selection criteria specified by where and
	 * whereArgs.
	 * 
	 * If rows were deleted, then listeners are notified of the change.
	 * 
	 * @return If a "where" clause is used, the number of rows affected is
	 *         returned, otherwise 0 is returned. To delete all rows and get a
	 *         row count, use "1" as the where clause.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	synchronized public int delete(Uri uri, String where, String[] whereArgs) {

		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "delete");
		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		String finalWhere;
		Cursor cursor;

		int count;

		// Does the delete based on the incoming URI pattern.
		switch (sUriMatcher.match(uri)) {

		// If the incoming pattern matches the general pattern for notes, does a
		// delete
		// based on the incoming "where" columns and arguments.
		case VISIBLE_NOTES:
		case NOTES:
			// Need to do it like this to be able to delete from the gtasks
			// table at the same time
			cursor = db.query(NotePad.Notes.TABLE_NAME,
					new String[] { NotePad.Notes._ID }, where, whereArgs, null,
					null, null);

			count = deleteNotesFromDb(db, cursor);

			cursor.close();
			break;

		// If the incoming URI matches a single note ID, does the delete based
		// on the
		// incoming data, but modifies the where clause to restrict it to the
		// particular note ID.
		case VISIBLE_NOTE_ID:
		case NOTE_ID:
			count = deleteNoteFromDb(
					db,
					uri.getPathSegments().get(
							NotePad.Notes.NOTE_ID_PATH_POSITION), where,
					whereArgs);
			break;

		case VISIBLE_LISTS:
		case LISTS:

			// get the IDs that are about to be deleted cursor =
			cursor = db.query(NotePad.Lists.TABLE_NAME,
					new String[] { NotePad.Lists._ID }, where, whereArgs, null,
					null, null);

			count = deleteListsFromDb(db, cursor);
			cursor.close();

			break;
		case VISIBLE_LIST_ID:
		case LISTS_ID:
			count = deleteListFromDb(db,
					uri.getPathSegments().get(NotePad.Lists.ID_PATH_POSITION),
					where, whereArgs);
			break;

		case GTASKS:
			count = db.delete(NotePad.GTasks.TABLE_NAME, // The database table
															// name
					where, // The incoming where clause column names
					whereArgs // The incoming where clause values
					);
			break;
		case GTASKS_ID:
			finalWhere = BaseColumns._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming note ID
							get(NotePad.GTasks.ID_PATH_POSITION);

			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}

			// Performs the delete.
			count = db.delete(NotePad.GTasks.TABLE_NAME, // The database table
															// name.
					finalWhere, // The final WHERE clause
					whereArgs // The incoming where clause values.
					);
			break;
		case GTASKLISTS:
			count = db.delete(NotePad.GTaskLists.TABLE_NAME, // The database
																// table
					// name
					where, // The incoming where clause column names
					whereArgs // The incoming where clause values
					);
			break;
		case GTASKLISTS_ID:
			finalWhere = BaseColumns._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming note ID
							get(NotePad.GTaskLists.ID_PATH_POSITION);

			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}

			// Performs the delete.
			count = db.delete(NotePad.GTaskLists.TABLE_NAME, // The database
																// table
					// name.
					finalWhere, // The final WHERE clause
					whereArgs // The incoming where clause values.
					);
			break;

		// If the incoming pattern is invalid, throws an exception.
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		/*
		 * Gets a handle to the content resolver object for the current context,
		 * and notifies it that the incoming URI changed. The object passes this
		 * along to the resolver framework, and observers that have registered
		 * themselves for the provider are notified.
		 */
		getContext().getContentResolver().notifyChange(uri, null, false);
		// Also tell lists watching the other URI
		getContext().getContentResolver().notifyChange(
				NotePad.Notes.CONTENT_VISIBLE_URI, null, false);
		// And update widgets
		updateAllWidgets();

		// Returns the number of rows deleted.
		return count;
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#update(Uri,ContentValues,String,String[])}
	 * Updates records in the database. The column names specified by the keys
	 * in the values map are updated with new data specified by the values in
	 * the map. If the incoming URI matches the note ID URI pattern, then the
	 * method updates the one record specified by the ID in the URI; otherwise,
	 * it updates a set of records. The record or records must match the input
	 * selection criteria specified by where and whereArgs. If rows were
	 * updated, then listeners are notified of the change.
	 * 
	 * @param uri
	 *            The URI pattern to match and update.
	 * @param values
	 *            A map of column names (keys) and new values (values).
	 * @param where
	 *            An SQL "WHERE" clause that selects records based on their
	 *            column values. If this is null, then all records that match
	 *            the URI pattern are selected.
	 * @param whereArgs
	 *            An array of selection criteria. If the "where" param contains
	 *            value placeholders ("?"), then each placeholder is replaced by
	 *            the corresponding element in the array.
	 * @return The number of rows updated.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	synchronized public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		if (SyncAdapter.SYNC_DEBUG_PRINTS)
			Log.d(TAG, "update");

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = 0;
		String finalWhere;

		// Gets the current system time in milliseconds
		Long now = Long.valueOf(System.currentTimeMillis());

		// Does the update based on the incoming URI pattern
		switch (sUriMatcher.match(uri)) {

		// If the incoming URI matches the general notes pattern, does the
		// update based on
		// the incoming data.
		case VISIBLE_NOTES:
		case NOTES:

			if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFIED) == false) {
				values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, 1);
			} else if (values.getAsInteger(NotePad.Notes.COLUMN_NAME_MODIFIED) < 0
					|| values.getAsInteger(NotePad.Notes.COLUMN_NAME_MODIFIED) > 1) {
				// Indicates that we do not want to override whatever this field
				// is in the data base.
				// Local operation that should not be synced
				values.remove(NotePad.Notes.COLUMN_NAME_MODIFIED);
			}
			// If the values map doesn't contain the modification date, sets the
			// value to the current
			// time.
			if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
				values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
			}
			if (values.containsKey(NotePad.Notes.COLUMN_NAME_HIDDEN) == false) {
				values.put(NotePad.Notes.COLUMN_NAME_HIDDEN, 0);
			}

			count += reorderNotes(db, values, where, whereArgs);

			// Does the update and returns the number of rows updated.
			count += db.update(NotePad.Notes.TABLE_NAME, // The database table
															// name.
					values, // A map of column names and new values to use.
					where, // The where clause column names.
					whereArgs // The where clause column values to select on.
					);
			break;

		// If the incoming URI matches a single note ID, does the update based
		// on the incoming
		// data, but modifies the where clause to restrict it to the particular
		// note ID.
		case VISIBLE_NOTE_ID:
		case NOTE_ID:
			if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFIED) == false) {
				values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, 1);
			} else if (values.getAsInteger(NotePad.Notes.COLUMN_NAME_MODIFIED) < 0
					|| values.getAsInteger(NotePad.Notes.COLUMN_NAME_MODIFIED) > 1) {
				// Indicates that we do not want to override whatever this field
				// is in the data base.
				// Local operation that should not be synced
				values.remove(NotePad.Notes.COLUMN_NAME_MODIFIED);
			}

			if (values.containsKey(NotePad.Notes.COLUMN_NAME_HIDDEN) == false) {
				values.put(NotePad.Notes.COLUMN_NAME_HIDDEN, 0);
			}
			// If the values map doesn't contain the modification date, sets the
			// value to the current
			// time.
			if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
				values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
			}

			/*
			 * Starts creating the final WHERE clause by restricting it to the
			 * incoming note ID.
			 */
			finalWhere = BaseColumns._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming note ID
							get(NotePad.Notes.NOTE_ID_PATH_POSITION);

			// If there were additional selection criteria, append them to the
			// final WHERE
			// clause
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}

			count += reorderNotes(db, values, finalWhere, whereArgs);

			// Does the update and returns the number of rows updated.
			count += db.update(NotePad.Notes.TABLE_NAME, // The database table
															// name.
					values, // A map of column names and new values to use.
					finalWhere, // The final WHERE clause to use
								// placeholders for whereArgs
					whereArgs // The where clause column values to select on, or
								// null if the values are in the where argument.
					);
			break;

		case VISIBLE_LISTS:
		case LISTS:
			if (values.containsKey(NotePad.Lists.COLUMN_NAME_MODIFIED) == false) {
				values.put(NotePad.Lists.COLUMN_NAME_MODIFIED, 1);
			}
			// If the values map doesn't contain the modification date, sets the
			// value to the current
			// time.
			if (values.containsKey(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE) == false) {
				values.put(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE, now);
			}

			// Does the update and returns the number of rows updated.
			count = db.update(NotePad.Lists.TABLE_NAME, // The database table
														// name.
					values, // A map of column names and new values to use.
					where, // The where clause column names.
					whereArgs // The where clause column values to select on.
					);
			break;
		case VISIBLE_LIST_ID:
		case LISTS_ID:
			if (values.containsKey(NotePad.Lists.COLUMN_NAME_MODIFIED) == false) {
				values.put(NotePad.Lists.COLUMN_NAME_MODIFIED, 1);
			}
			// If the values map doesn't contain the modification date, sets the
			// value to the current
			// time.
			if (values.containsKey(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE) == false) {
				values.put(NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE, now);
			}

			finalWhere = BaseColumns._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming note ID
							get(NotePad.Lists.ID_PATH_POSITION);

			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}

			// Does the update and returns the number of rows updated.
			count = db.update(NotePad.Lists.TABLE_NAME, // The database table
														// name.
					values, // A map of column names and new values to use.
					finalWhere, // The final WHERE clause to use
								// placeholders for whereArgs
					whereArgs // The where clause column values to select on, or
								// null if the values are in the where argument.
					);
			break;

		case GTASKS:
			// Does the update and returns the number of rows updated.
			count = db.update(NotePad.GTasks.TABLE_NAME, // The database table
															// name.
					values, // A map of column names and new values to use.
					where, // The where clause column names.
					whereArgs // The where clause column values to select on.
					);
			break;
		case GTASKS_ID:
			finalWhere = BaseColumns._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming note ID
							get(NotePad.GTasks.ID_PATH_POSITION);

			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}

			// Does the update and returns the number of rows updated.
			count = db.update(NotePad.GTasks.TABLE_NAME, // The database table
															// name.
					values, // A map of column names and new values to use.
					finalWhere, // The final WHERE clause to use
								// placeholders for whereArgs
					whereArgs // The where clause column values to select on, or
								// null if the values are in the where argument.
					);
			break;

		case GTASKLISTS:
			// Does the update and returns the number of rows updated.
			count = db.update(NotePad.GTaskLists.TABLE_NAME, // The database
																// table
					// name.
					values, // A map of column names and new values to use.
					where, // The where clause column names.
					whereArgs // The where clause column values to select on.
					);
			break;
		case GTASKLISTS_ID:
			finalWhere = BaseColumns._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming note ID
							get(NotePad.GTaskLists.ID_PATH_POSITION);

			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}

			// Does the update and returns the number of rows updated.
			count = db.update(NotePad.GTaskLists.TABLE_NAME, // The database
																// table
					// name.
					values, // A map of column names and new values to use.
					finalWhere, // The final WHERE clause to use
								// placeholders for whereArgs
					whereArgs // The where clause column values to select on, or
								// null if the values are in the where argument.
					);
			break;
		// If the incoming pattern is invalid, throws an exception.
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		/*
		 * Gets a handle to the content resolver object for the current context,
		 * and notifies it that the incoming URI changed. The object passes this
		 * along to the resolver framework, and observers that have registered
		 * themselves for the provider are notified.
		 */
		getContext().getContentResolver().notifyChange(uri, null, false);
		// Manually send an update to the visible notes URL because lists
		// are using this while the editor will use a different URI
		getContext().getContentResolver().notifyChange(
				NotePad.Notes.CONTENT_VISIBLE_URI, null, false);
		// And update widgets
		updateAllWidgets();

		// Returns the number of rows updated.
		return count;
	}

	/**
	 * Called in update. For all the notes that match the where criteria, they
	 * and their surrounding notes (such as sub notes) are moved if movement
	 * changing values are given in the ContentValues, such as:
	 * 
	 * parent, previous, deleted, list
	 * 
	 * If these fields aren't present in the ContentValues, then no changes are
	 * made.
	 * 
	 * Returns number of database rows updated
	 */
	private static int reorderNotes(SQLiteDatabase db,
			final ContentValues values, final String where,
			final String[] whereArgs) {
		Log.d("pos", "Reorder notes WHERE " + where);
		int count = 0;
		final Cursor c;

		if (values.containsKey(Notes.COLUMN_NAME_DELETED)
				|| values.containsKey(Notes.COLUMN_NAME_PARENT)
				|| values.containsKey(Notes.COLUMN_NAME_PREVIOUS)
				|| values.containsKey(Notes.COLUMN_NAME_LIST)) {
			c = db.query(Notes.TABLE_NAME, new String[] { Notes._ID,
					Notes.COLUMN_NAME_LIST, Notes.COLUMN_NAME_PREVTRUEPOS,
					Notes.COLUMN_NAME_TRUEPOS, Notes.COLUMN_NAME_NEXTTRUEPOS,
					Notes.COLUMN_NAME_PREVIOUS, Notes.COLUMN_NAME_PARENT },
					where, whereArgs, null, null, Notes.COLUMN_NAME_TRUEPOS);
		} else
			c = null;

		Log.d("pos", "reorder, c is " + c);

		if (values.containsKey(Notes.COLUMN_NAME_DELETED)) {
			Log.d("posdel", "Marking note as deleted");
			// Deletion
			while (c.moveToNext()) {
				final long id = c.getLong(c.getColumnIndex(Notes._ID));
				final String prevpos = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_PREVTRUEPOS));
				final String pos = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_TRUEPOS));
				final String nextpos = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_NEXTTRUEPOS));
				final String previousS = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_PREVIOUS));
				final String parentS = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_PARENT));

				final Long previous = previousS == null ? null : Long
						.parseLong(previousS);
				final Long parent = parentS == null ? null : Long
						.parseLong(parentS);

				// Update children and get values to set on neighbours
				final ContentValues neighbourVals = updateChildrenOfDeleted(db,
						id, parent, previous, prevpos, nextpos);

				Log.d("posdel",
						"prev new next: "
								+ neighbourVals
										.getAsString(Notes.COLUMN_NAME_NEXTTRUEPOS));

				// Connect above with below
				final ContentValues above = new ContentValues();
				above.put(Notes.COLUMN_NAME_NEXTTRUEPOS, neighbourVals
						.getAsString(Notes.COLUMN_NAME_NEXTTRUEPOS));
				count += db.update(Notes.TABLE_NAME, above,
						Notes.COLUMN_NAME_NEXTTRUEPOS + " IS ?",
						new String[] { pos });

				Log.d("posdel",
						"next new prev: "
								+ neighbourVals
										.getAsString(Notes.COLUMN_NAME_PREVTRUEPOS));

				// Connect below with above
				final ContentValues below = new ContentValues();
				below.put(Notes.COLUMN_NAME_PREVTRUEPOS, neighbourVals
						.getAsString(Notes.COLUMN_NAME_PREVTRUEPOS));
				count += db.update(Notes.TABLE_NAME, below,
						Notes.COLUMN_NAME_PREVTRUEPOS + " IS ?",
						new String[] { pos });

				Log.d("posdel",
						"gtasknext new previous: "
								+ neighbourVals
										.getAsLong(Notes.COLUMN_NAME_PREVIOUS));

				// Connect next with previous
				final ContentValues gtasknext = new ContentValues();
				gtasknext.put(Notes.COLUMN_NAME_PREVIOUS,
						neighbourVals.getAsLong(Notes.COLUMN_NAME_PREVIOUS));
				count += db.update(Notes.TABLE_NAME, gtasknext,
						Notes.COLUMN_NAME_PREVIOUS + " IS ?",
						new String[] { Long.toString(id) });

				Log.d("posdel", "Removing pos: " + pos);
				// Make sure position values are set to values that never will
				// appear for real
				final ContentValues rubbish = new ContentValues();
				rubbish.put(Notes.COLUMN_NAME_PARENT, -2734);
				rubbish.put(Notes.COLUMN_NAME_PREVIOUS, -5864);
				rubbish.put(Notes.COLUMN_NAME_PREVTRUEPOS, "DELPREV");
				rubbish.put(Notes.COLUMN_NAME_TRUEPOS, "DELPOS");
				rubbish.put(Notes.COLUMN_NAME_NEXTTRUEPOS, "DELNEXT");
				db.update(Notes.TABLE_NAME, rubbish, Notes._ID + " IS ?",
						new String[] { Long.toString(id) });
			}

		} else if (values.containsKey(Notes.COLUMN_NAME_PARENT)
				|| values.containsKey(Notes.COLUMN_NAME_PREVIOUS)
				|| values.containsKey(Notes.COLUMN_NAME_LIST)) {
			// Movement
			final Long newParent = values.getAsLong(Notes.COLUMN_NAME_PARENT);
			final Long newPrevious = values
					.getAsLong(Notes.COLUMN_NAME_PREVIOUS);
			final Long newList = values.getAsLong(Notes.COLUMN_NAME_LIST);

			while (c.moveToNext()) {
				final long id = c.getLong(c.getColumnIndex(Notes._ID));
				final String prevpos = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_PREVTRUEPOS));
				final String pos = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_TRUEPOS));
				final String nextpos = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_NEXTTRUEPOS));
				final Long listId = c.getLong(c
						.getColumnIndex(Notes.COLUMN_NAME_LIST));

				final String previousS = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_PREVIOUS));
				final String parentS = c.getString(c
						.getColumnIndex(Notes.COLUMN_NAME_PARENT));

				final Long previous = previousS == null ? null : Long
						.parseLong(previousS);
				final Long parent = parentS == null ? null : Long
						.parseLong(parentS);

				Log.d("posreordertest", "Gotten as strings: " + previousS
						+ ", " + parentS);

				// Only do position changes if these fields have actually
				// changed
				boolean listChanged = newList != null && newList != listId;
				if (listChanged || newParent != parent
						|| newPrevious != previous) {
					Log.d("posreorder", "we have changes fields!");
					Log.d("posreorder",
							"list changed: " + Boolean.toString(listChanged)
									+ ", parent changed: "
									+ Boolean.toString(newParent != parent)
									+ ", previous changed: "
									+ Boolean.toString(newPrevious != previous));

					Log.d("posreorder", "parent: " + parent + ", newParent: "
							+ newParent);
					Log.d("posreorder", "previous: " + previous
							+ ", newPrevious: " + newPrevious);

					// First remove with subtasks from current location with a
					// copy-delete move. This is to make Google Tasks API play
					// nice with us.
					// TODO if the parent is moved after a child, delete only
					// the parent, then put it there
					// Change from recursion to checking the truepos fields. Its last child will must have a value < new one
					// or the item must have a truepos > than new one
					if (!isDescendant(db, id, newParent) && !isDescendant(db, id, newPrevious)) {
						count += removeWithSubtasks(db, id, listId, parent,
								previous, prevpos, pos, nextpos);

						// Insert the lot in the new position
						count += insertWithSubtasks(db, id, newParent,
								newPrevious, newList);
					}
				}
			}
		}

		if (c != null)
			c.close();

		return count;
	}

	/**
	 * Returns true if beta is a descendant of alpha
	 */
	private static boolean isDescendant(SQLiteDatabase db, Long alpha, Long beta) {
		if (alpha == beta) {
			return true;
		}
		
		boolean foundInTree = false;
		
		final Cursor c = db.query(NotePad.Notes.TABLE_NAME, new String[] {
				Notes._ID},
				NotePad.Notes.COLUMN_NAME_PARENT + " IS ?",
				new String[] { alpha.toString() }, null, null,
				Notes.COLUMN_NAME_TRUEPOS);
		while (c.moveToNext()) {
			final Long id = c.getLong(c.getColumnIndex(Notes._ID));
			if (isDescendant(db, id, beta)) {
				foundInTree = true;
				break;
			}
		} 
		c.close();
		return foundInTree;
	}

	/**
	 * Will set the position values of the specified note and all its subnotes
	 * to the specified position. The list will be consistent after this
	 * operation, assuming they were properly removed from their earlier
	 * locations before calling this method
	 * 
	 * Returns number of database rows updated
	 */
	private static int insertWithSubtasks(SQLiteDatabase db, long id,
			Long newParent, Long newPrevious, Long newList) {
		int count = 0;
		final ContentValues values = new ContentValues();
		// The parent gets new parent and previous fields
		values.put(Notes.COLUMN_NAME_PARENT, newParent);
		values.put(Notes.COLUMN_NAME_PREVIOUS, newPrevious);
		// And list
		values.put(Notes.COLUMN_NAME_LIST, newList);

		// Get new position values

		final ContentValues newPosValues = getNewPositionValuesFor(newParent,
				newPrevious, newList, db);
		values.putAll(newPosValues);

		// Update neighbours
		count += bumpTaskAt(values.getAsString(Notes.COLUMN_NAME_PREVTRUEPOS),
				null, values.getAsString(Notes.COLUMN_NAME_TRUEPOS), newList,
				db);
		count += bumpTaskAt(values.getAsString(Notes.COLUMN_NAME_NEXTTRUEPOS),
				values.getAsString(Notes.COLUMN_NAME_TRUEPOS), null, newList,
				db);
		count += bumpGTaskAt(values.getAsLong(Notes.COLUMN_NAME_PARENT),
				values.getAsLong(Notes.COLUMN_NAME_PREVIOUS), newList, id, db);

		// Save parent
		count += db.update(Notes.TABLE_NAME, values, Notes._ID + " IS ?",
				new String[] { Long.toString(id) });

		// Update children now as we did the parent
		// Don't change parent/previous fields of them
		final Cursor c = db.query(Notes.TABLE_NAME, new String[] { Notes._ID,
				Notes.COLUMN_NAME_PARENT, Notes.COLUMN_NAME_PREVIOUS },
				Notes.COLUMN_NAME_PARENT + " IS ?",
				new String[] { Long.toString(id) }, null, null,
				Notes.COLUMN_NAME_TRUEPOS);
		while (c.moveToNext()) {
			final Long childId = c.getLong(c.getColumnIndex(Notes._ID));
			final String parentS = c.getString(c
					.getColumnIndex(Notes.COLUMN_NAME_PARENT));
			final String previousS = c.getString(c
					.getColumnIndex(Notes.COLUMN_NAME_PREVIOUS));

			final Long previous = previousS == null ? null : Long
					.parseLong(previousS);
			final Long parent = parentS == null ? null : Long
					.parseLong(parentS);

			count += insertWithSubtasks(db, childId, parent, previous, newList);
		}

		c.close();
		return count;
	}

	/**
	 * Removes a note with possible sub notes from its current position and
	 * stitches the neighbours together The list is not consistent after this
	 * method as the removed notes are in limbo.
	 * 
	 * Returns number of database rows updated
	 */
	private static int removeWithSubtasks(final SQLiteDatabase db,
			final Long id, final Long listId, final Long parent,
			final Long previous, final String prevTruePos,
			final String truePos, final String nextTruePos) {
		int count = 0;

		String nextTruePosOfLast = copyDeleteRecursively(db, id, listId,
				nextTruePos);

		// Connect above with below
		final ContentValues above = new ContentValues();
		above.put(Notes.COLUMN_NAME_NEXTTRUEPOS, nextTruePosOfLast);
		count += db.update(Notes.TABLE_NAME, above,
				Notes.COLUMN_NAME_NEXTTRUEPOS + " IS ? AND "
						+ Notes.COLUMN_NAME_LIST + " IS ?", new String[] {
						truePos, listId.toString() });

		// Connect below with above
		final ContentValues below = new ContentValues();
		below.put(Notes.COLUMN_NAME_PREVTRUEPOS, prevTruePos);
		count += db.update(Notes.TABLE_NAME, below,
				Notes.COLUMN_NAME_PREVTRUEPOS + " IS ? AND "
						+ Notes.COLUMN_NAME_LIST + " IS ?", new String[] {
						nextTruePosOfLast, listId.toString() });

		// Connect next with previous
		final ContentValues gtasknext = new ContentValues();
		gtasknext.put(Notes.COLUMN_NAME_PREVIOUS, previous);
		String sParent = parent != null ? parent.toString() : null;
		count += db.update(Notes.TABLE_NAME, gtasknext,
				Notes.COLUMN_NAME_PARENT + " IS ? AND "
						+ Notes.COLUMN_NAME_PREVIOUS + " IS ? AND "
						+ Notes.COLUMN_NAME_LIST + " IS ?", new String[] {
						sParent, Long.toString(id), listId.toString() });

		return count;
	}

	/**
	 * Will return the nexttruepos of the last recursive note under parent. If
	 * there are no children, will return parentnexttruepos.
	 */
	private static String copyDeleteRecursively(final SQLiteDatabase db,
			final Long parentId, final Long listId,
			final String parentNextTruePos) {

		final String last = parentNextTruePos;
		if (parentId == null)
			return last;

		// Insert a new deleted entry
		final ContentValues values = new ContentValues();
		values.put(NotePad.Notes.COLUMN_NAME_DELETED, 1);
		values.put(NotePad.Notes.COLUMN_NAME_LOCALHIDDEN, 1);
		values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);

		long cId = db.insert(Notes.TABLE_NAME, null, values);

		// Switch their local ids to the new deleted one
		if (cId > -1) {

			final ContentValues gvalues = new ContentValues();
			gvalues.put(NotePad.GTasks.COLUMN_NAME_DB_ID, cId);

			db.update(GTasks.TABLE_NAME, gvalues,
					NotePad.GTasks.COLUMN_NAME_DB_ID + " IS ?",
					new String[] { Long.toString(parentId) });
		}

		// Children
		final Cursor c = db.query(Notes.TABLE_NAME, new String[] { Notes._ID,
				Notes.COLUMN_NAME_NEXTTRUEPOS }, Notes.COLUMN_NAME_PARENT
				+ " IS ?", new String[] { parentId.toString() }, null, null,
				Notes.COLUMN_NAME_TRUEPOS);
		final Long id;
		final String pos;
		if (c.moveToLast()) {
			id = c.getLong(c.getColumnIndex(Notes._ID));
			pos = c.getString(c.getColumnIndex(Notes.COLUMN_NAME_NEXTTRUEPOS));
		} else {
			id = null;
			pos = last;
		}
		c.close();

		return copyDeleteRecursively(db, id, listId, pos);
	}

	/**
	 * Will return the last recursive child, the item that would be displayed as
	 * last in the list, under a parent.
	 * 
	 * If no children exists, this is the parent itself.
	 */
	private static Cursor getLastRecursivePosition(final SQLiteDatabase db,
			final Cursor parentCursor, final Long parentId) {

		// Children
		final Cursor c = db.query(NotePad.Notes.TABLE_NAME, new String[] {
				Notes._ID, Notes.COLUMN_NAME_TRUEPOS,
				Notes.COLUMN_NAME_NEXTTRUEPOS, Notes.COLUMN_NAME_INDENTLEVEL },
				NotePad.Notes.COLUMN_NAME_PARENT + " IS ?",
				new String[] { parentId.toString() }, null, null,
				Notes.COLUMN_NAME_TRUEPOS);
		if (c.moveToLast()) {
			final Long id = c.getLong(c.getColumnIndex(Notes._ID));
			parentCursor.close();
			return getLastRecursivePosition(db, c, id);
		} else
			return parentCursor;
	}

	/**
	 * Updates the children of a deleted note and returns a ContentValues with
	 * the following fields:
	 * 
	 * NEXTTRUEPOS - Set this on deleteds prevtruepos note. Is the first child
	 * or deleted.nexttruepos
	 * 
	 * PREVTRUEPOS - Set this on deleteds nexttruepos. Is deleted.prevtruepos
	 * 
	 * PREVIOUS - Set this on the note with previous = deleted. Is
	 * deleted.previous
	 */
	private static ContentValues updateChildrenOfDeleted(
			final SQLiteDatabase db, final Long parentId,
			final Long parentOfParent, final Long previousOfParent,
			final String prevTruePos, final String nextTruePos) {
		final ContentValues values = new ContentValues();
		// Children
		final Cursor c = db.query(Notes.TABLE_NAME, new String[] { Notes._ID,
				Notes.COLUMN_NAME_TRUEPOS },
				Notes.COLUMN_NAME_PARENT + " IS ?",
				new String[] { parentId.toString() }, null, null,
				Notes.COLUMN_NAME_TRUEPOS);
		while (c.moveToNext()) {
			final long id = c.getLong(c.getColumnIndex(Notes._ID));
			final String pos = c.getString(c
					.getColumnIndex(Notes.COLUMN_NAME_TRUEPOS));

			final ContentValues childValues = new ContentValues();
			childValues.put(Notes.COLUMN_NAME_PARENT, parentOfParent);
			// Only the first child gets a new previous task
			if (previousOfParent != null
					&& !values.containsKey(Notes.COLUMN_NAME_NEXTTRUEPOS)) {
				// Remember first child
				values.put(Notes.COLUMN_NAME_NEXTTRUEPOS, pos);
				childValues.put(Notes.COLUMN_NAME_PREVIOUS, previousOfParent);
				childValues.put(Notes.COLUMN_NAME_PREVTRUEPOS, prevTruePos);
			}
			// Update child
			db.update(Notes.TABLE_NAME, childValues, Notes._ID + " IS ?",
					new String[] { Long.toString(id) });

			// Also remember last direct child
			values.put(Notes.COLUMN_NAME_PREVIOUS, id);
		}

		c.close();

		// If there were no children the fields aren't set
		if (!values.containsKey(Notes.COLUMN_NAME_PREVTRUEPOS)
				&& prevTruePos != null)
			values.put(Notes.COLUMN_NAME_PREVTRUEPOS, prevTruePos);
		if (!values.containsKey(Notes.COLUMN_NAME_NEXTTRUEPOS)
				&& nextTruePos != null)
			values.put(Notes.COLUMN_NAME_NEXTTRUEPOS, nextTruePos);
		if (!values.containsKey(Notes.COLUMN_NAME_PREVIOUS)
				&& previousOfParent != null)
			values.put(Notes.COLUMN_NAME_PREVIOUS, previousOfParent);

		return values;
	}

	/**
	 * Performs the work provided in a single transaction. Done on sync
	 */
	@Override
	synchronized public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations) {
		ContentProviderResult[] result = new ContentProviderResult[operations
				.size()];
		int i = 0;
		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		// Begin a transaction
		db.beginTransaction();
		try {
			for (ContentProviderOperation operation : operations) {
				// Chain the result for back references
				result[i++] = operation.apply(this, result, i);
			}

			db.setTransactionSuccessful();
		} catch (OperationApplicationException e) {
			Log.d(TAG, "batch failed: " + e.getLocalizedMessage());
		} finally {
			db.endTransaction();
		}

		return result;
	}

	/**
	 * Instead of doing this in a service which might be killed, simply call
	 * this whenever something is changed in here
	 * 
	 * Update all widgets's views as this database has changed somehow
	 */
	private void updateAllWidgets() {
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(getContext().getApplicationContext());
		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(getContext()
						.getApplicationContext(), ListWidgetProvider.class));
		Log.d(TAG, "updateAllWidgets before: " + appWidgetIds.length);
		if (appWidgetIds.length > 0) {
			// Tell the widgets that the list items should be invalidated and
			// refreshed!
			// Will call onDatasetChanged in ListWidgetService, doing a new
			// requery
			appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds,
					R.id.notes_list);
		}
	}

	/**
	 * A test package can call this to get a handle to the database underlying
	 * NotePadProvider, so it can insert test data into the database. The test
	 * case class is responsible for instantiating the provider in a test
	 * context; {@link android.test.ProviderTestCase2} does this during the call
	 * to setUp()
	 * 
	 * @return a handle to the database helper object for the provider's data.
	 */
	DatabaseHelper getOpenHelperForTest() {
		return mOpenHelper;
	}
}
