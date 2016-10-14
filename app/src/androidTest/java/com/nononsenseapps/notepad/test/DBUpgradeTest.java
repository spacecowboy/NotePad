package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.data.local.sql.DatabaseHandler;
import com.nononsenseapps.notepad.data.local.sql.LegacyDBHelper;
import com.nononsenseapps.notepad.data.local.sql.LegacyDBHelper.NotePad;
import com.nononsenseapps.notepad.data.model.sql.Notification;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

public class DBUpgradeTest extends AndroidTestCase {
	static final String PREFIX = "dbupgrade_test_";

	String aTime = "2013-03-23T02:43:35.000Z";
	String anId = "MDIwMzMwNjA0MjM5MzQ4MzIzMjU6MDow";
	String anAccount = "fake@account.com";

	int numOfLegacyLists = 2;
	int numOfLegacyNotes = 4;

	private Context context;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		context = getContext();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	private void createTables(final SQLiteDatabase legacyDB) {
		// Lists
		legacyDB.execSQL("CREATE TABLE " + NotePad.Lists.TABLE_NAME + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
				+ NotePad.Lists.COLUMN_NAME_TITLE
				+ " TEXT DEFAULT '' NOT NULL,"
				+ NotePad.Lists.COLUMN_NAME_MODIFIED
				+ " INTEGER DEFAULT 0 NOT NULL,"
				+ NotePad.Lists.COLUMN_NAME_MODIFICATION_DATE
				+ " INTEGER DEFAULT 0 NOT NULL,"
				+ NotePad.Lists.COLUMN_NAME_DELETED
				+ " INTEGER DEFAULT 0 NOT NULL" + ");");

		legacyDB.execSQL("CREATE TABLE " + NotePad.GTaskLists.TABLE_NAME + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
				+ NotePad.GTaskLists.COLUMN_NAME_DB_ID
				+ " INTEGER UNIQUE NOT NULL REFERENCES "
				+ NotePad.Lists.TABLE_NAME + ","
				+ NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID
				+ " INTEGER NOT NULL,"
				+ NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT
				+ " INTEGER NOT NULL," + NotePad.GTaskLists.COLUMN_NAME_UPDATED
				+ " TEXT," + NotePad.GTaskLists.COLUMN_NAME_ETAG + " TEXT"
				+ ");");

		// Notes
		legacyDB.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
				+ NotePad.Notes.COLUMN_NAME_TITLE
				+ " TEXT DEFAULT '' NOT NULL," + NotePad.Notes.COLUMN_NAME_NOTE
				+ " TEXT DEFAULT '' NOT NULL,"
				+ NotePad.Notes.COLUMN_NAME_CREATE_DATE
				+ " INTEGER DEFAULT 0 NOT NULL,"
				+ NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
				+ " INTEGER DEFAULT 0 NOT NULL,"
				+ NotePad.Notes.COLUMN_NAME_DUE_DATE + " TEXT,"
				+ NotePad.Notes.COLUMN_NAME_LIST
				+ " INTEGER NOT NULL REFERENCES " + NotePad.Lists.TABLE_NAME
				+ "," + NotePad.Notes.COLUMN_NAME_GTASKS_STATUS
				+ " TEXT NOT NULL," + NotePad.Notes.COLUMN_NAME_POSITION
				+ " TEXT," + NotePad.Notes.COLUMN_NAME_HIDDEN
				+ " INTEGER DEFAULT 0 NOT NULL,"
				+ NotePad.Notes.COLUMN_NAME_MODIFIED
				+ " INTEGER DEFAULT 0 NOT NULL,"

				+ NotePad.Notes.COLUMN_NAME_INDENTLEVEL
				+ " INTEGER DEFAULT 0 NOT NULL,"
				+ NotePad.Notes.COLUMN_NAME_POSSUBSORT
				+ " TEXT DEFAULT '' NOT NULL,"
				+ NotePad.Notes.COLUMN_NAME_LOCALHIDDEN + " INTEGER DEFAULT 0,"

				+ NotePad.Notes.COLUMN_NAME_PARENT + " TEXT,"

				+ NotePad.Notes.COLUMN_NAME_DELETED
				+ " INTEGER DEFAULT 0 NOT NULL" + ");");

