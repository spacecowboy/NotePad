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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.RemoteTask;
import com.nononsenseapps.notepad.data.model.sql.RemoteTaskList;
import com.nononsenseapps.notepad.data.model.sql.Notification;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.data.model.gtasks.GoogleTask;
import com.nononsenseapps.notepad.data.model.gtasks.GoogleTaskList;
import com.nononsenseapps.notepad.ui.settings.SyncPrefs;
import com.nononsenseapps.notepad.util.RFC3339Date;

import java.util.Calendar;
import java.util.HashMap;

public class DatabaseHandler extends SQLiteOpenHelper {

    public static final String DEFAULT_TEXT_PREFIX = "test";
    private static DatabaseHandler singleton;

	public static DatabaseHandler getInstance(final Context context) {
		if (singleton == null) {
			singleton = new DatabaseHandler(context);
		}
		return singleton;
	}

    /**
     * Convenience method for {@link #setTestDatabase(Context, String)} with
     * {@link #DEFAULT_TEXT_PREFIX} as the prefix.
     * @param context
     */
    public static void setTestDatabase(@NonNull final Context context) {
        setTestDatabase(context, DEFAULT_TEXT_PREFIX);
    }

    /**
     * Configure the databasehandler to point to a test database instead of the regular database.
     * Please see {@link #resetTestDatabase(Context, String)} as well. Note that reset should be
     * called BEFORE this method if you intend to use the test database afterwards.
     *
     * @param context
     * @param testPrefix non-empty prefix to add to the database name
     */
	public static void setTestDatabase(@NonNull final Context context,
                                       @NonNull final String testPrefix) {
        if (testPrefix.isEmpty()) {
            throw new IllegalArgumentException("No testPrefix given. If you want to set the " +
                    "default database, call resetTestDatabase() instead.");
        }
        if (singleton != null) {
            singleton.close();
            singleton = null;
        }
        singleton = new DatabaseHandler(context, testPrefix);
    }

    /**
     * A convenience method which combines {@link #resetTestDatabase(Context, String)} and
     * {@link #setTestDatabase(Context, String)}. When this method returns, a test database has
     * been set, and it is guaranteed to be in a state matching a newly installed app.
     *
     * @param context
     */
    public static void setFreshTestDatabase(@NonNull final Context context) {
        resetTestDatabase(context, DEFAULT_TEXT_PREFIX);
        setTestDatabase(context, DEFAULT_TEXT_PREFIX);
    }

    /**
     * A convenience method which combines {@link #resetTestDatabase(Context, String)} and
     * {@link #setTestDatabase(Context, String)}. When this method returns, a test database has
     * been set, and it is guaranteed to be in a state matching a newly installed app.
     *
     * @param context
     * @param testPrefix non-empty prefix to add to the database name
     */
    public static void setFreshTestDatabase(@NonNull final Context context,
                                            @NonNull final String testPrefix) {
        resetTestDatabase(context, testPrefix);
        setTestDatabase(context, testPrefix);
    }

    /**
     * A convenience method which combines {@link #resetTestDatabase(Context, String)} and
     * {@link #setTestDatabase(Context, String)}. When this method returns, a test database has
     * been set, and it is guaranteed to be empty.
     *
     * @param context
     */
    public static void setEmptyTestDatabase(@NonNull final Context context) {
        setEmptyTestDatabase(context, DEFAULT_TEXT_PREFIX);
    }

    /**
     * A convenience method which combines {@link #resetTestDatabase(Context, String)} and
     * {@link #setTestDatabase(Context, String)}. When this method returns, a test database has
     * been set, and it is guaranteed to be empty.
     *
     * @param context
     * @param testPrefix non-empty prefix to add to the database name
     */
    public static void setEmptyTestDatabase(@NonNull final Context context,
                                            @NonNull final String testPrefix) {
        resetTestDatabase(context, testPrefix);
        setTestDatabase(context, testPrefix);
        // TODO don't use content resolver here
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(TaskList.URI, null, null);
        resolver.delete(Task.URI, null, null);
        resolver.delete(RemoteTaskList.URI, null, null);
        resolver.delete(RemoteTask.URI, null, null);
    }

    /**
     * Convenience method for {@link #resetTestDatabase(Context, String)} with
     * {@link #DEFAULT_TEXT_PREFIX} as the prefix.
     *
     * @param context
     * @return true if a database was deleted, false otherwise
     */
    public static boolean resetTestDatabase(@NonNull final Context context) {
        return resetTestDatabase(context, DEFAULT_TEXT_PREFIX);
    }

