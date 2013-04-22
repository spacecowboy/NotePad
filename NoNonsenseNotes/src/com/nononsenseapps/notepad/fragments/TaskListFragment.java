package com.nononsenseapps.notepad.fragments;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;

import com.mobeta.android.dslv.DragSortListView.DragSortListener;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter.ViewBinder;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.ui.DateView;
import com.nononsenseapps.ui.NoteCheckBox;
import com.nononsenseapps.utils.views.TitleNoteTextView;

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
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

@EFragment(R.layout.fragment_task_list)
public class TaskListFragment extends Fragment implements
		OnSharedPreferenceChangeListener {

	public static final String LIST_ID = "list_id";

	// DragSortListView listView;
	@ViewById(android.R.id.list)
	DragSortListView listView;

	SimpleSectionsAdapter mAdapter;

	private long mListId = -1;

	private OnFragmentInteractionListener mListener;

	private String mSortType = null;

	private int mRowCount = 3;
	private boolean mHideCheckbox = false;

	private String mListType = null;

	private LoaderCallbacks<Cursor> mCallback = null;

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

		if (getArguments().getLong(LIST_ID, -1) < 1) {
			throw new InvalidParameterException(
					"Must designate a list to open!");
		}

		mListId = getArguments().getLong(LIST_ID, -1);

		// Start loading data
		mAdapter = new SimpleSectionsAdapter(getActivity(),
				R.layout.tasklist_item_rich, R.layout.tasklist_header, null,
				new String[] { Task.Columns.TITLE, Task.Columns.NOTE,
						Task.Columns.DUE, Task.Columns.COMPLETED,
						Task.Columns.LEFT, Task.Columns.RIGHT }, new int[] {
						android.R.id.text1, android.R.id.text1, R.id.date,
						R.id.checkbox, R.id.drag_handle, R.id.dragpadding }, 0);

		// Set a drag listener
		mAdapter.setDropListener(new DropListener() {
			@Override
			public void drop(int from, int to) {
				Log.d("nononsenseapps drag", "Position from " + from + " to "
						+ to);

				final Task fromTask = new Task((Cursor) mAdapter.getItem(from));
				final Task toTask = new Task((Cursor) mAdapter.getItem(to));

				fromTask.moveTo(getActivity().getContentResolver(), toTask);
			}
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
			SimpleDateFormat weekdayFormatter = TimeFormatter
					.getLocalFormatterWeekday(getActivity());
			boolean isHeader = false;
			final String manualsort = getString(R.string.const_possubsort);
			final String notetype = getString(R.string.const_listtype_notes);
			String sTemp = "";
			final OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					Task.setCompleted(getActivity(), isChecked,
							((NoteCheckBox) buttonView).getNoteId());
				}
			};

			@Override
			public boolean setViewValue(View view, Cursor c, int colIndex) {
				// Check for headers, they have invalid ids
				isHeader = c.getLong(0) == -1;

				switch (colIndex) {
				// Matches order in Task.Columns.Fields
				case 1:
					sTemp = c.getString(colIndex);
					if (isHeader) {
						if (Task.HEADER_KEY_OVERDUE.equals(sTemp)) {
							sTemp = getString(R.string.date_header_overdue);
						}
						else if (Task.HEADER_KEY_TODAY.equals(sTemp)) {
							sTemp = getString(R.string.date_header_today);
						}
						else if (Task.HEADER_KEY_PLUS1.equals(sTemp)) {
							sTemp = getString(R.string.date_header_tomorrow);
						}
						else if (Task.HEADER_KEY_PLUS2.equals(sTemp)
								|| Task.HEADER_KEY_PLUS3.equals(sTemp)
								|| Task.HEADER_KEY_PLUS4.equals(sTemp)) {
							sTemp = weekdayFormatter.format(new Date(c
									.getLong(4)));
						}
						else if (Task.HEADER_KEY_LATER.equals(sTemp)) {
							sTemp = getString(R.string.date_header_future);
						}
						else if (Task.HEADER_KEY_NODATE.equals(sTemp)) {
							sTemp = getString(R.string.date_header_none);
						}
						else if (Task.HEADER_KEY_COMPLETE.equals(sTemp)) {
							sTemp = getString(R.string.date_header_completed);
						}
					}
					else {
						// Set height of text for non-headers
						if (mRowCount == 1) {
							((TitleNoteTextView) view).setSingleLine(true);
						}
						else {
							((TitleNoteTextView) view).setSingleLine(false);
							((TitleNoteTextView) view).setMaxLines(mRowCount);
						}
					}
					((TitleNoteTextView) view).setTextTitle(sTemp);
					return true;
				case 2:
					if (!isHeader) {
						// Only if task it not locked
						if (c.getInt(9) != 1) {
							((TitleNoteTextView) view).setTextRest(c
									.getString(colIndex));
						}
					}
					return true;
				case 3:
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
						}
						else {
							view.setVisibility(View.VISIBLE);
						}
					}
					return true;
				case 4:
					if (!isHeader) {
						// Always hide for note type
						if (mListType != null && mListType.equals(notetype)) {
							view.setVisibility(View.GONE);
						}
						// Show for tasks if present
						else {
							if (c.isNull(colIndex)) {
								view.setVisibility(View.GONE);
							}
							else {
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
						}
						else {
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
		mRowCount = prefs.getInt(getString(R.string.key_pref_item_max_height),
				3);
		mHideCheckbox = prefs.getBoolean(
				getString(R.string.pref_hidecheckboxes), false);

		// mSortType = prefs.getString(getString(R.string.pref_sorttype),
		// getString(R.string.default_sorttype));
		// mListType = prefs.getString(getString(R.string.pref_listtype),
		// getString(R.string.default_listtype));

		mCallback = new LoaderCallbacks<Cursor>() {
			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
				if (id == 0) {
					Log.d("nononsenseapps list", "creating loader 0");
					return new CursorLoader(getActivity(),
							TaskList.getUri(mListId), TaskList.Columns.FIELDS,
							null, null, null);
				}
				else {
					Log.d("nononsenseapps list", "creating loader 1");
					// What sorting to use
					final Uri targetUri;
					final String sortSpec;
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
						sortSpec = Task.Columns.TITLE;
					}
					else if (mSortType
							.equals(getString(R.string.const_duedate))) {
						targetUri = Task.URI_SECTIONED_BY_DATE;
						sortSpec = null;
					}
					else if (mSortType
							.equals(getString(R.string.const_modified))) {
						targetUri = Task.URI;
						sortSpec = Task.Columns.UPDATED + " DESC";
					}
					// manual sorting
					else {
						targetUri = Task.URI_INDENTED_QUERY;
						sortSpec = null;
					}

					// Task.URI_SECTIONED_BY_DATE
					// Task.URI_INDENTED_QUERY
					// Task.Columns.LEFT
					return new CursorLoader(getActivity(), targetUri,
							Task.Columns.FIELDS, Task.Columns.DBLIST + " IS ?",
							new String[] { Long.toString(mListId) }, sortSpec);
				}
			}

			@Override
			public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
				if (loader.getId() == 0) {
					Log.d("nononsenseapps list", "loader 0 finished");
					if (c.moveToFirst()) {
						final TaskList list = new TaskList(c);
						mSortType = list.sorting;
						mListType = list.listtype;
						// Reload tasks with new sorting
						getLoaderManager().restartLoader(1, null, this);
					}
				}
				else {
					Log.d("nononsenseapps list", "loader 1 finished");
					mAdapter.swapCursor(c);
				}
			}

			@Override
			public void onLoaderReset(Loader<Cursor> loader) {
				if (loader.getId() == 0) {
					// Nothing to do
				}
				else {
					mAdapter.swapCursor(null);
				}
			}
		};

		getLoaderManager().restartLoader(0, null, mCallback);
	}

	@AfterViews
	void loadList() {
		listView.setAdapter(mAdapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View origin, int pos,
					long id) {
				if (mListener != null && id > 0) {
					mListener.onFragmentInteraction(Task.getUri(id), origin);
				}
			}
		});

		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int pos, long id) {
				listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
				// Also select the item in question
				listView.setItemChecked(pos, true);
				return true;
			}
		});

		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			final HashMap<Long, Task> tasks = new HashMap<Long, Task>();
			ShareActionProvider mShareProvider;
			ActionMode mMode;
			final PasswordConfirmedListener pListener = new PasswordConfirmedListener() {
				@Override
				public void onPasswordConfirmed() {
					for (final Task t : tasks.values()) {
						try {
							t.delete(getActivity());
						}
						catch (Exception e) {
						}
					}
					try {
						Toast.makeText(
								getActivity(),
								getResources().getQuantityString(
										R.plurals.notedeleted_msg,
										tasks.size(), tasks.size()),
								Toast.LENGTH_SHORT).show();
					}
					catch (Exception e) {
						// Protect against faulty translations
					}
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

				final MenuItem actionItem = menu.findItem(R.id.menu_share);
				mShareProvider = (ShareActionProvider) actionItem
						.getActionProvider();
				mShareProvider
						.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
				mShareProvider.setShareIntent(getShareIntent());
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				// Respond to clicks on the actions in the CAB
				boolean finish = false;
				switch (item.getItemId()) {
				case R.id.menu_copy:
					final ClipboardManager clipboard = (ClipboardManager) getActivity()
							.getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setPrimaryClip(ClipData.newPlainText(
							getString(R.string.app_name), getShareText()));
					try {
						Toast.makeText(
								getActivity(),
								getResources().getQuantityString(
										R.plurals.notecopied_msg, tasks.size(),
										tasks.size()), Toast.LENGTH_SHORT)
								.show();
					}
					catch (Exception e) {
						// Protect against faulty translations
					}
					finish = true;
					break;
				case R.id.menu_delete:
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
					}
					else {
						for (final Task t : tasks.values()) {
							try {
								t.delete(getActivity());
							}
							catch (Exception e) {
								// Deleting a task that has already been deleted
								// should under no circumstances do stupid stuff
							}
						}
						try {
							Toast.makeText(
									getActivity(),
									getResources().getQuantityString(
											R.plurals.notedeleted_msg,
											tasks.size(), tasks.size()),
									Toast.LENGTH_SHORT).show();
						}
						catch (Exception e) {
							// Protect against faulty translations
						}
						finish = true;
					}
					break;
				case R.id.menu_indent:
					// TODO
					break;
				case R.id.menu_unindent:
					// TODO
					break;
				default:
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
				}
				else {
					tasks.remove(id);
				}

				mShareProvider.setShareIntent(getShareIntent());

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
				}
				catch (Exception e) {
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
					}
					else {
						sb.append(t.getText());
					}
				}
				return sb.toString();
			}

			Intent getShareIntent() {
				final Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
				shareIntent
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				return shareIntent;
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_tasklist, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add:
			if (mListener != null && mListId > 0) {
				mListener.addTaskInList("", mListId);
			}
			return true;
		case R.id.menu_managelists:
			// Show fragment
			if (mListId > 0) {
				DialogEditList_ dialog = DialogEditList_.getInstance(mListId);
				dialog.show(getFragmentManager(), "fragment_edit_list");
			}
			return true;
		case R.id.menu_clearcompleted:
			if (mListId > 0) {
				DialogDeleteCompletedTasks.showDialog(getFragmentManager(),
						mListId, null);
			}
			return true;
		case R.id.menu_deletelist:
		case R.id.menu_renamelist:
		case R.id.menu_setdefaultlist:
		default:
			return false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getLoaderManager().destroyLoader(0);
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

		// We want to be notified of future changes to auto refresh
		PreferenceManager.getDefaultSharedPreferences(getActivity())
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
		PreferenceManager.getDefaultSharedPreferences(getActivity())
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	static class SimpleSectionsAdapter extends SimpleDragSortCursorAdapter {
		DropListener dropListener = null;
		RemoveListener removeListener = null;
		final int mItemLayout;
		final int mHeaderLayout;
		final static int itemType = 0;
		final static int headerType = 1;

		public SimpleSectionsAdapter(Context context, int layout,
				int headerLayout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			mItemLayout = layout;
			mHeaderLayout = headerLayout;
		}

		int getViewLayout(final int position) {
			if (itemType == getItemViewType(position)) {
				return mItemLayout;
			}
			else {
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
			}
			else {
				return itemType;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				final LayoutInflater inflater = LayoutInflater.from(mContext);
				convertView = inflater.inflate(getViewLayout(position), parent,
						false);
			}
			return super.getView(position, convertView, parent);
		}
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences prefs,
			final String key) {
		boolean reload = false;
		if (key.equals(getString(R.string.pref_sorttype))) {
			mSortType = null;
			reload = true;
		}
		else if (key.equals(getString(R.string.key_pref_item_max_height))) {
			mRowCount = prefs.getInt(key, 3);
			reload = true;
		}
		else if (key.equals(getString(R.string.pref_hidecheckboxes))) {
			mHideCheckbox = prefs.getBoolean(key, false);
			reload = true;
		}
		else if (key.equals(getString(R.string.pref_listtype))) {
			mListType = null;
			reload = true;
		}

		if (reload && mCallback != null) {
			getLoaderManager().restartLoader(0, null, mCallback);
		}
	}
}
