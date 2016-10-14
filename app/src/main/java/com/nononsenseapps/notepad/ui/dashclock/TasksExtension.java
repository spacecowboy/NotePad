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

package com.nononsenseapps.notepad.ui.dashclock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class TasksExtension extends DashClockExtension {

	// final static Uri VISIBLE_NOTES_URI = Uri
	// .parse("content://com.nononsenseapps.NotePad/visiblenotes");
	// final static Uri VISIBLE_LISTS_URI = Uri
	// .parse("content://com.nononsenseapps.NotePad/visiblelists");
	public static final String DUEDATE_SORT_TYPE = new StringBuilder(
			"CASE WHEN ").append(Task.Columns.DUE).append(" IS NULL OR ")
			.append(Task.Columns.DUE).append(" IS '' THEN 1 ELSE 0 END, ")
			.append(Task.Columns.DUE).toString();
	private static final String WHERE_LIST_IS_AND = Task.Columns.DBLIST
			+ " IS ? AND ";
	private static final String WHERE_DATE_IS = new StringBuilder(
			Task.Columns.COMPLETED).append(" IS NULL AND ")
			.append(Task.Columns.DUE).append(" IS NOT NULL AND ")
			.append(Task.Columns.DUE).append(" <= ? ").toString();
	private static final String WHERE_ALL_NOTDONE = Task.Columns.COMPLETED
			+ " IS NULL";

	private String[] toA(final String... args) {
		return args;
	}

	private String[] appendTo(final String[] array, final String... items) {
		final String[] result = new String[array.length + items.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = array[i];
		}
		for (int i = 0; i < items.length; i++) {
			result[array.length + i] = items[i];
		}
		return result;
	}

	final static String[] NOTEFIELDS = new String[] { "_id", "title", "note",
			"duedate" };

	@Override
	protected void onInitialize(boolean isReconnect) {
		super.onInitialize(isReconnect);
		// Watch the notes URI
		addWatchContentUris(toA(TaskList.URI.toString(), Task.URI.toString()));
	}

	@Override
	protected void onUpdateData(int reason) {

		// Get preferences
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		final long listId = Long.parseLong(prefs
				.getString("list_spinner", "-1"));

		final boolean showOverdue = prefs.getBoolean("show_overdue", true);

		final String upperLimit = prefs.getString("list_due_upper_limit",
				getString(R.string.dashclock_pref_today));

		final boolean showSingle = prefs.getBoolean("show_single_only", false);

		final boolean showHeader = prefs.getBoolean("show_header", true);

		final ArrayList<Task> notes = getNotesFromDB(listId, upperLimit);

		// Show overdue?
		if (!showOverdue) {
			removeOverdue(notes);
		}

		if (showSingle && notes.size() > 1) {
			final Task first = notes.get(0);
			notes.clear();
			notes.add(first);
		}

		if (notes.isEmpty()) {
			publishUpdate(null);
		}
		else {

			final String short_header = getString(
					R.string.dashclock_tasks_count, notes.size());

			final String long_header;

			// If no header is to be displayed, show title of first
			if (showHeader) {
				long_header = getHeader(listId);
			}
			else {
				long_header = notes.get(0).title;
			}

			final Intent noteIntent = new Intent();
			if (notes.size() > 1) {
				noteIntent
						.setAction(Intent.ACTION_VIEW)
						.setData(
								TaskList.getUri(notes.get(0).dblist.longValue()))
						.putExtra(Task.TABLE_NAME, notes.get(0)._id);
			}
			else {
				noteIntent
						.setAction(Intent.ACTION_EDIT)
						.setData(Task.getUri(notes.get(0)._id))
						.putExtra(Task.Columns.DBLIST,
								notes.get(0).dblist.longValue());
			}

			// Publish the extension data update.
			publishUpdate(new ExtensionData().visible(true)
					.icon(R.drawable.ic_stat_notification_edit)
					.status(short_header).expandedTitle(long_header)
					.expandedBody(getBody(notes, showHeader))
					.clickIntent(noteIntent));
		}

	}

	@SuppressWarnings("unchecked")
	private void removeOverdue(final ArrayList<Task> notes) {
		for (Task note : (ArrayList<Task>) notes.clone()) {
			if (note.due != null
					&& note.due < Calendar.getInstance().getTimeInMillis())
				notes.remove(note);
		}
	}

	private String getBody(final ArrayList<Task> notes, final boolean showHeader) {
		String result = "";
		if (notes.size() == 1) {
			if (showHeader) {
				// Skip title if no header as the title is the header
				result += notes.get(0).title;
				result += "\n";
			}
			result += notes.get(0).note;
		}
		else {
			boolean first = true;
			boolean skippable = true;
			for (Task note : notes) {
				if (!showHeader && skippable) {
					// Skip first
					skippable = false;
					continue;
				}
				if (!first) result += "\n";
				result += note.title;
				first = false;
			}
		}
		return result;
	}

	/**
	 * Return a list of notes respecting the constraints set in preferences.
	 */
	private ArrayList<Task> getNotesFromDB(final long list,
			final String upperLimit) {
		// WHERE_LIST_IS, toA(list)
		String where = "";
		String[] whereArgs = new String[0];
		if (list > -1) {
			where += WHERE_LIST_IS_AND;
			whereArgs = appendTo(whereArgs, Long.toString(list));
		}

		where += getUpperQueryLimitWhere(upperLimit);
		whereArgs = getUpperQueryLimitWhereArgs(whereArgs, upperLimit);

		final Cursor cursor = getContentResolver().query(Task.URI,
				Task.Columns.FIELDS, where, whereArgs, DUEDATE_SORT_TYPE);

		final ArrayList<Task> result = new ArrayList<Task>();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				result.add(new Task(cursor));
			}
			cursor.close();
		}

		return result;
	}

	/**
	 * Returns the list name, or "Tasks" if all lists are to be shown.
	 */
	private String getHeader(final long list) {
		String header = getString(R.string.dashclock_tasks);

		if (list > -1) {
			final Cursor cursor = getContentResolver().query(TaskList.URI,
					TaskList.Columns.FIELDS, TaskList.Columns._ID + " IS ?",
					new String[] { Long.toString(list) }, null);
			if (cursor != null) {
				if (!cursor.isClosed() && !cursor.isAfterLast()) {
					if (cursor.moveToNext()) {
						header = cursor.getString(1);
					}
				}

				cursor.close();
			}
		}

		return header;
	}

	private String getUpperQueryLimitWhere(final String upperLimit) {
		String where = WHERE_DATE_IS;
		if (getString(R.string.dashclock_pref_none).equals(upperLimit)) {
			where = WHERE_ALL_NOTDONE;
		}
		return where;
	}

	private String[] getUpperQueryLimitWhereArgs(final String[] whereArgs,
			final String upperLimit) {
		final GregorianCalendar gc = new GregorianCalendar();
		gc.set(GregorianCalendar.HOUR_OF_DAY, 23);
		gc.set(GregorianCalendar.MINUTE, 59);
		final long base = gc.getTimeInMillis();
		final long day = 24 * 60 * 60 * 1000;
		if (getString(R.string.dashclock_pref_today).equals(upperLimit)) {
			return appendTo(whereArgs, Long.toString(gc.getTimeInMillis()));
		}
		else if (getString(R.string.dashclock_pref_tomorrow).equals(upperLimit)) {
			gc.setTimeInMillis(base + 1 * day);
			return appendTo(whereArgs, Long.toString(gc.getTimeInMillis()));
		}
		else if (getString(R.string.dashclock_pref_next7).equals(upperLimit)) {
			gc.setTimeInMillis(base + 7 * day);
			return appendTo(whereArgs, Long.toString(gc.getTimeInMillis()));
		}
		else {
			return whereArgs;
		}
	}

}
