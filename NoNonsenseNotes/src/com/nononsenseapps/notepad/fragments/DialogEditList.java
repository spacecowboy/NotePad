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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
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

		sortSpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
				R.layout.spinner_item, getActivity().getResources()
						.getStringArray(R.array.show_list_as)));

		modeSpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
				R.layout.spinner_item, getActivity().getResources()
						.getStringArray(R.array.sort_list_by)));
	}

	@UiThread
	void fillViews() {
		titleField.setText(mTaskList.title);
	}

	@AfterTextChange(R.id.titleField)
	void titleChanged(Editable text, TextView v) {
		okButton.setEnabled(text.length() > 0);
	}

	@Click(R.id.deleteButton)
	void deleteClicked() {
		if (mTaskList._id > 0) {
			DialogDeleteList.showDialog(getFragmentManager(), mTaskList._id, new DialogConfirmedListener() {
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
		// TODO save it
		Toast.makeText(getActivity(), "Remember to show save toast",
				Toast.LENGTH_SHORT).show();

		mTaskList.title = titleField.getText().toString();

		if (mTaskList._id > 0) {
			getActivity().getContentResolver().update(mTaskList.getUri(),
					mTaskList.getContent(), null, null);
		}
		else {
			final Uri result = getActivity().getContentResolver().insert(
					mTaskList.getBaseUri(), mTaskList.getContent());

			if (result != null) {
				mTaskList._id = Long.parseLong(result.getLastPathSegment());
			}
		}

		if (mTaskList._id > 0 && listener != null) {
			listener.onFinishEditDialog(mTaskList._id);
		}

		// TODO set as default list if checked

		// TODO save items if necessary
		this.dismiss();
	}
}
