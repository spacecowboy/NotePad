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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.helpers.PermissionsHelper;
import com.nononsenseapps.helpers.PreferencesHelper;
import com.nononsenseapps.helpers.SyncStatusMonitor;
import com.nononsenseapps.helpers.SyncStatusMonitor.OnSyncStartStopListener;
import com.nononsenseapps.helpers.ThemeHelper;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.databinding.ActivityMainBinding;
import com.nononsenseapps.notepad.fragments.DialogEditList;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment_;
import com.nononsenseapps.notepad.fragments.TaskListFragment;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;
import com.nononsenseapps.notepad.interfaces.ListOpener;
import com.nononsenseapps.notepad.interfaces.MenuStateController;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.notepad.prefs.AppearancePrefs;
import com.nononsenseapps.notepad.prefs.PrefsActivity;
import com.nononsenseapps.notepad.sync.orgsync.BackgroundSyncScheduler;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;
import com.nononsenseapps.ui.ExtraTypesCursorAdapter;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.UiThread.Propagation;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * This is extended by {@link ActivityMain_}. It was renamed to ActivityList
 * in release 6.0.0 beta, it has to do with getting rid of the annotations
 * library that generates {@link ActivityMain_}
 */
@EActivity(R.layout.activity_main)
public class ActivityMain extends AppCompatActivity
		implements OnFragmentInteractionListener, OnSyncStartStopListener,
		MenuStateController, OnSharedPreferenceChangeListener {

	// Set to true in bundle if exits should be animated
	public static final String ANIMATEEXIT = "animateexit";
	// Using tags for test
	public static final String DETAILTAG = "detailfragment";
	public static final String LISTPAGERTAG = "listpagerfragment";

	@ViewById(resName = "leftDrawer")
	ListView leftDrawer;

	@ViewById(resName = "drawerLayout")
	DrawerLayout drawerLayout;

	@ViewById(resName = "fragment1")
	View fragment1;

	// Only present on tablets
	@ViewById(resName = "fragment2")
	View fragment2;

	// Shown on tablets on start up. Hide on selection
	@ViewById(resName = "taskHint")
	View taskHint;

	/**
	 * Which slide animations the {@link TaskDetailFragment} should use for enter and exit
	 */
	boolean mReverseAnimation = false;

	boolean mAnimateExit = false;

	/**
	 * Changes depending on what we're showing since the started activity can receive new intents
	 */
	@InstanceState
	boolean isShowingEditor = false;

	boolean isDrawerClosed = true;
	SyncStatusMonitor syncStatusReceiver = null;

	// WIll only be the viewpager fragment
	ListOpener listOpener = null;

	/**
	 * Helper component that ties the action bar to the navigation drawer.
	 */
	private ActionBarDrawerToggle mDrawerToggle;

	// Only not if opening note directly
	private boolean shouldAddToBackStack = true;

	private Bundle state;
	private boolean shouldRestart = false;

	/**
	 * for both {@link R.layout#activity_main}
	 */
	private ActivityMainBinding mBinding;

	/**
	 * called when you rotate the screen, for example. With this, the {@link ActivityMain} can
	 * remember if the task detail view was showing before. Better than
	 * {@link AppCompatActivity#onRestoreInstanceState(Bundle)} because this runs before
	 * {@link #onCreate(Bundle)}, and it's important.
	 */
	private void restoreSavedInstanceState_(Bundle savedInstanceState) {
		if (savedInstanceState == null) return;
		isShowingEditor = savedInstanceState.getBoolean("isShowingEditor");
	}

	/*
		@Override
		public void onSaveInstanceState(@NonNull Bundle bundle_) {
			super.onSaveInstanceState(bundle_);
			bundle_.putBoolean("isShowingEditor", isShowingEditor);
		}
	*/
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerToggle != null) mDrawerToggle.syncState();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(R.id.activity_menu_group, isDrawerClosed);
		menu.setGroupVisible(R.id.activity_reverse_menu_group, !isDrawerClosed);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle your other action bar items...
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			if (isShowingEditor) {
				// Only true in portrait mode
				final View focusView = ActivityMain.this.getCurrentFocus();
				InputMethodManager inputManager = this.getSystemService(InputMethodManager.class);
				if (inputManager != null && focusView != null) {
					inputManager.hideSoftInputFromWindow(focusView.getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
				}

				// Should load the same list again
				// Try getting the list from the original intent
				final long listId = ActivityMainHelper.getListId(getIntent());

				final Intent intent = new Intent()
						.setAction(Intent.ACTION_VIEW)
						.setClass(ActivityMain.this, ActivityMain_.class);
				if (listId > 0) {
					intent.setData(TaskList.getUri(listId));
				}

				// Set the intent before, so we set the correct
				// action bar
				setIntent(intent);
				while (getSupportFragmentManager().popBackStackImmediate()) {
					// Need to pop the entire stack and then load
				}

				mReverseAnimation = true;
				NnnLogger.debug(ActivityMain.class,
						"starting activity from android.R.id.home");

				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			} else {
				// Handled by the drawer
			}
			return true;
		} else if (itemId == R.id.drawer_menu_createlist) {
			// Show fragment
			DialogEditList dialog = DialogEditList.getInstance();
			dialog.setListener(this::openList);
			dialog.show(getSupportFragmentManager(), "fragment_create_list");
			return true;
		} else if (itemId == R.id.menu_preferences) {
			Intent intent = new Intent();
			intent.setClass(this, PrefsActivity.class);
			startActivity(intent);
			return true;
		} else if (itemId == R.id.menu_sync) {
			handleSyncRequest();
			return true;
		} else if (itemId == R.id.menu_delete) {
			return false;
		} else {
			return false;
		}
	}

	@Override
	public void finish() {
		super.finish();
		// Only animate when specified. Should be when it was animated "in"
		if (mAnimateExit) {
			overridePendingTransition(R.anim.activity_slide_in_right,
					R.anim.activity_slide_out_right_full);
		}
	}


	/**
	 * Opens the specified list and closes the left drawer
	 */
	void openList(final long id) {
		// Open list
		Intent i = new Intent(ActivityMain.this, ActivityMain_.class)
				.setAction(Intent.ACTION_VIEW)
				.setData(TaskList.getUri(id))
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		// If editor is on screen, we need to reload fragments
		if (listOpener == null) {
			while (getSupportFragmentManager().popBackStackImmediate()) {
				// Need to pop the entire stack and then load
			}
			mReverseAnimation = true;
			startActivity(i);
		} else {
			// If not popped, then send the call to the fragment
			// directly
			NnnLogger.debug(ActivityMain.class, "calling listOpener");
			listOpener.openList(id);
		}

		// And then close drawer
		if (drawerLayout != null && leftDrawer != null) {
			drawerLayout.closeDrawer(leftDrawer);
		}
	}

	private void handleSyncRequest() {
		if (!PreferencesHelper.isSincEnabledAtAll(this)) {
			Toast.makeText(this, R.string.no_sync_method_chosen,
					Toast.LENGTH_SHORT).show();
			setRefreshOfAllSwipeLayoutsTo(false);
			return;
		}

		boolean syncing = false;

		if (OrgSyncService.areAnyEnabled(this)) {
			syncing = true;
			OrgSyncService.start(this);
		}

		if (syncing) {
			// In case of connectivity problems, stop the progress bar
			Handler handler = new Handler(Looper.getMainLooper());
			Executors.newSingleThreadExecutor().execute(() -> {
				// Background work here
				try {
					Thread.sleep(30 * 1000);
				} catch (InterruptedException e) {
					NnnLogger.exception(e);
				}

				handler.post(() -> {
					// UI Thread work here

					// Notify that the refresh has finished
					setRefreshOfAllSwipeLayoutsTo(false);
				});
			});
		} else {
			// explain to the user why the swipe-refresh was canceled
			Toast.makeText(this, R.string.no_sync_method_chosen,
					Toast.LENGTH_SHORT).show();
			setRefreshOfAllSwipeLayoutsTo(false);
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle b) {
		// Must do this before super.onCreate
		ThemeHelper.setTheme(this);
		ActivityHelper.setSelectedLanguage(this);
		super.onCreate(b);
		/*
		View bindings don't work here, you must keep android annotations
		restoreSavedInstanceState_(b);
		super.onCreate(b);
		mBinding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		loadContent();
		 */

		syncStatusReceiver = new SyncStatusMonitor();

		// First load, then don't add to backstack
		shouldAddToBackStack = false;

		// To know if we should animate exits
		if (getIntent() != null && getIntent().getBooleanExtra(ANIMATEEXIT, false)) {
			mAnimateExit = true;
		}

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// To listen on fragment changes
		getSupportFragmentManager().addOnBackStackChangedListener(() -> {
					if (isShowingEditor && !ActivityMainHelper.isNoteIntent(getIntent())) {
						setHomeAsDrawer(true);
					}
					// Always update menu
					invalidateOptionsMenu();
				}
		);

		if (b != null) {
			NnnLogger.debug(ActivityMain.class, "Activity Saved not null: " + b);
			this.state = b;
		}

		// Setup FAB. TODO are we going to add one ?
//		 mFab = (FloatingActionButton) findViewById(R.id.fab);
//		 mFab.setOnClickListener(view -> {
//			 //addTaskInList("", ListHelper.getARealList(this, id_of_the_list));
//		 });

		// Clear possible notifications, schedule future ones
		final Intent intent = getIntent();
		// Clear notification if present
		NotificationHelper.clearNotification(this, intent);
		// Schedule notifications
		NotificationHelper.schedule(this);
		// Schedule syncs
		BackgroundSyncScheduler.scheduleSync(this);
		// Sync if appropriate
		OrgSyncService.start(this);

		// keep showing the popup to ask for notification permissions on startup.
		// The callback function doesn't matter. Android will stop showing it if
		// the user denies the permission twice.
		if (!PermissionsHelper.hasPermissions(this, PermissionsHelper.FOR_NOTIFICATIONS))
			this.requestPermissions(PermissionsHelper.FOR_NOTIFICATIONS,
					PermissionsHelper.REQCODE_NOTIFICATIONS);
	}

	@Override
	public void onDestroy() {
		// avoid crashes due to drawer's resources being called when the activity is closing
		leftDrawer.setAdapter(null);

		super.onDestroy();
		OrgSyncService.stop(this);
	}

	@Override
	public void onBackPressed() {
		if (drawerLayout.isDrawerOpen(leftDrawer)) {
			// close the drawer on the left if it's open
			drawerLayout.closeDrawer(leftDrawer);
			return;
		}

		// Reset intent so we get proper fragment handling when the stack pops
		if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
			// if you remove this, it shows the wrong icons in the actionbar
			// when you navigate back from TaskDetailView
			setIntent(new Intent(this, ActivityMain_.class));
		}

		super.onBackPressed();
	}

	@Override
	public void onPause() {
		super.onPause();
		// deactivate monitor
		if (syncStatusReceiver != null) {
			syncStatusReceiver.stopMonitoring();
		}
		// deactivate any progress bar
		setRefreshOfAllSwipeLayoutsTo(false);
		// Pause sync monitors
		OrgSyncService.pause(this);
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		loadFragments();
		// Just to be sure it gets done. Clear notification if present
		NotificationHelper.clearNotification(this, intent);
	}

	@Override
	public void onResume() {
		if (shouldRestart) {
			restartAndRefresh();
		}
		super.onResume();

		// activate monitor
		if (syncStatusReceiver != null) {
			syncStatusReceiver.startMonitoring(this, this);
		}
		OrgSyncService.start(this);
	}

	/**
	 * Restarts the activity using the same intent that started it.
	 * Disables animations to get a seamless restart.
	 */
	private void restartAndRefresh() {
		shouldRestart = false;
		Intent intent = getIntent();
		overridePendingTransition(0, 0);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intent);
	}

	@UiThread(propagation = Propagation.REUSE)
	void loadFragments() {
		final Intent intent = getIntent();

		// Mandatory
		Fragment left = null;
		String leftTag = null;
		// Only if fragment2 is not null
		Fragment right = null;

		if (this.state != null) {
			this.state = null;
			if (isShowingEditor && fragment2 != null) {
				// Should only be true in portrait
				isShowingEditor = false;
			}

			// Find fragments
			// This is an instance state variable
			if (isShowingEditor) {
				// Portrait, with editor, modify action bar
				setHomeAsDrawer(false);
				// Done
				return;
			} else {
				// Find the listpager
				left = getSupportFragmentManager().findFragmentByTag(LISTPAGERTAG);
				listOpener = (ListOpener) left;

				if (left != null && fragment2 == null) {
					// Done
					return;
				} else if (left != null && fragment2 != null) {
					right = getSupportFragmentManager().findFragmentByTag(DETAILTAG);
				}

				if (left != null && right != null) {
					// Done
					return;
				}
			}
		}

		// Load stuff
		final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if (mReverseAnimation) {
			mReverseAnimation = false;
			transaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top,
					R.anim.slide_in_top, R.anim.slide_out_bottom);
		} else {
			transaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom,
					R.anim.slide_in_bottom, R.anim.slide_out_top);
		}

		// If it contains a noteId, load an editor. If also tablet, load the lists
		if (fragment2 != null) {
			if (ActivityMainHelper.getNoteId(intent) > 0) {
				right = TaskDetailFragment_.getInstance(ActivityMainHelper.getNoteId(intent));
			} else if (ActivityMainHelper.isNoteIntent(intent)) {
				// some text was shared to this app
				right = TaskDetailFragment_.getInstance(ActivityMainHelper.getNoteShareText(intent),
						TaskListViewPagerFragment.getAShowList(this,
								ActivityMainHelper.getListId(intent)));
			}
		} else if (ActivityMainHelper.isNoteIntent(intent)) {
			isShowingEditor = true;
			listOpener = null;
			leftTag = DETAILTAG;
			if (ActivityMainHelper.getNoteId(intent) > 0) {
				left = TaskDetailFragment_.getInstance(ActivityMainHelper.getNoteId(intent));
			} else {
				// Get a share text (null safe)
				// In a list (if specified, or default otherwise)
				left = TaskDetailFragment_.getInstance(
						ActivityMainHelper.getNoteShareText(intent),
						TaskListViewPagerFragment
								.getARealList(this, ActivityMainHelper.getListId(intent))
				);
			}
			// fucking stack
			while (getSupportFragmentManager().popBackStackImmediate()) {
				// Need to pop the entire stack and then load
			}
			if (shouldAddToBackStack) {
				transaction.addToBackStack(null);
			}
			setHomeAsDrawer(false);
		}
		/*
		 * Other case, is a list id or a tablet
		 */
		if (!ActivityMainHelper.isNoteIntent(intent) || fragment2 != null) {
			// If we're no longer in the editor, reset the action bar
			if (fragment2 == null) {
				setHomeAsDrawer(true);
			}
			// TODO
			isShowingEditor = false;

			left = TaskListViewPagerFragment.getInstance(
					ActivityMainHelper.getListIdToShow(intent,this));
			leftTag = LISTPAGERTAG;
			listOpener = (ListOpener) left;
		}

		if (fragment2 != null && right != null) {
			transaction.replace(R.id.fragment2, right, DETAILTAG);
			taskHint.setVisibility(View.GONE);
		}
		transaction.replace(R.id.fragment1, left, leftTag);

		// Commit transaction. Allow state loss as workaround for bug
		// https://code.google.com/p/android/issues/detail?id=19917
		transaction.commitAllowingStateLoss();
		// Next go, always add
		shouldAddToBackStack = true;
	}



	void setHomeAsDrawer(final boolean value) {
		mDrawerToggle.setDrawerIndicatorEnabled(value);
	}

	/**
	 * Loads the appropriate fragments depending on state and intent.
	 */
	@AfterViews
	protected void loadContent() {
		loadLeftDrawer();
		loadFragments();
	}

	/**
	 * Load a list of lists in the left drawer
	 */
	protected void loadLeftDrawer() {
		// TODO very long function. you should move everything related to drawer
		//  into static methods in ActivityMainHelper.java

		// TODO handle being called repeatably better?
		// Set a listener on drawer events
		if (mDrawerToggle == null) {
			// ActionBarDrawerToggle ties together the the proper interactions
			// between the navigation drawer and the action bar app icon.
			mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
					R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

				/**
				 * Called when a drawer has settled in a completely closed state.
				 */
				@Override
				public void onDrawerClosed(View view) {
					// hide 'Notes' (R.string.app_name_short) from the toolbar
					getSupportActionBar().setDisplayShowTitleEnabled(false);
					isDrawerClosed = true;
					invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
				}

				@Override
				public void onDrawerStateChanged(int newState) {
					super.onDrawerStateChanged(newState);

					// If it's not idle, it isn't closed
					if (DrawerLayout.STATE_IDLE != newState) {
						getSupportActionBar().setDisplayShowTitleEnabled(true);
						getSupportActionBar().setTitle(R.string.show_from_all_lists);
						// Is in motion, hide action items
						isDrawerClosed = false;
						invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
					}
				}
			};

			// Set the drawer toggle as the DrawerListener
			drawerLayout.setDrawerListener(mDrawerToggle);
		}

		ActionBar supActBar = getSupportActionBar();
		if (supActBar == null) {
			NnnLogger.error(ActivityMain.class,
					"Coding error: actionbar is null. A crash will follow");
		} else {
			supActBar.setDisplayHomeAsUpEnabled(true);
			supActBar.setHomeButtonEnabled(true);
			// hide 'Notes' (R.string.app_name_short) from the toolbar
			supActBar.setDisplayShowTitleEnabled(false);
		}

		// Use extra items. From top to bottom, they are "TASKS", "Overdue", "Today",
		// "Next 5 days", "Lists". Note that 2 of those are used as section titles & dividers
		final int[] extraIds = new int[] { -1, TaskListFragment.LIST_ID_OVERDUE,
				TaskListFragment.LIST_ID_TODAY, TaskListFragment.LIST_ID_WEEK, -1 };

		// The corresponding names. This is fine for initial conditions
		final int[] extraStrings = new int[] { R.string.tasks,
				R.string.date_header_overdue,
				R.string.date_header_today,
				R.string.next_5_days,
				R.string.lists };

		// Use this for real data
		final ArrayList<ArrayList<Object>> extraData = new ArrayList<>();
		// Task header
		extraData.add(new ArrayList<>());
		extraData.get(0).add(R.string.tasks);
		// Overdue
		extraData.add(new ArrayList<>());
		extraData.get(1).add(R.string.date_header_overdue);
		// Today
		extraData.add(new ArrayList<>());
		extraData.get(2).add(R.string.date_header_today);
		// Week
		extraData.add(new ArrayList<>());
		extraData.get(3).add(R.string.next_5_days);
		// Lists header
		extraData.add(new ArrayList<>());
		extraData.get(4).add(R.string.lists);

		final int[] extraTypes = new int[] { 1, 0, 0, 0, 1 };

		final ExtraTypesCursorAdapter adapter = new ExtraTypesCursorAdapter(
				this,
				R.layout.simple_listitem,
				null,
				new String[] { TaskList.Columns.TITLE, TaskList.Columns.VIEW_COUNT },
				new int[] { android.R.id.text1, android.R.id.text2 },
				extraIds, // id -1 for headers, ignore clicks on them
				extraStrings,
				extraTypes,
				new int[] { R.layout.drawer_header }
		);
		adapter.setExtraData(extraData);

		leftDrawer.setAdapter(adapter);

		// Set click handler (go to list)
		leftDrawer.setOnItemClickListener((arg0, v, pos, id) -> {
			if (id < -1) {
				// Set preference which type was chosen
				PreferenceManager
						.getDefaultSharedPreferences(ActivityMain.this)
						.edit()
						.putLong(TaskListFragment.LIST_ALL_ID_PREF_KEY, id)
						.commit();
			}
			openList(id);
		});

		// set long-click handler (open popup)
		leftDrawer.setOnItemLongClickListener((arg0, arg1, pos, id) -> {
			// Open dialog to edit list
			if (id > 0) {
				DialogEditList dialog = DialogEditList.getInstance(id);
				dialog.show(getSupportFragmentManager(), "fragment_edit_list");
				return true;
			} else if (id < -1) {
				// Set as "default"
				PreferenceManager
						.getDefaultSharedPreferences(ActivityMain.this)
						.edit()
						.putLong(getString(R.string.pref_defaultstartlist), id)
						.putLong(TaskListFragment.LIST_ALL_ID_PREF_KEY, id)
						.commit();
				Toast.makeText(ActivityMain.this, R.string.new_default_set,
						Toast.LENGTH_SHORT).show();
				// openList(id);
				return true;
			} else {
				return false;
			}
		});

		// Load count of tasks in each list, to show the number next to the list's name

		// Define the callback handler
		final LoaderCallbacks<Cursor> callbacks = new LoaderCallbacks<>() {

			final String[] COUNTROWS = new String[] { "COUNT(1)" };
			final String NOTCOMPLETED = Task.Columns.COMPLETED + " IS NULL ";

			@NonNull
			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
				// Normal lists
				switch (id) {
					case TaskListFragment.LIST_ID_OVERDUE:
						return new CursorLoader(ActivityMain.this, Task.URI, COUNTROWS,
								NOTCOMPLETED + TaskListFragment.andWhereOverdue(),
								null, null);
					case TaskListFragment.LIST_ID_TODAY:
						return new CursorLoader(ActivityMain.this, Task.URI, COUNTROWS,
								NOTCOMPLETED + TaskListFragment.andWhereToday(),
								null, null);
					case TaskListFragment.LIST_ID_WEEK:
						return new CursorLoader(ActivityMain.this, Task.URI, COUNTROWS,
								NOTCOMPLETED + TaskListFragment.andWhereWeek(),
								null, null);
					case 0:
					default:
						return new CursorLoader(ActivityMain.this, TaskList.URI_WITH_COUNT,
								new String[] { TaskList.Columns._ID, TaskList.Columns.TITLE,
										TaskList.Columns.VIEW_COUNT }, null,
								null, getResources()
								.getString(R.string.const_as_alphabetic,
										TaskList.Columns.TITLE));
				}
			}

			@Override
			public void onLoadFinished(Loader<Cursor> l, Cursor c) {
				switch (l.getId()) {
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
					case 0:
					default:
						adapter.swapCursor(c);
				}
			}

			private void updateExtra(final int pos, final int count) {
				while (extraData.get(pos).size() < 2) {
					// To avoid crashes
					extraData.get(pos).add("0");
				}
				extraData.get(pos).set(1, Integer.toString(count));
				adapter.notifyDataSetChanged();
			}

			@Override
			public void onLoaderReset(Loader<Cursor> l) {
				switch (l.getId()) {
					case TaskListFragment.LIST_ID_OVERDUE:
					case TaskListFragment.LIST_ID_TODAY:
					case TaskListFragment.LIST_ID_WEEK:
						break;
					case 0:
					default:
						adapter.swapCursor(null);
				}
			}
		};

		// Load actual data
		LoaderManager
				.getInstance(this)
				.restartLoader(0, null, callbacks);
		// special views
		LoaderManager
				.getInstance(this)
				.restartLoader(TaskListFragment.LIST_ID_OVERDUE, null, callbacks);
		LoaderManager
				.getInstance(this)
				.restartLoader(TaskListFragment.LIST_ID_TODAY, null, callbacks);
		LoaderManager
				.getInstance(this)
				.restartLoader(TaskListFragment.LIST_ID_WEEK, null, callbacks);
	}

	@Override
	public void onFragmentInteraction(final Uri taskUri, final long listId, final View origin) {
		final Intent intent = new Intent()
				.setAction(Intent.ACTION_EDIT)
				.setClass(this, ActivityMain_.class)
				.setData(taskUri)
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
				.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);
		// User clicked a task in the list
		if (fragment2 != null) {
			// tablet

			// Set the intent here also so rotations open the same item
			setIntent(intent);
			getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom)
					.replace(R.id.fragment2, TaskDetailFragment_.getInstance(taskUri))
					.commitAllowingStateLoss();
			taskHint.setVisibility(View.GONE);
		} else {
			// phone
			startActivity(intent);
		}
	}

	@Override
	public void addTaskInList(final String text, final long listId) {
		if (listId < 1) {
			// Cant add to invalid lists
			// Snackbar.make(mFab, "Please create a list first", Snackbar.LENGTH_LONG).show();
			return;
		}
		final Intent intent = new Intent()
				.setAction(Intent.ACTION_INSERT)
				.setClass(this, ActivityMain_.class)
				.setData(Task.URI)
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
				.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);
		if (fragment2 != null) {
			// Set intent to preserve state when rotating
			setIntent(intent);
			// Replace editor fragment
			getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom)
					.replace(R.id.fragment2, TaskDetailFragment_.getInstance(text, listId), DETAILTAG)
					.commitAllowingStateLoss();
			taskHint.setVisibility(View.GONE);
		} else {
			// Open an activity
			startActivity(intent);
		}
	}

	@Override
	public void closeFragment(final Fragment fragment) {
		if (fragment2 != null) {
			getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom)
					.remove(fragment)
					.commitAllowingStateLoss();
			taskHint.setAlpha(0f);
			taskHint.setVisibility(View.VISIBLE);
			taskHint.animate().alpha(1f).setStartDelay(500);
		} else {
			// Phone case, simulate back button
			simulateBack();
		}
	}

	private void simulateBack() {
		if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
			setIntent(new Intent(this, ActivityMain_.class));
		}

		if (!getSupportFragmentManager().popBackStackImmediate()) {
			finish();
		}
	}

	@Override
	public boolean childItemsVisible() {
		return isDrawerClosed;
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
		if (key == null) {
			// it happens sometimes during Espresso tests
			return;
		}
		if (key.equals(AppearancePrefs.KEY_THEME) || key.equals(getString(R.string.pref_locale))) {
			shouldRestart = true;
		} else if (key.startsWith("pref_restart")) {
			shouldRestart = true;
		}
	}

	// holds all the swipe-to-refresh layouts of the various TaskListFragments
	private final ArrayList<SwipeRefreshLayout> swpRefLayouts = new ArrayList<>();

	/**
	 * every {@link TaskListFragment} has its own instance of a {@link SwipeRefreshLayout},
	 * so here they're all added to a private list. Then, the {@link ActivityMain} will update
	 * them all when necessary
	 */
	public void addSwipeRefreshLayoutToList(SwipeRefreshLayout newSwpRefLayout) {
		// TODO do this Only if some sync is enabled

		// Show the accent color on the arrow while loading
		newSwpRefLayout.setColorSchemeResources(R.color.accent);

		// TODO the swipe-to-refresh layouts have been disabled because they make it impossible
		//  to manually drag down the 1° note. When you find a solution for this, delete this line:
		newSwpRefLayout.setEnabled(false);

		// Sets up a Listener that is invoked when the user performs a swipe-to-refresh gesture.
		newSwpRefLayout.setOnRefreshListener(
				() -> {
					Log.i("NNN", "onRefresh called from SwipeRefreshLayout");

					// This method performs the actual data-refresh operation.
					// The method must call setRefreshing(false) when it's finished.
					handleSyncRequest();
				}
		);
		swpRefLayouts.add(newSwpRefLayout);
	}

	/**
	 * sets the refreshing status of all {@link SwipeRefreshLayout} in this activity
	 *
	 * @param newState FALSE if they should stop the animation, TRUE if they should show it
	 */
	private void setRefreshOfAllSwipeLayoutsTo(boolean newState) {
		for (SwipeRefreshLayout layout : swpRefLayouts) {
			layout.setRefreshing(newState);
		}
	}

	@Override
	public void onSyncStartStop(final boolean isOngoing) {
		// Notify PullToRefreshAttacher of the refresh state
		this.runOnUiThread(() -> setRefreshOfAllSwipeLayoutsTo(isOngoing));
	}
}
