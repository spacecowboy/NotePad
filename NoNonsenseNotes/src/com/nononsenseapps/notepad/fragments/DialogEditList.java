package com.nononsenseapps.notepad.fragments;

import com.googlecode.androidannotations.annotations.AfterTextChange;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase.DialogConfirmedListener;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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

@EFragment(R.layout.fragment_dialog_editlist)
public class DialogEditList extends DialogFragment {

	public interface EditListDialogListener {
		void onFinishEditDialog(long id);
	}

	static final String LIST_ID = "list_id";

	@ViewById
	Spinner sortSpinner;

	@ViewById
	Spinner modeSpinner;

	@ViewById
	EditText titleField;

	@ViewById
	CheckBox defaultListBox;

	@ViewById
	TextView deleteButton;

	@ViewById(R.id.dialog_yes)
	Button okButton;

	@ViewById(R.id.dialog_no)
	Button cancelButton;

	private TaskList mTaskList;

	private EditListDialogListener listener;

	/**
	 * Use to create new list
	 */
	public static DialogEditList_ getInstance() {
		DialogEditList_ dialog = new DialogEditList_();
		dialog.setArguments(new Bundle());
		return dialog;
	}

	public static DialogEditList_ getInstance(final long listid) {
		DialogEditList_ dialog = new DialogEditList_();
		Bundle args = new Bundle();
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
		if (getArguments().getLong(LIST_ID, -1) > 0) {
			getDialog().setTitle(R.string.menu_managelists);
			getLoaderManager().restartLoader(0, null,
					new LoaderCallbacks<Cursor>() {

						@Override
						public Loader<Cursor> onCreateLoader(int arg0,
								Bundle arg1) {
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
		}
		else {
			getDialog().setTitle(R.string.menu_createlist);
			mTaskList = new TaskList();
		}

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
	}

	@UiThread
	void fillViews() {
		titleField.setText(mTaskList.title);
		selectSortKey();

		// TODO all fields

		// Check if this is the default list
		final long defList = Long.parseLong(PreferenceManager
				.getDefaultSharedPreferences(getActivity()).getString(
						getString(R.string.pref_defaultlist), "-1"));
		if (mTaskList._id > 0 && defList == mTaskList._id) {
			defaultListBox.setChecked(true);
		}
	}

	@AfterTextChange(R.id.titleField)
	void titleChanged(Editable text, TextView v) {
		okButton.setEnabled(text.length() > 0);
	}

	@Click(R.id.deleteButton)
	void deleteClicked() {
		if (mTaskList._id > 0) {
			DialogDeleteList.showDialog(getFragmentManager(), mTaskList._id,
					new DialogConfirmedListener() {
						@Override
						public void onConfirm() {
							dismiss();
						}
					});
		}
	}

	@Click(R.id.dialog_no)
	void cancelClicked() {
		dismiss();
	}

	@Click(R.id.dialog_yes)
	void okClicked() {
		Toast.makeText(getActivity(), R.string.saved, Toast.LENGTH_SHORT)
				.show();

		mTaskList.title = titleField.getText().toString();
		mTaskList.sorting = getSortValue();
		// TODO list type

		mTaskList.save(getActivity());

		if (mTaskList._id > 0 && defaultListBox.isChecked()) {
			PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.edit()
					.putString(getString(R.string.pref_defaultlist),
							Long.toString(mTaskList._id)).commit();
		}
		else if (mTaskList._id > 0) {
			// Remove pref if it is the default list currently
			final long defList = Long.parseLong(PreferenceManager
					.getDefaultSharedPreferences(getActivity()).getString(
							getString(R.string.pref_defaultlist), "-1"));
			if (defList == mTaskList._id) {
				PreferenceManager.getDefaultSharedPreferences(getActivity())
						.edit().remove(getString(R.string.pref_defaultlist))
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
			}
			else if (key.equals(getString(R.string.sort_list_due))) {
				result = getString(R.string.const_duedate);
			}
			else if (key.equals(getString(R.string.sort_list_manual))) {
				result = getString(R.string.const_possubsort);
			}
			else if (key.equals(getString(R.string.sort_list_updated))) {
				result = getString(R.string.const_modified);
			}
			else {
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
			}
			else if (mTaskList.sorting
					.equals(getString(R.string.const_alphabetic))) {
				sortSpinner.setSelection(1);
			}
			else if (mTaskList.sorting
					.equals(getString(R.string.const_modified))) {
				sortSpinner.setSelection(2);
			}
			else if (mTaskList.sorting
					.equals(getString(R.string.const_duedate))) {
				sortSpinner.setSelection(3);
			}
			else if (mTaskList.sorting
					.equals(getString(R.string.const_possubsort))) {
				sortSpinner.setSelection(4);
			}
		}
	}
}
