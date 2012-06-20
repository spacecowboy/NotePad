package com.nononsenseapps.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.ericharlow.DragNDrop.DropListener;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotePad.Notes;
import com.nononsenseapps.notepad.NotesEditorFragment;
import com.nononsenseapps.notepad.NotesListFragment;
import com.nononsenseapps.notepad.prefs.MainPrefs;

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

			// Create a deleted duplicate
			createDuplicateDeleted(noteId);

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

	private void createDuplicateDeleted(final long noteId) {
		final long listId = getFromListId(noteId);
		if (noteId > -1 && listId > -1) {
			// Insert a new deleted entry
			final ContentValues values = new ContentValues();
			values.put(NotePad.Notes.COLUMN_NAME_DELETED, 1);
			values.put(NotePad.Notes.COLUMN_NAME_LOCALHIDDEN, 1);
			values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);
			Uri cUri = context.getContentResolver().insert(
					NotePad.Notes.CONTENT_URI, values);

			// Switch their local ids to the new deleted one
			if (cUri != null) {
				Long cId = NotesEditorFragment.getIdFromUri(cUri);

				final ContentValues gvalues = new ContentValues();
				gvalues.put(NotePad.GTasks.COLUMN_NAME_DB_ID, cId);

				context.getContentResolver().update(NotePad.GTasks.CONTENT_URI,
						gvalues, NotePad.GTasks.COLUMN_NAME_DB_ID + " IS ?",
						new String[] { Long.toString(noteId) });
			}
		}
	}

	private void updateNote(final long noteId, ContentValues values) {
		final Uri uri = NotesEditorFragment.getUriFrom(noteId);
		if (uri != null && values != null) {
			context.getContentResolver().update(uri, values, null, null);
		}
	}

}
