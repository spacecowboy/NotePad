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

package com.nononsenseapps.notepad.legacydatabase;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static DatabaseHandler singleton;

	public static DatabaseHandler getInstance(final Context context) {
		if (singleton == null) {
			singleton = new DatabaseHandler(context);
		}
		return singleton;
	}

	private static final int DATABASE_VERSION = 15;
	public static final String DATABASE_NAME = "nononsense_notes.db";

	private final Context context;
	private final String testPrefix;

	/**
	 * Should use the singleton for normal cases
	 */
	private DatabaseHandler(Context context) {
		this(context, "");
	}

	/**
	 * Use only for JUNIT tests
	 */
	public DatabaseHandler(Context context, String testPrefix) {
		super(context, testPrefix + DATABASE_NAME, null, DATABASE_VERSION);
		// Good idea to have the context that doesn't die with the window
		this.context = context.getApplicationContext();
		this.testPrefix = testPrefix;
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			// This would require android16
			// db.setForeignKeyConstraintsEnabled(true);
			// This works everywhere
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TaskList.CREATE_TABLE);
		db.execSQL(Task.CREATE_TABLE);
		db.execSQL(Task.CREATE_DELETE_TABLE);
		db.execSQL(Task.CREATE_HISTORY_TABLE);
		db.execSQL(Notification.CREATE_TABLE);
		db.execSQL(RemoteTaskList.CREATE_TABLE);
		db.execSQL(RemoteTask.CREATE_TABLE);

		db.execSQL(Notification.CREATE_JOINED_VIEW);

		db.execSQL(Task.TRIGGER_PRE_INSERT);
		db.execSQL(Task.TRIGGER_PRE_DELETE);
		db.execSQL(Task.TRIGGER_POST_DELETE);
		db.execSQL(Task.TRIGGER_MOVE_LIST);
		db.execSQL(Task.CREATE_HISTORY_INSERT_TRIGGER);
		db.execSQL(Task.CREATE_HISTORY_UPDATE_TRIGGER);

		db.execSQL(RemoteTask.TRIGGER_LISTDELETE_CASCADE);
		// Mark as deleted when real item deleted
		db.execSQL(RemoteTask.TRIGGER_REALDELETE_MARK);
		db.execSQL(RemoteTaskList.TRIGGER_REALDELETE_MARK);
		// Create move list trigger
		db.execSQL(RemoteTask.TRIGGER_MOVE_LIST);

		// Search tables
		db.execSQL(Task.CREATE_FTS3_TABLE);
		db.execSQL(Task.CREATE_FTS3_INSERT_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_UPDATE_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_DELETE_TRIGGER);

		// Delete search tables
		db.execSQL(Task.CREATE_FTS3_DELETE_TABLE);
		db.execSQL(Task.CREATE_FTS3_DELETED_INSERT_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_DELETED_UPDATE_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_DELETED_DELETE_TRIGGER);
	}

	public static Cursor getLegacyLists(final SQLiteDatabase legacyDB) {
		return legacyDB.rawQuery("SELECT lists."
										+ BaseColumns._ID
										+ ",lists.title,gtasklists.googleid,gtasklists.googleaccount"
										+ " FROM " + LegacyDBHelper.NotePad.Lists.TABLE_NAME
										+ " LEFT OUTER JOIN "
										+ LegacyDBHelper.NotePad.GTaskLists.TABLE_NAME + " ON ("
										+ LegacyDBHelper.NotePad.Lists.TABLE_NAME + "."
										+ LegacyDBHelper.NotePad.Lists._ID + " = "
										+ LegacyDBHelper.NotePad.GTaskLists.TABLE_NAME + "."
										+ LegacyDBHelper.NotePad.GTaskLists.COLUMN_NAME_DB_ID + ")"
										+ " WHERE lists.deleted IS NOT 1", null);
	}

	public static Cursor getLegacyNotes(final SQLiteDatabase legacyDB) {
		return legacyDB.rawQuery("SELECT notes."
						+ BaseColumns._ID
						+ ",notes.title,notes.note,notes.duedate,notes.gtaskstatus,notes.list,notes.modified,gtasks.googleid,gtasks.googleaccount"
						+ " FROM "
						+ LegacyDBHelper.NotePad.Notes.TABLE_NAME
						+ " LEFT OUTER JOIN "
						+ LegacyDBHelper.NotePad.GTasks.TABLE_NAME
						+ " ON ("
						+ LegacyDBHelper.NotePad.Notes.TABLE_NAME
						+ "."
						+ LegacyDBHelper.NotePad.Notes._ID
						+ " = "
						+ LegacyDBHelper.NotePad.GTasks.TABLE_NAME
						+ "."
						+ LegacyDBHelper.NotePad.GTasks.COLUMN_NAME_DB_ID
						+ ")"
						+ " WHERE notes.deleted IS NOT 1 AND notes.hiddenflag IS NOT 1",
				null);
	}

	public static Cursor getLegacyNotifications(final SQLiteDatabase legacyDB) {
		return legacyDB.query(LegacyDBHelper.NotePad.Notifications.TABLE_NAME, new String[] {
										"time", "permanent", "noteid" }, null, null, null, null,
										null);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 10) {
			// Notification locations
			// Add columns
			String preName = "ALTER TABLE " + Notification.TABLE_NAME + " ADD COLUMN ";
			String postText = " TEXT";
			String postReal = " REAL";
			db.execSQL(preName + Notification.Columns.LOCATIONNAME + postText);
			db.execSQL(preName + Notification.Columns.LATITUDE + postReal);
			db.execSQL(preName + Notification.Columns.LONGITUDE + postReal);
			db.execSQL(preName + Notification.Columns.RADIUS + postReal);
			// Drop view
			db.execSQL("DROP VIEW IF EXISTS " + Notification.WITH_TASK_VIEW_NAME);
			// Recreate view with additional tables
			db.execSQL(Notification.CREATE_JOINED_VIEW);
		}
		if (oldVersion < 11) {
			// Mark as deleted when real item deleted
			db.execSQL(RemoteTask.TRIGGER_REALDELETE_MARK);
			db.execSQL(RemoteTaskList.TRIGGER_REALDELETE_MARK);
		}
		if (oldVersion < 12) {
			// Recreate trigger
			db.execSQL("DROP TRIGGER IF EXISTS task_post_delete");
			db.execSQL(Task.TRIGGER_POST_DELETE);
		}
		if (oldVersion < 13) {
			// Create move list trigger
			db.execSQL(RemoteTask.TRIGGER_MOVE_LIST);
			// Create trigger to fix positions when moving lists
			db.execSQL(Task.TRIGGER_MOVE_LIST);
		}
		if (oldVersion < 14) {
			// Update history update trigger
			db.execSQL("DROP TRIGGER IF EXISTS " + Task.HISTORY_UPDATE_TRIGGER_NAME);
			db.execSQL(Task.CREATE_HISTORY_UPDATE_TRIGGER);
		}
		if (oldVersion < 15) {
			// Drop view, changing to temporary view instead
			db.execSQL("DROP VIEW IF EXISTS " + Notification.WITH_TASK_VIEW_NAME);
		}
	}

}
