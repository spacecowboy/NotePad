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
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase.DialogConfirmedListener;

import org.androidannotations.annotations.AfterTextChange;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;
import org.androidannotations.annotations.ViewById;

@EFragment(resName = "fragment_dialog_editlist")
public class DialogEditList extends DialogFragment {

    static final String LIST_ID = "list_id";
    @ViewById(resName = "titleField")
    EditText titleField;
    @ViewById(resName = "defaultListBox")
    Switch defaultListBox;
    @ViewById(resName = "deleteButton")
    View deleteButton;
    @ViewById(resName = "dialog_yes")
    View okButton;
    @ViewById(resName = "dialog_no")
    View cancelButton;
    private TaskList mTaskList;
    private EditListDialogListener listener;

    public DialogEditList() {
    }

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

        if (getArguments().getLong(LIST_ID, -1) > 0) {
            getDialog().setTitle(R.string.menu_managelists);
            getLoaderManager().restartLoader(0, null, new LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
                    return new CursorLoader(getActivity(), TaskList.getUri(getArguments().getLong
                            (LIST_ID, -1)), TaskList.Columns.FIELDS, null, null, null);
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

        // Check if this is the default list
        final long defList = Long.parseLong(PreferenceManager.getDefaultSharedPreferences
                (getActivity()).getString(getString(R.string.pref_defaultlist), "-1"));
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
            DialogDeleteList.showDialog(getFragmentManager(), mTaskList._id, new
                    DialogConfirmedListener() {
                @Override
                public void onConfirm() {
                    dismiss();
                }
            });
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

        mTaskList.save(getActivity());

        if (mTaskList._id > 0 && defaultListBox.isChecked()) {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putLong(getString
                    (R.string.pref_defaultstartlist), mTaskList._id).putString(getString(R.string
                    .pref_defaultlist), Long.toString(mTaskList._id)).commit();
        } else if (mTaskList._id > 0) {
            // Remove pref if it is the default list currently
            final long defList = Long.parseLong(PreferenceManager.getDefaultSharedPreferences
                    (getActivity()).getString(getString(R.string.pref_defaultlist), "-1"));
            final long defStartList = PreferenceManager.getDefaultSharedPreferences(getActivity()
            ).getLong(getString(R.string.pref_defaultstartlist), -1);
            if (defList == mTaskList._id) {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().remove
                        (getString(R.string.pref_defaultlist)).commit();
            }
            if (defStartList == mTaskList._id) {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().remove
                        (getString(R.string.pref_defaultstartlist)).commit();
            }
        }

        if (mTaskList._id > 0 && listener != null) {
            listener.onFinishEditDialog(mTaskList._id);
        }

        // TODO save items if necessary
        this.dismiss();
    }

    public interface EditListDialogListener {
        void onFinishEditDialog(long id);
    }
}
