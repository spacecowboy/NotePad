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

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.databinding.FragmentDialogRestoreBinding;

public class DialogRestore extends DialogFragment {

	public interface OnListSelectedListener {
		void onListSelected(long listId);
	}

	private OnListSelectedListener listener;

	/**
	 * for {@link R.layout#fragment_dialog_restore}
	 */
	private FragmentDialogRestoreBinding mBinding;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		mBinding = FragmentDialogRestoreBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// here you call methods with the old @AfterViews annotation
		setup();
		mBinding.buttons.dialogNo.setOnClickListener(v->dismiss());
		mBinding.buttons.dialogYes.setOnClickListener(v->okClicked());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mBinding = null;
	}

	/**
	 * Use to create new list
	 */
	public static DialogRestore getInstance() {
		DialogRestore dialog = new DialogRestore();
		dialog.setArguments(new Bundle());
		return dialog;
	}

	public DialogRestore() {}

	public void setListener(final OnListSelectedListener listener) {
		this.listener = listener;
	}


	void setup() {
		getDialog().setTitle(R.string.restore_to);

		final SimpleCursorAdapter adapter =
				new SimpleCursorAdapter(getActivity(),
						R.layout.spinner_item, null,
						new String[] { TaskList.Columns.TITLE },
						new int[] { R.id.textViewSpinnerItem }, 0);

		mBinding.listSpinner.setAdapter(adapter);

		LoaderManager.getInstance(this).restartLoader(0, null,
				new LoaderCallbacks<Cursor>() {

					@NonNull
					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
						return new CursorLoader(getActivity(),
								TaskList.URI,
								TaskList.Columns.FIELDS,
								null,
								null,
								TaskList.Columns.TITLE);
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

	void okClicked() {
		Toast.makeText(getActivity(), R.string.saved, Toast.LENGTH_SHORT).show();

		// TODO do something
		if (listener != null) {
			listener.onListSelected(mBinding.listSpinner.getSelectedItemId());
		}

		this.dismiss();
	}
}
