package com.nononsenseapps.notepad.fragments;

import java.security.InvalidParameterException;
import java.util.Calendar;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.TimePickerDialogFragment;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase.DialogConfirmedListener;
import com.nononsenseapps.notepad.fragments.DialogDateTimePicker.DateTimeSetListener;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.utils.views.StyledEditText;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;

/**
 * A fragment representing a single Note detail screen. This fragment is either
 * contained in a {@link TaskListActivity} in two-pane mode (on tablets) or a
 * {@link TaskDetailActivity} on handsets.
 */
@EFragment(R.layout.fragment_task_detail)
public class TaskDetailFragment extends Fragment implements DateTimeSetListener {

	public static int LOADER_EDITOR_TASK = 1;
	public static int LOADER_EDITOR_TASKLISTS = 2;
	public static int LOADER_EDITOR_NOTIFICATIONS = 3;

	@ViewById
	StyledEditText taskText;

	@ViewById
	View detailsSection;

	@ViewById
	CheckBox taskCompleted;

	@ViewById
	Button dueDateBox;

	@ViewById
	LinearLayout notificationList;

	// Id of task to open
	public static final String ARG_ITEM_ID = "item_id";
	// If no id is given, a string can be accepted as initial state
	public static final String ARG_ITEM_CONTENT = "item_text";
	// A list id is necessary
	public static final String ARG_ITEM_LIST_ID = "item_list_id";

	// Dao version of the object this fragment represents
	private Task mTask;
	// Version when task was opened
	private Task mTaskOrg;
	// To save orgState
	// TODO

	private OnFragmentInteractionListener mListener;
	private ShareActionProvider mShareActionProvider;

	/**
	 * Performs no error checking. Only calls other getter with the last segment
	 * parsed as long
	 */
	public static TaskDetailFragment_ getInstance(final Uri itemUri) {
		return getInstance(Long.parseLong(itemUri.getLastPathSegment()));
	}

	/**
	 * Use to open an existing task
	 */
	public static TaskDetailFragment_ getInstance(final long itemId) {
		Bundle arguments = new Bundle();
		arguments.putLong(ARG_ITEM_ID, itemId);
		TaskDetailFragment_ fragment = new TaskDetailFragment_();
		fragment.setArguments(arguments);
		return fragment;
	}

	/**
	 * Use to create a new task
	 */
	public static TaskDetailFragment_ getInstance(String text, final long listId) {
		Bundle arguments = new Bundle();
		arguments.putString(ARG_ITEM_CONTENT, text);
		arguments.putLong(ARG_ITEM_LIST_ID, listId);
		TaskDetailFragment_ fragment = new TaskDetailFragment_();
		fragment.setArguments(arguments);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TaskDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		if (getArguments().getLong(ARG_ITEM_ID, -1) > 0) {
			// Load data from database
			getLoaderManager().restartLoader(LOADER_EDITOR_TASK, null,
					new LoaderCallbacks<Cursor>() {

						@Override
						public Loader<Cursor> onCreateLoader(int arg0,
								Bundle arg1) {
							return new CursorLoader(getActivity(), Task
									.getUri(getArguments().getLong(ARG_ITEM_ID,
											-1)), Task.Columns.FIELDS, null,
									null, null);
						}

						@Override
						public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
							c.moveToFirst();
							mTask = new Task(c);
							if (mTaskOrg == null) {
								mTaskOrg = new Task(c);
							}
							fillUIFromTask();
							// Don't want updates while editing
							getLoaderManager()
									.destroyLoader(LOADER_EDITOR_TASK);
						}

						@Override
						public void onLoaderReset(Loader<Cursor> arg0) {
						}
					});
		}
		else {
			if (getArguments().getLong(ARG_ITEM_LIST_ID, -1) < 1) {
				// throw new InvalidParameterException(
				// "Must specify a list id to create a note in!");
				Toast.makeText(getActivity(),
						"Must specify a list id to create a note in!",
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}

			mTaskOrg = new Task();
			mTask = new Task();
			mTask.dblist = getArguments().getLong(ARG_ITEM_LIST_ID);
			// New note but start with the text given
			mTask.setText(getArguments().getString(ARG_ITEM_CONTENT, ""));
			fillUIFromTask();
		}
	}

