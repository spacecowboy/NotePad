package com.nononsenseapps.notepad;

import java.util.HashSet;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.nononsenseapps.notepad.database.DAO;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.utils.views.TitleNoteTextView;

@EActivity(R.layout.activity_search)
public class ActivitySearchDeleted extends ActivitySearch {

	@AfterViews
	void setSelection() {
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		list.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			HashSet<Long> selectedItems = new HashSet<Long>();

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
				final MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.activity_deleted_context, menu);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.menu_restore:
					// TODO
					// Restore items, ask what list to restore them to
					// Finish activity
					return true;
				case R.id.menu_delete:
					// TODO dialog to verify?
					getContentResolver().delete(
							Task.URI_DELETED_QUERY,
							Task.Columns._ID
									+ " IN ("
									+ DAO.arrayToCommaString(selectedItems
											.toArray(new String[] {})) + ")", null);
					selectedItems.clear();
					mode.finish();
					return true;
				}
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				if (checked) {
					selectedItems.add(id);
				}
				else {
					selectedItems.remove(id);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	Uri getSearchUri() {
		return Task.URI_DELETED_QUERY;
	}

	@Override
	String[] getFields() {
		return Task.Columns.DELETEFIELDS;
	}

	@Override
	String getSortOrder() {
		return Task.Columns.TRIG_DELETED + " DESC";
	}

	@Override
	OnItemClickListener getOnItemClickListener() {
		return new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View origin, int pos,
					long id) {
				list.setItemChecked(pos, true);
			}
		};
	}

	@Override
	SimpleCursorAdapter getAdapter() {
		return new SimpleCursorAdapter(this, R.layout.tasklist_item_rich, null,
				new String[] { Task.Columns.TITLE, Task.Columns.NOTE,
						Task.Columns.DUE, Task.Columns.COMPLETED,
						Task.Columns.TRIG_DELETED, Task.Columns.TRIG_DELETED },
				new int[] { android.R.id.text1, android.R.id.text1, R.id.date,
						R.id.checkbox, R.id.drag_handle, R.id.dragpadding }, 0);
	}

	@Override
	ViewBinder getViewBinder() {
		// Get the global list settings
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Load pref for item height
		final int rowCount = prefs.getInt(
				getString(R.string.key_pref_item_max_height), 3);

		return new ViewBinder() {
			String sTemp = "";

			@Override
			public boolean setViewValue(View view, Cursor c, int colIndex) {
				switch (colIndex) {
				// Matches order in Task.Columns.Fields
				case 1:
					// Title
					sTemp = c.getString(colIndex);

					// Set height of text for non-headers
					if (rowCount == 1) {
						((TitleNoteTextView) view).setSingleLine(true);
					}
					else {
						((TitleNoteTextView) view).setSingleLine(false);
						((TitleNoteTextView) view).setMaxLines(rowCount);
					}

					// Change color based on complete status
					((TitleNoteTextView) view).useSecondaryColor(!c.isNull(3));

					((TitleNoteTextView) view).setTextTitle(sTemp);
					return true;
				case 2:
					// Note
					((TitleNoteTextView) view).setTextRest("");
					return true;
				default:
					view.setVisibility(View.GONE);
					return true;
				}
			}
		};
	}
}
