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

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.databinding.FragmentDialogEditlistBinding;
import com.nononsenseapps.notepad.ui.base.DialogConfirmBase.DialogConfirmedListener;

public class DialogEditList extends DialogFragment {

    static final String LIST_ID = "list_id";

    private TaskList mTaskList;
    private EditListDialogListener listener;
    private FragmentDialogEditlistBinding binding;

    public DialogEditList() {
    }

    /**
     * Use to create new list
     */
    public static DialogEditList getInstance() {
        DialogEditList dialog = new DialogEditList();
        dialog.setArguments(Bundle.EMPTY);
        return dialog;
    }

    public static DialogEditList getInstance(final long listid) {
        DialogEditList dialog = new DialogEditList();
        Bundle args = new Bundle();
        args.putLong(LIST_ID, listid);
        dialog.setArguments(args);
        return dialog;
    }

    public void setListener(final EditListDialogListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dialog_editlist, container, true);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setup();
    }

    void setup() {
        // New item hides delete button and disables OK initially
        if (getArguments().getLong(LIST_ID, -1) < 1) {
            binding.deleteButton.setVisibility(View.GONE);
            binding.buttons.dialogYes.setEnabled(false);
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

        binding.titleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                binding.buttons.dialogYes.setEnabled(editable.length() > 0);
            }
        });

        binding.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteClicked();
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
    }

    void fillViews() {
        binding.titleField.setText(mTaskList.title);

        // Check if this is the default list
        final long defList = Long.parseLong(PreferenceManager.getDefaultSharedPreferences
                (getActivity()).getString(getString(R.string.pref_defaultlist), "-1"));
        if (mTaskList._id > 0 && defList == mTaskList._id) {
            binding.defaultListBox.setChecked(true);
        }
    }

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

    void okClicked() {
        Toast.makeText(getActivity(), R.string.saved, Toast.LENGTH_SHORT).show();

        mTaskList.title = binding.titleField.getText().toString();

        mTaskList.save(getActivity());

        if (mTaskList._id > 0 && binding.defaultListBox.isChecked()) {
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
