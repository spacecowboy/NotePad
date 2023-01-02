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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.databinding.FragmentDialogEditlistBinding;

public class DialogEditList extends DialogFragment {

	public interface EditListDialogListener {
		void onFinishEditDialog(long id);
	}

	static final String LIST_ID = "list_id";

	private TaskList mTaskList;

	private EditListDialogListener listener;

	/**
	 * for {@link R.layout#fragment_dialog_editlist}
	 */
	private FragmentDialogEditlistBinding mBinding;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		mBinding = FragmentDialogEditlistBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// here you call methods with the old @AfterViews annotation
		setup();
		mBinding.titleField.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				// allow to press OK if there is a title
				mBinding.buttons.dialogYes.setEnabled(mBinding.titleField.length() > 0);
			}
		});
		mBinding.deleteButton.setOnClickListener(v -> deleteClicked());
		mBinding.buttons.dialogNo.setOnClickListener(v -> dismiss());
		mBinding.buttons.dialogYes.setOnClickListener(v -> okClicked());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mBinding = null;
	}

	/**
	 * Use to create new list
	 */
	public static DialogEditList getInstance() {
		DialogEditList dialog = new DialogEditList();
		dialog.setArguments(new Bundle());
		return dialog;
	}

	/**
	 * To edit an exising list
	 */
	public static DialogEditList getInstance(final long listid) {
		DialogEditList dialog = new DialogEditList();
		Bundle args = new Bundle();
		args.putLong(LIST_ID, listid);
		dialog.setArguments(args);
		return dialog;
	}

	protected DialogEditList() {}

	public void setListener(final EditListDialogListener listener) {
		this.listener = listener;
	}

	void setup() {
		// New item hides delete button and disables OK initially
		if (getArguments().getLong(LIST_ID, -1) < 1) {
			mBinding.deleteButton.setVisibility(View.GONE);
			mBinding.buttons.dialogYes.setEnabled(false);
		}

		mBinding.modeSpinner.setAdapter(new ArrayAdapter<>(
				getActivity(),
				R.layout.spinner_item,
				getActivity().getResources().getStringArray(R.array.show_list_as)));

		mBinding.sortSpinner.setAdapter(new ArrayAdapter<>(
				getActivity(),
				R.layout.spinner_item,
				getActivity().getResources().getStringArray(R.array.sort_list_by)));

		if (getArguments().getLong(LIST_ID, -1) > 0) {
			getDialog().setTitle(R.string.menu_managelists);
			LoaderManager.getInstance(this).restartLoader(0, null,
					new LoaderCallbacks<Cursor>() {

						@NonNull
						@Override
						public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
							return new CursorLoader(getActivity(),
									TaskList.getUri(getArguments().getLong(LIST_ID, -1)),
									TaskList.Columns.FIELDS, null, null,
									null);
						}

						@Override
						public void onLoadFinished(@NonNull Loader<Cursor> arg0, Cursor c) {
							if (c.moveToFirst()) {
								mTaskList = new TaskList(c);
								DialogEditList.this.getActivity().runOnUiThread(() -> fillViews());
							}
							// Don't need it anymore
							LoaderManager.getInstance(DialogEditList.this).destroyLoader(0);
						}

						@Override
						public void onLoaderReset(@NonNull Loader<Cursor> arg0) {}
					});
		} else {
			getDialog().setTitle(R.string.menu_createlist);
			mTaskList = new TaskList();
		}
	}

	@UiThread
	void fillViews() {
		mBinding.titleField.setText(mTaskList.title);
		selectSortKey();
		selectListTypeKey();

		// Check if this is the default list
		final long defList = Long.parseLong(PreferenceManager
				.getDefaultSharedPreferences(getActivity())
				.getString(getString(R.string.pref_defaultlist), "-1"));
		if (mTaskList._id > 0 && defList == mTaskList._id) {
			mBinding.defaultListBox.setChecked(true);
		}
	}


	void deleteClicked() {
		if (mTaskList._id > 0) {
			DialogDeleteList.showDialog(getParentFragmentManager(), mTaskList._id, this::dismiss);
		}
	}

	void okClicked() {
		Toast.makeText(getActivity(), R.string.saved, Toast.LENGTH_SHORT).show();

		mTaskList.title = mBinding.titleField.getText().toString();
		mTaskList.sorting = getSortValue();
		mTaskList.listtype = getListTypeValue();

		mTaskList.save(getActivity());

		if (mTaskList._id > 0 && mBinding.defaultListBox.isChecked()) {
			PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.edit()
					.putLong(getString(R.string.pref_defaultstartlist), mTaskList._id)
					.putString(getString(R.string.pref_defaultlist), Long.toString(mTaskList._id))
					.commit();
		} else if (mTaskList._id > 0) {
			// Remove pref if it is the default list currently
			final long defList = Long
					.parseLong(PreferenceManager
							.getDefaultSharedPreferences(getActivity())
							.getString(getString(R.string.pref_defaultlist), "-1"));
			final long defStartList = PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.getLong(getString(R.string.pref_defaultstartlist), -1);
			if (defList == mTaskList._id) {
				PreferenceManager.getDefaultSharedPreferences(getActivity())
						.edit()
						.remove(getString(R.string.pref_defaultlist))
						.commit();
			}
			if (defStartList == mTaskList._id) {
				PreferenceManager.getDefaultSharedPreferences(getActivity())
						.edit()
						.remove(getString(R.string.pref_defaultstartlist))
						.commit();
			}
		}

		if (mTaskList._id > 0 && listener != null) {
			listener.onFinishEditDialog(mTaskList._id);
		}

		// TODO save items if necessary
		this.dismiss();
	}

	String getSortValue() {
		String result = null;
		if (mBinding.sortSpinner != null) {
			final String key = (String) mBinding.sortSpinner.getSelectedItem();
			if (key.equals(getString(R.string.sort_list_alphabetical))) {
				result = getString(R.string.const_alphabetic);
			} else if (key.equals(getString(R.string.sort_list_due))) {
				result = getString(R.string.const_duedate);
			} else if (key.equals(getString(R.string.sort_list_manual))) {
				result = getString(R.string.const_possubsort);
			} else if (key.equals(getString(R.string.sort_list_updated))) {
				result = getString(R.string.const_modified);
			} else {
				// Default from global prefs
				result = null;
			}
		}
		return result;
	}

	void selectSortKey() {
		if (mBinding.sortSpinner != null && mTaskList != null) {
			if (mTaskList.sorting == null) {
				mBinding.sortSpinner.setSelection(0);
			} else if (mTaskList.sorting.equals(getString(R.string.const_alphabetic))) {
				mBinding.sortSpinner.setSelection(1);
			} else if (mTaskList.sorting.equals(getString(R.string.const_modified))) {
				mBinding.sortSpinner.setSelection(2);
			} else if (mTaskList.sorting.equals(getString(R.string.const_duedate))) {
				mBinding.sortSpinner.setSelection(3);
			} else if (mTaskList.sorting.equals(getString(R.string.const_possubsort))) {
				mBinding.sortSpinner.setSelection(4);
			}
		}
	}

	String getListTypeValue() {
		String result = null;
		if (mBinding.modeSpinner != null) {
			final String key = (String) mBinding.modeSpinner.getSelectedItem();
			if (key.equals(getString(R.string.show_items_as_notes))) {
				result = getString(R.string.const_listtype_notes);
			} else if (key.equals(getString(R.string.show_items_as_tasks))) {
				result = getString(R.string.const_listtype_tasks);
			} else {
				// Default from global prefs
				result = null;
			}
		}
		return result;
	}

	void selectListTypeKey() {
		if (mBinding.modeSpinner != null && mTaskList != null) {
			if (mTaskList.listtype == null) {
				mBinding.modeSpinner.setSelection(0);
			} else if (mTaskList.listtype.equals(getString(R.string.const_listtype_tasks))) {
				mBinding.modeSpinner.setSelection(1);
			} else if (mTaskList.listtype.equals(getString(R.string.const_listtype_notes))) {
				mBinding.modeSpinner.setSelection(2);
			}
		}
	}

}
