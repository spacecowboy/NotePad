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

import android.app.Activity;
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
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener;
import com.android.datetimepicker.time.TimePickerDialog;
import com.github.espiandev.showcaseview.ShowcaseView;
import com.github.espiandev.showcaseview.ShowcaseViews;
import com.github.espiandev.showcaseview.ShowcaseViews.ItemViewProperties;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.ActivityTaskHistory;
import com.nononsenseapps.notepad.ActivityTaskHistory_;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.R.layout;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase.DialogConfirmedListener;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.interfaces.MenuStateController;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.ui.NotificationItemHelper;
import com.nononsenseapps.utils.views.StyledEditText;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;
import org.androidannotations.annotations.ViewById;

import java.util.Calendar;

/**
 * A fragment representing a single Note detail screen.
 */
@EFragment
public class TaskDetailFragment extends Fragment implements OnDateSetListener {

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
			} else if (LOADER_EDITOR_TASK == id) {
				return new CursorLoader(getActivity(), Task.getUri(args
						.getLong(ARG_ITEM_ID, -1)), Task.Columns.FIELDS, null,
						null, null);
			} else if (LOADER_EDITOR_TASKLISTS == id) {
				return new CursorLoader(getActivity(), TaskList.getUri(args
						.getLong(ARG_ITEM_LIST_ID)), TaskList.Columns.FIELDS,
						null, null, null);
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

					args.clear();
					args.putLong(ARG_ITEM_ID,
							getArguments().getLong(ARG_ITEM_ID, stateId));
					getLoaderManager().restartLoader(
							LOADER_EDITOR_NOTIFICATIONS, args, loaderCallbacks);
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

	@ViewById(resName = "taskText")
	StyledEditText taskText;

	@ViewById(resName = "taskCompleted")
	CheckBox taskCompleted;

	@ViewById(resName = "dueDateBox")
	Button dueDateBox;

	@ViewById(resName = "notificationList")
	LinearLayout notificationList;

	@ViewById(resName = "taskSection")
	View taskSection;

	@ViewById(resName = "editScrollView")
	ScrollView editScrollView;

	@SystemService
	InputMethodManager inputManager;

	// Id of task to open
	public static final String ARG_ITEM_ID = "item_id";
	// If no id is given, a string can be accepted as initial state
	public static final String ARG_ITEM_CONTENT = "item_text";
	// A list id is necessary
	public static final String ARG_ITEM_LIST_ID = "item_list_id";
	private static final String SHOWCASED_EDITOR = "showcased_editor_window";
	// Random identifier
	private static final String DATE_DIALOG_TAG = "date_9374jf893jd893jt";

	// To override intent values with
	@InstanceState
	long stateId = -1;
	@InstanceState
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

	// This is the notification we are setting a location for
	private Notification pendingLocationNotification = null;

	private OnFragmentInteractionListener mListener;
	private ShareActionProvider mShareActionProvider;

	/*
	 * If in tablet and added, rotating to portrait actually recreats the
	 * fragment even though it isn't visible. So if this is true, don't load
	 * anything.
	 */
	private boolean dontLoad = false;

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

	// @Override
	// public void onCreate(Bundle savedInstanceState) {
	// super.onCreate(savedInstanceState);
	//
	// }

	/**
	 * Must handle this manually because annotations do not return null if
	 * container is null
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if (container == null) {
			dontLoad = true;
			return null;
		}
		setHasOptionsMenu(true);
		return inflater.inflate(layout.fragment_task_detail, container, false);
	}

	@Override
	public void onActivityCreated(final Bundle state) {
		super.onActivityCreated(state);

		if (dontLoad) {
			return;
		}

		boolean openKb = false;

		final Bundle args = new Bundle();
		if (getArguments().getLong(ARG_ITEM_ID, stateId) > 0) {
			// Load data from database
			args.putLong(ARG_ITEM_ID,
					getArguments().getLong(ARG_ITEM_ID, stateId));
			getLoaderManager().restartLoader(LOADER_EDITOR_TASK, args,
					loaderCallbacks);
		} else {
			// If not valid, find a valid list
			if (getArguments().getLong(ARG_ITEM_LIST_ID, stateListId) < 1) {
				getArguments().putLong(
						ARG_ITEM_LIST_ID,
						TaskListViewPagerFragment.getARealList(getActivity(),
								-1));
			}
			// Fail if still not valid
			if (getArguments().getLong(ARG_ITEM_LIST_ID, stateListId) < 1) {
				// throw new InvalidParameterException(
				// "Must specify a list id to create a note in!");
				Toast.makeText(getActivity(),
						"Must specify a list id to create a note in!",
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}
			args.putLong(ARG_ITEM_LIST_ID,
					getArguments().getLong(ARG_ITEM_LIST_ID, stateListId));
			getLoaderManager().restartLoader(LOADER_EDITOR_TASKLISTS, args,
					loaderCallbacks);

			openKb = true;
			mTaskOrg = new Task();
			mTask = new Task();
			mTask.dblist = getArguments().getLong(ARG_ITEM_LIST_ID);
			// New note but start with the text given
			mTask.setText(getArguments().getString(ARG_ITEM_CONTENT, ""));
			fillUIFromTask();
		}

		// showcase first time
		final boolean showcasing = showcaseEditor();

		if (!showcasing && openKb) {
			/**
			 * Only show keyboard for new/empty notes But not if the showcase
			 * view is showing
			 */
			taskText.requestFocus();
			inputManager.showSoftInput(taskText,
					InputMethodManager.SHOW_IMPLICIT);
		}
	}

	/**
	 * Returns true if showcase window is visible
	 */
	boolean showcaseEditor() {
		final boolean alreadyShowcased = PreferenceManager
				.getDefaultSharedPreferences(getActivity()).getBoolean(
						SHOWCASED_EDITOR, false);

		if (alreadyShowcased) {
			return false;
		}

		ShowcaseViews views = new ShowcaseViews(getActivity(),
				R.layout.showcase_view_template);
		views.addView(new ItemViewProperties(ItemViewProperties.ID_OVERFLOW,
				R.string.showcase_timemachine_title,
				R.string.showcase_timemachine_msg,
				ShowcaseView.ITEM_ACTION_OVERFLOW, 0.5f));
		views.show();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putBoolean(SHOWCASED_EDITOR, true).commit();
		return true;
	}

	@AfterViews
	void setListeners() {
		if (dontLoad) {
			return;
		}

		// Set chosen attributes on the text field
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

		taskText.setTitleRelativeLarger(prefs.getBoolean(
				getString(R.string.pref_editor_biggertitles), true));
		taskText.setTitleFontFamily(Integer.parseInt(prefs.getString(
				getString(R.string.pref_editor_title_fontfamily), "2")));
		taskText.setTitleFontStyle(Integer.parseInt(prefs.getString(
				getString(R.string.pref_editor_title_fontstyle), "0")));
		taskText.setBodyFontFamily(Integer.parseInt(prefs.getString(
				getString(R.string.pref_editor_body_fontfamily), "0")));
		taskText.setLinkify(prefs.getBoolean(
				getString(R.string.pref_editor_links), true));
		taskText.setTheTextSize(Integer.parseInt(prefs.getString(
				getString(R.string.pref_editor_fontsize), "1")));

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

	@Click(resName = "dueDateBox")
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
		final DatePickerDialog datedialog = DatePickerDialog
				.newInstance(this, localTime.get(Calendar.YEAR), localTime
						.get(Calendar.MONTH), localTime
						.get(Calendar.DAY_OF_MONTH));
		datedialog.show(getFragmentManager(), DATE_DIALOG_TAG);
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

	@Override
	public void onDateSet(DatePickerDialog dialog,
						  int year, int monthOfYear,
						  int dayOfMonth) {
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

	// @Override
	// public void onDateTimeSet(final long time) {
	// mTask.due = time;
	// setDueText();
	// }

	private void setDueText() {
		if (mTask.due == null) {
			dueDateBox.setText("");
		} else {
			// Due date
			dueDateBox.setText(TimeFormatter.getLocalDateOnlyStringLong(
					getActivity(), mTask.due));
		}
	}

	@Click(resName = "dueCancelButton")
	void onDueRemoveClick() {
		if (!isLocked()) {
			if (mTask != null) {
				mTask.due = null;
			}
			setDueText();
		}
	}

	@Click(resName = "notificationAdd")
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

	/**
	 * task.locked & mLocked
	 */
	public boolean isLocked() {
		if (mTask != null) {
			return mTask.locked & mLocked;
		}
		return false;
	}

	@UiThread(propagation = Propagation.ENQUEUE)
	void fillUIFromTask() {
		Log.d("nononsenseapps editor", "fillUI, act: " + getActivity());
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
		} else {
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
		} else {
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
			int titleEnd = text.indexOf("\n");
			if (titleEnd < 0) {
				titleEnd = text.length();
			}

			try {
				mShareActionProvider.setShareIntent(
						new Intent(Intent.ACTION_SEND).setType("text/plain")
								.putExtra(Intent.EXTRA_TEXT, text)
								.putExtra(Intent.EXTRA_SUBJECT,
										text.substring(0, titleEnd)));
			} catch (RuntimeException e) {
				// Can crash when too many transactions overflow the buffer
				Log.d("nononsensenotes", e.getLocalizedMessage());
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_add) {
			// TODO should not call if in tablet mode
			if (mListener != null && mTask != null && mTask.dblist > 0) {
				mListener.addTaskInList("", mTask.dblist);
			}
			return true;
		} else if (itemId == R.id.menu_revert) {
			// set to null to prevent modifications
			mTask = null;
			// Request a close from activity
			if (mListener != null) {
				mListener.closeFragment(this);
			}
			return true;
		} else if (itemId == R.id.menu_timemachine) {
			if (mTask != null && mTask._id > 0) {
				Intent timeIntent = new Intent(getActivity(),
						ActivityTaskHistory_.class);
				timeIntent.putExtra(Task.Columns._ID, mTask._id);
				startActivityForResult(timeIntent, 1);
				// ActivityTaskHistory.start(getActivity(), mTask._id);
			}
			return true;
		} else if (itemId == R.id.menu_delete) {
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
				} else {
					deleteAndClose();
				}
			}
			return true;
		} else if (itemId == R.id.menu_lock) {
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
		} else if (itemId == R.id.menu_unlock) {
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

		if (getActivity() instanceof MenuStateController) {
			final boolean visible = ((MenuStateController) getActivity())
					.childItemsVisible();

			menu.setGroupVisible(R.id.editor_menu_group, visible);
			// Outside group to allow for action bar placement
			if (menu.findItem(R.id.menu_delete) != null)
				menu.findItem(R.id.menu_delete).setVisible(visible);
			if (menu.findItem(R.id.menu_revert) != null)
				menu.findItem(R.id.menu_revert).setVisible(visible);
			if (menu.findItem(R.id.menu_share) != null)
				menu.findItem(R.id.menu_share).setVisible(visible);
			if (menu.findItem(R.id.menu_lock) != null)
				menu.findItem(R.id.menu_lock).setVisible(
						visible && mTask != null && !mTask.locked);
			if (menu.findItem(R.id.menu_unlock) != null)
				menu.findItem(R.id.menu_unlock).setVisible(
						visible && mTask != null && mTask.locked);
		}
	}

	@OnActivityResult(1)
	void onTimeTravelResult(int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			onTimeTravel(data);
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
		} else {
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
		stateId = mTask._id;
		stateListId = mTask.dblist;

		if (getActivity() == null)
			return;

		final Intent orgIntent = getActivity().getIntent();
		if (orgIntent == null || orgIntent.getAction() == null
				|| !orgIntent.getAction().equals(Intent.ACTION_INSERT))
			return;

		if (mTask == null || mTask._id < 1)
			return;

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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (dontLoad) {
			return;
		}
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
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
		super.onSaveInstanceState(state);
	}

	/**
	 * Inserts a notification item in the UI
	 *
	 * @param not
	 */
	@UiThread(propagation = Propagation.REUSE)
	void addNotification(final Notification not) {
		if (getActivity() != null) {
			View nv = LayoutInflater.from(getActivity()).inflate(
					R.layout.notification_view, null);

			// So we can update the view later
			not.view = nv;

			// Setup all the listeners etc
			NotificationItemHelper
					.setup(this, notificationList, nv, not, mTask);

			notificationList.addView(nv);
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
		Fragment dateDialog = getFragmentManager().findFragmentByTag(
				DATE_DIALOG_TAG);
		if (dateDialog != null) {
			((DatePickerDialog) dateDialog).setOnDateSetListener(this);
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
		final String theme = PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getString(MainPrefs.KEY_THEME,
				getString(R.string.const_theme_light_ab));

		final TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
				null, 0, 0,
				android.text.format.DateFormat.is24HourFormat(getActivity()));
		timePickerDialog.setThemeDark(!theme.contains("light"));
		return timePickerDialog;
	}

	public Notification getPendingLocationNotification() {
		return pendingLocationNotification;
	}

	public void setPendingLocationNotification(
			Notification pendingLocationNotification) {
		this.pendingLocationNotification = pendingLocationNotification;
	}
}
