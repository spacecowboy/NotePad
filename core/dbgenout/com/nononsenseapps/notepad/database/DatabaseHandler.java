package com.nononsenseapps.notepad.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database handler, SQLite wrapper and ORM layer.
 *
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "SampleDB";
    private final Context context;

    private static DatabaseHandler instance = null;

    public synchronized static DatabaseHandler getInstance(Context context) {
        if (instance == null)
            instance = new DatabaseHandler(context.getApplicationContext());
        return instance;
    }

    public DatabaseHandler(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null,
                DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            // This line requires android16
            // db.setForeignKeyConstraintsEnabled(true);
            // This line works everywhere though
            db.execSQL("PRAGMA foreign_keys=ON;");

            // Create temporary triggers and views
            DatabaseTriggers.createTemp(db);
            DatabaseViews.createTemp(db);
        }
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        
        db.execSQL("DROP TABLE IF EXISTS " + taskviewItem.TABLE_NAME);
        db.execSQL(taskviewItem.CREATE_TABLE);


        // Create Triggers and Views
        DatabaseTriggers.create(db);
        DatabaseViews.create(db);
    }

    // Upgrading database
    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion,
            int newVersion) {
        // Try to drop and recreate. You should do something clever here
        onCreate(db);
    }

    // Convenience methods
    public synchronized boolean putItem(final DBItem item) {
        boolean success = false;
        int result = 0;
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = item.getContent();

        if (item.getId() > -1) {
            result += db.update(item.getTableName(), values,
                    DBItem.COL_ID + " IS ?",
                    new String[] { String.valueOf(item.getId()) });
        }

        // Update failed or wasn't possible, insert instead
        if (result < 1) {
            final long id = db.insert(item.getTableName(), null, values);

            if (id > 0) {
                item.setId(id);
                success = true;
            }
        }
        else {
            success = true;
        }

        if (success) {
            item.notifyProvider(context);
        }
        return success;
    }

    public synchronized int deleteItem(DBItem item) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final int result = db.delete(item.getTableName(), DBItem.COL_ID
                + " IS ?", new String[] { Long.toString(item.getId()) });

        if (result > 0) {
            item.notifyProvider(context);
        }

        return result;
    }


    
    public synchronized Cursor gettaskviewItemCursor(final long id) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(taskviewItem.TABLE_NAME,
                taskviewItem.FIELDS, taskviewItem.COL_ID + " IS ?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        return cursor;
    }

    public synchronized taskviewItem gettaskviewItem(final long id) {
        final Cursor cursor = gettaskviewItemCursor(id);
        final taskviewItem result;
        if (cursor.moveToFirst()) {
            result = new taskviewItem(cursor);
        }
        else {
            result = null;
        }

        cursor.close();
        return result;
    }

    public synchronized Cursor getAlltaskviewItemsCursor(final String selection,
                                                        final String[] args,
                                                        final String sortOrder) {
        final SQLiteDatabase db = this.getReadableDatabase();

        final Cursor cursor = db.query(taskviewItem.TABLE_NAME,
                taskviewItem.FIELDS, selection, args, null, null, sortOrder, null);

        return cursor;
    }

    public synchronized List<taskviewItem> getAlltaskviewItems(final String selection,
                                                             final String[] args,
                                                             final String sortOrder) {
        final List<taskviewItem> result = new ArrayList<taskviewItem>();

        final Cursor cursor = getAlltaskviewItemsCursor(selection, args, sortOrder);

        while (cursor.moveToNext()) {
            taskviewItem q = new taskviewItem(cursor);
            result.add(q);
        }

        cursor.close();
        return result;
    }

}
