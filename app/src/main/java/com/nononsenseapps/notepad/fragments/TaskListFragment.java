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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter.ViewBinder;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.interfaces.MenuStateController;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.ui.DateView;
import com.nononsenseapps.ui.NoteCheckBox;
import com.nononsenseapps.helpers.PreferencesHelper;
import com.nononsenseapps.ui.TitleNoteTextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@EFragment(resName = "fragment_task_list")
public class TaskListFragment extends Fragment implements OnSharedPreferenceChangeListener {

	// Must be less than -1
	public static final String LIST_ALL_ID_PREF_KEY = "show_all_tasks_choice_id";
	public static final int LIST_ID_ALL = -2;
	public static final int LIST_ID_OVERDUE = -3;
	public static final int LIST_ID_TODAY = -4;
	public static final int LIST_ID_WEEK = -5;

	public static final String LIST_ID = "list_id";

	/**
	 * {@link android.R.id#list }
	 */
	@ViewById(resName = "list")
	DragSortListView listView;

	@SystemService
	LayoutInflater layoutInflater;

	SimpleSectionsAdapter mAdapter;
	private long mListId = -1;
	private OnFragmentInteractionListener mListener;
	private String mSortType = null;
	private int mRowCount = 3;
	private boolean mHideCheckbox = false;
	private String mListType = null;
	private LoaderCallbacks<Cursor> mCallback = null;
	private ActionMode mMode;
	private boolean mDeleteWasUndone = false;

	public static TaskListFragment_ getInstance(final long listId) {
		TaskListFragment_ f = new TaskListFragment_();
		Bundle args = new Bundle();
		args.putLong(LIST_ID, listId);
		f.setArguments(args);
		return f;
	}

	public TaskListFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		setHasOptionsMenu(true);

		if (getArguments().getLong(LIST_ID, -1) == -1) {
			throw new InvalidParameterException("Must designate a list to open!");
		}
		mListId = getArguments().getLong(LIST_ID, -1);

		// Start loading data
		mAdapter = new SimpleSectionsAdapter(getActivity(),
				R.layout.tasklist_item_rich,
				R.layout.tasklist_header,
				null,
				new String[] { Task.Columns.TITLE, Task.Columns.NOTE,
						Task.Columns.DUE, Task.Columns.COMPLETED,
						Task.Columns.LEFT, Task.Columns.RIGHT },
				new int[] { android.R.id.text1, android.R.id.text1, R.id.date,
						R.id.checkbox, R.id.drag_handle, R.id.dragpadding },
				0);

		// Set a drag listener
		mAdapter.setDropListener((from, to) -> {
			Log.d("nononsenseapps drag", "Position from " + from + " to " + to);

			final Task fromTask = new Task((Cursor) mAdapter.getItem(from));
			final Task toTask = new Task((Cursor) mAdapter.getItem(to));

			fromTask.moveTo(getActivity().getContentResolver(), toTask);
		});

		/*
		 * listAdapter.setRemoveListener(new RemoveListener() {
		 *
		 * @Override public void remove(int which) { Log.d(TAG, "Remove pos: " +
		 * which); Log.d(TAG, "Remove id: " + listAdapter.getItemId(which));
		 *
		 * getActivity().getContentResolver().delete(
		 * Uri.withAppendedPath(Task.URI, "" + listAdapter.getItemId(which)),
		 * null, null); }
		 *
		 * });
		 */

