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

package com.nononsenseapps.notepad.ui.editor;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.NestedScrollView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.util.Log;

import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener;
import com.android.datetimepicker.time.TimePickerDialog;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Notification;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.ui.base.DialogConfirmBase.DialogConfirmedListener;
import com.nononsenseapps.notepad.ui.common.DialogDeleteTask;
import com.nononsenseapps.notepad.ui.common.DialogPassword;
import com.nononsenseapps.notepad.ui.common.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.ui.common.MenuStateController;
import com.nononsenseapps.notepad.ui.common.NotificationItemHelper;
import com.nononsenseapps.notepad.ui.common.StyledEditText;
import com.nononsenseapps.notepad.ui.settings.MainPrefs;
import com.nononsenseapps.notepad.util.FragmentHelper;
import com.nononsenseapps.notepad.util.SharedPreferencesHelper;
import com.nononsenseapps.notepad.util.TimeFormatter;

import java.util.Calendar;

import static com.nononsenseapps.notepad.util.ListHelper.getARealList;

/**
 * A fragment representing a single Note detail screen.
 */
public class TaskDetailFragment extends Fragment implements OnDateSetListener {

    private final static String TAG = "RICKSMESSAGE";
    // Id of task to open
    public static final String ARG_ITEM_ID = "item_id";
    // If no id is given, a string can be accepted as initial state
    public static final String ARG_ITEM_CONTENT = "item_text";
    // A list id is necessary
    public static final String ARG_ITEM_LIST_ID = "item_list_id";
    // Random identifier
    private static final String DATE_DIALOG_TAG = "date_9374jf893jd893jt";
    public static int LOADER_EDITOR_TASK = 3001;
    public static int LOADER_EDITOR_TASKLISTS = 3002;
    public static int LOADER_EDITOR_NOTIFICATIONS = 3003;

