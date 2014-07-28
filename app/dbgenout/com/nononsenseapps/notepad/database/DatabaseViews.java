package com.nononsenseapps.notepad.database;

import android.database.sqlite.SQLiteDatabase;

public class DatabaseViews {

    /**
     * Create permanent views. They are dropped first,
     * if they already exist.
     */
    public static void create(final SQLiteDatabase db) {
        
    }

    /**
     * Create temporary views. Nothing is done if they
     * already exist.
     */
    public static void createTemp(final SQLiteDatabase db) {
        db.execSQL(taskview_template);
    }

    private static final String taskview_template =
"CREATE TEMP VIEW IF NOT EXISTS taskview_template AS SELECT _id FROM taskfts WHERE ftstable MATCH 'textfilter';";
}