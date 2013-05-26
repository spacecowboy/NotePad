package com.nononsenseapps.notepad.fragments;

import java.util.Calendar;

import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment;
import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment.TimePickerDialogHandler;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.OnActivityResult;
import com.googlecode.androidannotations.annotations.SystemService;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.ActivityLocation;
import com.nononsenseapps.notepad.ActivityLocation_;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.ActivityTaskHistory;
import com.nononsenseapps.notepad.ActivityTaskHistory_;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogCalendar.DateSetListener;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase.DialogConfirmedListener;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.ui.WeekDaysView;
import com.nononsenseapps.utils.views.StyledEditText;
import com.nononsenseapps.ui.WeekDaysView.onCheckedDaysChangeListener;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A fragment representing a single Note detail screen. This fragment is either
 * contained in a {@link TaskListActivity} in two-pane mode (on tablets) or a
 * {@link TaskDetailActivity} on handsets.
 */
@EFragment(R.layout.fragment_task_detail)
public class TaskDetailFragment extends Fragment implements
		TimePickerDialogHandler, DateSetListener {

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
				if (c.moveToFirst()) {
					if (mTask == null) {
						mTask = new Task(c);
						if (mTaskOrg == null) {
							mTaskOrg = new Task(c);
						}
						fillUIFromTask();
						// Don't want updates while editing
						// getLoaderManager().destroyLoader(LOADER_EDITOR_TASK);
					}
					else {
						// Don't want updates while editing
						// getLoaderManager().destroyLoader(LOADER_EDITOR_TASK);
						// Only update the list if that changes
						Log.d("nononsenseapps listedit",
								"Updating list in task from " + mTask.dblist);
						mTask.dblist = new Task(c).dblist;
						Log.d("nononsenseapps listedit",
								"Updating list in task to " + mTask.dblist);
						if (mTaskOrg != null) {
							mTaskOrg.dblist = mTask.dblist;
						}
					}
					// Load the list to see if we should hide task bits
					Bundle args = new Bundle();
					args.putLong(ARG_ITEM_LIST_ID, mTask.dblist);
					getLoaderManager().restartLoader(LOADER_EDITOR_TASKLISTS,
							args, this);
				}
				else {
					// Should kill myself maybe?
				}
			}
			else if (LOADER_EDITOR_NOTIFICATIONS == ldr.getId()) {
				while (c.moveToNext()) {
					addNotification(new Notification(c));
				}
				// Don't update while editing
				// TODO this allows updating of the location name etc
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

	@SystemService
	InputMethodManager inputManager;

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

	// This is the notification we are setting a location for
	private Notification pendingLocationNotification = null;

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
		final Calendar localTime = Calendar.getInstance();
		final DialogCalendar datePicker = DialogCalendar.getInstance();
		datePicker.setListener(this);
		datePicker.show(getFragmentManager(), "date");
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
	}

	@Override
	public void onDialogTimeCancel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDateSet(final long time) {
		final Calendar localTime = Calendar.getInstance();
		localTime.setTimeInMillis(time);

		// set to 23:59 to be more or less consistent with earlier date only
		// implementation
		localTime.set(Calendar.HOUR_OF_DAY, 23);
		localTime.set(Calendar.MINUTE, 59);

		mTask.due = localTime.getTimeInMillis();
		setDueText();

		// and ask for time as well
		final TimePickerDialogFragment picker = getTimePickerFragment();
		picker.setListener(this);
		picker.show(getFragmentManager(), "time");
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
			if (mTask != null) {
				mTask.due = null;
			}
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
			// TODO remove this, no need to default
			// final Calendar local = Calendar.getInstance();
			// local.add(Calendar.MINUTE, 1);
			// final long soon = local.getTimeInMillis();
			// if (mTask.due == null || mTask.due < soon) {
			// not.time = soon;
			// }
			// else {
			// not.time = mTask.due;
			// }
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

		// Open keyboard on new notes so users can start typing directly
		// need small delay (100ms) for it to open consistently
		// TODO
		// if (mTask._id < 1) {
		// (new Handler()).postDelayed(new Runnable() {
		// public void run() {
		// MotionEvent e = MotionEvent.obtain(
		// SystemClock.uptimeMillis(),
		// SystemClock.uptimeMillis(),
		// MotionEvent.ACTION_DOWN, 0, 0, 0);
		// taskText.dispatchTouchEvent(e);
		// e.recycle();
		// e = MotionEvent.obtain(SystemClock.uptimeMillis(),
		// SystemClock.uptimeMillis(), MotionEvent.ACTION_UP,
		// 0, 0, 0);
		// taskText.dispatchTouchEvent(e);
		// e.recycle();
		// }
		// }, 100);
		// }

		/**
		 * Only show keyboard for new/empty notes
		 */
		if (taskText.getText().length() == 0) {
			taskText.requestFocus();
			inputManager.showSoftInput(taskText,
					InputMethodManager.SHOW_IMPLICIT);
		}
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
			mShareActionProvider = (ShareActionProvider) item
					.getActionProvider();
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
				Intent timeIntent = new Intent(getActivity(),
						ActivityTaskHistory_.class);
				timeIntent.putExtra(Task.Columns._ID, mTask._id);
				startActivityForResult(timeIntent, 1);
				// ActivityTaskHistory.start(getActivity(), mTask._id);
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
						mTask.save(getActivity());
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

	@OnActivityResult(1)
	void onTimeTravelResult(int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			onTimeTravel(data);
		}
	}

	@OnActivityResult(2)
	public void onLocationResult(int resultCode, Intent data) {
		// Location
		if (resultCode == Activity.RESULT_OK
				&& pendingLocationNotification != null) {
			// update text field and shit
			pendingLocationNotification.latitude = data.getExtras().getDouble(
					ActivityLocation.EXTRA_LATITUDE);
			pendingLocationNotification.longitude = data.getExtras().getDouble(
					ActivityLocation.EXTRA_LONGITUDE);
			pendingLocationNotification.radius = (double) data.getExtras()
					.getInt(ActivityLocation.EXTRA_RADIUS);
			pendingLocationNotification.locationName = data.getExtras()
					.getString(ActivityLocation.EXTRA_LOCATION_NAME);
			if (pendingLocationNotification.view != null
					&& pendingLocationNotification.locationName != null) {
				pendingLocationNotification.view.findViewById(
				// Hide time parts
						R.id.notificationDateTime).setVisibility(View.GONE);
				pendingLocationNotification.view.findViewById(R.id.weekdays)
						.setVisibility(View.GONE);
				// Show location reminder
				pendingLocationNotification.view
						.findViewById(R.id.repeatSwitch).setVisibility(
								View.VISIBLE);
				// Show repeat section
				pendingLocationNotification.view.findViewById(
						R.id.openRepeatField).setVisibility(View.VISIBLE);

				// Fill in location name
				((TextView) pendingLocationNotification.view
						.findViewById(R.id.notificationLocation))
						.setText(pendingLocationNotification.locationName);
			}
			// do in background
			pendingLocationNotification.saveInBackground(getActivity(), false);
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
				// Set the intent to open the task.
				// So we dont create a new one on rotation for example
				fixIntent();

				// TODO, should restart notification loader for new tasks
			}
		}
	}

	void fixIntent() {
		if (getActivity() == null) return;

		final Intent orgIntent = getActivity().getIntent();
		if (orgIntent == null || orgIntent.getAction() == null
				|| !orgIntent.getAction().equals(Intent.ACTION_INSERT)) return;

		if (mTask == null || mTask._id < 1) return;

		final Intent intent = new Intent().setAction(Intent.ACTION_EDIT)
				.setClass(getActivity(), ActivityMain_.class)
				.setData(mTask.getUri())
				.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, mTask.dblist);

		getActivity().setIntent(intent);
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
			View nv;// = notificationList.findViewById((int) not._id);
			// if (nv == null) {
			nv = LayoutInflater.from(getActivity()).inflate(
					R.layout.notification_view, null);
			// nv.setId((int) not._id);
			// }
			// else {
			// notificationList.removeView(nv);
			// }

			// So we can update the view later
			not.view = nv;

			// Hide this if it's a new notification
			final TextView openRepeatField = (TextView) nv
					.findViewById(R.id.openRepeatField);

			// Set date time text
			final TextView notTimeButton = (TextView) nv
					.findViewById(R.id.notificationDateTime);
			if (not.time != null) {
				notTimeButton.setText(not.getLocalDateTimeText(getActivity()));
			}

			if (not.radius != null) {
				notTimeButton.setVisibility(View.GONE);
				openRepeatField.setVisibility(View.VISIBLE);
			}

			final View notRemoveButton = nv
					.findViewById(R.id.notificationRemove);

			// Remove button
			notRemoveButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (!isLocked()) {
						// Remove row from UI
						notificationList.removeView((View) v.getParent());
						// Remove from database and renotify
						not.delete(getActivity());
					}
				}
			});

			// Location button
			final TextView location = (TextView) nv
					.findViewById(R.id.notificationLocation);
			if (not.locationName != null) location.setText(not.locationName);

			if (not.time != null && not.radius == null) {
				location.setVisibility(View.GONE);
			}

			location.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO
					pendingLocationNotification = not;

					Intent i = new Intent(getActivity(),
							ActivityLocation_.class);
					i.putExtra(ActivityLocation.EXTRA_ID, not._id);
					if (not.latitude != null && not.longitude != null
							&& not.radius != null) {
						i.putExtra(ActivityLocation.EXTRA_LATITUDE,
								not.latitude)
								.putExtra(ActivityLocation.EXTRA_LONGITUDE,
										not.longitude)
								.putExtra(ActivityLocation.EXTRA_RADIUS,
										not.radius);
					}
					startActivityForResult(i, 2);
				}
			});

			final View closeRepeatField = nv
					.findViewById(R.id.closeRepeatField);
			final View repeatDetails = nv.findViewById(R.id.repeatDetails);
			final View weekDays = nv.findViewById(R.id.weekdays);

			// Location repeat
			final Switch repeatSwitch = (Switch) nv
					.findViewById(R.id.repeatSwitch);
			repeatSwitch.setChecked(not.isLocationRepeat());
			if (not.isLocationRepeat()) {
				openRepeatField.setText(R.string.always);
			}
			else {
				openRepeatField.setText(not.getRepeatAsText(getActivity()));
			}
			repeatSwitch
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							not.setLocationRepeat(isChecked);
							not.saveInBackground(getActivity(), true);
							if (isChecked) {
								openRepeatField.setText(R.string.always);
							}
							else {
								openRepeatField.setText("");
							}

						}
					});

			if (not.time != null && not.radius == null) {
				openRepeatField.setVisibility(View.VISIBLE);
				weekDays.setVisibility(View.VISIBLE);
				repeatSwitch.setVisibility(View.GONE);
			}
			else {
				weekDays.setVisibility(View.GONE);
				repeatSwitch.setVisibility(View.VISIBLE);
			}

			// Date button
			notTimeButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (!isLocked()) {
						final DialogCalendar datePicker;

						if (mTask != null && mTask.due != null) {
							datePicker = DialogCalendar.getInstance(mTask.due);
						}
						else {
							datePicker = DialogCalendar.getInstance();
						}
						datePicker.setListener(new DateSetListener() {

							@Override
							public void onDateSet(long time) {
								final Calendar localTime = Calendar
										.getInstance();
								localTime.setTimeInMillis(time);
								if (not.time != null) {
									final Calendar notTime = Calendar
											.getInstance();
									notTime.setTimeInMillis(not.time);
									localTime.set(Calendar.HOUR_OF_DAY,
											notTime.get(Calendar.HOUR_OF_DAY));
									localTime.set(Calendar.MINUTE,
											notTime.get(Calendar.MINUTE));
								}

								not.time = localTime.getTimeInMillis();

								// Enable repeat options
								openRepeatField.setVisibility(View.VISIBLE);
								weekDays.setVisibility(View.VISIBLE);
								repeatSwitch.setVisibility(View.GONE);
								// Fill in time so far
								notTimeButton.setText(not
										.getLocalDateTimeText(getActivity()));

								// Hide location
								location.setVisibility(View.GONE);

								// Now display time picker
								final TimePickerDialogFragment timePicker = getTimePickerFragment();
								timePicker
										.setListener(new TimePickerDialogHandler() {
											@Override
											public void onDialogTimeSet(
													int hourOfDay, int minute) {
												final Calendar localTime = Calendar
														.getInstance();
												if (not.time != null) {
													localTime
															.setTimeInMillis(not.time);
												}
												localTime.set(
														Calendar.HOUR_OF_DAY,
														hourOfDay);
												localTime.set(Calendar.MINUTE,
														minute);

												not.time = localTime
														.getTimeInMillis();

												// Fill in time so far
												notTimeButton.setText(not
														.getLocalDateTimeText(getActivity()));

												not.save(getActivity(), true);
											}

											@Override
											public void onDialogTimeCancel() {
												// TODO why is this not
												// scheduling?
												not.save(getActivity(), true);
											}

										});

								timePicker.show(getFragmentManager(), "time");
							}
						});

						datePicker.show(getFragmentManager(), "date");

					}
				}
			});

			openRepeatField.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.setVisibility(View.GONE);
					repeatDetails.setVisibility(View.VISIBLE);
				}
			});
			closeRepeatField.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					repeatDetails.setVisibility(View.GONE);
					openRepeatField.setVisibility(View.VISIBLE);
				}
			});

			WeekDaysView days = ((WeekDaysView) nv.findViewById(R.id.weekdays));
			days.setCheckedDays(not.repeats);
			days.setOnCheckedDaysChangedListener(new onCheckedDaysChangeListener() {

				@Override
				public void onChange(final long checkedDays) {
					not.repeats = checkedDays;
					openRepeatField.setText(not.getRepeatAsText(getActivity()));
					not.saveInBackground(getActivity(), true);
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

	// @Override
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

	/**
	 * Returns an appropriately themed time picker fragment
	 */
	TimePickerDialogFragment getTimePickerFragment() {
		final String theme = PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getString(MainPrefs.KEY_THEME,
				getString(R.string.const_theme_light_ab));
		if (theme.contains("light")) {
			return TimePickerDialogFragment
					.newInstance(R.style.BetterPickersDialogFragment_Light);
		}
		else {
			// dark
			return TimePickerDialogFragment
					.newInstance(R.style.BetterPickersDialogFragment);
		}
	}
}
