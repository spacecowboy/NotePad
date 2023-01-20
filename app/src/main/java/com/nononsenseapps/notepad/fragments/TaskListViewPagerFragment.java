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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.nononsenseapps.helpers.ListHelper;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.activities.main.ActivityMain;
import com.nononsenseapps.notepad.interfaces.ListOpener;
import com.nononsenseapps.notepad.activities.ActivitySearchDeleted;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.databinding.FragmentTasklistViewpagerBinding;
import com.nononsenseapps.notepad.fragments.DialogEditList.EditListDialogListener;
import com.nononsenseapps.notepad.interfaces.MenuStateController;
import com.nononsenseapps.ui.ViewsHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import java.util.Objects;

/**
 * Displays many listfragments across a viewpager. Supports selecting a certain one on startup
 */
@EFragment(R.layout.fragment_tasklist_viewpager)
public class TaskListViewPagerFragment extends Fragment implements
		EditListDialogListener, ListOpener {

	public static final String START_LIST_ID = "start_list_id";

	@ViewById(resName = "pager")
	ViewPager pager;

	private SectionsPagerAdapter mSectionsPagerAdapter;
	SimpleCursorAdapter mTaskListsAdapter;

	// boolean firstLoad = true;

	private long mListIdToSelect = -1;

	/**
	 * for {@link R.layout#fragment_tasklist_viewpager}
	 */
	private FragmentTasklistViewpagerBinding mBinding;

	/*@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		mBinding = FragmentTasklistViewpagerBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// here you call methods with the old @AfterViews annotation
		setAdapter();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mBinding = null;
	}
	*/
	public static TaskListViewPagerFragment getInstance() {
		return getInstance(-1);
	}

	public static TaskListViewPagerFragment getInstance(final long startListId) {
		TaskListViewPagerFragment_ f = new TaskListViewPagerFragment_();
		Bundle args = new Bundle();
		args.putLong(START_LIST_ID, startListId);
		f.setArguments(args);
		return f;
	}

	public TaskListViewPagerFragment() {
		super();
	}

	public SectionsPagerAdapter getSectionsPagerAdapter() {
		return mSectionsPagerAdapter;
	}

	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setHasOptionsMenu(true);

		mListIdToSelect = getArguments().getLong(START_LIST_ID, -1);
		NnnLogger.debug(TaskDetailFragment.class, "onCreate: " + savedState);
		if (savedState != null) {
			mListIdToSelect = savedState.getLong(START_LIST_ID);
		}

		// Adapter for list titles and ids
		mTaskListsAdapter = new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_dropdown_item_1line, null,
				new String[] { TaskList.Columns.TITLE },
				new int[] { android.R.id.text1 }, 0);
		// Adapter for view pager
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getChildFragmentManager(), mTaskListsAdapter);
	}

	@Override
	public void onActivityCreated(final Bundle state) {
		super.onActivityCreated(state);

		LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<>() {

			@NonNull
			@Override
			public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
				return new CursorLoader(getActivity(), TaskList.URI,
						new String[] { TaskList.Columns._ID,
								TaskList.Columns.TITLE }, null, null,
						getResources().getString(R.string.const_as_alphabetic,
								TaskList.Columns.TITLE));
			}

			@Override
			public void onLoadFinished(@NonNull Loader<Cursor> arg0, Cursor c) {
				mTaskListsAdapter.swapCursor(c);
				final int pos;
				if (mListIdToSelect != -1) {
					pos = mSectionsPagerAdapter
							.getItemPosition(mListIdToSelect);
				} else {
					pos = -1;
				}
				if (pos >= 0) {
					pager.setCurrentItem(pos);
					mListIdToSelect = -1;
				}
			}

			@Override
			public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
				mTaskListsAdapter.swapCursor(null);
			}
		};

		// Load actual data
		LoaderManager.getInstance(this).restartLoader(0, null, loaderCallbacks);
	}

	@AfterViews
	void setAdapter() {
		// Set space between fragments
		pager.setPageMargin(ViewsHelper.convertDip2Pixels(getActivity(), 16));
		// Set adapters
		pager.setAdapter(mSectionsPagerAdapter);

	}

	/**
	 * Create the {@link SearchView} to find notes while browsing {@link ActivityMain}
	 */
	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_tasklists_viewpager, menu);

		if (menu.findItem(R.id.menu_search) == null) {
			return;
		}
		SearchView searchView = (SearchView) menu
				.findItem(R.id.menu_search)
				.getActionView();
		// Assumes current activity is the searchable activity
		SearchManager sMan = this.getActivity().getSystemService(SearchManager.class);
		searchView.setSearchableInfo(sMan.getSearchableInfo(getActivity().getComponentName()));
		// expand the searchview by default when the user clicks on the icon
		searchView.setIconifiedByDefault(false);
		searchView.setQueryRefinementEnabled(false);
		searchView.setSubmitButtonEnabled(false);

		// enlarge the suggestions box so that it occupies the whole screen
		var autoCompTxtVi = (AutoCompleteTextView) searchView
				.findViewById(androidx.appcompat.R.id.search_src_text);

		// TODO in some devices, the default layout for the search suggestions cuts the text
		//  horizontally. To fix this, you need a custom layout for the search suggestion list
		//  item. To use it, you must make a custom adapter, instead of using setSearchableInfo().
		//  Start here:
		// CursorAdapter xx1 = searchView.getSuggestionsAdapter();
		// ListAdapter xx2 = autoCompTxtVi.getAdapter();
		// assert xx1 == xx2;

		final View dropDownSugg = searchView.findViewById(autoCompTxtVi.getDropDownAnchor());
		if (dropDownSugg == null) return;
		dropDownSugg.addOnLayoutChangeListener(
				(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
					// gives more horizontal space, to show more text of a search suggestion item
					autoCompTxtVi.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
				});
	}

	@Override
	public void onPrepareOptionsMenu(@NonNull Menu menu) {
		if (!(getActivity() instanceof MenuStateController)) {
			return;
		}

		final boolean visible = ((MenuStateController) getActivity()).childItemsVisible();
		menu.setGroupVisible(R.id.viewpager_menu_group, visible);

		// Outside group to allow for action bar placement
		if (menu.findItem(R.id.menu_search) != null)
			menu.findItem(R.id.menu_search).setVisible(visible);
		if (menu.findItem(R.id.menu_sync) != null)
			menu.findItem(R.id.menu_sync).setVisible(visible);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_search) {
			// Always visible, but do this if not visible
			// getActivity().onSearchRequested();
			return true;
		} else if (itemId == R.id.menu_deletedtasks) {
			startActivity(new Intent(getActivity(), ActivitySearchDeleted.class));
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onFinishEditDialog(final long id) {
		openList(id);
	}

	@Override
	public void openList(final long id) {
		// If it fails, will load on refresh
		mListIdToSelect = id;
		NnnLogger.debug(TaskListViewPagerFragment.class, "openList: " + mListIdToSelect);
		if (mSectionsPagerAdapter != null) {

			final int pos;
			if (id < 1)
				pos = 0;
			else
				pos = mSectionsPagerAdapter.getItemPosition(id);

			if (pos > -1) {
				pager.setCurrentItem(pos, true);
				mListIdToSelect = -1;
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		// even if 'pager' is tagged @NonNull, but it may actually be null, for example when
		// you load the task history activity
		if (mTaskListsAdapter != null && Objects.nonNull(pager)) {
			long id = mTaskListsAdapter.getItemId(pager.getCurrentItem());
			outState.putLong(START_LIST_ID, id);
			NnnLogger.debug(TaskListViewPagerFragment.class, "Saved state, id=" + id);
		}
	}

	@Override
	public void onDestroy() {
		if (mSectionsPagerAdapter != null) {
			mSectionsPagerAdapter.destroy();
		}
		LoaderManager.getInstance(this).destroyLoader(0);

		super.onDestroy();
	}

	/**
	 * Might be a meta list
	 */
	public static long getAShowList(final Context context, final long tempList) {
		long returnList = tempList;

		if (returnList == -1) {
			// Then check if a default list is specified
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);
			returnList = prefs
					.getLong(
							context.getString(R.string.pref_defaultstartlist),
							Long.parseLong(prefs.getString(context
									.getString(R.string.pref_defaultlist), "-1")));
		}

		if (returnList == -1) {
			returnList = ListHelper.getARealList(context, returnList);
		}

		// If nothing was found, show ALL
		if (returnList == -1) {
			returnList = TaskListFragment.LIST_ID_ALL;
		}

		return returnList;
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		private final CursorAdapter wrappedAdapter;
		private final DataSetObserver subObserver;
		private final OnSharedPreferenceChangeListener prefListener;

		private long all_id = -2;

		public SectionsPagerAdapter(final FragmentManager fm,
									final CursorAdapter wrappedAdapter) {
			super(fm);
			this.wrappedAdapter = wrappedAdapter;

			subObserver = new DataSetObserver() {
				@Override
				public void onChanged() {
					notifyDataSetChanged();
				}

				@Override
				public void onInvalidated() {
					// Probably destroying the loader
				}
			};

			if (wrappedAdapter != null)
				wrappedAdapter.registerDataSetObserver(subObserver);

			// also monitor changes of all tasks choice
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getActivity());

			prefListener = (sharedPreferences, key) -> {
				if (TaskListFragment.LIST_ALL_ID_PREF_KEY.equals(key)) {
					all_id = prefs.getLong(
							TaskListFragment.LIST_ALL_ID_PREF_KEY,
							TaskListFragment.LIST_ID_WEEK);
					notifyDataSetChanged();
				}
			};
			prefs.registerOnSharedPreferenceChangeListener(prefListener);

			// Set all value
			all_id = prefs.getLong(TaskListFragment.LIST_ALL_ID_PREF_KEY,
					TaskListFragment.LIST_ID_WEEK);
		}

		public void destroy() {
			if (wrappedAdapter != null) {
				wrappedAdapter.unregisterDataSetObserver(subObserver);
			}
			if (prefListener != null) {
				PreferenceManager.getDefaultSharedPreferences(getActivity())
						.unregisterOnSharedPreferenceChangeListener(
								prefListener);
			}
		}

		@NonNull
		@Override
		public Fragment getItem(int pos) {
			long id = getItemId(pos);
			// if (id < 0) return null;
			return TaskListFragment_.getInstance(id);
		}

		@Override
		public long getItemId(int position) {
			long id = all_id;
			if (wrappedAdapter != null && position > 0) {
				Cursor c = (Cursor) wrappedAdapter.getItem(position - 1);
				if (c != null && !c.isAfterLast() && !c.isBeforeFirst()) {
					id = c.getLong(0);
				}
			}
			return id;
		}

		@Override
		public int getCount() {
			if (wrappedAdapter != null)
				return 1 + wrappedAdapter.getCount();
			else
				return 1;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position >= getCount()) return null;
			CharSequence title = "";

			if (position == 0) {
				switch ((int) all_id) {
					case TaskListFragment.LIST_ID_OVERDUE:
						title = getString(R.string.date_header_overdue);
						break;
					case TaskListFragment.LIST_ID_TODAY:
						title = getString(R.string.date_header_today);
						break;
					case TaskListFragment.LIST_ID_WEEK:
						title = getString(R.string.next_5_days);
						break;
					case TaskListFragment.LIST_ID_ALL:
					default:
						title = getString(R.string.all_tasks);
						break;
				}
			} else if (wrappedAdapter != null) {
				Cursor c = (Cursor) wrappedAdapter.getItem(position - 1);
				if (c != null && !c.isAfterLast() && !c.isBeforeFirst()) {
					title = c.getString(1);
				}
			}

			return title;
		}

		/**
		 * {@inheritDoc}
		 *
		 * Called when the host view is attempting to determine if an item's
		 * position has changed. Returns POSITION_UNCHANGED if the position of
		 * the given item has not changed or POSITION_NONE if the item is no
		 * longer present in the adapter.
		 *
		 * Argument is the object previously returned by instantiateItem
		 */
		@Override
		public int getItemPosition(@NonNull Object object) {
			Fragment f = (Fragment) object;
			long listId = f.getArguments().getLong(TaskListFragment.LIST_ID);
			return getItemPosition(listId);
		}

		/**
		 * Returns a negative number if id wasn't found in adapter
		 */
		public int getItemPosition(final long listId) {
			int length = getCount();
			int result = POSITION_NONE;
			int position;
			for (position = 0; position < length; position++) {
				if (listId == getItemId(position)) {
					result = position;
					break;
				}
			}

			return result;
		}
	}
}