		mAdapter.setViewBinder(new ViewBinder() {
			final SimpleDateFormat weekdayFormatter = TimeFormatter
					.getLocalFormatterWeekday(getActivity());
			boolean isHeader = false;

			final String manualsort = getString(R.string.const_possubsort);
			final String notetype = getString(R.string.const_listtype_notes);
			String sTemp = "";
			final OnCheckedChangeListener checkBoxListener =
					(buttonView, isChecked) -> Task.setCompleted(
							getActivity(), isChecked, ((NoteCheckBox) buttonView).getNoteId());

			@Override
			public boolean setViewValue(View view, Cursor c, int colIndex) {
				// Check for headers, they have invalid ids
				isHeader = c.getLong(0) == -1;

				switch (colIndex) {
					// Matches order in Task.Columns.Fields
					case 1:
						// Title
						sTemp = c.getString(colIndex);
						if (isHeader) {
							if (Task.HEADER_KEY_OVERDUE.equals(sTemp)) {
								sTemp = getString(R.string.date_header_overdue);
							} else if (Task.HEADER_KEY_TODAY.equals(sTemp)) {
								sTemp = getString(R.string.date_header_today);
							} else if (Task.HEADER_KEY_PLUS1.equals(sTemp)) {
								sTemp = getString(R.string.date_header_tomorrow);
							} else if (Task.HEADER_KEY_PLUS2.equals(sTemp)
									|| Task.HEADER_KEY_PLUS3.equals(sTemp)
									|| Task.HEADER_KEY_PLUS4.equals(sTemp)) {
								sTemp = weekdayFormatter.format(new Date(c
										.getLong(4)));
							} else if (Task.HEADER_KEY_LATER.equals(sTemp)) {
								sTemp = getString(R.string.date_header_future);
							} else if (Task.HEADER_KEY_NODATE.equals(sTemp)) {
								sTemp = getString(R.string.date_header_none);
							} else if (Task.HEADER_KEY_COMPLETE.equals(sTemp)) {
								sTemp = getString(R.string.date_header_completed);
							}
						} else {
							// Set height of text for non-headers
							((TitleNoteTextView) view).setMaxLines(mRowCount);
							// if (mRowCount == 1) {
							// ((TitleNoteTextView) view).setSingleLine(true);
							// }
							// else {
							// ((TitleNoteTextView) view).setSingleLine(false);
							// }

							// Change color based on complete status
							((TitleNoteTextView) view).useSecondaryColor(!c
									.isNull(3));

						}
						((TitleNoteTextView) view).setTextTitle(sTemp);
						return true;
					case 2:
						// Note
						if (!isHeader) {
							// Only if task it not locked
							// or only one line
							if (c.getInt(9) != 1 && mRowCount > 1) {
								((TitleNoteTextView) view).setTextRest(c
										.getString(colIndex));
							} else {
								((TitleNoteTextView) view).setTextRest("");
							}
						}
						return true;
					case 3:
						// Checkbox
						if (!isHeader) {
							((NoteCheckBox) view).setOnCheckedChangeListener(null);
							((NoteCheckBox) view).setChecked(!c.isNull(colIndex));
							((NoteCheckBox) view).setNoteId(c.getLong(0));
							((NoteCheckBox) view)
									.setOnCheckedChangeListener(checkBoxListener);
							if (mHideCheckbox
									|| (mListType != null && mListType
									.equals(notetype))) {
								view.setVisibility(View.GONE);
							} else {
								view.setVisibility(View.VISIBLE);
							}
						}
						return true;
					case 4:
						// Due date
						if (!isHeader) {
							// Always hide for note type
							if (mListType != null && mListType.equals(notetype)) {
								view.setVisibility(View.GONE);
							}
							// Show for tasks if present
							else {
								if (c.isNull(colIndex)) {
									view.setVisibility(View.GONE);
								} else {
									view.setVisibility(View.VISIBLE);
									((DateView) view).setTimeText(c
											.getLong(colIndex));
								}
							}
						}
						return true;
					case 6:
						// left, handle
					case 7:
						// right, padding
						if (!isHeader) {
							if (mSortType != null && mSortType.equals(manualsort)) {
								view.setVisibility(View.VISIBLE);
							} else {
								view.setVisibility(View.GONE);
							}
						}
						return true;
					default:
						break;
				}
				return false;
			}
		});
	}

	@Override
	public void onActivityCreated(final Bundle state) {
		super.onActivityCreated(state);

		// Get the global list settings
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

		// Load pref for item height
		mRowCount = prefs.getInt(getString(R.string.key_pref_item_max_height), 3);
		mHideCheckbox = prefs.getBoolean(getString(R.string.pref_hidecheckboxes), false);

		mCallback = new LoaderCallbacks<>() {
			@NonNull
			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
				if (id == 0 /* LOADER_CURRENT_LIST */) {
					return new CursorLoader(getActivity(),
							TaskList.getUri(mListId), TaskList.Columns.FIELDS,
							null, null, null);
				}

				// id != 0 => load stuff

				// What sorting to use
				Uri targetUri;
				String sortSpec;
				if (mListType == null) {
					mListType = prefs.getString(
							getString(R.string.pref_listtype),
							getString(R.string.default_listtype));
				}

				if (mSortType == null) {
					mSortType = prefs.getString(
							getString(R.string.pref_sorttype),
							getString(R.string.default_sorttype));
				}
				if (mSortType.equals(getString(R.string.const_alphabetic))) {
					targetUri = Task.URI;
					sortSpec = getString(R.string.const_as_alphabetic,
							Task.Columns.TITLE);
				} else if (mSortType
						.equals(getString(R.string.const_duedate))) {
					targetUri = Task.URI_SECTIONED_BY_DATE;
					sortSpec = null;
				} else if (mSortType
						.equals(getString(R.string.const_modified))) {
					targetUri = Task.URI;
					sortSpec = Task.Columns.UPDATED + " DESC";
				}
				// manual sorting
				else {
					targetUri = Task.URI;
					sortSpec = Task.Columns.LEFT;
				}

				String where;
				String[] whereArgs;

				if (mListId > 0) {
					where = Task.Columns.DBLIST + " IS ?";
					whereArgs = new String[] { Long.toString(mListId) };
				} else {
					targetUri = Task.URI;
					sortSpec = Task.Columns.DUE;
					whereArgs = null;
					where = Task.Columns.COMPLETED + " IS NULL";
					switch ((int) mListId) {
						case LIST_ID_OVERDUE:
							where += andWhereOverdue();
							break;
						case LIST_ID_TODAY:
							where += andWhereToday();
							break;
						case LIST_ID_WEEK:
							where += andWhereWeek();
							break;
						case LIST_ID_ALL:
						default:
							// Show completed also in this case
							where = null;
							break;
					}
				}

				return new CursorLoader(getActivity(), targetUri,
						Task.Columns.FIELDS, where, whereArgs, sortSpec);
			}

			@Override
			public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor c) {
				if (loader.getId() == 0) {
					if (c != null && c.moveToFirst()) {
						final TaskList list = new TaskList(c);
						mSortType = list.sorting;
						mListType = list.listtype;
						// Reload tasks with new sorting
						getLoaderManager().restartLoader(1, null, this);
					}
				} else { // loader.getId() == LOADER_TASKS
					mAdapter.swapCursor(c);
				}
			}

			@Override
			public void onLoaderReset(@NonNull Loader<Cursor> loader) {
				if (loader.getId() == 0) {
					// Nothing to do
				} else {
					mAdapter.swapCursor(null);
				}
			}
		};

		if (mListId > 0) {
			getLoaderManager().restartLoader(0, null, mCallback);
		} else {
			// Setting sort types for all tasks always to due date
			mSortType = getString(R.string.const_duedate);
			getLoaderManager().restartLoader(1, null, mCallback);
		}
	}

	public static String whereOverDue() {
		return Task.Columns.DUE + " BETWEEN " + Task.OVERDUE + " AND "
				+ Task.TODAY_START;
	}

	public static String andWhereOverdue() {
		return " AND " + whereOverDue();
	}

	public static String whereToday() {
		return Task.Columns.DUE + " BETWEEN " + Task.TODAY_START + " AND "
				+ Task.TODAY_PLUS(1);
	}

	public static String andWhereToday() {
		return " AND " + whereToday();
	}

	public static String whereWeek() {
		return Task.Columns.DUE + " BETWEEN " + Task.TODAY_START + " AND ("
				+ Task.TODAY_PLUS(5) + " -1)";
	}

	public static String andWhereWeek() {
		return " AND " + whereWeek();
	}

	@AfterViews
	void setupPullToRefresh() {
		// every list gets its own instance of the swipetorefresh layout
		var ptrL = (SwipeRefreshLayout) this.getView().findViewById(R.id.ptrLayout);

		// The pull-to-refresh layout is defined in fragment_task_list.xml

		// now we add it to ActivityMain, which will take care of it
		((ActivityMain) getActivity()).addSwipeRefreshLayoutToList(ptrL);
	}

	@AfterViews
	void loadList() {
		listView.setAdapter(mAdapter);

		listView.setOnItemClickListener((arg0, origin, pos, id) -> {
			if (mListener != null && id > 0) {
				mListener.onFragmentInteraction(Task.getUri(id), mListId, origin);
			}
		});

		listView.setOnItemLongClickListener((arg0, view, pos, id) -> {
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			// Also select the item in question
			listView.setItemChecked(pos, true);
			return true;
		});

		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			final HashMap<Long, Task> tasks = new HashMap<>();

			/**
			 * Delete tasks and display a snackbar with an undo action
			 *
			 */
			private void deleteTasks(final Map<Long, Task> taskMap) {
				final Task[] tasks = taskMap.values().toArray(new Task[0]);

				// If any are locked, ask for password first
				final boolean locked = PreferencesHelper.isPasswordSet(getActivity());

				// Reset undo flag
				mDeleteWasUndone = false;

				// Dismiss callback
				final Snackbar.Callback dismissCallback = new Snackbar.Callback() {
					@Override
					public void onDismissed(Snackbar snackbar, int event) {
						// Do nothing if dismissed because action was pressed
						// Dismiss wil be called more than once if undo is pressed
						if (Snackbar.Callback.DISMISS_EVENT_ACTION != event && !mDeleteWasUndone) {
							// Delete them
							Executors.newSingleThreadExecutor().execute(() -> {
								// Background work here
								for (Task t : tasks) {
									try {
										t.delete(getActivity());
									} catch (Exception ignored) {}
								}
							});
						}
					}
				};

			}

			// Undo callback
			final View.OnClickListener undoListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mDeleteWasUndone = true;
					// Returns removed items to view
					mAdapter.reset();
				}
			};

			final PasswordConfirmedListener pListener = new PasswordConfirmedListener() {
				@Override
				@Background
				public void onPasswordConfirmed() {
					for (final Task t : tasks.values()) {
						try {
							t.delete(getActivity());
						} catch (Exception e) {
							NnnLogger.warning(TaskListFragment.class, "Can't delete task");
						}
					}

					String msg;
					try {
						msg = getResources()
								.getQuantityString(R.plurals.notedeleted_msg, tasks.size(), tasks.size());

					} catch (Exception e) {
						// Protect against faulty translations
						msg = getResources().getString(R.string.deleted);
					}

					// TODO should use a Snackbar instead of Toasts
					// Snackbar.make(mFab, msg, Snackbar.LENGTH_LONG).setAction(R.string.undo, listener).setCallback(dismissCallback).show();
					Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();

					if (mMode != null) mMode.finish();
				}
			};

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// Here you can perform updates to the CAB due to
				// an invalidate() request
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// Here you can make any necessary updates to the activity when
				// the CAB is removed. By default, selected items are
				// deselected/unchecked.
				tasks.clear();
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// Must setup the contextual action menu
				final MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.fragment_tasklist_context, menu);

				// Must clear for reuse
				tasks.clear();

				// For password
				mMode = mode;

				return true;
			}

			@Override
			public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
				// Respond to clicks on the actions in the CAB
				boolean finish = false;
				int itemId = item.getItemId();
				if (itemId == R.id.menu_copy) {
					final ClipboardManager clipboard = (ClipboardManager) getActivity()
							.getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setPrimaryClip(ClipData.newPlainText(
							getString(R.string.app_name_short), getShareText()));
					try {
						Toast.makeText(getActivity(), getResources().getQuantityString(
										R.plurals.notecopied_msg, tasks.size(),
										tasks.size()),
								Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						// Protect against faulty translations
					}
					finish = true;
				} else if (itemId == R.id.menu_delete) {
					boolean locked = false;
					for (final Task t : tasks.values()) {
						if (t.locked) {
							locked = true;
							break;
						}
					}
					if (locked) {
						DialogPassword_ delpf = new DialogPassword_();
						delpf.setListener(pListener);
						delpf.show(getFragmentManager(), "multi_delete_verify");
					} else {
						DialogDeleteTask.showDialog(getFragmentManager(), -1,
								pListener::onPasswordConfirmed);
					}
				} else if (itemId == R.id.menu_switch_list) {
					// show move to list dialog
					DialogMoveToList.getInstance(
									tasks.keySet().toArray(new Long[0]))
							.show(getFragmentManager(), "move_to_list_dialog");
					finish = true;
				} else if (itemId == R.id.menu_share) {
					startActivity(getShareIntent());
					finish = true;
				} else {
					finish = false;
				}

				if (finish) mode.finish(); // Action picked, so close the CAB
				return finish;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
												  int position, long id, boolean checked) {
				if (checked) {
					tasks.put(id, new Task((Cursor) listView.getAdapter()
							.getItem(position)));
				} else {
					tasks.remove(id);
				}

				try {
					// Only show the title string on screens that are wide
					// enough
					// E.g. large screens or if you are in landscape
					final Configuration conf = getResources()
							.getConfiguration();
					if (conf.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
							|| conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
						mode.setTitle(getResources().getQuantityString(
								R.plurals.mode_choose, tasks.size(),
								tasks.size()));
					}
				} catch (Exception e) {
					// Protect against faulty translations
				}
			}

			String getShareText() {
				final StringBuilder sb = new StringBuilder();
				for (Task t : tasks.values()) {
					if (sb.length() > 0) {
						sb.append("\n\n");
					}
					if (t.locked) {
						sb.append(t.title);
					} else {
						sb.append(t.getText());
					}
				}
				return sb.toString();
			}

			String getShareSubject() {
				String result = "";
				for (Task t : tasks.values()) {
					result += ", " + t.title;
				}
				return result.length() > 0 ? result.substring(2) : result;
			}

			Intent getShareIntent() {
				final Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
				shareIntent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject());
				shareIntent
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				return shareIntent;
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_tasklist, menu);
	}

	@Override
	public void onPrepareOptionsMenu(@NonNull Menu menu) {
		if (getActivity() instanceof MenuStateController) {
			final boolean visible = ((MenuStateController) getActivity())
					.childItemsVisible();

			menu.setGroupVisible(R.id.list_menu_group, visible);
			if (!visible) {
				if (mMode != null) mMode.finish();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_add) {
			if (mListener != null && mListId > 0) {
				mListener.addTaskInList("", mListId);
			} else if (mListener != null) {
				mListener.addTaskInList("", TaskListViewPagerFragment
						.getARealList(getActivity(), -1));
			}
			return true;
		} else if (itemId == R.id.menu_clearcompleted) {
			if (mListId != -1) {
				DialogDeleteCompletedTasks.showDialog(getFragmentManager(), mListId, null);
			}
			return true;
		} else if (itemId == R.id.menu_sort_title) {
			// TODO reorder the notes like we do in DialogEditList
			Toast.makeText(this.getContext(), R.string.feature_is_WIP, Toast.LENGTH_SHORT).show();
			// SharedPreferencesHelper.setSortingAlphabetic(this);
			return true;
		} else if (itemId == R.id.menu_sort_due) {
			Toast.makeText(this.getContext(), R.string.feature_is_WIP, Toast.LENGTH_SHORT).show();
			// SharedPreferencesHelper.setSortingDue(this);
			return true;
		} else if (itemId == R.id.menu_sort_manual) {
			Toast.makeText(this.getContext(), R.string.feature_is_WIP, Toast.LENGTH_SHORT).show();
			// SharedPreferencesHelper.setSortingManual(this);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getLoaderManager().destroyLoader(0);
	}

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			// the activity must implement OnFragmentInteractionListener!
		}

		// We want to be notified of future changes to auto refresh
		PreferenceManager.getDefaultSharedPreferences(getActivity())
				.registerOnSharedPreferenceChangeListener(this);
	}


	@Override
	public void onDetach() {
		mListener = null;
		PreferenceManager.getDefaultSharedPreferences(getActivity())
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDetach();
	}

	static class SimpleSectionsAdapter extends SimpleDragSortCursorAdapter {
		DropListener dropListener = null;
		RemoveListener removeListener = null;
		final int mItemLayout;
		final int mHeaderLayout;
		final static int itemType = 0;
		final static int headerType = 1;
		final SharedPreferences prefs;
		final Context context;

		public SimpleSectionsAdapter(Context context, int layout,
									 int headerLayout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			this.context = context;
			mItemLayout = layout;
			mHeaderLayout = headerLayout;
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
		}

		int getViewLayout(final int position) {
			if (itemType == getItemViewType(position)) {
				return mItemLayout;
			} else {
				return mHeaderLayout;
			}
		}

		@Override
		public void remove(int which) {
			if (removeListener != null) removeListener.remove(which);
			super.remove(which);

		}

		@Override
		public void drop(int from, int to) {
			// Call any listener that has been defined
			if (dropListener != null) dropListener.drop(from, to);
			// Call super to handle UI mapping (for smoothness)
			super.drop(from, to);
		}

		public void setDropListener(DropListener dropListener) {
			this.dropListener = dropListener;
		}

		public void setRemoveListener(RemoveListener removeListener) {
			this.removeListener = removeListener;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			final Cursor c = (Cursor) getItem(position);
			// If the id is invalid, it's a header
			if (c.getLong(0) < 1) {
				return headerType;
			} else {
				return itemType;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				final LayoutInflater inflater = LayoutInflater.from(this.context);
				convertView = inflater.inflate(getViewLayout(position), parent,
						false);
				if (itemType == getItemViewType(position)) {
					setPrefsOnView(convertView.findViewById(android.R.id.text1));
				}
			}
			return super.getView(position, convertView, parent);
		}

		private void setPrefsOnView(final TitleNoteTextView view) {
			String fontPref1 = prefs.getString(context.getString(R.string.pref_list_title_fontfamily), "1");
			view.setTitleFontFamily(Integer.parseInt(fontPref1));

			String fontPref2 = prefs.getString(context.getString(R.string.pref_list_title_fontstyle), "1");
			view.setTitleFontStyle(Integer.parseInt(fontPref2));

			String fontPref3 = prefs.getString(context.getString(R.string.pref_list_body_fontfamily), "0");
			view.setBodyFontFamily(Integer.parseInt(fontPref3));

			boolean shouldShowLinks = prefs.getBoolean(context.getString(R.string.pref_list_links), true);
			view.setLinkify(shouldShowLinks);

			String fontPref4 = prefs.getString(context.getString(R.string.pref_list_fontsize), "1");
			view.setTheTextSize(Integer.parseInt(fontPref4));
		}

	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
		if (isDetached()) {
			// Fix crash report
			return;
		}
		if (key == null) {
			// it happens sometimes during Espresso tests
			return;
		}
		try {
			boolean reload = false;
			if (key.equals(getString(R.string.pref_sorttype))) {
				mSortType = null;
				reload = true;
			} else if (key.equals(getString(R.string.key_pref_item_max_height))) {
				mRowCount = prefs.getInt(key, 3);
				reload = true;
			} else if (key.equals(getString(R.string.pref_hidecheckboxes))) {
				mHideCheckbox = prefs.getBoolean(key, false);
				reload = true;
			} else if (key.equals(getString(R.string.pref_listtype))) {
				mListType = null;
				reload = true;
			}

			if (reload && mCallback != null) {
				getLoaderManager().restartLoader(0, null, mCallback);
			}
		} catch (IllegalStateException ignored) {
			// Fix crash report
			// Might get a race condition where fragment is detached when getString is called
		}
	}
}