	@AfterViews
	void setListeners() {
		taskText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				setShareIntent(s.toString());
			}
		});
	}

	@Click(R.id.dueDateBox)
	void onDateClick() {
		DialogDateTimePicker_.showDialog(getFragmentManager(), mTask.due, this);
	}

	@Override
	public void onDateTimeSet(final long time) {
		mTask.due = time;
		setDueText();
	}

	private void setDueText() {
		if (mTask.due == null) {
			dueDateBox.setText("");
		}
		else {
			// Due date
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getActivity());

			// TODO does this respect timezones?
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(mTask.due);
			dueDateBox.setText(DateFormat.format(prefs.getString(getActivity()
					.getString(R.string.key_pref_dateformat_long),
					getActivity().getString(R.string.dateformat_long_1)), cal));
		}
	}

	@Click(R.id.dueCancelButton)
	void onDueRemoveClick() {
		mTask.due = null;
		setDueText();
	}

	@Click(R.id.notificationAdd)
	void onAddReminder() {
		if (mTask != null) {
			// TODO
			// IF no id, have to save first
			if (mTask._id < 1) {
				saveTask();
			}
			// Only allow if save succeeded
			if (mTask._id < 1) {
				Toast.makeText(getActivity(),
						R.string.please_type_before_reminder,
						Toast.LENGTH_SHORT).show();
				return;
			}
			// TODO add item to DB
			final Notification not = new Notification(mTask._id);
			if (mTask.due == null) {
				final Calendar local = Calendar.getInstance();
				local.add(Calendar.MINUTE, 1);
				not.time = local.getTimeInMillis();
			}
			else {
				not.time = mTask.due;
			}
			// TODO save item

			// TODO add item to UI
			addNotification(not);
		}
	}

	@UiThread
	void fillUIFromTask() {
		taskText.setText(mTask.getText());
		setDueText();
		taskCompleted.setChecked(mTask.completed != null);
		taskCompleted.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked)
					mTask.completed = Calendar.getInstance().getTimeInMillis();
				else
					mTask.completed = null;
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_tasks_detail, menu);

		// Locate MenuItem with ShareActionProvider
		MenuItem item = menu.findItem(R.id.menu_share);

		// Fetch and store ShareActionProvider
		mShareActionProvider = (ShareActionProvider) item.getActionProvider();
		setShareIntent("");
	}

	// Call to update the share intent
	private void setShareIntent(final String text) {
		if (mShareActionProvider != null && taskText != null) {
			mShareActionProvider.setShareIntent(new Intent(Intent.ACTION_SEND)
					.setType("text/plain").putExtra(Intent.EXTRA_TEXT, text));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Launch main showing lists
			final Intent intent = new Intent()
					.setAction(Intent.ACTION_VIEW)
					.setClass(getActivity(), ActivityMain_.class)
					.setFlags(
							Intent.FLAG_ACTIVITY_CLEAR_TASK
									| Intent.FLAG_ACTIVITY_NEW_TASK);
			if (mTask != null) {
				intent.setData(TaskList.getUri(mTask.dblist));
			}
			startActivity(intent);
			getActivity().finish();
			return true;
		case R.id.menu_add:
			// TODO should not call if in tablet mode
			if (mListener != null && mTask != null && mTask.dblist > 0) {
				mListener.addTaskInList("", mTask.dblist);
			}
			return true;
		case R.id.menu_revert:
			// set to null to prevent modifications
			mTask = null;
			// Request a close from activity
			if (mListener != null) {
				mListener.closeFragment(this);
			}
			return true;
		case R.id.menu_delete:
			deleteAndClose();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void deleteAndClose() {
		if (mTask != null && mTask._id > 0) {
			DialogDeleteTask.showDialog(getFragmentManager(), mTask._id,
					new DialogConfirmedListener() {
						@Override
						public void onConfirm() {
							// Prevents save attempts
							mTask = null;
							// Request a close from activity
							if (mListener != null) {
								mListener
										.closeFragment(TaskDetailFragment.this);
							}
						}
					});
		}
		else {
			// Prevents save attempts
			mTask = null;
			// Request a close from activity
			if (mListener != null) {
				mListener.closeFragment(this);
			}
		}
	}

	private void saveTask() {
		// if mTask is null, the task has been deleted or cancelled
		if (mTask != null) {
			// Needed for comparison
			mTask.setText(taskText.getText().toString());
			// if new item, only save if something has been entered
			if ((mTask._id > 0 && !mTask.equals(mTaskOrg))
					|| (mTask._id == -1 && isThereContent())) {
				// mTask.setText(taskText.getText().toString());
				mTask.save(getActivity());
			}
		}
	}

	boolean isThereContent() {
		// TODO check more fields
		boolean result = false;
		result |= taskText.getText().length() > 0;
		result |= dueDateBox.getText().length() > 0;

		return result;
	}

	@Override
	public void onPause() {
		super.onPause();
		saveTask();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		}
		catch (ClassCastException e) {
			// throw new ClassCastException(activity.toString()
			// + " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onSaveInstanceState(final Bundle state) {
		// TODO
		// Save orgstate
	}

	/**
	 * Inserts a notification item in the UI
	 * 
	 * @param not
	 */
	private void addNotification(final Notification not) {
		if (getActivity() != null) {

			View nv = LayoutInflater.from(getActivity()).inflate(
					R.layout.notification_view, null);
			// Set date time text
			final Button notTimeButton = (Button) nv
					.findViewById(R.id.notificationDateTime);
			notTimeButton.setText(not.getLocalDateTimeText(getActivity()));

			// Remove button
			nv.findViewById(R.id.notificationRemove).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							// Remove row from UI
							notificationList.removeView((View) v.getParent());
							// Remove from database and renotify
							// TODO
							// NotificationHelper.deleteNotification(activity,
							// not.id);
						}
					});

			// Date button
			nv.findViewById(R.id.notificationDateTime).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							// TODO open dual dialog as for due date
							DialogDateTimePicker_.showDialog(
									getFragmentManager(), not.time,
									new DateTimeSetListener() {
										@Override
										public void onDateTimeSet(long time) {
											not.time = time;
											notTimeButton.setText(not
													.getLocalDateTimeText(getActivity()));
											// TODO save to database etc
										}
									});
							/*
							 * FragmentTransaction ft = getFragmentManager()
							 * .beginTransaction(); Fragment prev =
							 * getFragmentManager()
							 * .findFragmentByTag("notificationdatedialog"); if
							 * (prev != null) { ft.remove(prev); }
							 * ft.addToBackStack(null);
							 * 
							 * // Create and show the dialog.
							 * TimePickerDialogFragment newFragment = new
							 * TimePickerDialogFragment(); Bundle args = new
							 * Bundle(); args.putBoolean(
							 * TimePickerDialogFragment.KEY_DATEPICKER, true);
							 * newFragment.setArguments(args);
							 * newFragment.setCallbacks((Button) v, not);
							 * newFragment.show(ft, "notificationdatedialog");
							 */
						}
					});

			notificationList.addView(nv);
		}
	}
}
