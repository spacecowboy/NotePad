package com.nononsenseapps.notepad.fragments;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.ui.DateView;
import com.nononsenseapps.ui.NoteCheckBox;
import com.nononsenseapps.utils.views.TitleNoteTextView;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

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
						Task.Columns.DUE, Task.Columns.COMPLETED, Task.Columns.LEFT, Task.Columns.RIGHT }, new int[] {
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
					((TitleNoteTextView) view).setTextTitle(sTemp);
					return true;
				case 2:
					if (!isHeader) {
						((TitleNoteTextView) view).setTextRest(c
								.getString(colIndex));
					}
					return true;
				case 3:
					if (!isHeader) {
						((NoteCheckBox) view).setOnCheckedChangeListener(null);
						((NoteCheckBox) view).setChecked(!c.isNull(colIndex));
						((NoteCheckBox) view).setNoteId(c.getLong(0));
						((NoteCheckBox) view)
								.setOnCheckedChangeListener(checkBoxListener);
					}
					return true;
				case 4:
					if (!isHeader) {
						if (c.isNull(colIndex)) {
							view.setVisibility(View.GONE);
						}
						else {
							view.setVisibility(View.VISIBLE);
							((DateView) view).setTimeText(c.getLong(colIndex));
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
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				if (mListener != null && id > 0) {
					mListener.onFragmentInteraction(Task.getUri(id));
				}
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
		// if (key.equals(getString(R.string.pref_sorttype))) {
		Log.d("nononsenseapps list", "prefs changed. restartin loader");
		mSortType = null;
		if (mCallback != null) {
			getLoaderManager().restartLoader(0, null, mCallback);
		}
		// }
	}
}
