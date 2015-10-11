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

package com.nononsenseapps.notepad.activities;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.TaskListFragment;
import com.nononsenseapps.notepad.fragments.TaskListFragment_;
import com.nononsenseapps.notepad.util.PrefUtils;

/**
 * Main List activity. Its purpose is to setup the views and layout for the benefit of
 * the underlying fragments.
 */
public class ActivityList extends AppCompatActivity implements LoaderManager
        .LoaderCallbacks<Cursor> {

    private static final int LOADER_LISTS = 0;
    private static final String START_LIST_ID = "start_list_id";
    DrawerLayout mDrawerLayout;
    private SimpleCursorAdapter mTaskListsAdapter;
    private Adapter mFragmentAdapter;
    private ViewPager mViewPager;
    private long mListIdToSelect = -1;
    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_menu_24dp_white);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ListView navView = (ListView) findViewById(R.id.left_drawer);

        if (navView != null) {
            setupDrawerContent(navView);
        }

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        if (mViewPager != null) {
            setupViewPager(mViewPager);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG).setAction
                        ("Action", null).show();
            }
        });

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        //mTabLayout.setupWithViewPager(mViewPager);

        // Handle arguments
        handleArgs(savedInstanceState);

        // Start loading
        getSupportLoaderManager().restartLoader(LOADER_LISTS, Bundle.EMPTY, this);
    }

    private void handleArgs(Bundle savedInstanceState) {
        mListIdToSelect = getIntent().getLongExtra(START_LIST_ID, -1);
        if (savedInstanceState != null) {
            mListIdToSelect = savedInstanceState.getLong(START_LIST_ID);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        mTaskListsAdapter = new SimpleCursorAdapter(this, android.R.layout
                .simple_dropdown_item_1line, null, new String[]{TaskList.Columns.TITLE}, new
                int[]{android.R.id.text1}, 0);
        // Set space between fragments
        //pager.setPageMargin(ViewsHelper.convertDip2Pixels(getActivity(), 16));
        mFragmentAdapter = new Adapter(getSupportFragmentManager(), mTaskListsAdapter);
        viewPager.setAdapter(mFragmentAdapter);
    }

    /**
     * Drawer and its content must be non-null
     */
    private void setupDrawerContent(@NonNull ListView navView) {
        if (mDrawerLayout == null) {
            return;
        }

        // TODO add content

        // When the user runs the app for the first time, we want to land them with the
        // navigation drawer open. But just the first time.
        if (!PrefUtils.isWelcomeDone(this)) {
            // first run of the app starts with the nav drawer open
            PrefUtils.markWelcomeDone(this);
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LISTS:
                return new CursorLoader(this, TaskList.URI, new String[]{TaskList.Columns._ID,
                        TaskList.Columns.TITLE}, null, null, getResources().getString(R.string
                        .const_as_alphabetic, TaskList.Columns.TITLE));
        }
        return null;
    }

    /**
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_LISTS:
                mTaskListsAdapter.swapCursor(data);
                mTabLayout.setupWithViewPager(mViewPager);
                final int pos;
                if (mListIdToSelect != -1) {
                    pos = mFragmentAdapter.getItemPosition(mListIdToSelect);
                } else {
                    pos = -1;
                }
                if (pos >= 0) {
                    mViewPager.setCurrentItem(pos);
                    mListIdToSelect = -1;
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (mFragmentAdapter != null) {
            mFragmentAdapter.destroy();
        }
        getLoaderManager().destroyLoader(LOADER_LISTS);

        super.onDestroy();
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_LISTS:
                mTaskListsAdapter.swapCursor(null);
                break;
        }
    }

    class Adapter extends FragmentPagerAdapter {
        private final CursorAdapter wrappedAdapter;
        private final DataSetObserver subObserver;
        private final SharedPreferences.OnSharedPreferenceChangeListener prefListener;

        private long all_id = -2;

        public Adapter(FragmentManager fm, CursorAdapter wrappedAdapter) {
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

            if (wrappedAdapter != null) {
                wrappedAdapter.registerDataSetObserver(subObserver);
            }

            // also monitor changes of all tasks choice
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences
                    (ActivityList.this);

            prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String
                        key) {
                    if (TaskListFragment.LIST_ALL_ID_PREF_KEY.equals(key)) {
                        all_id = prefs.getLong(TaskListFragment.LIST_ALL_ID_PREF_KEY,
                                TaskListFragment.LIST_ID_WEEK);
                        notifyDataSetChanged();
                    }
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(prefListener);

            // Set all value
            all_id = prefs.getLong(TaskListFragment.LIST_ALL_ID_PREF_KEY, TaskListFragment
                    .LIST_ID_WEEK);
        }

        public void destroy() {
            if (wrappedAdapter != null) {
                wrappedAdapter.unregisterDataSetObserver(subObserver);
            }
            if (prefListener != null) {
                PreferenceManager.getDefaultSharedPreferences(ActivityList.this)
                        .unregisterOnSharedPreferenceChangeListener(prefListener);
            }
        }

        /**
         * Called when the host view is attempting to determine if an item's position
         * has changed. Returns {@link #POSITION_UNCHANGED} if the position of the given
         * item has not changed or {@link #POSITION_NONE} if the item is no longer present
         * in the adapter.
         * <p/>
         * <p>The default implementation assumes that items will never
         * change position and always returns {@link #POSITION_UNCHANGED}.
         *
         * @param object Object representing an item, previously returned by a call to
         *               {@link #instantiateItem(View, int)}.
         * @return object's new position index from [0, {@link #getCount()}),
         * {@link #POSITION_UNCHANGED} if the object's position has not changed,
         * or {@link #POSITION_NONE} if the item is no longer present.
         */
        @Override
        public int getItemPosition(Object object) {
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

        @Override
        public Fragment getItem(int position) {
            long id = getItemId(position);
            // todo cache?
            return TaskListFragment_.getInstance(id);
        }

        /**
         * Return a unique identifier for the item at the given position.
         * <p/>
         * <p>The default implementation returns the given position.
         * Subclasses should override this method if the positions of items can change.</p>
         *
         * @param position Position within this adapter
         * @return Unique identifier for the item at position
         */
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
            if (position >= getCount())
                return null;
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
    }
}
