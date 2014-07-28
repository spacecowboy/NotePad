package com.nononsenseapps.notepad.fragments;

import org.androidannotations.annotations.AfterTextChange;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
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
import android.support.v4.widget.SimpleCursorAdapter;
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

@EFragment(resName="fragment_dialog_restore")
public class DialogRestore extends DialogFragment {

	public interface OnListSelectedListener {
		void onListSelected(long listId);
	}

	@ViewById(resName="listSpinner")
	Spinner listSpinner;

	@ViewById(resName="dialog_yes")
	Button okButton;

	@ViewById(resName="dialog_no")
	Button cancelButton;

	private OnListSelectedListener listener;

	/**
	 * Use to create new list
	 */
	public static DialogRestore_ getInstance() {
		DialogRestore_ dialog = new DialogRestore_();
		dialog.setArguments(new Bundle());
		return dialog;
	}

	public DialogRestore() {

	}

	public void setListener(final OnListSelectedListener listener) {
		this.listener = listener;
	}

	@AfterViews
	void setup() {
		getDialog().setTitle(R.string.restore_to);
		
		final SimpleCursorAdapter adapter = 
				new SimpleCursorAdapter(getActivity(), 
						R.layout.spinner_item, null, 
						new String[] {TaskList.Columns.TITLE}, 
						new int[] {R.id.textViewSpinnerItem}, 0);
		
		listSpinner.setAdapter(adapter);
		
		getLoaderManager().restartLoader(0, null,
				new LoaderCallbacks<Cursor>() {

					@Override
					public Loader<Cursor> onCreateLoader(int arg0,
							Bundle arg1) {
						return new CursorLoader(getActivity(),
								TaskList.URI,
								TaskList.Columns.FIELDS, null, null, TaskList.Columns.TITLE);
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

	@Click(resName="dialog_no")
	void cancelClicked() {
		dismiss();
	}

	@Click(resName="dialog_yes")
	void okClicked() {
		Toast.makeText(getActivity(), R.string.saved, Toast.LENGTH_SHORT)
				.show();

		// TODO do something
		if (listener != null) {
			listener.onListSelected(listSpinner.getSelectedItemId());
		}
		
		this.dismiss();
	}
}