    /**
     * Delete the given database and reset to the default state. Note that giving an empty "" prefix
     * is not allowed.
     *
     * @param context
     * @param testPrefix non-empty prefix to add to the database name
     * @return true if a database was deleted, false otherwise
     */
    public static boolean resetTestDatabase(@NonNull final Context context,
                                            @NonNull final String testPrefix) {
        if (testPrefix.isEmpty()) {
            throw new IllegalArgumentException("No testPrefix given, this would actually delete " +
                    "the standard database which is probably not what you want...");
        }
        if (singleton != null) {
            singleton.close();
            singleton = null;
        }
        return context.deleteDatabase(testPrefix + DATABASE_NAME);
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

		initializedDB(db);
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

	private void initializedDB(final SQLiteDatabase db) throws SQLiteException {
		// Load legacy DB if it exists
		// Open database and copy information
		// Remember to do try except

		db.beginTransaction();
		try {
			final HashMap<Long, Long> listIDMap = new HashMap<Long, Long>();
			final HashMap<Long, Long> taskIDMap = new HashMap<Long, Long>();
			final LegacyDBHelper legacyDBHelper = new LegacyDBHelper(context, testPrefix);
			final SQLiteDatabase legacyDB = legacyDBHelper.getReadableDatabase();

			// First copy lists
			Cursor c = getLegacyLists(legacyDB);

			while (!c.isClosed() && c.moveToNext()) {
				TaskList tl = new TaskList();
				tl.title = c.getString(1);
				tl.updated = Calendar.getInstance().getTimeInMillis();

				// insert into db
				tl.insert(context, db);
				// remember id
				listIDMap.put(c.getLong(0), tl._id);

				// handle gtask info
				GoogleTaskList rl = null;
				if (c.getString(2) != null && !c.getString(2).isEmpty() && c.getString(3) != null
												&& !c.getString(3).isEmpty()) {
					rl = new GoogleTaskList(tl._id, c.getString(2), tl.updated, c.getString(3));
					rl.insert(context, db);
				}
			}
			c.close();

			// Then notes
			if (!listIDMap.isEmpty()) {
				// query
				c = getLegacyNotes(legacyDB);

				// iterate over notes
				while (!c.isClosed() && c.moveToNext()) {
					Task t = new Task();
					t.title = c.getString(1);
					t.note = c.getString(2);

					if (t.note.contains("[locked]")) {
						t.locked = true;
						t.note = t.note.replace("[locked]", "");
					}

					try {
						t.due = RFC3339Date.parseRFC3339Date(c.getString(3)).getTime();
					} catch (Exception e) {
					}

					// completed must be converted
					if (c.getString(4) != null && "completed".equals(c.getString(4))) {
						t.setAsCompleted();
					}
					t.dblist = listIDMap.get(c.getLong(5));

					t.updated = c.getLong(6);

					// insert
					// Just make extra sure list exists
					if (t.dblist != null) {
						t.insert(context, db);

						// put in idmap
						taskIDMap.put(c.getLong(0), t._id);
					}

					// gtask
					GoogleTask gt = null;
					if (!c.isNull(7) && !c.getString(7).isEmpty() && !c.isNull(8)
													&& !c.getString(8).isEmpty()) {
						gt = new GoogleTask(t, c.getString(8));
						gt.remoteId = c.getString(7);
						gt.updated = t.updated;
						gt.insert(context, db);
					}

				}
				c.close();
			}

			// Then notifications
			if (!taskIDMap.isEmpty()) {
				c = getLegacyNotifications(legacyDB);

				while (!c.isClosed() && c.moveToNext()) {
					// Make sure id exists
					if (taskIDMap.containsValue(c.getLong(2))) {
						Notification n = new Notification(taskIDMap.get(c.getLong(2)));
						n.time = c.getLong(0);
						// permanent was not supported at the time
						// insert
						n.insert(context, db);
					}
				}
				c.close();
			}

			// Complete, close the legacy db
			legacyDB.close();
		} catch (SQLException e) {
			// Database must have been empty. Ignore it
			// Test reasons, throw it!
			// throw e;
		}

		// If no lists, insert a list and example note.

		Cursor c = db.query(TaskList.TABLE_NAME, TaskList.Columns.FIELDS, null, null, null, null,
										null);

		if (!c.isClosed() && c.getCount() > 0) {
			// Done
		} else {
			// If preferences has sync enabled, don't create this list
			// The backup agent has restored a reinstallation
			if (PreferenceManager.getDefaultSharedPreferences(context).contains(
											SyncPrefs.KEY_ACCOUNT)) {

			} else {

				// Create a list
				final TaskList tl = new TaskList();
				tl.title = context.getString(R.string.tasks);
				tl.insert(context, db);

				// final Task t = new Task();
				// t.dblist = tl._id;
				// t.title = context.getString(R.string.default_notetitle);
				// t.note = context.getString(R.string.default_notetext);
				// t.insert(context, db);
			}
		}
		c.close();
		db.setTransactionSuccessful();
		db.endTransaction();
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
