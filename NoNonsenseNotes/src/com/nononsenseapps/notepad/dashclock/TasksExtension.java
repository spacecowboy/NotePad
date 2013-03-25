package com.nononsenseapps.notepad.dashclock;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.util.TimeHelper;

public class TasksExtension extends DashClockExtension {

	//final static Uri VISIBLE_NOTES_URI = Uri
	//		.parse("content://com.nononsenseapps.NotePad/visiblenotes");
	//final static Uri VISIBLE_LISTS_URI = Uri
	//		.parse("content://com.nononsenseapps.NotePad/visiblelists");
	public static final String DUEDATE_SORT_TYPE = "CASE WHEN " + "duedate"
			+ " IS NULL OR " + "duedate" + " IS '' THEN 1 ELSE 0 END, "
			+ "duedate";
	private static final String WHERE_LIST_IS_AND = "list IS ? AND ";
	private static final String WHERE_DATE_IS = "gtaskstatus IS 'needsAction' AND "
			+ "duedate IS NOT NULL AND "
			+ "duedate IS NOT '' AND "
			+ "date(duedate) <= ?";
	private static final String WHERE_ALL_NOTDONE = "gtaskstatus IS 'needsAction'";

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
		addWatchContentUris(toA(NotePad.Notes.CONTENT_VISIBLE_URI.toString()));
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

		final ArrayList<Note> notes = getNotesFromDB(listId, upperLimit);

		// Show overdue?
		if (!showOverdue) {
			removeOverdue(notes);
		}

		if (showSingle && notes.size() > 1) {
			final Note first = notes.get(0);
			notes.clear();
			notes.add(first);
		}

		if (notes.isEmpty()) {
			publishUpdate(null);
		} else {

			final String short_header = getString(R.string.dashclock_tasks_count,
					notes.size());

			final String long_header;

			// If no header is to be displayed, show title of first
			if (showHeader) {
				long_header = getHeader(listId);
			} else {
				long_header = notes.get(0).title;
			}

			final Intent noteIntent = new Intent();
			if (notes.size() > 1) {
				noteIntent.setAction(Intent.ACTION_VIEW).setData(
						listId >= 0 ? Uri.withAppendedPath(NotePad.Lists.CONTENT_VISIBLE_URI,
								Long.toString(listId)) : NotePad.Lists.CONTENT_VISIBLE_URI);
			} else {
				noteIntent
						.setAction(Intent.ACTION_EDIT)
						.setData(
								Uri.withAppendedPath(NotePad.Notes.CONTENT_VISIBLE_URI,
										Long.toString(notes.get(0).id)));
				if (listId >= 0) {
					noteIntent.putExtra("list", listId);
				}
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
	private void removeOverdue(final ArrayList<Note> notes) {
		for (Note note : (ArrayList<Note>) notes.clone()) {
			if (note.duedate != null
					&& !note.duedate.isEmpty()
					&& TimeHelper.dateBefore(note.duedate,
							TimeHelper.dateToday()))
				notes.remove(note);
		}
	}

	private String getBody(final ArrayList<Note> notes, final boolean showHeader) {
		String result = "";
		if (notes.size() == 1) {
			if (showHeader) {
				// Skip title if no header as the title is the header
				result += notes.get(0).title;
				result += "\n";
			}
			result += notes.get(0).note;
		} else {
			boolean first = true;
			boolean skippable = true;
			for (Note note : notes) {
				if (!showHeader && skippable) {
					// Skip first
					skippable = false;
					continue;
				}
				if (!first)
					result += "\n";
				result += note.title;
				first = false;
			}
		}
		return result;
	}

	/**
	 * Return a list of notes respecting the constraints set in preferences.
	 */
	private ArrayList<Note> getNotesFromDB(final long list,
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

		final Cursor cursor = getContentResolver().query(NotePad.Notes.CONTENT_VISIBLE_URI,
				NOTEFIELDS, where, whereArgs, DUEDATE_SORT_TYPE);

		final ArrayList<Note> result = new ArrayList<Note>();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				result.add(new Note(cursor));
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
			final Cursor cursor = getContentResolver().query(NotePad.Lists.CONTENT_VISIBLE_URI,
					new String[] { BaseColumns._ID, "title" },
					"" + BaseColumns._ID + " IS ?",
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
		if (getString(R.string.dashclock_pref_today).equals(upperLimit)) {
			return appendTo(whereArgs, TimeHelper.dateToday());
		} else if (getString(R.string.dashclock_pref_tomorrow).equals(upperLimit)) {
			return appendTo(whereArgs, TimeHelper.dateTomorrow());
		} else if (getString(R.string.dashclock_pref_next7).equals(upperLimit)) {
			return appendTo(whereArgs, TimeHelper.dateEightDay());
		} else {
			return whereArgs;
		}
	}

	private static class Note {
		public long id = -1;
		public String title = "";
		public String note = "";
		public String duedate = "";

		public Note(final Cursor c) {
			id = c.getLong(0);
			title = c.getString(1);
			note = c.getString(2);
			duedate = c.getString(3);
		}
	}

}
