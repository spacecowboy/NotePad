package com.nononsenseapps.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ericharlow.DragNDrop.DropListener;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotePad.Notes;
import com.nononsenseapps.notepad.NotesEditorFragment;
import com.nononsenseapps.notepad.NotesListFragment;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.util.TimeHelper;

/**
 * This class handles drag n drop actions for the sectioned adapter
 */
public class SectionDropListener implements DropListener {

	private final SectionAdapter adapter;
	private final Context context;

	public SectionDropListener(Context context, SectionAdapter adapter) {
		this.adapter = adapter;
		this.context = context;
		if (adapter == null || context == null) {
			throw new NullPointerException("The values must NOT be null!");
		}
	}

	@Override
	public void onDrop(final int from, final int to) {
		final ContentValues values = new ContentValues();

		final long noteId = adapter.getItemId(from);

		toList(from, to, values);

		// Position is a special case as it can update a whole bunch of notes
		if (toPosition(from, to, values)) {

		} else {
			toDate(from, to, values);

			// Update note with new information
			updateNote(noteId, values);
		}
	}

	/**
	 * Inserts appropriate information about the target list. Can be the same as
	 * the current. Will always return the current list.
	 */
	private void toList(final int from, final int to, ContentValues values) {
		if (NotesListFragment.SECTION_STATE_LISTS.equals(adapter.getState())) {
			// Get new list
			final Long toList = adapter.getSectionIdOfPos(to);
			if (toList != null)
				values.put(Notes.COLUMN_NAME_LIST, toList);
		}
	}

	/**
	 * This structure won't work in this case
	 * 
	 * Since you are always moving the "parent", it is quite easy to get the
	 * final position. the parent sets its own parent to equal the target drop.
	 * Position is calculated from the drop.
	 */
	private boolean toPosition(final int from, final int to,
			ContentValues values) {
		// Get current sort order since that is valid even if the adapter
		// is in list state
		final String sortChoice = PreferenceManager
				.getDefaultSharedPreferences(context).getString(
						MainPrefs.KEY_SORT_TYPE, "");
		if (MainPrefs.POSSUBSORT.equals(sortChoice)) {

			return true;
		} else
			return false;
	}

	private void toDate(final int from, final int to, ContentValues values) {
		if (NotesListFragment.SECTION_STATE_DATE.equals(adapter.getState())) {
			Log.d("dragdate", "toDate");
			// Get the targeted date. This will be a localized string like
			// "Tomorrow"
			final String targetDate = adapter.getSection(to);

			// A special case is if we are moving to the completed section
			if (context.getString(R.string.date_header_completed).equals(
					targetDate)) {
				Log.d("dragdate", "just completing note");
				// Just set completed, don't touch the date
				values.put(Notes.COLUMN_NAME_GTASKS_STATUS,
						context.getString(R.string.gtask_status_completed));
			} else {
				String newDate = "";
				// If the note was dropped on another note, use the same date
				if (SectionAdapter.TYPE_ITEM == adapter.getItemViewType(to)) {
					final long noteId = adapter.getItemId(to);
					Log.d("dragdate", "using existing note date");
					if (noteId > -1) {
						Cursor c = context.getContentResolver().query(
								NotesEditorFragment.getUriFrom(noteId),
								new String[] { Notes.COLUMN_NAME_DUE_DATE },
								null, null, null);

						if (c != null) {
							if (c.moveToFirst())
								newDate = c
										.getString(c
												.getColumnIndex(Notes.COLUMN_NAME_DUE_DATE));
							Log.d("dragdate", "which was: " + newDate);

							c.close();
						}
					}
				}
				// Else do the best we can with the header
				else {
					if (context.getString(R.string.date_header_none).equals(
							targetDate)) {
						newDate = "";
					} else if (context.getString(R.string.date_header_7days)
							.equals(targetDate)) {
						newDate = TimeHelper.get3339DaysFromToday(3);
					} else if (context.getString(R.string.date_header_future)
							.equals(targetDate)) {
						newDate = TimeHelper.get3339DaysFromToday(8);
					} else if (context.getString(R.string.date_header_overdue)
							.equals(targetDate)) {
						newDate = TimeHelper.get3339DaysFromToday(-1);
					} else if (context.getString(R.string.date_header_today)
							.equals(targetDate)) {
						newDate = TimeHelper.get3339DaysFromToday(0);
					} else if (context.getString(R.string.date_header_tomorrow)
							.equals(targetDate)) {
						newDate = TimeHelper.get3339DaysFromToday(1);
					}
				}

				Log.d("dragdate", "new date: " + newDate);
				values.put(Notes.COLUMN_NAME_DUE_DATE, newDate);
				values.put(Notes.COLUMN_NAME_GTASKS_STATUS,
						context.getString(R.string.gtask_status_uncompleted));
			}
		}
	}

	/**
	 * Returns -1 in case shit hit the fan
	 */
	private long getFromListId(final long noteId) {
		long listId = -1;
		if (noteId > -1) {
			Cursor c = context.getContentResolver().query(
					NotesEditorFragment.getUriFrom(noteId),
					new String[] { Notes.COLUMN_NAME_LIST }, null, null, null);

			if (c != null) {
				if (c.moveToFirst())
					listId = c
							.getLong(c.getColumnIndex(Notes.COLUMN_NAME_LIST));

				c.close();
			}
		}

		return listId;
	}

	private void updateNote(final long noteId, ContentValues values) {
		final Uri uri = NotesEditorFragment.getUriFrom(noteId);
		if (uri != null && values != null) {
			context.getContentResolver().update(uri, values, null, null);
		}
	}

}
