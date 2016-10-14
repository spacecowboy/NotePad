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

package com.nononsenseapps.notepad.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.local.sql.LegacyDBHelper;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.ui.editor.TaskDetailFragment;
import com.nononsenseapps.notepad.ui.list.TaskListFragment;

/**
 * Simple utility class to hold some general functions.
 */
public class ListHelper {

    /**
     * If temp list is > 0, returns it if it exists. Else, checks if a default list is set
     * then returns that. If none set, then returns first (alphabetical) list
     * Returns #{TaskListFragment.LIST_ID_ALL} if no lists in database.
     */
    public static long getAViewList(final Context context, final long tempList) {
        long returnList = tempList;
        if (returnList == TaskListFragment.LIST_ID_ALL) {
            // This is fine
            return returnList;
        }
        // Otherwise, try and get a real list
        returnList = getARealList(context, returnList);

        if (returnList < 1) {
            // Return all in this case
            returnList = TaskListFragment.LIST_ID_ALL;
        }

        return returnList;
    }

    /**
     * If temp list is > 0, returns it if it exists. Else, checks if a default list is set
     * then returns that. If none set, then returns first (alphabetical) list.
     * If no lists exist in the database, returns -1.
     */
    public static long getARealList(final Context context, final long tempList) {
        long returnList = tempList;

        if (returnList < 1 && returnList != TaskListFragment.LIST_ID_ALL) {
            // Then check if a default list is specified
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            returnList = Long.parseLong(prefs.getString(context.getString(R.string
                    .pref_defaultlist), "-1"));
        }

        if (returnList > 0) {
            // See if it exists
            final Cursor c = context.getContentResolver().query(TaskList.URI, TaskList.Columns
                    .FIELDS, TaskList.Columns._ID + " IS ?", new String[]{Long.toString
                    (returnList)}, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    returnList = c.getLong(0);
                } else {
                    returnList = -1;
                }
                c.close();
            }
        }

        if (returnList < 1) {
            // Fetch a valid list from database if previous attempts are invalid
            final Cursor c = context.getContentResolver().query(TaskList.URI, TaskList.Columns
                    .FIELDS, null, null, context.getResources().getString(R.string
                    .const_as_alphabetic, TaskList.Columns.TITLE));
            if (c != null) {
                if (c.moveToFirst()) {
                    returnList = c.getLong(0);
                }
                c.close();
            }
        }

        return returnList;
    }

    /**
     * Returns a list id from an intent if it contains one, either as part of
     * its URI or as an extra
     * <p/>
     * Returns -1 if no id was contained, this includes insert actions
     */
    public static long getListId(final Intent intent) {
        long retval = -1;
        if (intent != null &&
                intent.getData() != null &&
                (Intent.ACTION_EDIT.equals(intent.getAction()) ||
                        Intent.ACTION_VIEW.equals(intent.getAction()) ||
                        Intent.ACTION_INSERT.equals(intent.getAction()))) {
            if ((intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Lists
                    .PATH_VISIBLE_LISTS) ||
                    intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Lists
                            .PATH_LISTS) ||
                    intent.getData().getPath().startsWith(TaskList.URI.getPath()))) {
                try {
                    retval = Long.parseLong(intent.getData().getLastPathSegment());
                } catch (NumberFormatException e) {
                    retval = -1;
                }
            } else if (-1 != intent.getLongExtra(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_LIST,
                    -1)) {
                retval = intent.getLongExtra(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_LIST, -1);
            } else if (-1 != intent.getLongExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, -1)) {
                retval = intent.getLongExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, -1);
            } else if (-1 != intent.getLongExtra(Task.Columns.DBLIST, -1)) {
                retval = intent.getLongExtra(Task.Columns.DBLIST, -1);
            }
        }
        return retval;
    }
}
