package com.nononsenseapps.notepad.database;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

/**
 * Represents taskview in the database.
 *
 */
public class taskviewItem extends DBItem {
    public static final String TABLE_NAME = "taskview";

    public static Uri URI() {
        return Uri.withAppendedPath(
            Uri.parse(ItemProvider.SCHEME
                      + ItemProvider.AUTHORITY), TABLE_NAME);
    }

    // Column names
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_FILTERTEXT = "filterText";
    public static final String COL_FILTERDUEEARLIEST = "filterDueEarliest";
    public static final String COL_FILTERDUELATEST = "filterDueLatest";
    public static final String COL_FILTERCOMPLETED = "filterCompleted";

    // For database projection so order is consistent
    public static final String[] FIELDS = { COL_ID, COL_TITLE, COL_FILTERTEXT, COL_FILTERDUEEARLIEST, COL_FILTERDUELATEST, COL_FILTERCOMPLETED };

    public Long _id = -1;
    public String title = "";
    public String filterText = "";
    public Long filterDueEarliest;
    public Long filterDueLatest;
    public Long filterCompleted;

    public static final int BASEURICODE = 0x2d72bbd;
    public static final int BASEITEMCODE = 0x824cd6d;

    public static void addMatcherUris(UriMatcher sURIMatcher) {
        sURIMatcher.addURI(ItemProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
        sURIMatcher.addURI(ItemProvider.AUTHORITY, TABLE_NAME + "/#", BASEITEMCODE);
    }

    public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.com.nononsenseapps.notepad.database." + TABLE_NAME;
    public static final String TYPE_ITEM = "vnd.android.cursor.item/vnd.com.nononsenseapps.notepad.database." + TABLE_NAME;

    public taskviewItem() {
        super();
    }

    public taskviewItem(final Cursor cursor) {
        super();
        // Projection expected to match FIELDS array
        this._id = cursor.getLong(0);
        this.title = cursor.getString(1);
        this.filterText = cursor.getString(2);
        this.filterDueEarliest = cursor.isNull(3) ? null : cursor.getLong(3);
        this.filterDueLatest = cursor.isNull(4) ? null : cursor.getLong(4);
        this.filterCompleted = cursor.isNull(5) ? null : cursor.getLong(5);
    }

    public ContentValues getContent() {
        ContentValues values = new ContentValues();
        
        values.put(COL_TITLE, title);
        values.put(COL_FILTERTEXT, filterText);
        if (filterDueEarliest != null) {
            values.put(COL_FILTERDUEEARLIEST, filterDueEarliest);
        } else {
            values.putNull(COL_FILTERDUEEARLIEST);
        }
        if (filterDueLatest != null) {
            values.put(COL_FILTERDUELATEST, filterDueLatest);
        } else {
            values.putNull(COL_FILTERDUELATEST);
        }
        if (filterCompleted != null) {
            values.put(COL_FILTERCOMPLETED, filterCompleted);
        } else {
            values.putNull(COL_FILTERCOMPLETED);
        }

        return values;
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    public String[] getFields() {
        return FIELDS;
    }

    public long getId() {
        return _id;
    }

    public void setId(final long id) {
        _id = id;
    }

    public static final String CREATE_TABLE =
"CREATE TABLE taskview"
+"  (_id INTEGER PRIMARY KEY,"
+"  title TEXT NOT NULL DEFAULT '',"
+"  filterText TEXT NOT NULL DEFAULT '',"
+"  filterDueEarliest INTEGER,"
+"  filterDueLatest INTEGER,"
+"  filterCompleted INTEGER"
+""
+"  )";
}
