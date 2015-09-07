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

/**
 * This class defines some of the contract which a provider must adhere to.
 */
public class ProviderContract {

    // This is the action which a provider must provide an intent-filter for.
    public static final String ACTION_PROVIDER = "com.nononsenseapps.notepad.PROVIDER";

    /*
    * These are the columns a provider is expected to supply.
     */
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
}
