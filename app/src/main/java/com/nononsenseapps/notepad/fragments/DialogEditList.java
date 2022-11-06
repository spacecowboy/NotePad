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

import org.androidannotations.annotations.AfterTextChange;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.UiThread.Propagation;

import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase.DialogConfirmedListener;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@EFragment(resName = "fragment_dialog_editlist")
public class DialogEditList extends DialogFragment {

	public interface EditListDialogListener {
		void onFinishEditDialog(long id);
	}

	static final String LIST_ID = "list_id";

	@ViewById(resName = "sortSpinner")
	Spinner sortSpinner;

	@ViewById(resName = "modeSpinner")
	Spinner modeSpinner;

	@ViewById(resName = "titleField")
	EditText titleField;

	@ViewById(resName = "defaultListBox")
	CheckBox defaultListBox;

	@ViewById(resName = "deleteButton")
	TextView deleteButton;

	@ViewById(resName = "dialog_yes")
	Button okButton;

	@ViewById(resName = "dialog_no")
	Button cancelButton;

	private TaskList mTaskList;

	private EditListDialogListener listener;

	/**
	 * Use to create new list
	 */
	public static DialogEditList_ getInstance() {
		DialogEditList_ dialog = new DialogEditList_();
		dialog.setArguments(Bundle.EMPTY);
		return dialog;
	}

	public static DialogEditList_ getInstance(final long listid) {
		DialogEditList_ dialog = new DialogEditList_();
		Bundle args = Bundle.EMPTY;
		args.putLong(LIST_ID, listid);
		dialog.setArguments(args);
		return dialog;
	}

	public DialogEditList() {

	}

	public void setListener(final EditListDialogListener listener) {
		this.listener = listener;
	}

	@AfterViews
	void setup() {
		// New item hides delete button and disables OK initially
		if (getArguments().getLong(LIST_ID, -1) < 1) {
			deleteButton.setVisibility(View.GONE);
			okButton.setEnabled(false);
		}

		modeSpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
				R.layout.spinner_item, getActivity().getResources()
				.getStringArray(R.array.show_list_as)));

		sortSpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
				R.layout.spinner_item, getActivity().getResources()
				.getStringArray(R.array.sort_list_by)));

		if (getArguments().getLong(LIST_ID, -1) > 0) {
			getDialog().setTitle(R.string.menu_managelists);
			getLoaderManager().restartLoader(0, null,
					new LoaderCallbacks<Cursor>() {

						@NonNull
						@Override
						public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
							return new CursorLoader(getActivity(),
									TaskList.getUri(getArguments().getLong(
											LIST_ID, -1)),
									TaskList.Columns.FIELDS, null, null, null);
						}

						@Override
						public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
							if (c.moveToFirst()) {
								mTaskList = new TaskList(c);
								fillViews();
							}
							// Don't need it anymore
							getLoaderManager().destroyLoader(0);
						}

						@Override
						public void onLoaderReset(Loader<Cursor> arg0) {

						}
					});
		} else {
			getDialog().setTitle(R.string.menu_createlist);
			mTaskList = new TaskList();
		}
	}

	@UiThread(propagation = Propagation.REUSE)
	void fillViews() {
		titleField.setText(mTaskList.title);
		selectSortKey();
		selectListTypeKey();

		// Check if this is the default list
		final long defList = Long.parseLong(PreferenceManager
				.getDefaultSharedPreferences(getActivity()).getString(
						getString(R.string.pref_defaultlist), "-1"));
		if (mTaskList._id > 0 && defList == mTaskList._id) {
			defaultListBox.setChecked(true);
		}
	}

	@AfterTextChange(resName = "titleField")
	void titleChanged(Editable text, TextView v) {
		okButton.setEnabled(text.length() > 0);
	}

	@Click(resName = "deleteButton")
	void deleteClicked() {
		if (mTaskList._id > 0) {
			DialogDeleteList.showDialog(getFragmentManager(), mTaskList._id, () -> dismiss());
		}
	}

	@Click(resName = "dialog_no")
	void cancelClicked() {
		dismiss();
	}

	@Click(resName = "dialog_yes")
	void okClicked() {
		Toast.makeText(getActivity(), R.string.saved, Toast.LENGTH_SHORT).show();

		mTaskList.title = titleField.getText().toString();
		mTaskList.sorting = getSortValue();
		mTaskList.listtype = getListTypeValue();

		mTaskList.save(getActivity());

		if (mTaskList._id > 0 && defaultListBox.isChecked()) {
			PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.edit()
					.putLong(getString(R.string.pref_defaultstartlist), mTaskList._id)
					.putString(getString(R.string.pref_defaultlist),
							Long.toString(mTaskList._id))
					.commit();
		} else if (mTaskList._id > 0) {
			// Remove pref if it is the default list currently
			final long defList = Long.parseLong(PreferenceManager
					.getDefaultSharedPreferences(getActivity()).getString(
							getString(R.string.pref_defaultlist), "-1"));
			final long defStartList = PreferenceManager
					.getDefaultSharedPreferences(getActivity()).getLong(
							getString(R.string.pref_defaultstartlist), -1);
			if (defList == mTaskList._id) {
				PreferenceManager.getDefaultSharedPreferences(getActivity())
						.edit().remove(getString(R.string.pref_defaultlist))
						.commit();
			}
			if (defStartList == mTaskList._id) {
				PreferenceManager.getDefaultSharedPreferences(getActivity())
						.edit().remove(getString(R.string.pref_defaultstartlist))
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
		if (sortSpinner != null) {
			final String key = (String) sortSpinner.getSelectedItem();
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
		if (sortSpinner != null && mTaskList != null) {
			if (mTaskList.sorting == null) {
				sortSpinner.setSelection(0);
			} else if (mTaskList.sorting
					.equals(getString(R.string.const_alphabetic))) {
				sortSpinner.setSelection(1);
			} else if (mTaskList.sorting
					.equals(getString(R.string.const_modified))) {
				sortSpinner.setSelection(2);
			} else if (mTaskList.sorting
					.equals(getString(R.string.const_duedate))) {
				sortSpinner.setSelection(3);
			} else if (mTaskList.sorting
					.equals(getString(R.string.const_possubsort))) {
				sortSpinner.setSelection(4);
			}
		}
	}

	String getListTypeValue() {
		String result = null;
		if (modeSpinner != null) {
			final String key = (String) modeSpinner.getSelectedItem();
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
		if (modeSpinner != null && mTaskList != null) {
			if (mTaskList.listtype == null) {
				modeSpinner.setSelection(0);
			} else if (mTaskList.listtype
					.equals(getString(R.string.const_listtype_tasks))) {
				modeSpinner.setSelection(1);
			} else if (mTaskList.listtype
					.equals(getString(R.string.const_listtype_notes))) {
				modeSpinner.setSelection(2);
			}
		}
	}
}
