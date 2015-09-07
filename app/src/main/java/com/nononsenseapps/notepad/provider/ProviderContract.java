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

package com.nononsenseapps.notepad.provider;

import android.support.annotation.IntDef;
import android.support.annotation.StringDef;

/**
 * This class defines some of the contract which a provider must adhere to.
 */
public class ProviderContract {

    // This is the action which a provider must provide an intent-filter for.
    public static final String ACTION_PROVIDER = "com.nononsenseapps.notepad.PROVIDER";

    // These are the columns a provider is expected to supply.
    // Non-null
    public static final String COLUMN_ID = "id";
    // Non-null
    public static final String COLUMN_TYPEMASK = "typemask";
    // Non-null
    public static final String COLUMN_TITLE = "title";
    // Nullable
    public static final String COLUMN_DESCRIPTION = "description";
    // Nullable
    public static final String COLUMN_STATUS = "status";
    // Nullable
    public static final String COLUMN_DUE = "due";
    // Projection used by the main list
    public static final String[] sMainListProjection = new String[]{COLUMN_ID,
            COLUMN_TYPEMASK, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_STATUS, COLUMN_DUE};

    @StringDef({
            COLUMN_ID,
            COLUMN_TYPEMASK,
            COLUMN_TITLE,
            COLUMN_DESCRIPTION,
            COLUMN_STATUS,
            COLUMN_DUE
    })
    public @interface ColumnName {
    }

    // Bitmasks for use with typemask
    // Item has data (id, title, etc).
    public static final long TYPE_DATA = 0x1;
    // Item contains sub-items. Note that either folder or data (or both) must be 1.
    public static final long TYPE_FOLDER = 0x10;
    // Item has a status associated with it (to-do, done, waiting, etc).
    public static final long TYPE_STATUS = 0x100;
    // Item supports due date.
    public static final long TYPE_DUE_DATE = 0x1000;
    // Item supports due time, in addition to due date.
    public static final long TYPE_DUE_TIME = 0x10000;
    // Item supports reminder date & time.
    public static final long TYPE_REMINDER = 0x100000;
    // Item supports description (otherwise only title).
    public static final long TYPE_DESCRIPTION = 0x1000000;

    @IntDef({
            TYPE_DATA,
            TYPE_FOLDER,
            TYPE_STATUS,
            TYPE_DUE_DATE,
            TYPE_DUE_TIME,
            TYPE_REMINDER,
            TYPE_DESCRIPTION
    })
    public @interface TypeMask {
    }

    /**
     * Convenience method to OR together a bunch of bitmasks.
     * @param bitvalues to combine
     * @return a bitmask
     */
    public static long getTypeMask(@TypeMask long... bitvalues) {
        long bitmask = 0x0;
        for (long bitvalue: bitvalues) {
            bitmask |= bitvalue;
        }
        return bitmask;
    }

    /**
     * Convenience method to check if a bitmask contains a certain bitvalue.
     * @param bitmask to check
     * @param type typemask bit to check
     * @return true if bitmask has the bit set to 1, else false
     */
    public static boolean isType(long bitmask, @TypeMask long type) {
        return 0 < (bitmask & type);
    }
}