		legacyDB.execSQL("CREATE TABLE " + NotePad.GTasks.TABLE_NAME + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
				+ NotePad.GTasks.COLUMN_NAME_DB_ID
				+ " INTEGER UNIQUE NOT NULL REFERENCES " + NotePad.Notes.TABLE_NAME
				+ "," + NotePad.GTasks.COLUMN_NAME_GTASKS_ID
				+ " INTEGER NOT NULL,"
				+ NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT
				+ " INTEGER NOT NULL," + NotePad.GTasks.COLUMN_NAME_UPDATED
				+ " TEXT," + NotePad.GTasks.COLUMN_NAME_ETAG + " TEXT" + ");");

		// Notifications
		legacyDB.execSQL("CREATE TABLE " + NotePad.Notifications.TABLE_NAME
				+ " (" + NotePad.Notifications._ID + " INTEGER PRIMARY KEY,"
				+ NotePad.Notifications.COLUMN_NAME_TIME
				+ " INTEGER NOT NULL DEFAULT 0,"
				+ NotePad.Notifications.COLUMN_NAME_PERMANENT
				+ " INTEGER NOT NULL DEFAULT 0,"
				+ NotePad.Notifications.COLUMN_NAME_NOTEID + " INTEGER,"
				+ "FOREIGN KEY(" + NotePad.Notifications.COLUMN_NAME_NOTEID
				+ ") REFERENCES " + NotePad.Notes.TABLE_NAME + "("
				+ NotePad.Notes._ID + ") ON DELETE CASCADE" + ");");
	}

	private void initializeDB(final SQLiteDatabase legacyDB) {

		legacyDB.beginTransaction();
		// Need to create the tables so we have something to test with.
		createTables(legacyDB);

		// Insert some lists, and some notes
		final ContentValues values = new ContentValues();
		for (int i = 0; i < numOfLegacyLists; i++) {
			values.clear();
			// One plain
			values.put(LegacyDBHelper.NotePad.Lists.COLUMN_NAME_TITLE, "List"
					+ i);
			values.put(LegacyDBHelper.NotePad.Lists.COLUMN_NAME_MODIFIED, 1);
			values.put(LegacyDBHelper.NotePad.Lists.COLUMN_NAME_DELETED, 0);

			final long listId = legacyDB.insert(
					LegacyDBHelper.NotePad.Lists.TABLE_NAME, null, values);

			assertTrue("Failed to insert legacy test list: " + listId,
					listId > 0);

			long gtasklistid = -1;
			// One with google id
			if (i % 2 == 0) {
				values.clear();
				values.put(LegacyDBHelper.NotePad.GTaskLists.COLUMN_NAME_DB_ID,
						listId);
				values.put(
						LegacyDBHelper.NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT,
						anAccount);
				values.put(
						LegacyDBHelper.NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID,
						anId);
				values.put(
						LegacyDBHelper.NotePad.GTaskLists.COLUMN_NAME_UPDATED,
						aTime);

				gtasklistid = legacyDB.insert(
						LegacyDBHelper.NotePad.GTaskLists.TABLE_NAME, null,
						values);

				assertTrue(
						"Failed to insert google dummy list: " + gtasklistid,
						gtasklistid > 0);
			}

			// Insert notes
			for (int j = 0; j < numOfLegacyNotes; j++) {
				values.clear();
				values.put(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_TITLE,
						"default" + j);
				values.put(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_NOTE,
						"defaulttext");
				values.put(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_MODIFIED, 1);
				values.put(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_DELETED, 0);
				values.put(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_LIST,
						listId);

				// Gets the current system time in milliseconds
				Long now = Long.valueOf(System.currentTimeMillis());
				values.put(
						LegacyDBHelper.NotePad.Notes.COLUMN_NAME_CREATE_DATE,
						now);
				values.put(
						LegacyDBHelper.NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
						now);
				values.put(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_DUE_DATE,
						"");
				values.put(
						LegacyDBHelper.NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
						"needsAction");

				values.put(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_POSSUBSORT,
						"");
				values.put(
						LegacyDBHelper.NotePad.Notes.COLUMN_NAME_INDENTLEVEL, 0);

				final long noteId = legacyDB.insert(
						LegacyDBHelper.NotePad.Notes.TABLE_NAME, null, values);

				assertTrue("Note insertion should not fail", noteId > 0);
				if (gtasklistid > -1) {
					// Give SOME of the notes google ids
					if (j % 2 == 0) {
						values.clear();
						values.put(
								LegacyDBHelper.NotePad.GTasks.COLUMN_NAME_DB_ID,
								noteId);
						values.put(
								LegacyDBHelper.NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT,
								anAccount);
						values.put(
								LegacyDBHelper.NotePad.GTasks.COLUMN_NAME_GTASKS_ID,
								anId + j);
						values.put(
								LegacyDBHelper.NotePad.GTasks.COLUMN_NAME_UPDATED,
								aTime);

						final long gtaskid = legacyDB.insert(
								LegacyDBHelper.NotePad.GTasks.TABLE_NAME, null,
								values);
						assertTrue("Gtask insert should not fail", gtaskid > 0);
					}
				}

				// Give all a notification
				values.clear();
				values.put(
						LegacyDBHelper.NotePad.Notifications.COLUMN_NAME_NOTEID,
						noteId);
				values.put(
						LegacyDBHelper.NotePad.Notifications.COLUMN_NAME_TIME,
						System.currentTimeMillis());

				final long notId = legacyDB.insert(
						LegacyDBHelper.NotePad.Notifications.TABLE_NAME, null,
						values);
				assertTrue("legacy notificaiton insert failed", notId > 0);
			}
		}

		legacyDB.setTransactionSuccessful();
		legacyDB.endTransaction();
	}

	@MediumTest
	public void testExistingUpgrade() {
		// First delete test databases if they exist
		context.deleteDatabase(PREFIX + LegacyDBHelper.LEGACY_DATABASE_NAME);
		context.deleteDatabase(PREFIX + DatabaseHandler.DATABASE_NAME);

		final SQLiteDatabase legacyDB = new LegacyDBHelper(context, PREFIX)
				.getWritableDatabase();
		initializeDB(legacyDB);
		
		// Check that things exist
		Cursor c = DatabaseHandler.getLegacyLists(legacyDB);

		assertEquals("LegacyDB not correct for tests", numOfLegacyLists,
				c.getCount());
		c.close();

		c = DatabaseHandler.getLegacyNotes(legacyDB);
		assertEquals("LegacyDB not correct for tests", numOfLegacyLists
				* numOfLegacyNotes, c.getCount());
		c.close();
		
		c = DatabaseHandler.getLegacyNotifications(legacyDB);
		assertEquals("LegacyDB not correct for tests", numOfLegacyLists
				* numOfLegacyNotes, c.getCount());
		c.close();

		// Check that new database correctly converts old
		final SQLiteDatabase db = new DatabaseHandler(context, PREFIX).getReadableDatabase();
		
		c = db.query(TaskList.TABLE_NAME, TaskList.Columns.FIELDS, null, null,
				null, null, null);
		assertEquals("Unexpected amount of lists returned", numOfLegacyLists,
				c.getCount());
		
		// TODO Examine details
		c.close();

		c = db.query(Task.TABLE_NAME, Task.Columns.FIELDS, null, null, null,
				null, null);
		assertEquals("Incorrect number of notes converted", numOfLegacyLists
				* numOfLegacyNotes, c.getCount());
		
		// TODO examine details
		c.close();

		c = db.query(Notification.TABLE_NAME, Notification.Columns.FIELDS,
				null, null, null, null, null);
		assertEquals("Incorrect number of notifications converted",
				numOfLegacyLists * numOfLegacyNotes, c.getCount());
		// TODO examine details
		c.close();

		db.close();
		legacyDB.close();
		
		assertTrue(
				"Could not delete database",
				context.deleteDatabase(PREFIX
						+ LegacyDBHelper.LEGACY_DATABASE_NAME));
		assertTrue("Could not delete database",
				context.deleteDatabase(PREFIX + DatabaseHandler.DATABASE_NAME));
	}
}
