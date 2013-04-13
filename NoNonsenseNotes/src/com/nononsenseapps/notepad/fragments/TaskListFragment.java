package com.nononsenseapps.notepad.fragments;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;

import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.ui.DateView;
import com.nononsenseapps.ui.NoteCheckBox;
import com.nononsenseapps.utils.views.TitleNoteTextView;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
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

@EFragment
public class TaskListFragment extends Fragment {

	public static final String LIST_ID = "list_id";

	// DragSortListView listView;
	@ViewById(android.R.id.list)
	AbsListView listView;

	SimpleCursorAdapter mAdapter;

	private long mListId = -1;

	private OnFragmentInteractionListener mListener;

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
						Task.Columns.DUE, Task.Columns.COMPLETED }, new int[] {
						android.R.id.text1, android.R.id.text1, R.id.date,
						R.id.checkbox }, 0);

		mAdapter.setViewBinder(new ViewBinder() {
			SimpleDateFormat weekdayFormatter = TimeFormatter
					.getLocalFormatterWeekday(getActivity());
			boolean isHeader = false;
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
						// TODO fetch strings
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
				default:
					break;
				}
				return false;
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_task_list, container,
				false);
		return view;
	}

	@Override
	public void onActivityCreated(final Bundle state) {
		super.onActivityCreated(state);

		getLoaderManager().restartLoader(0, null,
				new LoaderCallbacks<Cursor>() {

					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
						// Task.URI_INDENTED_QUERY
						// Task.Columns.LEFT
						return new CursorLoader(getActivity(),
								Task.URI_SECTIONED_BY_DATE,
								Task.Columns.FIELDS, Task.Columns.DBLIST
										+ " IS ?", new String[] { Long
										.toString(mListId) }, null);
					}

					@Override
					public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
						mAdapter.swapCursor(c);
					}

					@Override
					public void onLoaderReset(Loader<Cursor> arg0) {
						mAdapter.swapCursor(null);
					}
				});
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
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	static class SimpleSectionsAdapter extends SimpleCursorAdapter {
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
}
