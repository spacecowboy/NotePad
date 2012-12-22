/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import sheetrock.panda.changelog.ChangeLog;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.helpers.UpdateNotifier;
import com.nononsenseapps.helpers.dualpane.DualLayoutActivity;
import com.nononsenseapps.notepad.NotesListFragment.Callbacks;
import com.nononsenseapps.notepad.PasswordDialog.ActionResult;
import com.nononsenseapps.notepad.interfaces.PasswordChecker;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.prefs.PrefsActivity;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.SyncAdapter;
import com.nononsenseapps.ui.ListPagerAdapter;

import android.content.res.Configuration;

/**
 * Showing a single fragment in an activity.
 */
public class MainActivity extends DualLayoutActivity implements
		OnSharedPreferenceChangeListener, OnNavigationListener,
		LoaderManager.LoaderCallbacks<Cursor>, PasswordChecker, Callbacks {
	private static final String TAG = "FragmentLayout";
	private static final String CURRENT_LIST_ID = "currentlistid";
	private static final String CURRENT_LIST_POS = "currentlistpos";
	private static final String RESUMING = "resuming";
	private static final int CREATE_LIST = 0;
	private static final int RENAME_LIST = 1;
	private static final int DELETE_LIST = 2;
	// public static boolean lightTheme = false;
	public static String currentTheme = MainPrefs.THEME_LIGHT;
	public static boolean shouldRestart = false;

	// For my special dropdown navigation items
	public static final int ALL_NOTES_ID = -2;
	public static final int CREATE_LIST_ID = -3;

	private SimpleCursorAdapter mSpinnerAdapter;
	private SimpleCursorAdapter mSectionAdapter;
	private long currentListId = -1;
	private int currentListPos = 0;

	private boolean resuming = false;

	private long listIdToSelect = -1;
	private boolean beforeBoot = false; // Used to indicate the intent handling
	// private NotesListFragment list;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private ListPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	private long noteIdToSelect;
	private Intent noteIntentToSelect;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Must set theme before calling super
		readAndSetSettings();
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			resuming = savedInstanceState.getBoolean(RESUMING);
		}

		if (currentContent.equals(CONTENTVIEW.DUAL)
				|| currentContent.equals(CONTENTVIEW.LEFT)) {
			leftOrTabletCreate(savedInstanceState);
		} else {
			rightCreate();
		}

		// Synchronize on app open
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String accountName = prefs.getString(SyncPrefs.KEY_ACCOUNT, "");
		boolean syncEnabled = prefs
				.getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false);
		boolean syncOnStart = prefs.getBoolean(SyncPrefs.KEY_SYNC_ON_START,
				true);
		if (accountName != null && !accountName.equals("") && syncEnabled
				&& syncOnStart) {
			requestSync(accountName);
		}
	}

	private void leftOrTabletCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			// currentListId = savedInstanceState.getLong(CURRENT_LIST_ID);
			listIdToSelect = savedInstanceState.getLong(CURRENT_LIST_ID);
			// currentListPos = savedInstanceState.getInt(CURRENT_LIST_POS);
		}

		// Set up dropdown navigation
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Will set cursor in Loader
		// mSpinnerAdapter = new ExtrasCursorAdapter(this,
		// R.layout.actionbar_dropdown_item, null,
		// new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
		// new int[] { android.R.id.text1 }, new int[] { -9, -8 },
		// new int[] { R.string.show_from_all_lists, R.string.error_title });
		mSpinnerAdapter = new SimpleCursorAdapter(this,
				R.layout.actionbar_dropdown_item, null,
				new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 }, 0);

		mSpinnerAdapter
				.setDropDownViewResource(R.layout.actionbar_dropdown_item);

		// This will listen for navigation callbacks
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

		// setContentView(R.layout.fragment_layout);

		// setUpList();

		mSectionAdapter = new SimpleCursorAdapter(this,
				R.layout.actionbar_dropdown_item, null,
				new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 }, 0);

		mSectionsPagerAdapter = new ListPagerAdapter(this,
				getFragmentManager(), mSectionAdapter);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.leftFragment);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int pos) {
				currentListId = mSectionsPagerAdapter.getItemId(pos);
				currentListPos = pos;
				actionBar.setSelectedNavigationItem(pos);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});

		// Set up navigation list
		// Set a default list to open if one is set
		if (listIdToSelect < 0) {
			listIdToSelect = getAList(this, -1);
			// Select the first note in that list to open also
			noteIdToSelect = getANote(this, listIdToSelect);
		}
		// Handle the intent first, so we know what to possibly select once
		// the
		// loader is finished
		beforeBoot = true;

		if (!resuming) {
			onNewIntent(getIntent());
		}

		getLoaderManager().initLoader(0, null, this);
	}

	private static long getANote(Context context, long listId) {
		long noteId = -1;
		if (listId < 0)
			return noteId;

		Cursor cursor;
		final String listString = Long.toString(listId);
		cursor = context.getContentResolver().query(
				NotePad.Notes.CONTENT_VISIBLE_URI,
				new String[] { NotePad.Notes._ID },
				NotePad.Notes.COLUMN_NAME_LIST + " IS ?",
				new String[] { listString },
				NotePad.Notes.SORT_ORDER + " LIMIT 1");

		if (cursor != null && cursor.moveToFirst()) {
			noteId = cursor.getLong(cursor.getColumnIndex(NotePad.Notes._ID));
			Log.d(TAG, "getANote: " + noteId);
		}
		if (cursor != null)
			cursor.close();

		return noteId;
	}

	private void rightCreate() {
		// Set up navigation (adds nice arrow to icon)
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(false);
		}
		if (!resuming) {
			onNewIntent(getIntent());
		}
	}

	// private void setUpList() {
	// if (leftFragment != null) {
	// NotesListFragment list = (NotesListFragment) leftFragment;
	//
	// this.list = list;
	// }
	// }

	/**
	 * Launches the main activity
	 */
	@Override
	protected void goUp() {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setClass(this,
				MainActivity.class);

		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_options_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Hide entire sub menu in editor.
		if (currentContent == CONTENTVIEW.RIGHT) {
			MenuItem manageLists = menu.findItem(R.id.menu_managelists);
			if (manageLists != null)
				manageLists.setVisible(false);
			MenuItem clear = menu.findItem(R.id.menu_clearcompleted);
			if (clear != null)
				clear.setVisible(false);
			MenuItem sync = menu.findItem(R.id.menu_sync);
			if (sync != null)
				sync.setVisible(false);
		} else {
			MenuItem deleteList = menu.findItem(R.id.menu_deletelist);
			MenuItem renameList = menu.findItem(R.id.menu_renamelist);
			MenuItem setDefaultList = menu.findItem(R.id.menu_setdefaultlist);
			if (null != mSectionAdapter) {
				if (deleteList != null) {
					// Only show this button if there is a list
					if (mSectionAdapter.getCount() <= 0) {
						deleteList.setVisible(false);
					} else {
						deleteList.setVisible(true);
					}
				}
				if (renameList != null) {
					// Only show this button if there is a list
					if (mSectionAdapter.getCount() <= 0) {
						renameList.setVisible(false);
					} else {
						renameList.setVisible(true);
					}
				}
				if (setDefaultList != null) {
					// Only show this button if there is a list
					if (mSectionAdapter.getCount() <= 0) {
						setDefaultList.setVisible(false);
					} else {
						setDefaultList.setVisible(true);
					}
				}
			} else {
				// with null adapter, must hide
				if (deleteList != null) {
					deleteList.setVisible(false);
				}
				if (renameList != null) {
					renameList.setVisible(false);
				}
				if (setDefaultList != null)
					setDefaultList.setVisible(false);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * If the user has a search button, ideally he should be able to use it.
	 * Expand the search provider in that case
	 */
	@TargetApi(14)
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_SEARCH:
			// Ignore this in HC because it will crash there
			if (getResources().getBoolean(R.bool.atLeast14)) {
				NotesListFragment list = getLeftFragment();
				if (list != null && list.mSearchItem != null) {
					list.mSearchItem.expandActionView();
				} else if (list != null) {
					onSearchRequested();
				}
			}
			return true;
		case KeyEvent.KEYCODE_BACK:
			// Exit app
			finish();
			return true;
		}
		return false;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(RESUMING, true);
		// Save current list
		outState.putLong(CURRENT_LIST_ID, currentListId);
		outState.putInt(CURRENT_LIST_POS, currentListPos);
	}

	private void handleEditIntent(Intent intent) {
		// First, if we should display a list
		if (intent.getData() != null
				&& (intent.getData().getPath()
						.startsWith(NotePad.Lists.PATH_VISIBLE_LIST_ID) || intent
						.getData().getPath()
						.startsWith(NotePad.Lists.PATH_LIST_ID))) {
			// Get id to display
			String newId = intent.getData().getPathSegments()
					.get(NotePad.Lists.ID_PATH_POSITION);
			long listId = Long.parseLong(newId);
			// Handle it differently depending on if the app has already
			// loaded or not.
			openListFromIntent(listId, null);
		} else if (intent.getData() != null
				&& (intent.getData().getPath()
						.startsWith(NotePad.Notes.PATH_VISIBLE_NOTES) || intent
						.getData().getPath()
						.startsWith(NotePad.Notes.PATH_NOTES))) {
			openNoteFragment(intent);
		}
	}

	/**
	 * Creates an editor fragment for the note specified in the intent. Use for
	 * edit and complete intents.
	 * 
	 * @param intent
	 */
	private void openNoteFragment(Intent intent) {
		Log.d(TAG, "openNoteFragment");
		Bundle arguments = new Bundle();
		arguments.putLong(NotesEditorFragment.KEYID,
				NotesEditorFragment.getIdFromUri(intent.getData()));
		NotesEditorFragment fragment = new NotesEditorFragment();
		fragment.setArguments(arguments);
		getFragmentManager().beginTransaction()
				.replace(R.id.rightFragment, fragment).commit();
		// Open appropriate list if tablet mode
		if (this.currentContent == CONTENTVIEW.DUAL) {
			long listId = getAList(intent);
			/*
			 * Open the containing list if we have to. No need to change lists
			 * if we are already displaying all notes.
			 */
			openListFromIntent(listId, intent);
		}
	}

	private void handleInsertIntent(Intent intent) {
		if (intent.getType() != null
				&& intent.getType().equals(NotePad.Lists.CONTENT_TYPE)
				|| intent.getData() != null
				&& intent.getData().equals(NotePad.Lists.CONTENT_VISIBLE_URI)) {
			// get Title
			if (intent.getExtras() != null) {
				String title = intent.getExtras().getString(
						NotePad.Lists.COLUMN_NAME_TITLE, "");
				createList(title);
			}
		} else if (intent.getType() != null
				&& (intent.getType().equals(NotePad.Notes.CONTENT_TYPE) || intent
						.getType().startsWith("text/"))
				|| intent.getData() != null
				&& intent.getData().equals(NotePad.Notes.CONTENT_VISIBLE_URI)) {
			Log.d("FragmentLayout", "INSERT NOTE");
			// Get list to create note in first
			long listId = getAList(intent);
			String text = "";
			if (intent.getExtras() != null) {
				text = intent.getExtras()
						.getCharSequence(Intent.EXTRA_TEXT, "").toString();
			}

			if (listId > -1) {
				Uri noteUri = MainActivity.createNote(this, listId, text);

				if (noteUri != null) {
					Bundle arguments = new Bundle();
					arguments.putLong(NotesEditorFragment.KEYID,
							NotesEditorFragment.getIdFromUri(noteUri));
					NotesEditorFragment fragment = new NotesEditorFragment();
					fragment.setArguments(arguments);
					getFragmentManager().beginTransaction()
							.replace(R.id.rightFragment, fragment).commit();
				}

				// Open appropriate list if tablet mode
				if (this.currentContent == CONTENTVIEW.DUAL) {
					// Open the containing list if we have to. No need to
					// change
					// lists
					// if we are already displaying all notes.
					openListFromIntent(listId, intent);
				}
			}
		}
	}

	public static long getAList(Context context, long tempList) {
		long returnList = -1;

		if (tempList < 0) {
			// Then check if a default list is specified
			tempList = Long.parseLong(PreferenceManager
					.getDefaultSharedPreferences(context).getString(
							MainPrefs.KEY_DEFAULT_LIST, "-1"));
			Log.d(TAG, "Default list: " + tempList);
		}
		// Not guaranteed that this is valid. Check the database even if it
		// exists.
		// Limit to 1 result. Returns default list if it exists
		final String criteria = NotePad.Lists._ID + " = ?";
		Cursor cursor;
		if (tempList > -1) {
			final String listString = Long.toString(tempList);
			cursor = context.getContentResolver().query(
					NotePad.Lists.CONTENT_VISIBLE_URI,
					new String[] { NotePad.Lists._ID },
					criteria + " OR NOT (EXISTS (SELECT NULL FROM "
							+ NotePad.Lists.TABLE_NAME + " WHERE " + criteria
							+ "))", new String[] { listString, listString },
					NotePad.Lists.SORT_ORDER + " LIMIT 1");
		} else {
			cursor = context.getContentResolver().query(
					NotePad.Lists.CONTENT_VISIBLE_URI,
					new String[] { NotePad.Lists._ID }, null, null,
					NotePad.Lists.SORT_ORDER + " LIMIT 1");
		}
		if (cursor != null && cursor.moveToFirst()) {
			returnList = cursor.getLong(cursor
					.getColumnIndex(NotePad.Lists._ID));
			Log.d(TAG, "database list: " + returnList);
		}
		if (cursor != null)
			cursor.close();

		return returnList;
	}

	/**
	 * Will find a suitable list. Which is first ' If the intent contains a list
	 * or the default list, if no valid default list then the first list is
	 * returned. If there are no lists, then -1 is returned.
	 */
	private long getAList(Intent intent) {
		long tempList = -1;
		// First see if the intent specifies a list
		if (intent != null && intent.getExtras() != null) {
			long intentId = intent.getExtras().getLong(
					NotePad.Notes.COLUMN_NAME_LIST, -1);

			if (intentId > -1) {
				tempList = intentId;
				Log.d(TAG, "Intent list: " + intentId);
			}
		}

		return getAList(this, tempList);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// Search
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			// list.onQueryTextChange(query);
			NotesListFragment list = getLeftFragment();
			if (list != null && list.mSearchView != null) {
				list.mSearchView.setQuery(query, false);
			} else if (list != null) {
				list.onQueryTextSubmit(query);
			}
			// Edit or View a list or a note.
		} else if (Intent.ACTION_EDIT.equals(intent.getAction())
				|| Intent.ACTION_VIEW.equals(intent.getAction())) {
			handleEditIntent(intent);
		} else if (Intent.ACTION_INSERT.equals(intent.getAction())) {
			handleInsertIntent(intent);
		} else if (Intent.ACTION_SEND.equals(intent.getAction())) {
			handleInsertIntent(intent);
		} else if (getString(R.string.complete_note_broadcast_intent).equals(
				intent.getAction())) {
			// Sent from lock-screen widget on 4.2 and above.
			// Send complete broadcast and finish
			long noteId = NotesEditorFragment.getIdFromUri(intent.getData());
			// This will complete the note
			if (noteId > -1) {
				Intent bintent = new Intent(this,
						NotePadBroadcastReceiver.class);
				bintent.setAction(getString(R.string.complete_note_broadcast_intent));
				bintent.putExtra(NotePad.Notes._ID, noteId);
				Log.d(TAG, "Sending complete broadcast");
				sendBroadcast(bintent);

				openNoteFragment(intent);

				// Toast.makeText(this, getString(R.string.completed),
				// Toast.LENGTH_SHORT).show();
			}
		} else {
			// Open a note
			if (noteIdToSelect > -1 && currentContent == CONTENTVIEW.DUAL) {
				Bundle arguments = new Bundle();
				arguments.putLong(NotesEditorFragment.KEYID, noteIdToSelect);
				NotesEditorFragment fragment = new NotesEditorFragment();
				fragment.setArguments(arguments);
				getFragmentManager().beginTransaction()
						.replace(R.id.rightFragment, fragment).commit();
				noteIdToSelect = -1;
			}
		}
	}

	/**
	 * This is meant to be called from the intent handling. It handles the two
	 * possible cases that this app was already running when it received the
	 * intent or it was started fresh with the intent meaning we have to handle
	 * the opening asynchronously.
	 * 
	 * @param listId
	 */
	private void openListFromIntent(long listId, Intent intent) {
		if (beforeBoot) {
			// Set the variable to be selected after the loader has
			// finished its query
			listIdToSelect = listId;
			noteIntentToSelect = intent;
			Log.d(TAG, "beforeBoot setting future id");
		} else {
			// Select the list directly since the loader is done
			int pos = getPosOfId(listId);
			Log.d("FragmentLayout", "pos: " + pos);
			if (pos > -1) {
				// select it
				ActionBar ab = getActionBar();
				if (ab != null && ab.getSelectedNavigationIndex() != pos) {
					ab.setSelectedNavigationItem(pos);
				}
				NotesListFragment list = getLeftFragment();
				if (list != null) {
					list.handleNoteIntent(intent);
				}
			}
		}
	}

	@Override
	protected void onResume() {
		if (shouldRestart) {
			restartAndRefresh();
		}
		super.onResume();
		// Show changelog if it is new
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun())
			cl.getLogDialog().show();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DELETE_LIST:
			final Dialog deleteDialog = new Dialog(this);
			deleteDialog.setContentView(R.layout.delete_list_dialog);
			deleteDialog.setTitle(R.string.menu_deletelist);

			Button dYesButton = (Button) deleteDialog
					.findViewById(R.id.d_dialog_yes);
			dYesButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteCurrentList();
					deleteDialog.dismiss();
				}
			});

			Button dNoButton = (Button) deleteDialog
					.findViewById(R.id.d_dialog_no);
			dNoButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteDialog.cancel();
				}
			});
			return deleteDialog;
		case CREATE_LIST:
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.create_list_dialog);
			dialog.setTitle(R.string.menu_createlist);

			EditText title = (EditText) dialog.findViewById(R.id.editTitle);
			title.setText("");

			Button yesButton = (Button) dialog.findViewById(R.id.dialog_yes);
			yesButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					EditText title = (EditText) dialog
							.findViewById(R.id.editTitle);
					createList(title.getText().toString());
					title.setText("");
					dialog.dismiss();
				}
			});

			Button noButton = (Button) dialog.findViewById(R.id.dialog_no);
			noButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getActionBar().setSelectedNavigationItem(currentListPos);
					dialog.cancel();
				}
			});
			return dialog;
		case RENAME_LIST:
			final Dialog renameDialog = new Dialog(this);
			renameDialog.setContentView(R.layout.rename_list_dialog);
			renameDialog.setTitle(R.string.menu_renamelist);

			CharSequence currentTitle = "";
			if (mSectionsPagerAdapter != null)
				currentTitle = mSectionsPagerAdapter
						.getPageTitle(currentListPos);

			EditText renameTitle = (EditText) renameDialog
					.findViewById(R.id.renameTitle);
			renameTitle.setText(currentTitle);

			Button rYesButton = (Button) renameDialog
					.findViewById(R.id.r_dialog_yes);
			rYesButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					EditText renameTitle = (EditText) renameDialog
							.findViewById(R.id.renameTitle);
					renameList(renameTitle.getText().toString());
					renameTitle.setText("");
					renameDialog.dismiss();
				}
			});

			Button rNoButton = (Button) renameDialog
					.findViewById(R.id.r_dialog_no);
			rNoButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					renameDialog.cancel();
				}
			});
			return renameDialog;

		default:
			return null;
		}
	}

	protected void createList(String title) {
		// I will not allow empty names for lists
		if (!title.equals("")) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Lists.COLUMN_NAME_TITLE, title);
			// Add list
			Uri listUri = getContentResolver().insert(
					NotePad.Lists.CONTENT_URI, values);
			listIdToSelect = Long.parseLong(listUri.getPathSegments().get(
					NotePad.Lists.ID_PATH_POSITION));
			UpdateNotifier.notifyChangeList(getApplicationContext());
		}
	}

	private int getPosOfId(long id) {
		int length = mSectionAdapter.getCount();
		int position;
		boolean found = false;
		for (position = 0; position < length; position++) {
			if (id == mSpinnerAdapter.getItemId(position)) {
				found = true;
				break;
			}
		}
		if (!found) {
			// Happens both if list is empty
			// and if id is -1
			position = -1;
		}
		return position;
	}

	protected void renameList(String title) {
		// I will not allow empty names for lists
		// Also must have a valid id
		if (!title.equals("") && currentListId > -1) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Lists.COLUMN_NAME_TITLE, title);
			// Update list
			getContentResolver().update(
					Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE,
							Long.toString(currentListId)), values, null, null);
			UpdateNotifier.notifyChangeList(getApplicationContext(), Uri
					.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE,
							Long.toString(currentListId)));
		}
	}

	/**
	 * Returns true if user has activated sync and there is a valid account name
	 * selected (not "")
	 * 
	 * @return
	 */
	public static boolean shouldMarkAsDeleted(Context context) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (settings == null)
			return false;
		else
			return (!settings.getString(SyncPrefs.KEY_ACCOUNT, "").isEmpty());
	}

	/**
	 * Marks the current list and all the tasks contained in it as deleted in
	 * the database. Will be deleted on next sync.
	 */
	protected void deleteCurrentList() {
		Log.d("deletebug", "currentlistid: " + currentListId);
		if (currentListId > -1) {
			// Only mark as deleted so it is synced
			if (shouldMarkAsDeleted(this)) {
				ContentValues values = new ContentValues();
				values.put(NotePad.Lists.COLUMN_NAME_DELETED, 1);
				// Mark list as deleted
				getContentResolver().update(
						Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE,
								Long.toString(currentListId)), values, null,
						null);
				// Mark tasks as hidden locally. They are deleted with the list
				// in
				// the sync
				values = new ContentValues();
				values.put(NotePad.Notes.COLUMN_NAME_DELETED, 1);
				values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, 0); // Yes zero,
																	// we
																	// don't
																	// want to
																	// sync
																	// tasks in
																	// deleted
																	// lists
				Log.d("deletebug", "marking as deleted");
				getContentResolver()
						.update(NotePad.Notes.CONTENT_URI,
								values,
								NotePad.Notes.COLUMN_NAME_LIST + " IS "
										+ currentListId, null);
			} else {
				// Delete for real
				Log.d("deletebug", "actually deleting");
				getContentResolver().delete(
						Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE,
								Long.toString(currentListId)), null, null);
			}
			Log.d("deletebug", "notify!");
			UpdateNotifier.notifyChangeList(this, Uri.withAppendedPath(
					NotePad.Lists.CONTENT_ID_URI_BASE,
					Long.toString(currentListId)));
			UpdateNotifier.notifyChangeNote(this);

			// Remove default setting if this is the default list
			long defaultListId = Long.parseLong(PreferenceManager
					.getDefaultSharedPreferences(this).getString(
							MainPrefs.KEY_DEFAULT_LIST, "-1"));
			if (currentListId == defaultListId) {
				// Remove knowledge of default list
				SharedPreferences.Editor prefEditor = PreferenceManager
						.getDefaultSharedPreferences(this).edit();
				prefEditor.remove(MainPrefs.KEY_DEFAULT_LIST);
				prefEditor.commit();
			}
		}
	}

	public void restartAndRefresh() {
		shouldRestart = false;
		Intent intent = getIntent();
		overridePendingTransition(0, 0);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intent);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Need to restart to allow themes and such to go into effect
		if (key.equals(MainPrefs.KEY_THEME)
				|| key.equals(MainPrefs.KEY_HIDDENCHECKBOX)
				|| key.equals(MainPrefs.KEY_HIDDENDATE)
				|| key.equals(MainPrefs.KEY_HIDDENNOTE)
				|| key.equals(MainPrefs.KEY_TITLEROWS)
				|| key.equals(MainPrefs.KEY_SORT_ORDER)
				|| key.equals(MainPrefs.KEY_SORT_TYPE)
				|| key.equals(getString(R.string.pref_locale))
				|| key.equals(MainPrefs.KEY_LISTHEADERS)) {
			shouldRestart = true;
		}
	}

	private void readAndSetSettings() {
		// Read settings and set
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		currentTheme = prefs.getString(MainPrefs.KEY_THEME, currentTheme);

		setTypeOfTheme();

		// Set language
		Configuration config = getResources().getConfiguration();

		String lang = prefs.getString(getString(R.string.pref_locale), "");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if ("".equals(lang))
				locale = Locale.getDefault();
			else if (lang.length() == 5) {
				locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			} else {
				locale = new Locale(lang.substring(0, 2));
			}
			// Locale.setDefault(locale);
			config.locale = locale;
			getResources().updateConfiguration(config,
					getResources().getDisplayMetrics());
		}

		String sortType = prefs.getString(MainPrefs.KEY_SORT_TYPE,
				NotePad.Notes.DEFAULT_SORT_TYPE);
		String sortOrder = prefs.getString(MainPrefs.KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);

		NotePad.Notes.SORT_ORDER = sortType + " " + sortOrder;

		// We want to be notified of future changes
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	private void setTypeOfTheme() {
		if (MainPrefs.THEME_LIGHT_ICS_AB.equals(currentTheme)) {
			setTheme(R.style.ThemeHoloLightDarkActonBar);
		} else if (MainPrefs.THEME_LIGHT.equals(currentTheme)) {
			setTheme(R.style.ThemeHoloLight);
		} else if (MainPrefs.THEME_BLACK.equals(currentTheme)) {
			setTheme(R.style.ThemeHoloBlack);
		} else {
			setTheme(R.style.ThemeHolo);
		}
	}

	@Override
	public void PasswordVerified(ActionResult result) {
		NotesEditorFragment editor = (NotesEditorFragment) getRightFragment();
		if (editor != null) {
			editor.OnPasswordVerified(result);
		}
	}

	@TargetApi(14)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			onDeleteAction();
			break;
		case R.id.menu_preferences:
			showPrefs();
			return true;
		case R.id.menu_setdefaultlist:
			long currentId = -1;
			if (mSectionsPagerAdapter != null)
				currentId = mSectionsPagerAdapter.getItemId(currentListPos);

			if (currentId != -1) {
				PreferenceManager
						.getDefaultSharedPreferences(this)
						.edit()
						.putString(MainPrefs.KEY_DEFAULT_LIST,
								Long.toString(currentId)).commit();

				Toast.makeText(this, getString(R.string.default_list_set),
						Toast.LENGTH_SHORT).show();
			}

			break;
		case R.id.menu_createlist:
			showDialog(CREATE_LIST);
			return true;
		case R.id.menu_renamelist:
			// Create dialog
			showDialog(RENAME_LIST);
			return true;
		case R.id.menu_deletelist:
			// Create dialog
			showDialog(DELETE_LIST);
			return true;
		case R.id.menu_search:
			// Cant do this in HC, because it will crash.
			// Ignore search button in HC
			if (getResources().getBoolean(R.bool.atLeast14)) {
				NotesListFragment list = getLeftFragment();
				if (list != null && list.mSearchItem != null) {
					list.mSearchItem.expandActionView();
				} else if (list != null) {
					// Launches the search window
					onSearchRequested();
				}
			}
			return true;
		case R.id.menu_revert:
			if (currentContent.equals(CONTENTVIEW.RIGHT)) {
				goUp();
			}
			break;
		case R.id.menu_sync:

			String accountName = PreferenceManager.getDefaultSharedPreferences(
					this).getString(SyncPrefs.KEY_ACCOUNT, "");
			boolean syncEnabled = PreferenceManager
					.getDefaultSharedPreferences(this).getBoolean(
							SyncPrefs.KEY_SYNC_ENABLE, false);
			if (accountName != null && !accountName.equals("") && syncEnabled) {
				requestSync(accountName);
			} else {
				// Enable syncing
				enableSync();
			}
			return false; // Editor will listen for this also and saves when it
							// receives it
		}
		return super.onOptionsItemSelected(item);
	}

	private void enableSync() {
		final Activity activity = this;
		// Get the first Google account on the device
		final Account[] accounts = AccountManager.get(activity)
				.getAccountsByType("com.google");
		if (accounts.length > 0) {
			final Account account = accounts[0];

			// Request access
			AccountManager.get(activity).getAuthToken(account,
					SyncAdapter.AUTH_TOKEN_TYPE, null, activity,
					new AccountManagerCallback<Bundle>() {

						@Override
						public void run(AccountManagerFuture<Bundle> future) {
							// This is the callback class, it handles all the
							// steps
							// after requesting access
							try {
								String token = future.getResult().getString(
										AccountManager.KEY_AUTHTOKEN);
								if (token != null && !token.equals("")
										&& account != null) {
									// Get preference editor
									Editor editor = PreferenceManager
											.getDefaultSharedPreferences(
													activity).edit();

									// Write account name to prefs
									editor.putString(SyncPrefs.KEY_ACCOUNT,
											account.name);

									// Set it syncable
									ContentResolver.setIsSyncable(account,
											NotePad.AUTHORITY, 1);

									// Write to pref
									editor.putBoolean(
											SyncPrefs.KEY_SYNC_ENABLE, true)
											.putBoolean(
													SyncPrefs.KEY_BACKGROUND_SYNC,
													true);

									// Enable periodic sync
									long pollFrequency = 3600; // seconds
									ContentResolver.addPeriodicSync(account,
											NotePad.AUTHORITY, new Bundle(),
											pollFrequency);

									// Commit prefs
									editor.commit();

									// Then request sync
									requestSync(account.name);
								}
							} catch (OperationCanceledException e) {
								Log.e("SyncFix", "Error1");
								// if the request was canceled for any reason
							} catch (AuthenticatorException e) {
								Log.e("SyncFix", "Error2");
								// if there was an error communicating with the
								// authenticator or
								// if the authenticator returned an invalid
								// response
							} catch (IOException e) {
								Log.e("SyncFix", "Error3");
								// if the authenticator returned an error
								// response that
								// indicates that it encountered an IOException
								// while
								// communicating with the authentication server
							}
						}
					}, null);

		}
	}

	private void requestSync(String accountName) {
		if (accountName != null && !"".equals(accountName)) {
			Account account = SyncPrefs.getAccount(AccountManager.get(this),
					accountName);
			// Don't start a new sync if one is already going
			if (!ContentResolver.isSyncActive(account, NotePad.AUTHORITY)) {
				Bundle options = new Bundle();
				// This will force a sync regardless of what the setting is
				// in accounts manager. Only use it here where the user has
				// manually desired a sync to happen NOW.
				options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
				ContentResolver
						.requestSync(account, NotePad.AUTHORITY, options);
			}
		}
	}

	private void showPrefs() {
		// launch a new activity to display the dialog
		Intent intent = new Intent();
		intent.setClass(this, PrefsActivity.class);
		startActivity(intent);
	}

	/**
	 * Calls deleteNotes wrapped in ArrayList
	 * 
	 * @param id
	 */
	public static void deleteNote(Context context, long id) {
		Log.d(TAG, "deleteNote: " + id);
		// Only do this for valid id
		if (id > -1) {
			ArrayList<Long> idList = new ArrayList<Long>();
			idList.add(id);
			deleteNotes(context, idList);
		}
	}

	/**
	 * Delete all notes given from database Only marks them as deleted if sync
	 * is enabled
	 * 
	 * @param ids
	 */
	public static void deleteNotes(Context context, Iterable<Long> ids) {
		ContentResolver resolver = context.getContentResolver();
		boolean shouldMark = shouldMarkAsDeleted(context);
		for (long id : ids) {
			Log.d(TAG, "deleteNotes: " + id);
			if (shouldMark) {
				ContentValues values = new ContentValues();
				values.put(NotePad.Notes.COLUMN_NAME_DELETED, "1");
				resolver.update(NotesEditorFragment.getUriFrom(id), values,
						null, null);
			} else {
				resolver.delete(NotesEditorFragment.getUriFrom(id), null, null);
			}
			UpdateNotifier.notifyChangeNote(context,
					NotesEditorFragment.getUriFrom(id));
		}
	}

	/**
	 * Inserts a new note in the designated list
	 * 
	 * @param resolver
	 * @param listId
	 * @return
	 */
	public static Uri createNote(Context context, long listId, String noteText) {
		if (listId > -1) {
			ContentValues values = new ContentValues();
			// Must always include list
			values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);
			values.put(NotePad.Notes.COLUMN_NAME_NOTE, noteText);
			try {
				ContentResolver resolver = context.getContentResolver();
				Uri uri = resolver.insert(NotePad.Notes.CONTENT_URI, values);

				UpdateNotifier.notifyChangeNote(context, uri);
				return uri;
			} catch (SQLException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	public void onMultiDelete(Collection<Long> ids, long curId) {
		if (ids.contains(curId)) {
			NotesEditorFragment editor = (NotesEditorFragment) getFragmentManager()
					.findFragmentById(R.id.rightFragment);
			if (editor != null) {
				editor.clearNoSave();
			}
		}
		deleteNotes(this, ids);
	}

	@Override
	public NotesListFragment getLeftFragment() {
		NotesListFragment retval = null;
		if (mSectionsPagerAdapter != null) {
			retval = (NotesListFragment) mSectionsPagerAdapter
					.getCurrentPrimaryItem();
		}
		return retval;
	}

	public void onDeleteAction() {
		// both list and editor should be notified
		NotesListFragment list = (NotesListFragment) getLeftFragment();
		NotesEditorFragment editor = (NotesEditorFragment) getRightFragment();
		Log.d(TAG, "onDeleteAction, list: " + list + ", editor: " + editor);
		// tell list to do what it should
		// if (list != null)
		// list.onDelete();

		if (editor != null) {
			deleteNote(this, editor.getCurrentNoteId());
			editor.clearNoSave();
		}
		if (list == null) {
			// this is an editor view displaying inactive data. kill it
			finish();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getLoaderManager().destroyLoader(0);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// Change the active list
		Log.d(TAG, "onNavigationItemSelected: " + itemId);

		Log.d(TAG, "show list pos: " + itemPosition);
		// Display list
		if (itemId != currentListId) {
			Log.d(TAG, "set current item");
			mViewPager.setCurrentItem(itemPosition);
			currentListId = itemId;
		}
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "onCreateLoader: " + id);

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		Uri baseUri = NotePad.Lists.CONTENT_VISIBLE_URI;
		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		return new CursorLoader(this, baseUri, new String[] {
				NotePad.Lists._ID, NotePad.Lists.COLUMN_NAME_TITLE,
				NotePad.Lists.COLUMN_NAME_DELETED }, null, null,
				NotePad.Lists.SORT_ORDER // Use the default sort order.
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d(TAG, "onLoadFinished");
		mSpinnerAdapter.swapCursor(data);
		mSectionAdapter.swapCursor(data);
		int position = -1;
		if (listIdToSelect > -1) {
			position = getPosOfId(listIdToSelect);
		}

		if (position < 0 && currentListId < 0) {
			position = 0;
			currentListId = mSectionAdapter.getItemId(0);
		}

		if (position > -1) {
			mViewPager.setCurrentItem(position);
		}
		listIdToSelect = -1;

		beforeBoot = false; // Need to do it here

		if (noteIntentToSelect != null) {
			NotesListFragment list = getLeftFragment();

			if (list != null) {
				list.handleNoteIntent(noteIntentToSelect);
			}

			noteIntentToSelect = null;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(TAG, "onLoaderReset");
		mSpinnerAdapter.swapCursor(null);
		mSectionAdapter.swapCursor(null);
	}

	@Override
	public void onItemSelected(long id) {
		Log.d(TAG, "onItemSelected: " + id);
		// Open a note
		if (id > -1) {
			if (getCurrentContent().equals(DualLayoutActivity.CONTENTVIEW.DUAL)) {
				Bundle arguments = new Bundle();
				arguments.putLong(NotesEditorFragment.KEYID, id);
				NotesEditorFragment fragment = new NotesEditorFragment();
				fragment.setArguments(arguments);
				getFragmentManager().beginTransaction()
						.replace(R.id.rightFragment, fragment)
						.commitAllowingStateLoss();
			} else {
				Intent intent = new Intent();
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.setClass(this, RightActivity.class)
						.setData(
								Uri.withAppendedPath(
										NotePad.Notes.CONTENT_VISIBLE_ID_URI_BASE,
										Long.toString(id)))
						.setAction(Intent.ACTION_EDIT);

				startActivity(intent);
			}
		}
	}
}
