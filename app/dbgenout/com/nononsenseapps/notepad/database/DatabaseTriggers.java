package com.nononsenseapps.notepad.database;

import android.database.sqlite.SQLiteDatabase;

public class DatabaseTriggers {

    /**
     * Create permanent triggers. They are dropped first,
     * if they already exist.
     */
    public static void create(final SQLiteDatabase db) {
        
    }

    /**
     * Create temporary triggers. Nothing is done if they
     * already exist.
     */
    public static void createTemp(final SQLiteDatabase db) {
        
    }

    
}