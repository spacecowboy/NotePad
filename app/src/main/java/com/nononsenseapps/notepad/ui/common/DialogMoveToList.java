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

package com.nononsenseapps.notepad.ui.common;

import android.content.ContentValues;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.DAO;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.databinding.FragmentDialogMovetolistBinding;
import com.nononsenseapps.notepad.util.ArrayHelper;
import com.nononsenseapps.notepad.util.AsyncTaskHelper;

import java.util.Collection;

public class DialogMoveToList extends DialogFragment {

	static final String TASK_IDS = "task_ids";

	private long[] taskIds = null;
	private FragmentDialogMovetolistBinding binding;


	public static DialogMoveToList getInstance(final Collection<Long> taskIds) {
		DialogMoveToList dialog = new DialogMoveToList();
		Bundle args = new Bundle();
        // To array fixes threading issues
		args.putLongArray(TASK_IDS, ArrayHelper.toArray(taskIds));
		dialog.setArguments(args);
		return dialog;
	}

	public DialogMoveToList() {

	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dialog_movetolist, container, true);
		return binding.getRoot();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setup();
	}

	// public void setListener(final EditListDialogListener listener) {
	// this.listener = listener;
	// }

	void setup() {
		if (!getArguments().containsKey(TASK_IDS)) {
			dismiss();
		}
		this.taskIds = getArguments().getLongArray(TASK_IDS);

		if (taskIds.length < 1) {
			dismiss();
		}

		getDialog().setTitle(R.string.move_to);

		// Must select item first
		binding.buttons.dialogYes.setEnabled(false);

		// Adapter for list titles and ids
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
				getActivity(), R.layout.simple_light_list_item_activated_1,
				null, new String[] { TaskList.Columns.TITLE },
				new int[] { android.R.id.text1 }, 0);
		// Set it to the view
		binding.listView.setAdapter(adapter);

		binding.listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				binding.buttons.dialogYes.setEnabled(true);
			}
		});

        binding.buttons.dialogNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        binding.buttons.dialogYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                okClicked();
            }
        });

		// Load content
		getLoaderManager().restartLoader(0, null,
				new LoaderCallbacks<Cursor>() {

					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
						return new CursorLoader(getActivity(), TaskList.URI,
								TaskList.Columns.FIELDS, null, null,
								getResources().getString(
										R.string.const_as_alphabetic,
										TaskList.Columns.TITLE));
					}

					@Override
					public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
						adapter.swapCursor(c);
					}

					@Override
					public void onLoaderReset(Loader<Cursor> arg0) {
						adapter.swapCursor(null);
					}
				});
	}

	void moveItems(final long toListId, final long[] taskIds) {
        final ContentValues val = new ContentValues();
        val.put(Task.Columns.DBLIST, toListId);

        // where _ID in (1, 2, 3)
        final String whereId = new StringBuilder(Task.Columns._ID)
                .append(" IN (").append(DAO.arrayToCommaString(taskIds))
                .append(")").toString();

        AsyncTaskHelper.background(new AsyncTaskHelper.Job() {
            @Override
            public void doInBackground() {
                getActivity().getContentResolver().update(Task.URI, val, whereId, null);
            }
        });
	}

	void okClicked() {
		// move items
		if (binding.listView.getCheckedItemPosition() == AdapterView.INVALID_POSITION) {
			return;
		}

		final Cursor c = (Cursor) binding.listView.getItemAtPosition(binding.listView
				.getCheckedItemPosition());
		if (c != null) {
			final long targetListId = c.getLong(0);
			final String targetListTitle = c.getString(1);

			if (taskIds.length > 0 && targetListId > 0) {
				moveItems(targetListId, taskIds);
			}

			try {
				Toast.makeText(
						getActivity(),
						getString(R.string.moved_x_to_list, taskIds.length,
								targetListTitle), Toast.LENGTH_SHORT).show();
			}
			catch (Exception e) {
				// Guard against translations
			}
		}

		this.dismiss();
	}
}
