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

import java.util.ArrayList;
import java.util.Collection;

import sheetrock.panda.changelog.ChangeLog;

import com.nononsenseapps.helpers.dualpane.DualLayoutActivity;
import com.nononsenseapps.notepad.PasswordDialog.ActionResult;
import com.nononsenseapps.notepad.interfaces.PasswordChecker;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.prefs.PrefsActivity;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.ui.ExtrasCursorAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Showing a single fragment in an activity.
 */
public class MainActivity extends DualLayoutActivity implements
		OnSharedPreferenceChangeListener, OnNavigationListener,
		LoaderManager.LoaderCallbacks<Cursor>, PasswordChecker {
	private static final String TAG = "FragmentLayout";
	private static final String CURRENT_LIST_ID = "currentlistid";
	private static final String CURRENT_LIST_POS = "currentlistpos";
	private static final int CREATE_LIST = 0;
	private static final int RENAME_LIST = 1;
	private static final int DELETE_LIST = 2;
	// public static boolean lightTheme = false;
	public static String currentTheme = MainPrefs.THEME_LIGHT;
	public static boolean shouldRestart = false;

	// For my special dropdown navigation items
	public static final int ALL_NOTES_ID = -2;
	public static final int CREATE_LIST_ID = -3;

	private NotesListFragment list;
	private Menu optionsMenu;

	private ExtrasCursorAdapter mSpinnerAdapter;
	private long currentListId = -1;
	private int currentListPos = 0;

	private long listIdToSelect = -1;
	private boolean beforeBoot = false; // Used to indicate the intent handling

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Must set theme before calling super
		readAndSetSettings();
		super.onCreate(savedInstanceState);

		if (currentContent.equals(CONTENTVIEW.DUAL)
				|| currentContent.equals(CONTENTVIEW.LEFT)) {
			leftOrTabletCreate(savedInstanceState);
		} else {
			rightCreate();
		}
	}

	private void leftOrTabletCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			currentListId = savedInstanceState.getLong(CURRENT_LIST_ID);
			listIdToSelect = currentListId;
			currentListPos = savedInstanceState.getInt(CURRENT_LIST_POS);
		}

		// Set up dropdown navigation
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Will set cursor in Loader
		// mSpinnerAdapter = new ExtrasCursorAdapter(this,
		// R.layout.actionbar_dropdown_item, null,
		// new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
		// new int[] { android.R.id.text1 }, new int[] { -9, -8 },
		// new int[] { R.string.show_from_all_lists, R.string.error_title });
		mSpinnerAdapter = new ExtrasCursorAdapter(this,
				R.layout.actionbar_dropdown_item, null,
				new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 }, new int[] { ALL_NOTES_ID,
						CREATE_LIST_ID },
				new int[] { R.string.show_from_all_lists,
						R.string.menu_createlist });

		mSpinnerAdapter
				.setDropDownViewResource(R.layout.actionbar_dropdown_item);

		// This will listen for navigation callbacks
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

		// setContentView(R.layout.fragment_layout);

		setUpList();

		// Set up navigation list
		// Set a default list to open if one is set
		if (listIdToSelect < 0) {
			listIdToSelect = Long.parseLong(PreferenceManager
					.getDefaultSharedPreferences(this).getString(
							MainPrefs.KEY_DEFAULT_LIST, "-1"));
		}
		// Handle the intent first, so we know what to possibly select once
		// the
		// loader is finished
		beforeBoot = true;

		onNewIntent(getIntent());

		getLoaderManager().initLoader(0, null, this);
	}

	private void rightCreate() {
		// Set up navigation (adds nice arrow to icon)
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(false);
		}
		onNewIntent(getIntent());
	}

	private void setUpList() {
		if (leftFragment != null) {
			NotesListFragment list = (NotesListFragment) leftFragment;

			this.list = list;
		}
	}

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
		optionsMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem deleteList = menu.findItem(R.id.menu_deletelist);
		if (null != mSpinnerAdapter) {
			if (deleteList != null) {
				// Only show this button if there are more than one list
				if (mSpinnerAdapter.getCount() <= 3 || currentListId < 0) {
					deleteList.setVisible(false);
				} else {
					deleteList.setVisible(true);
				}
			}
			MenuItem renameList = menu.findItem(R.id.menu_renamelist);
			if (renameList != null) {
				// Only show this button if there is a list
				if (mSpinnerAdapter.getCount() == 0 || currentListId < 0) {
					renameList.setVisible(false);
				} else {
					renameList.setVisible(true);
				}
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
			if (getResources().getBoolean(R.bool.atLeastIceCreamSandwich)) {
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
			openListFromIntent(listId);
		} else if (intent.getData() != null
				&& (intent.getData().getPath()
						.startsWith(NotePad.Notes.PATH_VISIBLE_NOTES) || intent
						.getData().getPath()
						.startsWith(NotePad.Notes.PATH_NOTES))) {
			if (rightFragment != null) {
				String newId = intent.getData().getPathSegments()
						.get(NotePad.Notes.NOTE_ID_PATH_POSITION);
				long noteId = Long.parseLong(newId);

				if (noteId > -1) {
					((NotesEditorFragment) rightFragment).displayNote(noteId);
				}
			}
			if (list != null) {
				long listId = ALL_NOTES_ID;
				if (intent.getExtras() != null) {
					listId = intent.getExtras().getLong(
							NotePad.Notes.COLUMN_NAME_LIST, ALL_NOTES_ID);
				}
				// Open the containing list if we have to. No need to change
				// lists
				// if we are already displaying all notes.
				if (listId != -1 && currentListId != ALL_NOTES_ID
						&& currentListId != listId) {
					openListFromIntent(listId);
				}
				// Need to highlight note in tablet mode
				if (listId != -1) {
					list.handleNoteIntent(intent);
				}
			}
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
				Uri noteUri = MainActivity.createNote(getContentResolver(),
						listId, text);

				if (noteUri != null) {
					long newNoteIdToOpen = NotesListFragment
							.getNoteIdFromUri(noteUri);

					if (rightFragment != null) {
						((NotesEditorFragment) rightFragment)
								.displayNote(newNoteIdToOpen);
					}

					intent.setAction(Intent.ACTION_EDIT);
					intent.setData(noteUri);
				}
			}

			// Open appropriate list if tablet mode
			if (list != null) {
				long intentId = -1;
				if (intent.getExtras() != null
						&& intent.getExtras().containsKey(
								NotePad.Notes.COLUMN_NAME_LIST)) {
					intentId = intent.getExtras().getLong(
							NotePad.Notes.COLUMN_NAME_LIST, -1);
				}

				// Change to the valid list if intent is crap
				if (intentId != ALL_NOTES_ID && intentId == -1)
					intentId = listId;

				// Open the containing list if we have to. No need to change
				// lists
				// if we are already displaying all notes.
				if (intentId != -1 && currentListId != ALL_NOTES_ID
						&& currentListId != intentId) {
					openListFromIntent(intentId);
				}
				if (intentId != -1) {
					list.handleNoteIntent(intent);
				}
			}
		}
	}

	/**
	 * Will find a suitable list. Which is first ' If the intent contains a list
	 * or the default list, if no valid default list then the first list is
	 * returned. If there are no lists, then -1 is returned.
	 */
	private long getAList(Intent intent) {
		long returnList = -1;
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

		if (tempList < 0) {
			// Then check if a default list is specified
			tempList = Long.parseLong(PreferenceManager
					.getDefaultSharedPreferences(this).getString(
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
			cursor = getContentResolver().query(
					NotePad.Lists.CONTENT_VISIBLE_URI,
					new String[] { NotePad.Lists._ID },
					criteria + " OR NOT (EXISTS (SELECT NULL FROM "
							+ NotePad.Lists.TABLE_NAME + " WHERE " + criteria
							+ "))", new String[] { listString, listString },
					NotePad.Lists.SORT_ORDER + " LIMIT 1");
		} else {
			cursor = getContentResolver().query(
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

		// Return the result, whatever it may be
		return returnList;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// Search
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			// list.onQueryTextChange(query);
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
	private void openListFromIntent(long listId) {
		if (beforeBoot) {
			// Set the variable to be selected after the loader has
			// finished its query
			listIdToSelect = listId;
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

			EditText renameTitle = (EditText) renameDialog
					.findViewById(R.id.renameTitle);
			renameTitle.setText("");

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
			// Also create an empty note in it
			if (listUri != null) {
				createNote(
						getContentResolver(),
						Long.parseLong(listUri.getPathSegments().get(
								NotePad.Lists.ID_PATH_POSITION)), "");
				// Select list
				listIdToSelect = Long.parseLong(listUri.getLastPathSegment());
			}
		}
	}

	private int getPosOfId(long id) {
		int length = mSpinnerAdapter.getCount();
		int position;
		for (position = 0; position < length; position++) {
			if (id == mSpinnerAdapter.getItemId(position)) {
				break;
			}
		}
		if (position == length) {
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
		// Only if id is valid and if it is NOT the last list
		// 3 instead of 1 because we insert "create new" and "all items" in the
		// list
		if (currentListId > -1 && mSpinnerAdapter.getCount() > 3) {
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
				getContentResolver()
						.update(NotePad.Notes.CONTENT_URI,
								values,
								NotePad.Notes.COLUMN_NAME_LIST + " IS "
										+ currentListId, null);
			} else {
				// Delete for real
				getContentResolver().delete(
						Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE,
								Long.toString(currentListId)), null, null);
			}

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
				|| key.equals(MainPrefs.KEY_LISTITEM)
				|| key.equals(MainPrefs.KEY_SORT_ORDER)
				|| key.equals(MainPrefs.KEY_SORT_TYPE)
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
		case R.id.menu_changelog:
			ChangeLog cl = new ChangeLog(this);
			cl.getFullLogDialog().show();
			return true;
		case R.id.menu_preferences:
			showPrefs();
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
			if (getResources().getBoolean(R.bool.atLeastIceCreamSandwich)) {
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
		}
		return super.onOptionsItemSelected(item);
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
			if (shouldMark) {
				ContentValues values = new ContentValues();
				values.put(NotePad.Notes.COLUMN_NAME_DELETED, "1");
				resolver.update(NotesEditorFragment.getUriFrom(id), values,
						null, null);
			} else {
				resolver.delete(NotesEditorFragment.getUriFrom(id), null, null);
			}
		}
	}

	/**
	 * Inserts a new note in the designated list
	 * 
	 * @param resolver
	 * @param listId
	 * @return
	 */
	public static Uri createNote(ContentResolver resolver, long listId,
			String noteText) {
		if (listId > -1) {
			ContentValues values = new ContentValues();
			// Must always include list
			values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);
			values.put(NotePad.Notes.COLUMN_NAME_NOTE, noteText);
			try {
				return resolver.insert(NotePad.Notes.CONTENT_URI, values);
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

	public void onDeleteAction() {
		// both list and editor should be notified
		NotesListFragment list = (NotesListFragment) getLeftFragment();
		NotesEditorFragment editor = (NotesEditorFragment) getRightFragment();
		// tell list to do what it should
		if (list != null)
			list.onDelete();
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
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// Change the active list

		if (CREATE_LIST_ID == itemId) {
			// Create list
			showDialog(CREATE_LIST);
		} else if (list != null) {
			// Display list
			currentListId = itemId;
			currentListPos = itemPosition;
			list.showList(itemId);
		}
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		Uri baseUri = NotePad.Lists.CONTENT_URI;
		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		return new CursorLoader(this, baseUri, new String[] { BaseColumns._ID,
				NotePad.Lists.COLUMN_NAME_TITLE },
				NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1", // un-deleted
																	// records.
				null, NotePad.Lists.SORT_ORDER // Use the default sort order.
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mSpinnerAdapter.swapCursor(data);

		if (listIdToSelect > -1 || listIdToSelect == ALL_NOTES_ID) {
			int position = getPosOfId(listIdToSelect);
			if (position > -1) {
				currentListPos = position;
				currentListId = listIdToSelect;
				getActionBar().setSelectedNavigationItem(position);
			}
			listIdToSelect = -1;
		}

		if (optionsMenu != null) {
			MenuItem createNote = optionsMenu.findItem(R.id.menu_add);
			if (createNote != null) {
				// Only show this button if there is a list to create notes in
				if (mSpinnerAdapter.getCount() == 0) {
					createNote.setVisible(false);
				} else {
					createNote.setVisible(true);
				}
			}
		}
		beforeBoot = false; // Need to do it here
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mSpinnerAdapter.swapCursor(null);
	}
}
