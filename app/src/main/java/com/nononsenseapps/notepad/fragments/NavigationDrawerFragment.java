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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import java.util.ArrayList;

/**
 * This fragment is the view which is displayed in the left drawer.
 * Any activity containing it must implement NavigationDrawerCallbacks.
 */
public class NavigationDrawerFragment extends Fragment implements LoaderManager
        .LoaderCallbacks<Cursor> {
    static final String[] COUNTROWS = new String[]{"COUNT(1)"};
    static final String NOTCOMPLETED = Task.Columns.COMPLETED + " IS NULL ";

    private static final int LOADER_NAVDRAWER_LISTS = 0;

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    ArrayList<ArrayList<Object>> mExtraData;
    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;
    private Adapter mAdapter;
    private NavigationDrawerCallbacks mCallbacks;

    public NavigationDrawerFragment() {
    }

    private void updateExtra(final int pos, final int count) {
        while (mExtraData.get(pos).size() < 2) {
            // To avoid crashes
            mExtraData.get(pos).add("0");
        }
        mExtraData.get(pos).set(1, Integer.toString(count));
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(DrawerLayout drawerLayout, Toolbar toolbar) {
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        //mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // Fix the color of the status bar

        final ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_menu_24dp_white);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, toolbar, R.string
                .navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences
                            (getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            //mDrawerLayout.openDrawer(fragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallbacks = (NavigationDrawerCallbacks) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            //mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        // To get call to handle home button
        hasOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_navdrawer, container, false);

        RecyclerView list = (RecyclerView) rootView.findViewById(R.id.left_drawer);

        mAdapter = new Adapter(new HeaderItem(TaskListFragment.LIST_ID_ALL, R.string
                .show_from_all_lists));
        list.setAdapter(mAdapter);
        list.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        list.setLayoutManager(layoutManager);

        // todo remove
        /*// Use extra items for All Lists
        final int[] extraIds = new int[]{-1, TaskListFragment.LIST_ID_OVERDUE, TaskListFragment
                .LIST_ID_TODAY, TaskListFragment.LIST_ID_WEEK, -1};
        // This is fine for initial conditions
        final int[] extraStrings = new int[]{R.string.tasks, R.string.date_header_overdue, R
                .string.date_header_today, R.string.next_5_days, R.string.lists};
        // Use this for real data
        mExtraData = new ArrayList<ArrayList<Object>>();
        // Task header
        mExtraData.add(new ArrayList<Object>());
        mExtraData.get(0).add(R.string.tasks);
        // Overdue
        mExtraData.add(new ArrayList<Object>());
        mExtraData.get(1).add(R.string.date_header_overdue);
        // Today
        mExtraData.add(new ArrayList<Object>());
        mExtraData.get(2).add(R.string.date_header_today);
        // Week
        mExtraData.add(new ArrayList<Object>());
        mExtraData.get(3).add(R.string.next_5_days);
        // Lists header
        mExtraData.add(new ArrayList<Object>());
        mExtraData.get(4).add(R.string.lists);

        final int[] extraTypes = new int[]{1, 0, 0, 0, 1};

        mAdapter = new ExtraTypesCursorAdapter(getContext(), R.layout.simple_light_list_item_2,
                null, new String[]{TaskList.Columns.TITLE, TaskList.Columns.VIEW_COUNT}, new
                int[]{android.R.id.text1, android.R.id.text2},
                // id -1 for headers, ignore clicks on them
                extraIds, extraStrings, extraTypes, new int[]{R.layout.drawer_header});
        mAdapter.setExtraData(mExtraData);
        list.setAdapter(mAdapter);*/
        // Set click handler
        /*list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int pos, long id) {
                if (id < 0) {
                    // Set preference which type was chosen
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong
                            (TaskListFragment.LIST_ALL_ID_PREF_KEY, id).commit();
                }
                mCallbacks.openList(id);
                mDrawerLayout.closeDrawers();
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                // Open dialog to edit list
                if (id > 0) {
                    DialogEditList_ dialog = DialogEditList_.getInstance(id);
                    dialog.show(getFragmentManager(), "fragment_edit_list");
                    return true;
                } else if (id < -1) {
                    // Set as "default"
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong
                            (getString(R.string.pref_defaultstartlist), id).putLong
                            (TaskListFragment.LIST_ALL_ID_PREF_KEY, id).commit();
                    Toast.makeText(getContext(), R.string.new_default_set, Toast.LENGTH_SHORT)
                            .show();
                    // openList(id);
                    return true;
                } else {
                    return false;
                }
            }
        });*/

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Start loading
        getLoaderManager().restartLoader(LOADER_NAVDRAWER_LISTS, null, this);
        // special views
        // todo remove
        /*getLoaderManager().restartLoader(TaskListFragment.LIST_ID_OVERDUE, null, this);
        getLoaderManager().restartLoader(TaskListFragment.LIST_ID_TODAY, null, this);
        getLoaderManager().restartLoader(TaskListFragment.LIST_ID_WEEK, null, this);*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
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
        // Normal lists
        switch (id) {
            case TaskListFragment.LIST_ID_OVERDUE:
                return new CursorLoader(getContext(), Task.URI, COUNTROWS, NOTCOMPLETED +
                        TaskListFragment.andWhereOverdue(), null, null);
            case TaskListFragment.LIST_ID_TODAY:
                return new CursorLoader(getContext(), Task.URI, COUNTROWS, NOTCOMPLETED +
                        TaskListFragment.andWhereToday(), null, null);
            case TaskListFragment.LIST_ID_WEEK:
                return new CursorLoader(getContext(), Task.URI, COUNTROWS, NOTCOMPLETED +
                        TaskListFragment.andWhereWeek(), null, null);
            case LOADER_NAVDRAWER_LISTS:
            default:
                return new CursorLoader(getContext(), TaskList.URI_WITH_COUNT, new
                        String[]{TaskList.Columns._ID, TaskList.Columns.TITLE, TaskList.Columns
                        .VIEW_COUNT}, null, null, getResources().getString(R.string
                        .const_as_alphabetic, TaskList.Columns.TITLE));
        }
    }

    /**
     * @param loader The Loader that has finished.
     * @param c      The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {
            case TaskListFragment.LIST_ID_OVERDUE:
                if (c.moveToFirst()) {
                    updateExtra(1, c.getInt(0));
                }
                break;
            case TaskListFragment.LIST_ID_TODAY:
                if (c.moveToFirst()) {
                    updateExtra(2, c.getInt(0));
                }
                break;
            case TaskListFragment.LIST_ID_WEEK:
                if (c.moveToFirst()) {
                    updateExtra(3, c.getInt(0));
                }
                break;
            case LOADER_NAVDRAWER_LISTS:
            default:
                mAdapter.setData(c);
        }
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
            case TaskListFragment.LIST_ID_OVERDUE:
            case TaskListFragment.LIST_ID_TODAY:
            case TaskListFragment.LIST_ID_WEEK:
                break;
            case LOADER_NAVDRAWER_LISTS:
                mAdapter.setData(null);
                break;
        }
    }

    public interface NavigationDrawerCallbacks {
        void openList(long id);
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEWTYPE_HEADER = 0;
        private static final int VIEWTYPE_ITEM = 1;
        private final HeaderItem[] headers;
        Cursor mCursor = null;

        public Adapter(HeaderItem... headers) {
            setHasStableIds(true);
            this.headers = headers;
        }

        public void setData(Cursor cursor) {
            mCursor = cursor;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder vh;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            switch (viewType) {
                case VIEWTYPE_HEADER:
                    vh = new HeaderViewHolder(inflater.inflate(R.layout.simple_light_list_item_2,
                            parent, false));
                    break;
                case VIEWTYPE_ITEM:
                default:
                    vh = new CursorViewHolder(inflater.inflate(R.layout.simple_light_list_item_2,
                            parent, false));
                    break;
            }
            return vh;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEWTYPE_HEADER:
                    ((HeaderViewHolder) holder).bind(headers[position]);
                    break;
                case VIEWTYPE_ITEM:
                    mCursor.moveToPosition(actualPosition(position));
                    ((CursorViewHolder) holder).bind(mCursor);
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (isHeader(position)) {
                return VIEWTYPE_HEADER;
            } else {
                return VIEWTYPE_ITEM;
            }
        }

        @Override
        public long getItemId(int position) {
            if (isHeader(position)) {
                return headers[position].id;
            } else {
                mCursor.moveToPosition(actualPosition(position));
                return mCursor.getLong(0);
            }
        }

        @Override
        public int getItemCount() {
            int result = headers.length;
            if (mCursor != null) {
                result += mCursor.getCount();
            }
            return result;
        }

        /**
         * @param position as seen from the outside
         * @return position as seen by internal data (header position if header, wrapped position
         * if..)
         */
        public int actualPosition(int position) {
            if (isHeader(position)) {
                return position;
            } else {
                return position - headers.length;
            }
        }

        /**
         * @param position as seen from the outside
         * @return true if position is on a header, false otherwise
         */
        public boolean isHeader(int position) {
            return position < headers.length;
        }
    }

    /**
     * The interface of the extra items in this adapter.
     */
    public class HeaderItem {
        public final long id;
        public final int title;

        public HeaderItem(long id, @StringRes int title) {
            this.id = id;
            this.title = title;
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mTitle;
        private final TextView mCount;
        private HeaderItem mItem;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitle = (TextView) itemView.findViewById(android.R.id.text1);
            mCount = (TextView) itemView.findViewById(android.R.id.text2);
        }

        public void bind(HeaderItem headerItem) {
            mItem = headerItem;
            mTitle.setText(headerItem.title);
            mCount.setVisibility(View.GONE);
        }

        @Override
        public void onClick(View v) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong
                    (TaskListFragment.LIST_ALL_ID_PREF_KEY, mItem.id).commit();
            mCallbacks.openList(mItem.id);
            mDrawerLayout.closeDrawers();
        }
    }

    private class CursorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mTitle;
        private final TextView mCount;
        private long id = -1;

        public CursorViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitle = (TextView) itemView.findViewById(android.R.id.text1);
            mCount = (TextView) itemView.findViewById(android.R.id.text2);
        }

        public void bind(Cursor cursor) {
            id = cursor.getLong(cursor.getColumnIndex(TaskList.Columns._ID));
            mTitle.setText(cursor.getString(cursor.getColumnIndex(TaskList.Columns.TITLE)));
            mCount.setText(cursor.getString(cursor.getColumnIndex(TaskList.Columns.VIEW_COUNT)));
        }

        @Override
        public void onClick(View v) {
            /*if (id < 0) {
                // Set preference which type was chosen
                // TODO move to sort/filter options
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong
                        (TaskListFragment.LIST_ALL_ID_PREF_KEY, id).commit();
            }*/
            mCallbacks.openList(id);
            mDrawerLayout.closeDrawers();
        }
    }
}
