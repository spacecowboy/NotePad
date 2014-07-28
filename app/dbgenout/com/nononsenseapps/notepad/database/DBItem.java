package com.nononsenseapps.notepad.database;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public abstract class DBItem {
    public static final String COL_ID = "_id";

    public DBItem() {}

    public DBItem(final Cursor cursor) {}

    public abstract ContentValues getContent();

    public abstract String getTableName();

    public abstract long getId();

    public abstract void setId(final long id);

    public abstract String[] getFields();

    public Uri getUri() {
        return Uri.withAppendedPath(getBaseUri(), Long.toString(getId()));
    }

    public Uri getBaseUri() {
        return Uri.withAppendedPath(
            Uri.parse(ItemProvider.SCHEME
                      + ItemProvider.AUTHORITY), getTableName());
    }

    public void notifyProvider(final Context context) {
        try {
            context.getContentResolver().notifyChange(getUri(), null, false);
        }
        catch (UnsupportedOperationException e) {
           // Catch this for test suite. Mock provider cant notify
        }
    }

}