    StyledEditText taskText;
    CheckBox taskCompleted;
    Button dueDateBox;
    LinearLayout notificationList;
    View taskSection;
    NestedScrollView editScrollView;
    InputMethodManager inputManager;
    // To override intent values with
    // todo replace functionality of  @InstanceState
    long stateId = -1;
    // todo replace functionality of @InstanceState
    long stateListId = -1;
    // Dao version of the object this fragment represents
    private Task mTask;
    // Version when task was opened
    private Task mTaskOrg;
    // To save orgState
    // TODO
    // AND with task.locked. If result is true, note is locked and has not been
    // unlocked, otherwise good to show
    private boolean mLocked = true;
    LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
            if (LOADER_EDITOR_NOTIFICATIONS == id) {
                return new CursorLoader(getActivity(), Notification.URI, Notification.Columns
                        .FIELDS, Notification.Columns.TASKID + " IS ?", new String[]{Long
                        .toString(args.getLong(ARG_ITEM_ID, -1))}, Notification.Columns.TIME);
            } else if (LOADER_EDITOR_TASK == id) {
                return new CursorLoader(getActivity(), Task.getUri(args.getLong(ARG_ITEM_ID, -1))
                        , Task.Columns.FIELDS, null, null, null);
            } else if (LOADER_EDITOR_TASKLISTS == id) {
                return new CursorLoader(getActivity(), TaskList.getUri(args.getLong
                        (ARG_ITEM_LIST_ID)), TaskList.Columns.FIELDS, null, null, null);
            } else {
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> ldr, Cursor c) {
            if (LOADER_EDITOR_TASK == ldr.getId()) {
                if (c != null && c.moveToFirst()) {
                    if (mTask == null) {
                        mTask = new Task(c);
                        if (mTaskOrg == null) {
                            mTaskOrg = new Task(c);
                        }
                        fillUIFromTask();
                        // Don't want updates while editing
                        // getLoaderManager().destroyLoader(LOADER_EDITOR_TASK);
                    } else {
                        // Don't want updates while editing
                        // getLoaderManager().destroyLoader(LOADER_EDITOR_TASK);
                        // Only update the list if that changes
                        Log.d("nononsenseapps listedit", "Updating list in task from " + mTask
                                .dblist);
                        mTask.dblist = new Task(c).dblist;
                        Log.d("nononsenseapps listedit", "Updating list in task to " + mTask
                                .dblist);
                        if (mTaskOrg != null) {
                            mTaskOrg.dblist = mTask.dblist;
                        }
                    }
                    // Load the list to see if we should hide task bits
                    Bundle args = new Bundle();
                    args.putLong(ARG_ITEM_LIST_ID, mTask.dblist);
                    getLoaderManager().restartLoader(LOADER_EDITOR_TASKLISTS, args, this);

                    args.clear();
                    args.putLong(ARG_ITEM_ID, getArguments().getLong(ARG_ITEM_ID, stateId));
                    getLoaderManager().restartLoader(LOADER_EDITOR_NOTIFICATIONS, args,
                            loaderCallbacks);
                } else {
                    // Should kill myself maybe?
                }
            } else if (LOADER_EDITOR_NOTIFICATIONS == ldr.getId()) {
                while (c != null && c.moveToNext()) {
                    addNotification(new Notification(c));
                }
                // Don't update while editing
                // TODO this allows updating of the location name etc
                getLoaderManager().destroyLoader(LOADER_EDITOR_NOTIFICATIONS);
            } else if (LOADER_EDITOR_TASKLISTS == ldr.getId()) {
                // At current only loading a single list
                if (c != null && c.moveToFirst()) {
                    final TaskList list = new TaskList(c);
                    hideTaskParts(list);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> arg0) {
        }
    };
    private TaskEditorCallbacks mListener;
    private ActionProvider mShareActionProvider;
    /*
         * If in tablet and added, rotating to portrait actually recreats the
         * fragment even though it isn't visible. So if this is true, don't load
         * anything.
         */
    private boolean dontLoad = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TaskDetailFragment() {
        // Make sure arguments are non-null
        setArguments(new Bundle());
    }

    void setListeners() {
        if (dontLoad) {
            return;
        }

        taskText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setShareIntent(s.toString());
            }
        });
    }

    void onDateClick() {
        final Calendar localTime = Calendar.getInstance();
        if (mTask != null && mTask.due != null) {
            //datePicker = DialogCalendar.getInstance(mTask.due);
            localTime.setTimeInMillis(mTask.due);
        }// else {
        //	datePicker = DialogCalendar.getInstance();
        //}

        //final DialogCalendar datePicker;
        //datePicker.setListener(this);
        //datePicker.show(getFragmentManager(), DATE_DIALOG_TAG);
        final DatePickerDialog datedialog = DatePickerDialog.newInstance(this, localTime.get
                (Calendar.YEAR), localTime.get(Calendar.MONTH), localTime.get(Calendar
                .DAY_OF_MONTH));
        datedialog.show(getFragmentManager(), DATE_DIALOG_TAG);
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        final Calendar localTime = Calendar.getInstance();
        if (mTask.due != null) {
            localTime.setTimeInMillis(mTask.due);
        }
        localTime.set(Calendar.YEAR, year);
        localTime.set(Calendar.MONTH, monthOfYear);
        localTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        // set to 23:59 to be more or less consistent with earlier date only
        // implementation
        localTime.set(Calendar.HOUR_OF_DAY, 23);
        localTime.set(Calendar.MINUTE, 59);

        mTask.due = localTime.getTimeInMillis();
        setDueText();

        // Dont ask for time for due date
        // final TimePickerDialogFragment picker = getTimePickerFragment();
        // picker.setListener(this);
        // picker.show(getFragmentManager(), "time");
    }

    private void setDueText() {
        if (mTask.due == null) {
            dueDateBox.setText("");
        } else {
            // Due date
            dueDateBox.setText(TimeFormatter.getLocalDateOnlyStringLong(getActivity(), mTask.due));
        }
    }

    void onDueRemoveClick() {
        if (!isLocked()) {
            if (mTask != null) {
                mTask.due = null;
            }
            setDueText();
        }
    }

    // @Override
    // public void onDialogTimeSet(int hourOfDay, int minute) {
    // final Calendar localTime = Calendar.getInstance();
    // if (mTask.due != null) {
    // localTime.setTimeInMillis(mTask.due);
    // }
    // localTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
    // localTime.set(Calendar.MINUTE, minute);
    //
    // mTask.due = localTime.getTimeInMillis();
    // setDueText();
    // }
    //
    // @Override
    // public void onDialogTimeCancel() {
    // // TODO Auto-generated method stub
    //
    // }

    void onAddReminder() {
        if (mTask != null && !isLocked()) {
            // IF no id, have to save first
            if (mTask._id < 1) {
                saveTask();
            }
            // Only allow if save succeeded
            if (mTask._id < 1) {
                Toast.makeText(getActivity(), R.string.please_type_before_reminder, Toast
                        .LENGTH_SHORT).show();
                return;
            }
            final Notification not = new Notification(mTask._id);
            not.save(getActivity(), true);

            // add item to UI
            addNotification(not);

            // And scroll to bottom. takes 300ms for item to appear.
            editScrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    editScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            }, 300);
        }
    }

    // @Override
    // public void onDateTimeSet(final long time) {
    // mTask.due = time;
    // setDueText();
    // }

    /**
     * task.locked & mLocked
     */
    public boolean isLocked() {
        return SharedPreferencesHelper.isPasswordSet(getActivity()) & mLocked;
    }

    void fillUIFromTask() {
        Log.d("nononsenseapps editor", "fillUI, act: " + getActivity());
        if (isLocked()) {
            FragmentHelper.handle(new Runnable() {
                @Override
                public void run() {
                    taskText.setText(mTask.title);
                    DialogPassword pflock = new DialogPassword();
                    pflock.setListener(new PasswordConfirmedListener() {
                        @Override
                        public void onPasswordConfirmed() {
                            mLocked = false;
                            fillUIFromTask();
                        }
                    });
                    pflock.show(getFragmentManager(), "read_verify");
                }
            });
        } else {
            taskText.setText(mTask.getText());
        }
        setDueText();
        taskCompleted.setChecked(mTask.completed != null);
        taskCompleted.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    mTask.completed = Calendar.getInstance().getTimeInMillis();
                else
                    mTask.completed = null;
            }
        });
        // Lock fields
        setFieldStatus();

    }

    /**
     * Set fields to enabled/disabled depending on lockstate
     */
    void setFieldStatus() {
        final boolean status = !isLocked();
        taskText.setEnabled(status);
        taskCompleted.setEnabled(status);
        dueDateBox.setEnabled(status);
    }

    void hideTaskParts(final TaskList list) {
        String type;
        if (list.listtype == null) {
            type = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString
                    (getString(R.string.pref_listtype), getString(R.string.default_listtype));
        } else {
            type = list.listtype;
        }
        taskSection.setVisibility(type.equals(getString(R.string.const_listtype_notes)) ? View
                .GONE : View.VISIBLE);
    }

    // Call to update the share intent
    private void setShareIntent(final String text) {
        if (mShareActionProvider != null && taskText != null) {
            int titleEnd = text.indexOf("\n");
            if (titleEnd < 0) {
                titleEnd = text.length();
            }

            try {
                // Todo fix for support library version
                /*mShareActionProvider.setShareIntent(new Intent(Intent.ACTION_SEND).setType
                        ("text/plain").putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent
                        .EXTRA_SUBJECT, text.substring(0, titleEnd)));*/
            } catch (RuntimeException e) {
                // Can crash when too many transactions overflow the buffer
                Log.d("nononsensenotes", e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (dontLoad) {
            return;
        }
        try {
            mListener = (TaskEditorCallbacks) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement TaskEditorCallbacks");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inputManager = (InputMethodManager) getContext().getSystemService(Context
                .INPUT_METHOD_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_task_detail, container, false);

        taskText = (StyledEditText) rootView.findViewById(R.id.taskText);
        taskCompleted = (CheckBox) rootView.findViewById(R.id.taskCompleted);
        dueDateBox = (Button) rootView.findViewById(R.id.dueDateBox);
        notificationList = (LinearLayout) rootView.findViewById(R.id.notificationList);
        taskSection = rootView.findViewById(R.id.taskSection);
        editScrollView = (NestedScrollView) rootView.findViewById(R.id.editScrollView);

        dueDateBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDateClick();
            }
        });

        rootView.findViewById(R.id.dueCancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDueRemoveClick();
            }
        });

        rootView.findViewById(R.id.notificationAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddReminder();
            }
        });

        setListeners();

        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle state) {
        super.onActivityCreated(state);

        if (dontLoad) {
            return;
        }

        boolean openKb = false;

        final Bundle args = new Bundle();

        long idToOpen = mListener.getEditorTaskId();

        getArguments().putLong(ARG_ITEM_ID, idToOpen);
        if (idToOpen > 0) {
            // Load data from database
            args.putLong(ARG_ITEM_ID, idToOpen);
            getLoaderManager().restartLoader(LOADER_EDITOR_TASK, args, loaderCallbacks);
        } else {
            // If not valid, find a valid list
            long listId = mListener.getListOfTask();
            if (listId < 1) {
                listId = getARealList(getActivity(), -1);
            }
            // Fail if still not valid
            if (listId < 1) {
                // throw new InvalidParameterException(
                // "Must specify a list id to create a note in!");
                Toast.makeText(getActivity(), "Must specify a list id to create a note in!",
                        Toast.LENGTH_SHORT).show();
                getActivity().finish();
                return;
            }
            getArguments().putLong(ARG_ITEM_LIST_ID, listId);
            args.putLong(ARG_ITEM_LIST_ID, listId);
            getLoaderManager().restartLoader(LOADER_EDITOR_TASKLISTS, args, loaderCallbacks);

            openKb = true;
            mTaskOrg = new Task();
            mTask = new Task();
            mTask.dblist = listId;
            // New note but start with the text given
            mTask.setText(mListener.getInitialTaskText());
            fillUIFromTask();
        }

        if (openKb) {
            /**
             * Only show keyboard for new/empty notes But not if the showcase
             * view is showing
             */
            taskText.requestFocus();
            inputManager.showSoftInput(taskText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dontLoad) {
            return;
        }

        // Hide data from snoopers
        if (mTask != null && isLocked()) {
            fillUIFromTask();
        }

        // See if there was a dialog and set listener again
        Fragment dateDialog = getFragmentManager().findFragmentByTag(DATE_DIALOG_TAG);
        if (dateDialog != null) {
            ((DatePickerDialog) dateDialog).setOnDateSetListener(this);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle state) {
        super.onSaveInstanceState(state);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dontLoad) {
            return;
        }

        saveTask();
        // Set locked again
        mLocked = true;
        // If task is actually locked, remove text
        if (isLocked() && mTask != null && taskText != null) {
            taskText.setText(mTask.title);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_tasks_detail, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_share);

        if (item != null) {
            // Fetch and store ShareActionProvider
            mShareActionProvider = MenuItemCompat.getActionProvider(item);
            setShareIntent("");
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_share).setEnabled(!isLocked());

        if (getActivity() instanceof MenuStateController) {
            final boolean visible = ((MenuStateController) getActivity()).childItemsVisible();

            // Outside group to allow for action bar placement
            if (menu.findItem(R.id.menu_delete) != null)
                menu.findItem(R.id.menu_delete).setVisible(visible);
            if (menu.findItem(R.id.menu_revert) != null)
                menu.findItem(R.id.menu_revert).setVisible(visible);
            if (menu.findItem(R.id.menu_share) != null)
                menu.findItem(R.id.menu_share).setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        /*if (itemId == R.id.menu_add) {
            // TODO should not call if in tablet mode
			if (mListener != null && mTask != null && mTask.dblist > 0) {
				mListener.addTaskInList("", mTask.dblist);
			}
			return true;
		} else */
        if (itemId == R.id.menu_revert) {
            // set to null to prevent modifications
            mTask = null;
            // Request a close from activity
            if (mListener != null) {
                mListener.closeEditor(this);
            }
            return true;
        } else if (itemId == R.id.menu_delete) {
            if (mTask != null) {
                if (isLocked()) {
                    DialogPassword delpf = new DialogPassword();
                    delpf.setListener(new PasswordConfirmedListener() {
                        @Override
                        public void onPasswordConfirmed() {
                            deleteAndClose();
                        }
                    });
                    delpf.show(getFragmentManager(), "delete_verify");
                } else {
                    deleteAndClose();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAndClose() {
        if (mTask != null && mTask._id > 0 && !isLocked()) {
            DialogDeleteTask.showDialog(getFragmentManager(), mTask._id, new
                    DialogConfirmedListener() {
                @Override
                public void onConfirm() {
                    // Prevents save attempts
                    mTask = null;
                    // Request a close from activity
                    // Todo let listener handle delete, and use Snack bar.
                    if (mListener != null) {
                        mListener.closeEditor(TaskDetailFragment.this);
                    }
                }
            });
        } else {
            // Prevents save attempts
            mTask = null;
            // Request a close from activity
            if (mListener != null) {
                mListener.closeEditor(TaskDetailFragment.this);
            }
        }
    }

    private void saveTask() {
        // if mTask is null, the task has been deleted or cancelled
        // If the task is not unlocked, editing is disabled
        if (mTask != null && !isLocked()) {
            // Needed for comparison
            mTask.setText(taskText.getText().toString());
            // if new item, only save if something has been entered
            if ((mTask._id > 0 && !mTask.equals(mTaskOrg)) || (mTask._id == -1 && isThereContent
                    ())) {
                // mTask.setText(taskText.getText().toString());
                mTask.save(getActivity());
                // Set the intent to open the task.
                // So we dont create a new one on rotation for example
                fixIntent();

                // TODO, should restart notification loader for new tasks
            }
        }
    }

    void fixIntent() {
        stateId = mTask._id;
        stateListId = mTask.dblist;

        if (getActivity() == null)
            return;

        final Intent orgIntent = getActivity().getIntent();
        if (orgIntent == null || orgIntent.getAction() == null || !orgIntent.getAction().equals
                (Intent.ACTION_INSERT))
            return;

        if (mTask == null || mTask._id < 1)
            return;

        final Intent intent = new Intent().setAction(Intent.ACTION_EDIT).setClass(getActivity(),
                ActivityEditor.class).setData(mTask.getUri()).putExtra(TaskDetailFragment
                .ARG_ITEM_LIST_ID, mTask.dblist);

        getActivity().setIntent(intent);
    }

    boolean isThereContent() {
        boolean result = false;
        result |= taskText.getText().length() > 0;
        result |= dueDateBox.getText().length() > 0;

        return result;
    }

    /**
     * Inserts a notification item in the UI
     *
     * @param not
     */
    void addNotification(final Notification not) {
        if (getActivity() != null) {
            View nv = LayoutInflater.from(getActivity()).inflate(R.layout.notification_view, null);

            // So we can update the view later
            not.view = nv;

            // Setup all the listeners etc
            NotificationItemHelper.setup(this, notificationList, nv, not, mTask);

            notificationList.addView(nv);
            Log.i(TAG, "Add notificaation @TaskDetailFragment Ln#750");

        }
    }

    /**
     * Returns an appropriately themed time picker fragment
     */
    // public TimePickerDialogFragment getTimePickerFragment() {
    // final String theme = PreferenceManager.getDefaultSharedPreferences(
    // getActivity()).getString(MainPrefs.KEY_THEME,
    // getString(R.string.const_theme_light_ab));
    // if (theme.contains("light")) {
    // return TimePickerDialogFragment
    // .newInstance(R.style.BetterPickersDialogFragment_Light);
    // }
    // else {
    // // dark
    // return TimePickerDialogFragment
    // .newInstance(R.style.BetterPickersDialogFragment);
    // }
    // }

    /**
     * Returns an appropriately themed time picker fragment. Up to caller to set
     * callback and desired starting time.
     */
    public TimePickerDialog getTimePickerDialog() {
        final String theme = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(MainPrefs.KEY_THEME, getString(R.string.const_theme_light_ab));

        final TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(null, 0, 0,
                android.text.format.DateFormat.is24HourFormat(getActivity()));
        timePickerDialog.setThemeDark(!theme.contains("light"));
        return timePickerDialog;
    }

    public interface TaskEditorCallbacks {
        /**
         * @return the id of the task to open. Negative number indicates a new task. In which
         * case, the fragment will call getListOfTask.
         */
        long getEditorTaskId();

        /**
         * @return The list where this task (should) live(s). Used for creating new tasks.
         */
        long getListOfTask();

        void closeEditor(Fragment fragment);

        /**
         * @return The text a new task should contain, or the empty string.
         */
        @NonNull
        String getInitialTaskText();
    }
}
