package com.nononsenseapps.notepad.fragments;

import java.util.Calendar;

import com.doomonafireball.betterpickers.datepicker.DatePickerDialogFragment;
import com.doomonafireball.betterpickers.datepicker.DatePickerDialogFragment.DatePickerDialogHandler;
import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment;
import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment.TimePickerDialogHandler;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.ActivityTaskHistory;
import com.nononsenseapps.notepad.ActivityTaskHistory_;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase.DialogConfirmedListener;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.utils.views.StyledEditText;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

/**
 * A fragment representing a single Note detail screen. This fragment is either
 * contained in a {@link TaskListActivity} in two-pane mode (on tablets) or a
 * {@link TaskDetailActivity} on handsets.
 */
@EFragment(R.layout.fragment_task_detail)
public class TaskDetailFragment extends Fragment implements
		DatePickerDialogHandler, TimePickerDialogHandler {

	public static int LOADER_EDITOR_TASK = 3001;
	public static int LOADER_EDITOR_TASKLISTS = 3002;
	public static int LOADER_EDITOR_NOTIFICATIONS = 3003;

	LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
			if (LOADER_EDITOR_NOTIFICATIONS == id) {
				return new CursorLoader(getActivity(), Notification.URI,
						Notification.Columns.FIELDS,
						Notification.Columns.TASKID + " IS ?",
						new String[] { Long.toString(args.getLong(ARG_ITEM_ID,
								-1)) }, Notification.Columns.TIME);
			}
			else if (LOADER_EDITOR_TASK == id) {
				return new CursorLoader(getActivity(), Task.getUri(args
						.getLong(ARG_ITEM_ID, -1)), Task.Columns.FIELDS, null,
						null, null);
			}
			else if (LOADER_EDITOR_TASKLISTS == id) {
				return new CursorLoader(getActivity(), TaskList.getUri(args
						.getLong(ARG_ITEM_LIST_ID)), TaskList.Columns.FIELDS,
						null, null, null);
			}
			else {
				return null;
			}
		}

		@Override
		public void onLoadFinished(Loader<Cursor> ldr, Cursor c) {
			if (LOADER_EDITOR_TASK == ldr.getId()) {
				c.moveToFirst();
				mTask = new Task(c);
				if (mTaskOrg == null) {
					mTaskOrg = new Task(c);
				}
				fillUIFromTask();
				// Don't want updates while editing
				getLoaderManager().destroyLoader(LOADER_EDITOR_TASK);
				// Load the list to see if we should hide task bits
				Bundle args = new Bundle();
				args.putLong(ARG_ITEM_LIST_ID, mTask.dblist);
				getLoaderManager().restartLoader(LOADER_EDITOR_TASKLISTS, args,
						loaderCallbacks);
			}
			else if (LOADER_EDITOR_NOTIFICATIONS == ldr.getId()) {
				while (c.moveToNext()) {
					addNotification(new Notification(c));
				}
				// Don't update while editing
				getLoaderManager().destroyLoader(LOADER_EDITOR_NOTIFICATIONS);
			}
			else if (LOADER_EDITOR_TASKLISTS == ldr.getId()) {
				// At current only loading a single list
				if (c.moveToFirst()) {
					final TaskList list = new TaskList(c);
					hideTaskParts(list);
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
		}
	};

	@ViewById
	StyledEditText taskText;

	@ViewById
	CheckBox taskCompleted;

	@ViewById
	Button dueDateBox;

	@ViewById
	LinearLayout notificationList;

	@ViewById
	View taskSection;

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
	// AND with task.locked. If result is true, note is locked and has not been
	// unlocked, otherwise good to show
	private boolean mLocked = true;

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
	public void onActivityCreated(final Bundle state) {
		super.onActivityCreated(state);

		// TODO opening from a notification should delete the notification

		final Bundle args = new Bundle();
		if (getArguments().getLong(ARG_ITEM_ID, -1) > 0) {
			// Load data from database
			args.putLong(ARG_ITEM_ID, getArguments().getLong(ARG_ITEM_ID, -1));
			getLoaderManager().restartLoader(LOADER_EDITOR_TASK, args,
					loaderCallbacks);
			getLoaderManager().restartLoader(LOADER_EDITOR_NOTIFICATIONS, args,
					loaderCallbacks);
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
			args.putLong(ARG_ITEM_LIST_ID,
					getArguments().getLong(ARG_ITEM_LIST_ID, -1));
			getLoaderManager().restartLoader(LOADER_EDITOR_TASKLISTS, args,
					loaderCallbacks);

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
		final TimePickerDialogFragment picker = TimePickerDialogFragment
				.newInstance();
		picker.setListener(this);
		picker.show(getFragmentManager(), "time");
	}

	@Override
	public void onDialogTimeSet(int hourOfDay, int minute) {
		final Calendar localTime = Calendar.getInstance();
		if (mTask.due != null) {
			localTime.setTimeInMillis(mTask.due);
		}
		localTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
		localTime.set(Calendar.MINUTE, minute);

		mTask.due = localTime.getTimeInMillis();
		setDueText();
		// Ask for date, yes the order is screwed up.
		final DatePickerDialogFragment picker = DatePickerDialogFragment
				.newInstance(-1,
				// localTime.get(Calendar.MONTH),
						-1,
						// localTime.get(Calendar.DAY_OF_MONTH),
						localTime.get(Calendar.YEAR));
		picker.setListener(this);
		picker.show(getFragmentManager(), "date");
	}

	@Override
	public void onDialogDateSet(int year, int monthOfYear, int dayOfMonth) {
		final Calendar localTime = Calendar.getInstance();
		if (mTask.due != null) {
			localTime.setTimeInMillis(mTask.due);
		}
		localTime.set(Calendar.YEAR, year);
		localTime.set(Calendar.MONTH, monthOfYear);
		localTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

		mTask.due = localTime.getTimeInMillis();
		setDueText();
	}

	// @Override
	// public void onDateTimeSet(final long time) {
	// mTask.due = time;
	// setDueText();
	// }

	private void setDueText() {
		if (mTask.due == null) {
			dueDateBox.setText("");
		}
		else {
			// Due date
			dueDateBox.setText(TimeFormatter.getLocalDateStringLong(
					getActivity(), mTask.due));
		}
	}

	@Click(R.id.dueCancelButton)
	void onDueRemoveClick() {
		if (!isLocked()) {
			mTask.due = null;
			setDueText();
		}
	}

	@Click(R.id.notificationAdd)
	void onAddReminder() {
		if (mTask != null && !isLocked()) {
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
			final Notification not = new Notification(mTask._id);
			final Calendar local = Calendar.getInstance();
			local.add(Calendar.MINUTE, 1);
			final long soon = local.getTimeInMillis();
			if (mTask.due == null || mTask.due < soon) {
				not.time = soon;
			}
			else {
				not.time = mTask.due;
			}
			not.save(getActivity(), true);

			// add item to UI
			addNotification(not);
		}
	}

	/**
	 * task.locked & mLocked
	 */
	boolean isLocked() {
		if (mTask != null) {
			return mTask.locked & mLocked;
		}
		return false;
	}

	@UiThread
	void fillUIFromTask() {
		if (isLocked()) {
			taskText.setText(mTask.title);
			DialogPassword_ pflock = new DialogPassword_();
			pflock.setListener(new PasswordConfirmedListener() {
				@Override
				public void onPasswordConfirmed() {
					mLocked = false;
					fillUIFromTask();
				}
			});
			pflock.show(getFragmentManager(), "read_verify");
		}
		else {
			taskText.setText(mTask.getText());
		}
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
			type = PreferenceManager.getDefaultSharedPreferences(getActivity())
					.getString(getString(R.string.pref_listtype),
							getString(R.string.default_listtype));
		}
		else {
			type = list.listtype;
		}
		taskSection.setVisibility(type
				.equals(getString(R.string.const_listtype_notes)) ? View.GONE
				: View.VISIBLE);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_tasks_detail, menu);

		// Locate MenuItem with ShareActionProvider
		MenuItem item = menu.findItem(R.id.menu_share);

		if (item != null) {
			// Fetch and store ShareActionProvider
			mShareActionProvider = (ShareActionProvider) item.getActionProvider();
			setShareIntent("");
		}
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
		case R.id.menu_timemachine:
			if (mTask != null && mTask._id > 0) {
				Intent timeIntent = new Intent(getActivity(), ActivityTaskHistory_.class);
				timeIntent.putExtra(Task.Columns._ID, mTask._id);
				startActivityForResult(timeIntent, 1);
				//ActivityTaskHistory.start(getActivity(), mTask._id);
			}
			return true;
		case R.id.menu_delete:
			if (mTask != null) {
				if (mTask.locked) {
					DialogPassword_ delpf = new DialogPassword_();
					delpf.setListener(new PasswordConfirmedListener() {
						@Override
						public void onPasswordConfirmed() {
							deleteAndClose();
						}
					});
					delpf.show(getFragmentManager(), "delete_verify");
				}
				else {
					deleteAndClose();
				}
			}
			return true;
		case R.id.menu_lock:
			DialogPassword_ pflock = new DialogPassword_();
			pflock.setListener(new PasswordConfirmedListener() {
				@Override
				public void onPasswordConfirmed() {
					if (mTask != null) {
						mLocked = true;
						mTask.locked = true;
						saveTask();
						fillUIFromTask();
						Toast.makeText(getActivity(), R.string.locked,
								Toast.LENGTH_SHORT).show();
					}
				}
			});
			pflock.show(getFragmentManager(), "lock_verify");
			return true;
		case R.id.menu_unlock:
			DialogPassword_ pf = new DialogPassword_();
			pf.setListener(new PasswordConfirmedListener() {
				@Override
				public void onPasswordConfirmed() {
					if (mTask != null) {
						mTask.locked = false;
						Toast.makeText(getActivity(), R.string.unlocked,
								Toast.LENGTH_SHORT).show();

						if (mLocked) {
							mLocked = false;
							fillUIFromTask();
						}
					}
				}
			});
			pf.show(getFragmentManager(), "unlock_verify");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_timemachine).setEnabled(
				mTask != null && mTask._id > 0 && !isLocked());
		menu.findItem(R.id.menu_lock)
				.setVisible(mTask != null && !mTask.locked);
		menu.findItem(R.id.menu_unlock).setVisible(
				mTask != null && mTask.locked);
		menu.findItem(R.id.menu_share).setEnabled(!isLocked());
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == Activity.RESULT_OK) {
				onTimeTravel(data);
			}
		}
		else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void deleteAndClose() {
		if (mTask != null && mTask._id > 0 && !isLocked()) {
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
		// If the task is not unlocked, editing is disabled
		if (mTask != null && !isLocked()) {
			// Needed for comparison
			mTask.setText(taskText.getText().toString());
			// if new item, only save if something has been entered
			if ((mTask._id > 0 && !mTask.equals(mTaskOrg))
					|| (mTask._id == -1 && isThereContent())) {
				// mTask.setText(taskText.getText().toString());
				mTask.save(getActivity());

				// TODO, should restart notification loader for new tasks
			}
		}
	}

	boolean isThereContent() {
		boolean result = false;
		result |= taskText.getText().length() > 0;
		result |= dueDateBox.getText().length() > 0;
		result |= (mTask.locked != mTaskOrg.locked);

		return result;
	}

	@Override
	public void onPause() {
		super.onPause();
		saveTask();
		// Set locked again
		mLocked = true;
		// If task is actually locked, remove text
		if (isLocked() && mTask != null && taskText != null) {
			taskText.setText(mTask.title);
		}
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
	@UiThread
	void addNotification(final Notification not) {
		if (getActivity() != null) {

			View nv = LayoutInflater.from(getActivity()).inflate(
					R.layout.notification_view, null);
			// Set date time text
			final TextView notTimeButton = (TextView) nv
					.findViewById(R.id.notificationDateTime);
			notTimeButton.setText(not.getLocalDateTimeText(getActivity()));

			// Remove button
			nv.findViewById(R.id.notificationRemove).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							if (!isLocked()) {
								// Remove row from UI
								notificationList.removeView((View) v
										.getParent());
								// Remove from database and renotify
								not.delete(getActivity());
							}
						}
					});

			// Date button
			nv.findViewById(R.id.notificationDateTime).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							if (!isLocked()) {
								final TimePickerDialogFragment timePicker = TimePickerDialogFragment
										.newInstance();
								timePicker
										.setListener(new TimePickerDialogHandler() {
											@Override
											public void onDialogTimeSet(
													int hourOfDay, int minute) {
												final Calendar localTime = Calendar
														.getInstance();
												localTime
														.setTimeInMillis(not.time);
												localTime.set(
														Calendar.HOUR_OF_DAY,
														hourOfDay);
												localTime.set(Calendar.MINUTE,
														minute);

												not.time = localTime
														.getTimeInMillis();
												notTimeButton.setText(not
														.getLocalDateTimeText(getActivity()));
												not.save(getActivity(), false);

												// Ask for date, yes the order
												// is
												// screwed up.
												final DatePickerDialogFragment datePicker = DatePickerDialogFragment
														.newInstance(
																-1,
																// localTime.get(Calendar.MONTH),
																-1,
																// localTime.get(Calendar.DAY_OF_MONTH),
																localTime
																		.get(Calendar.YEAR));
												datePicker
														.setListener(new DatePickerDialogHandler() {
															@Override
															public void onDialogDateSet(
																	int year,
																	int monthOfYear,
																	int dayOfMonth) {
																final Calendar localTime = Calendar
																		.getInstance();
																localTime
																		.setTimeInMillis(not.time);
																localTime
																		.set(Calendar.YEAR,
																				year);
																localTime
																		.set(Calendar.MONTH,
																				monthOfYear);
																localTime
																		.set(Calendar.DAY_OF_MONTH,
																				dayOfMonth);

																not.time = localTime
																		.getTimeInMillis();
																notTimeButton
																		.setText(not
																				.getLocalDateTimeText(getActivity()));

																not.save(
																		getActivity(),
																		true);
															}
														});
												datePicker.show(
														getFragmentManager(),
														"date");
											}

										});
								timePicker.show(getFragmentManager(), "time");
							}
						}
					});

			notificationList.addView(nv);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Hide data from snoopers
		if (mTask != null && isLocked()) {
			fillUIFromTask();
		}
	}

	//@Override
	public void onTimeTravel(Intent data) {
		if (taskText != null) {
			taskText.setText(data
					.getStringExtra(ActivityTaskHistory.RESULT_TEXT_KEY));
		}
		// Need to set here also for password to work
		if (mTask != null) {
			mTask.setText(data
					.getStringExtra(ActivityTaskHistory.RESULT_TEXT_KEY));
		}
	}
}
