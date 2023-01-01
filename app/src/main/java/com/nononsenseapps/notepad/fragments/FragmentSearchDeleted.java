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

package com.nononsenseapps.notepad.fragments;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter.ViewBinder;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.DAO;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.ui.TitleNoteTextView;

import java.util.HashSet;

public class FragmentSearchDeleted extends FragmentSearch {

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setSelection();
	}

	public static FragmentSearchDeleted getInstance(final String initialQuery) {
		FragmentSearchDeleted f = new FragmentSearchDeleted();
		Bundle args = new Bundle();
		args.putString(QUERY, initialQuery);
		f.setArguments(args);
		return f;
	}

	private FragmentSearchDeleted() {}

	void setSelection() {
		mBinding.list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		mBinding.list.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			final HashSet<Long> selectedItems = new HashSet<>();

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectedItems.clear();
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				final MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.activity_deleted_context, menu);
				return true;
			}

			String[] getIdArray() {
				final String[] result = new String[selectedItems.size()];
				int i = 0;
				for (final long id : selectedItems) {
					result[i] = Long.toString(id);
					i++;
				}
				return result;
			}

			void deleteSelected(final ActionMode mode) {
				String whereCondition = Task.Columns._ID + " IN ("
						+ DAO.arrayToCommaString(getIdArray()) + ")";
				getActivity()
						.getContentResolver()
						.delete(Task.URI_DELETED_QUERY, whereCondition, null);
				selectedItems.clear();
				// mode.finish() touches the views, so it MUST run on the UI thread
				FragmentSearchDeleted.this.getActivity().runOnUiThread(mode::finish);
			}

			void restoreSelected(final ActionMode mode, final long listId) {
				for (final Long id : selectedItems) {
					final int pos = getPosOfId(id);
					if (pos > -1) {
						final Cursor c = (Cursor) mBinding.list.getItemAtPosition(pos);

						// restore task
						final Task t = new Task();
						t.dblist = listId;
						t.title = c.getString(1);
						t.note = c.getString(2);
						t.completed = c.isNull(3) ? null : c.getLong(3);
						t.due = c.isNull(4) ? null : c.getLong(4);
						t.save(getActivity());
					}
				}
				notifySuccess();
				deleteSelected(mode);
			}

			int getPosOfId(final long id) {
				int length = mBinding.list.getCount();
				int position;
				boolean found = false;
				for (position = 0; position < length; position++) {
					if (id == mBinding.list.getItemIdAtPosition(position)) {
						found = true;
						break;
					}
				}
				if (!found) {
					// Happens both if list is empty
					// and if id is -1
					position = -1;
				}
				return position;
			}

			/** Show a {@link Toast} in a thread-safe way */
			@UiThread
			void notifySuccess() {
				new Handler(Looper.getMainLooper())
						.post(() -> Toast.makeText(getActivity(), R.string.saved,
								Toast.LENGTH_SHORT).show());
			}

			@Override
			public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
				int itemId = item.getItemId();
				if (itemId == R.id.menu_restore) {
					DialogRestore d = DialogRestore.getInstance();
					d.setListener(listId -> {
						if (listId > 0) {
							restoreSelected(mode, listId);
						}
					});
					d.show(getFragmentManager(), "listselect");
					return true;
				} else if (itemId == R.id.menu_delete) {
					DialogDeleteTask.showDialog(getFragmentManager(), -1,
							() -> deleteSelected(mode));
					return true;
				}
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
												  boolean checked) {
				if (checked) {
					selectedItems.add(id);
				} else {
					selectedItems.remove(id);
				}
			}
		});
	}

	@Override
	protected Uri getSearchUri() {
		return Task.URI_DELETED_QUERY;
	}

	@Override
	protected String[] getFields() {
		return Task.Columns.DELETEFIELDS;
	}

	@Override
	protected String getSortOrder() {
		return Task.Columns.TRIG_DELETED + " DESC";
	}

	@Override
	protected OnItemClickListener getOnItemClickListener() {
		return (arg0, origin, pos, id) -> mBinding.list.setItemChecked(pos, true);
	}

	@Override
	protected SimpleCursorAdapter getAdapter() {
		return new SimpleCursorAdapter(getActivity(),
				R.layout.tasklist_item_rich,
				null,
				new String[] { Task.Columns.TITLE, Task.Columns.NOTE, Task.Columns.DUE,
						Task.Columns.COMPLETED, Task.Columns.TRIG_DELETED,
						Task.Columns.TRIG_DELETED },
				new int[] { android.R.id.text1, android.R.id.text1, R.id.date, R.id.checkbox,
						R.id.drag_handle, R.id.dragpadding },
				0);
	}

	@Override
	protected ViewBinder getViewBinder() {
		// Get the global list settings
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

		// Load preference for note height, or show 3 lines if it was not set
		final int rowCount = prefs.getInt(getString(R.string.key_pref_item_max_height), 3);

		return (view, c, colIndex) -> {
			switch (colIndex) {
				// the code here decides how the notes on the archive look like.
				// Each number in the "case" instruction matches the order in Task.Columns.Fields,
				// in fact c.getColumnNames() returns the fields of the note in the database:
				// ["_id", "title", "note", "completed", "due", "dblist", "deletedtime" ]
				case 1:
					// Title, in column "title"
					String noteTitle = c.getString(colIndex);

					// Set height of text for non-headers
					if (rowCount == 1) {
						((TitleNoteTextView) view).setSingleLine(true);
					} else {
						((TitleNoteTextView) view).setSingleLine(false);
						((TitleNoteTextView) view).setMaxLines(rowCount);
					}

					// Change color based on complete status (column 3 is the "completed" status)
					((TitleNoteTextView) view).useSecondaryColor(!c.isNull(3));

					// TODO yes, completed note appear in dark gray in the archive view. I didn't
					//  know this. Make a TapTargetView to explain this to users. It could target
					//  the search icon, it doesn't matter. Just put it in onResume() or somewhere
					//  reasonable

					((TitleNoteTextView) view).setTextTitle(noteTitle);
					return true;
				case 2:
					// Note content, in column "note". Let's show it even in the "Archive" view,
					// so that the user can distinguish 2 deleted notes with the same title
					String noteContent = c.getString(colIndex);
					((TitleNoteTextView) view).setTextRest(noteContent);
					return true;
				default:
					// we won't show any other field of the note. Maybe it would be nice to show
					// the due date ? But it's an archived note, so I guess the user does not care?
					view.setVisibility(View.GONE);
					return true;
			}
		};
	}
}
