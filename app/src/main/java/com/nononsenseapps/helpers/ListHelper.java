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

package com.nononsenseapps.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.TaskListFragment;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;

/**
 * Simple utility class to hold some general functions for lists
 */
public final class ListHelper {

	/**
	 * If temp list is > 0, returns it if it exists. Else, checks if a default list is set
	 * then returns that. If none set, then returns first (alphabetical) list
	 * Returns #{TaskListFragment.LIST_ID_ALL} if no lists in database.
	 */
	public static long getAViewList(final Context context, final long tempList) {
		long returnList = tempList;

		// TODO useless, you already have getAShowList() in this class

		if (returnList == TaskListFragment.LIST_ID_ALL) {
			// This is fine
			return returnList;
		}
		// Otherwise, try and get a real list
		returnList = getARealList(context, returnList);

		if (returnList < 1) {
			// If nothing was found, return all of them in this case
			returnList = TaskListFragment.LIST_ID_ALL;
		}


		return returnList;
	}

	/**
	 * Guarantees default list is valid.
	 *
	 * @return If "tempList" > 0, returns it if it exists.
	 * Else, checks if a default list is set then returns that.
	 * If none set, then returns first (alphabetical) list.
	 * If no lists exist in the database, returns -1.
	 */
	public static long getARealList(final Context context, final long tempList) {
		long returnList = tempList;

		if (returnList < 1 && returnList != TaskListFragment.LIST_ID_ALL) {
			assert returnList == -1;  // ... I think ??

			// Then check if a default list is specified
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String defListPrefName = context.getString(R.string.pref_defaultlist);
			returnList = Long.parseLong(prefs.getString(defListPrefName, "-1"));
		}

		if (returnList > 0) {
			// See if it exists
			final Cursor c = context
					.getContentResolver()
					.query(TaskList.URI,
							TaskList.Columns.FIELDS,
							TaskList.Columns._ID + " IS ?",
							new String[] { Long.toString(returnList) },
							null);
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
			assert returnList == -1; // ... I think ??

			// Fetch a valid list from database if previous attempts are invalid
			String orderingSql = context
					.getResources()
					.getString(R.string.const_as_alphabetic, TaskList.Columns.TITLE);
			final Cursor c = context
					.getContentResolver()
					.query(TaskList.URI, TaskList.Columns.FIELDS, null,
							null, orderingSql);
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
	 * For {@link TaskListViewPagerFragment}
	 *
	 * @param tempList from ActivityMainHelper.getListId()
	 * @return and ID that might belong to a "meta list"
	 */
	public static long getAShowList(final Context context, final long tempList) {
		long returnList = tempList;

		if (returnList == -1) {
			// Then check if a default list is specified
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String defListId = prefs.getString(
					context.getString(R.string.pref_defaultlist), "-1");
			returnList = prefs.getLong(
					context.getString(R.string.pref_defaultstartlist), Long.parseLong(defListId));
		}

		if (returnList == -1) {
			returnList = getARealList(context, returnList);
		}

		// If nothing was found, show ALL
		if (returnList == -1) {
			returnList = TaskListFragment.LIST_ID_ALL;
		}

		return returnList;
	}
}
