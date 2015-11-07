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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.helpers.SyncHelper;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.fragments.DialogAbout;
import com.nononsenseapps.notepad.fragments.DialogEditList;
import com.nononsenseapps.notepad.fragments.DialogEditList_;
import com.nononsenseapps.notepad.fragments.NavigationDrawerFragment;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.notepad.fragments.TaskListFragment;
import com.nononsenseapps.notepad.sync.orgsync.BackgroundSyncScheduler;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;
import com.nononsenseapps.util.ListHelper;
import com.nononsenseapps.util.SharedPreferencesHelper;

/**
 * Main List activity. Its purpose is to setup the views and layout for the benefit of
 * the underlying fragments.
 */
public class ActivityList extends ActivityBase implements NavigationDrawerFragment
        .NavigationDrawerCallbacks, TaskListFragment.TaskListCallbacks {

    private static final String START_LIST_ID = "start_list_id";
    private FloatingActionButton mFab;
    private long mCurrentList = TaskListFragment.LIST_ID_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        // Set up the drawer.
        mNavigationDrawerFragment.setUp((DrawerLayout) findViewById(R.id.drawer_layout), toolbar);

        // Setup FAB
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });

        // Handle arguments
        handleArgs(savedInstanceState);

        // Clear notification if present
        NotificationHelper.clearNotification(this, getIntent());
        // Schedule notifications
        NotificationHelper.schedule(this);
        // Schedule syncs
        BackgroundSyncScheduler.scheduleSync(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OrgSyncService.stop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Sync if appropriate
        OrgSyncService.start(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        OrgSyncService.pause(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState == null) {
            outState = new Bundle();
        }
        outState.putLong(START_LIST_ID, mCurrentList);
        super.onSaveInstanceState(outState);
    }

    private void handleArgs(Bundle savedInstanceState) {
        long mListIdToSelect;
        if (savedInstanceState != null) {
            mListIdToSelect = savedInstanceState.getLong(START_LIST_ID);
        } else {
            mListIdToSelect = ListHelper.getListId(getIntent());
        }
        mListIdToSelect = ListHelper.getAViewList(this, mListIdToSelect);

        openList(mListIdToSelect);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // hide manual sort for all item
        menu.findItem(R.id.menu_sort_manual).setVisible(mCurrentList > 0)
                .setEnabled(mCurrentList > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync:
                handleSyncRequest();
                break;
            case R.id.menu_sort_due:
                SharedPreferencesHelper.setSortingDue(this);
                break;
            case R.id.menu_sort_manual:
                SharedPreferencesHelper.setSortingManual(this);
                break;
            case R.id.menu_sort_title:
                SharedPreferencesHelper.setSortingAlphabetic(this);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    private void handleSyncRequest() {
        SyncHelper.onManualSyncRequest(this);
    }

    @Override
    public void openList(long id) {
        mCurrentList = id;
        invalidateOptionsMenu();
        getSupportFragmentManager().beginTransaction().replace(R.id.listfragment_container, TaskListFragment.getInstance(id)).commit();
    }

    @Override
    public void createList() {
        DialogEditList_ dialog = DialogEditList_.getInstance();
        dialog.setListener(new DialogEditList.EditListDialogListener() {

            @Override
            public void onFinishEditDialog(long id) {
                openList(id);
            }
        });
        dialog.show(getSupportFragmentManager(), "fragment_create_list");
    }

    @Override
    public void editList(long id) {
        DialogEditList_ dialog = DialogEditList_.getInstance(id);
        dialog.setListener(new DialogEditList.EditListDialogListener() {

            @Override
            public void onFinishEditDialog(long id) {
                //openList(id);
            }
        });
        dialog.show(getSupportFragmentManager(), "fragment_create_list");
    }

    @Override
    public void openSettings() {
        Intent intent = new Intent();
        intent.setClass(this, ActivitySettings.class);
        startActivity(intent);
    }

    @Override
    public void openAbout() {
        DialogAbout.showDialog(getSupportFragmentManager());
    }

    @Override
    public void openTask(final Uri taskUri, final long listId, final View origin) {
        // Todo change activity
        final Intent intent = new Intent().setAction(Intent.ACTION_EDIT).setClass(this,
                ActivityEditor.class).setData(taskUri).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);
        // User clicked a task in the list
        // tablet
        // todo tablet
        /*if (fragment2 != null) {
            // Set the intent here also so rotations open the same item
            setIntent(intent);
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim
                    .slide_in_top, R.anim.slide_out_bottom).replace(R.id.fragment2,
                    TaskDetailFragment_.getInstance(taskUri)).commitAllowingStateLoss();
            taskHint.setVisibility(View.GONE);
        }
        // phone
        else {*/
        startActivity(intent);
        // }
        //}
    }

    /**
     * Show a snackbar indicating that items were deleted, together with an undo button.
     */
    @Override
    public void deleteTasksWithUndo(Snackbar.Callback dismissCallback,
            final View.OnClickListener listener, final Task... tasks) {
        CharSequence text;
        try {
            text = getResources().getQuantityString(R.plurals.notedeleted_msg, tasks.length,
                    tasks.length);
        } catch (Exception e) {
            // Protect against faulty translations
            text = getResources().getString(R.string.deleted);
        }
        Snackbar.make(mFab, text, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, listener)
                .setCallback(dismissCallback)
                .show();
    }

    public void addTask() {
        addTaskInList("", ListHelper.getARealList(this, mCurrentList));
    }

    public void addTaskInList(final String text, final long listId) {
        if (listId < 1) {
            // Cant add to invalid lists
            Snackbar.make(mFab, "Please create a list first", Snackbar.LENGTH_LONG).show();
            return;
        }
        final Intent intent = new Intent().setAction(Intent.ACTION_INSERT).setClass(this,
                ActivityEditor.class).setData(Task.URI)//.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);
        // todo tablet
        /*if (fragment2 != null) {
            // Set intent to preserve state when rotating
            setIntent(intent);
            // Replace editor fragment
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim
                    .slide_in_top, R.anim.slide_out_bottom).replace(R.id.fragment2,
                    TaskDetailFragment_.getInstance(text, listId)).commitAllowingStateLoss();
            taskHint.setVisibility(View.GONE);
        } else {*/
        // Open an activity
        startActivity(intent);
        //}
    }
}
