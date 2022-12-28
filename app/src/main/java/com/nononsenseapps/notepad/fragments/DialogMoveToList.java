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

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.DAO;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.databinding.FragmentDialogMovetolistBinding;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;

/**
 * When you long-click a note, you can press a button on the actionbar to move it
 * to anoter list. Then, this popup shows up to let the user choose the destination
 */
@EFragment()
public class DialogMoveToList extends DialogFragment {

	static final String TASK_IDS = "task_ids";

	private TaskList mTaskList;

	private long[] taskIds = null;

	/**
	 * for {@link R.layout#fragment_dialog_movetolist}
	 */
	private FragmentDialogMovetolistBinding mBinding;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		mBinding = FragmentDialogMovetolistBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// here you call methods with the old @AfterViews annotation
		setup();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mBinding = null;
	}

	public static DialogMoveToList getInstance(final Long... tasks) {
		long[] taskIds = new long[tasks.length];
		for (int i = 0; i < tasks.length; i++) {
			taskIds[i] = tasks[i];
		}

		return getInstance(taskIds);
	}

	public static DialogMoveToList getInstance(final long... taskIds) {
		DialogMoveToList dialog = new DialogMoveToList_();
		Bundle args = new Bundle();
		args.putLongArray(TASK_IDS, taskIds);
		dialog.setArguments(args);
		return dialog;
	}

	public DialogMoveToList() {}

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
		mBinding.buttons.dialogYes.setEnabled(false);

		// Adapter for list titles and ids
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
				getActivity(), R.layout.simple_light_list_item_activated_1,
				null, new String[] { TaskList.Columns.TITLE },
				new int[] { android.R.id.text1 }, 0);
		// Set it to the view
		mBinding.listView.setAdapter(adapter);

		mBinding.listView.setOnItemClickListener(
				(arg0, arg1, pos, id) -> mBinding.buttons.dialogYes.setEnabled(true));

		// Load content
		LoaderManager.getInstance(this).restartLoader(0, null,
				new LoaderCallbacks<Cursor>() {

					@NonNull
					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
						return new CursorLoader(getActivity(), TaskList.URI,
								TaskList.Columns.FIELDS, null, null,
								getResources().getString(
										R.string.const_as_alphabetic,
										TaskList.Columns.TITLE));
					}

					@Override
					public void onLoadFinished(@NonNull Loader<Cursor> arg0, Cursor c) {
						adapter.swapCursor(c);
					}

					@Override
					public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
						adapter.swapCursor(null);
					}
				});
	}

	@Background
	void moveItems(final long toListId, final long[] taskIds) {

		final ContentValues val = new ContentValues();
		val.put(Task.Columns.DBLIST, toListId);

		// where _ID in (1, 2, 3)
		final String whereId = Task.Columns._ID + " IN (" + DAO.arrayToCommaString(taskIds) + ")";

		getActivity().getContentResolver().update(Task.URI, val, whereId, null);
	}

	@Click(resName = "dialog_no")
	void cancelClicked() {
		dismiss();
	}

	@Click(resName = "dialog_yes")
	void okClicked() {
		// move items
		if (mBinding.listView.getCheckedItemPosition() == AdapterView.INVALID_POSITION) {
			return;
		}

		final Cursor c = (Cursor) mBinding.listView
				.getItemAtPosition(mBinding.listView.getCheckedItemPosition());
		if (c != null) {
			final long targetListId = c.getLong(0);
			final String targetListTitle = c.getString(1);

			if (taskIds.length > 0 && targetListId > 0) {
				moveItems(targetListId, taskIds);
			}

			try {
				Toast.makeText(getActivity(),
						getString(R.string.moved_x_to_list, taskIds.length, targetListTitle),
						Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				// Guard against translations
			}
		}

		this.dismiss();
	}
}
