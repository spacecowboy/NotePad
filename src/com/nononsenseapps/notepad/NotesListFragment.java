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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nononsenseapps.helpers.dualpane.DualLayoutActivity;
import com.nononsenseapps.helpers.dualpane.NoNonsenseListFragment;
import com.nononsenseapps.notepad.interfaces.OnModalDeleteListener;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.prefs.PrefsActivity;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.SyncAdapter;
import com.nononsenseapps.ui.NoteCheckBox;
import com.nononsenseapps.ui.SectionAdapter;
import com.nononsenseapps.util.TimeHelper;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import android.text.format.Time;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Adapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

public class NotesListFragment extends NoNonsenseListFragment implements
		SearchView.OnQueryTextListener, OnItemLongClickListener,
		OnModalDeleteListener, LoaderManager.LoaderCallbacks<Cursor>,
		OnSharedPreferenceChangeListener {
	private int mCurCheckPosition = 0;

	private static final String[] PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_NOTE,
			NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_INDENTLEVEL,
			NotePad.Notes.COLUMN_NAME_GTASKS_STATUS };

	// public static final String SELECTEDPOS = "selectedpos";
	// public static final String SELECTEDID = "selectedid";

	// For logging and debugging
	private static final String TAG = "NotesListFragment";

	private static final int CHECK_SINGLE = 1;
	private static final int CHECK_MULTI = 2;
	private static final int CHECK_SINGLE_FUTURE = 3;

	private static final String SAVEDPOS = "listSavedPos";
	private static final String SAVEDID = "listSavedId";
	private static final String SAVEDLISTID = "listSavedListId";

	private static final String SHOULD_OPEN_NOTE = "shouldOpenNote";

	public static final String SECTION_STATE_LISTS = "listnames";
	private static final int LOADER_LISTNAMES = -78;
	// Will sort modification dates
	private final Comparator<String> alphaComparator = new Comparator<String>() {
		@Override
		public int compare(String lhs, String rhs) {
			return lhs.compareTo(rhs);
		}
	};

	private static final int LOADER_REGULARLIST = -99;
	// Date loaders
	public static final String SECTION_STATE_DATE = MainPrefs.DUEDATESORT;
	private static final int LOADER_DATEOVERDUE = -101;
	private static final int LOADER_DATETODAY = -102;
	private static final int LOADER_DATETOMORROW = -103;
	private static final int LOADER_DATEWEEK = -104;
	private static final int LOADER_DATEFUTURE = -105;
	private static final int LOADER_DATENONE = -106;
	private static final int LOADER_DATECOMPLETED = -107;
	// This will sort date headers properly
	private Comparator<String> dateComparator;
	// Modification loaders
	public static final String SECTION_STATE_MOD = MainPrefs.MODIFIEDSORT;
	private static final int LOADER_MODTODAY = -201;
	private static final int LOADER_MODYESTERDAY = -202;
	private static final int LOADER_MODWEEK = -203;
	private static final int LOADER_MODPAST = -204;
	// Will sort modification dates
	private Comparator<String> modComparator;

	private final Map<Long, String> listNames = new LinkedHashMap<Long, String>();

	private long mCurId;

	private boolean idInvalid = false;

	public SearchView mSearchView;
	public MenuItem mSearchItem;

	private String currentQuery = "";
	private int checkMode = CHECK_SINGLE;

	private ModeCallbackHC modeCallback;

	private long mCurListId = -1;

	// private OnEditorDeleteListener onDeleteListener;

	// private SimpleCursorAdapter mAdapter;
	private SectionAdapter mSectionAdapter;

	private boolean autoOpenNote = false;
	private long newNoteIdToSelect = -1;

	private Menu mOptionsMenu;
	private View mRefreshIndeterminateProgressView = null;

	private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(SyncAdapter.SYNC_STARTED)) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setRefreshActionItemState(true);
					}
				});
			} else if (intent.getAction().equals(SyncAdapter.SYNC_FINISHED)) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setRefreshActionItemState(false);
					}
				});
				tellUser(context,
						intent.getExtras().getInt(SyncAdapter.SYNC_RESULT));
			}
		}

		private void tellUser(Context context, int result) {
			int text = R.string.sync_failed;
			switch (result) {
			case SyncAdapter.ERROR:
				text = R.string.sync_failed;
				break;
			case SyncAdapter.LOGIN_FAIL:
				text = R.string.sync_login_failed;
				break;
			case SyncAdapter.SUCCESS:
			default:
				return;
			}

			Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
			toast.show();
		}
	};

	private static String sortType = NotePad.Notes.DUEDATE_SORT_TYPE;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Set adapter
		// mSectionAdapter = new SectionAdapter(activity);
		// mAdapter = getThemedAdapter(null);
		// setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		// Set list preferences
		setSingleCheck();

		if (savedInstanceState != null) {
			mCurListId = savedInstanceState.getLong(SAVEDLISTID, -1);
			mCurCheckPosition = savedInstanceState.getInt(SAVEDPOS, 0);
			mCurId = savedInstanceState.getLong(SAVEDID, -1);
		} else {
			mCurCheckPosition = 0;
			mCurId = -1;
		}

		// Sync any possible changes from server on start here. NOT in onresume
		// TODO activate this once changes in the editor is not overwritten
		/*
		 * String accountName = PreferenceManager.getDefaultSharedPreferences(
		 * activity).getString(SyncPrefs.KEY_ACCOUNT, "");
		 * 
		 * if (accountName != null && !accountName.equals("") &&
		 * NotePadProvider.SyncAuto(activity)) { Account account =
		 * SyncPrefs.getAccount( AccountManager.get(activity), accountName); //
		 * Don't start a new sync if one is already going if
		 * (!ContentResolver.isSyncActive(account, NotePad.AUTHORITY)) { Bundle
		 * options = new Bundle(); // This will force a sync regardless of what
		 * the setting is // in // accounts manager. Only use it here where the
		 * user has // manually // desired a sync to happen NOW.
		 * options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		 * ContentResolver .requestSync(account, NotePad.AUTHORITY, options); }
		 * }
		 */
	}

	public void handleNoteIntent(Intent intent) {
		Log.d(TAG, "handleNoteIntent");
		if (intent != null
				&& (Intent.ACTION_EDIT.equals(intent.getAction()) || Intent.ACTION_VIEW
						.equals(intent.getAction()))) {
			// Are we displaying the correct list already?
			long listId = -1;
			if (null != intent.getExtras())
				listId = intent.getExtras().getLong(
						NotePad.Notes.COLUMN_NAME_LIST, -1);
			if (listId == mCurListId || mCurListId == MainActivity.ALL_NOTES_ID) {
				// just highlight it
				String newId = intent.getData().getPathSegments()
						.get(NotePad.Notes.NOTE_ID_PATH_POSITION);
				long noteId = Long.parseLong(newId);
				int pos = getPosOfId(noteId);
				if (pos > -1) {
					selectPos(pos);
				} else {
					newNoteIdToSelect = noteId;
				}
			} else {
				// it's something we have to handle once the list has been
				// loaded
				String newId = intent.getData().getPathSegments()
						.get(NotePad.Notes.NOTE_ID_PATH_POSITION);
				long noteId = Long.parseLong(newId);
				if (noteId > -1) {
					newNoteIdToSelect = noteId;
				}
			}
		}
	}

	/**
	 * Will try to open the previously open note, but will default to first note
	 * if none was open
	 */
	private void showFirstBestNote() {
		if (mSectionAdapter != null) {
			if (mSectionAdapter.isEmpty()) {
				// DOn't do shit
			} else {
				showNoteAndSelect(mCurCheckPosition);
			}
		}
	}

	private void setupSearchView() {

		Log.d("NotesListFragment", "setup search view");
		if (mSearchView != null) {
			mSearchView.setIconifiedByDefault(true);
			mSearchView.setOnQueryTextListener(this);
			mSearchView.setSubmitButtonEnabled(false);
			mSearchView.setQueryHint(getString(R.string.search_hint));
		}
	}

	private int getPosOfId(long id) {
		if (mSectionAdapter != null)
			return -1;

		int length = mSectionAdapter.getCount();
		int position;
		for (position = 0; position < length; position++) {
			if (id == mSectionAdapter.getItemId(position)) {
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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate menu from XML resource
		// if (FragmentLayout.lightTheme)
		// inflater.inflate(R.menu.list_options_menu_light, menu);
		// else
		mOptionsMenu = menu;
		inflater.inflate(R.menu.list_options_menu, menu);

		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) activity
				.getSystemService(Context.SEARCH_SERVICE);
		mSearchItem = menu.findItem(R.id.menu_search);
		mSearchView = (SearchView) mSearchItem.getActionView();
		if (mSearchView != null)
			mSearchView.setSearchableInfo(searchManager
					.getSearchableInfo(activity.getComponentName()));
		// searchView.setIconifiedByDefault(true); // Do iconify the widget;
		// Don't
		// // expand by default
		// searchView.setSubmitButtonEnabled(false);
		// searchView.setOnCloseListener(this);
		// searchView.setOnQueryTextListener(this);

		setupSearchView();

		// Generate any additional actions that can be performed on the
		// overall list. In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
	}

	public static int getNoteIdFromUri(Uri noteUri) {
		if (noteUri != null)
			return Integer.parseInt(noteUri.getPathSegments().get(
					NotePad.Notes.NOTE_ID_PATH_POSITION));
		else
			return -1;
	}

	private void handleNoteCreation(long listId) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_INSERT);
		intent.setData(NotePad.Notes.CONTENT_VISIBLE_URI);
		intent.putExtra(NotePad.Notes.COLUMN_NAME_LIST, listId);

		// If tablet mode, deliver directly
		if (activity.getCurrentContent().equals(
				DualLayoutActivity.CONTENTVIEW.DUAL)) {
			((MainActivity) activity).onNewIntent(intent);
		}
		// Otherwise start a new editor activity
		else {
			intent.setClass(activity, RightActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add:
			handleNoteCreation(mCurListId);

			return true;
		case R.id.menu_sync:

			String accountName = PreferenceManager.getDefaultSharedPreferences(
					activity).getString(SyncPrefs.KEY_ACCOUNT, "");
			boolean syncEnabled = PreferenceManager
					.getDefaultSharedPreferences(activity).getBoolean(
							SyncPrefs.KEY_SYNC_ENABLE, false);
			if (accountName != null && !accountName.equals("") && syncEnabled) {
				requestSync(accountName);
			} else {
				// Enable syncing
				enableSync();

				// The user might want to enable syncing. Open preferences
				// Intent intent = new Intent();
				// intent.setClass(activity, PrefsActivity.class);
				// startActivity(intent);
			}
			return false; // Editor will listen for this also and saves when it
							// receives it
		case R.id.menu_clearcompleted:
			ContentValues values = new ContentValues();
			values.put(NotePad.Notes.COLUMN_NAME_MODIFIED, -1); // -1 anything
																// that isnt 0
																// or 1
																// indicates
																// that we dont
																// want to
																// change the
																// current value
			values.put(NotePad.Notes.COLUMN_NAME_LOCALHIDDEN, 1);
			// Handle all notes showing
			String inList;
			String[] args;
			if (mCurListId == MainActivity.ALL_NOTES_ID) {
				inList = "";
				args = new String[] { getText(R.string.gtask_status_completed)
						.toString() };
			} else {
				inList = " AND " + NotePad.Notes.COLUMN_NAME_LIST + " IS ?";
				args = new String[] {
						getText(R.string.gtask_status_completed).toString(),
						Long.toString(mCurListId) };
			}

			activity.getContentResolver().update(NotePad.Notes.CONTENT_URI,
					values,
					NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " IS ?" + inList,
					args);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void enableSync() {
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
											SyncPrefs.KEY_SYNC_ENABLE, true);

									// Enable periodic sync
									long freqMin = 60; // minutes
									long pollFrequency = 60 * freqMin; // seconds
									ContentResolver.addPeriodicSync(account,
											NotePad.AUTHORITY, new Bundle(),
											pollFrequency);

									// Write period to prefs
									editor.putString(SyncPrefs.KEY_SYNC_FREQ,
											Long.toString(freqMin));

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
			Account account = SyncPrefs.getAccount(
					AccountManager.get(activity), accountName);
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// To get the call back to add items to the menu
		setHasOptionsMenu(true);

		// Listen to changes to sort order
		PreferenceManager.getDefaultSharedPreferences(activity)
				.registerOnSharedPreferenceChangeListener(this);

		if (getResources().getBoolean(R.bool.atLeastIceCreamSandwich)) {
			// Share action provider
			modeCallback = new ModeCallbackICS(this);
		} else {
			// Share button
			modeCallback = new ModeCallbackHC(this);
		}
		if (modeCallback != null)
			modeCallback.setDeleteListener(this);

		if (savedInstanceState != null) {

			Log.d("NotesListFragment", "onCreate saved not null");
			// mCurCheckPosition = savedInstanceState.getInt(SAVEDPOS);
			// mCurId = savedInstanceState.getLong(SAVEDID);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		Log.d("NotesListFragment", "onSaveInstanceState");
		outState.putInt(SAVEDPOS, mCurCheckPosition);
		outState.putLong(SAVEDID, mCurId);
		outState.putLong(SAVEDLISTID, mCurListId);
	}

	@Override
	public void onPause() {
		super.onPause();
		activity.unregisterReceiver(syncFinishedReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.d("NotesListFragment", "onResume");

		activity.registerReceiver(syncFinishedReceiver, new IntentFilter(
				SyncAdapter.SYNC_FINISHED));
		activity.registerReceiver(syncFinishedReceiver, new IntentFilter(
				SyncAdapter.SYNC_STARTED));

		String accountName = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(SyncPrefs.KEY_ACCOUNT, "");
		// Sync state might have changed, make sure we're spinning when we
		// should
		if (accountName != null && !accountName.isEmpty())
			setRefreshActionItemState(ContentResolver.isSyncActive(SyncPrefs
					.getAccount(AccountManager.get(activity), accountName),
					NotePad.AUTHORITY));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.d("listproto", "OnListItemClick pos " + position + " id " + id);
		showNote(position);
	}

	/**
	 * Larger values than the list contains are re-calculated to valid
	 * positions. If list is empty, no note is opened.
	 * 
	 * returns position of note in list
	 */
	private void showNote(int index) {
		// if it's -1 to start with, we try with zero
		if (index < 0) {
			index = 0;
		}

		if (mSectionAdapter != null) {
			index = index >= mSectionAdapter.getCount() ? mSectionAdapter
					.getCount() - 1 : index;

			Log.d(TAG, "showNote valid index to show is: " + index);

			if (index > -1) {
				Log.d("listproto", "Going to try and open index: " + index);
				mCurCheckPosition = index;
				mCurId = mSectionAdapter.getItemId(index);
				Log.d("listproto", "Section adapter gave me this id: " + mCurId);

				if (activity.getCurrentContent().equals(
						DualLayoutActivity.CONTENTVIEW.DUAL)) {

					Log.d("NotesLIstFragmenT", "It is dualPane!");
					// We can display everything in-place with fragments, so
					// update
					// the list to highlight the selected item and show the
					// data.

					Log.d("NotesListFragment", "Showing note: " + mCurId + ", "
							+ mCurCheckPosition);

					// Check what fragment is currently shown, replace if
					// needed.
					if (activity.getRightFragment() != null) {
						// We want to know about changes here

						Log.d("NotesListFragment", "Would open note here: "
								+ mCurId);
						((NotesEditorFragment) activity.getRightFragment())
								.displayNote(mCurId);
					}

				} else {

					Log.d("NotesListFragment",
							"Showing note in SinglePane: id " + mCurId
									+ ", pos: " + mCurCheckPosition);
					// Otherwise we need to launch a new activity to display
					// the dialog fragment with selected text.
					Intent intent = new Intent();
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.setClass(activity, RightActivity.class)
							.setData(
									Uri.withAppendedPath(
											NotePad.Notes.CONTENT_VISIBLE_ID_URI_BASE,
											Long.toString(mCurId)))
							.putExtra(NotePad.Notes.COLUMN_NAME_LIST,
									mCurListId).setAction(Intent.ACTION_EDIT);

					startActivity(intent);
				}
			} else {
				// Empty search, do NOT display new note.
				mCurCheckPosition = 0;
				mCurId = -1;
				// Default show first note when search is cancelled.
			}
		}
	}

	private void showNoteAndSelect(int index) {
		showNote(index);
		selectPos(mCurCheckPosition);
	}

	/**
	 * Will re-list all notes, and show the note with closest position to
	 * original
	 */
	public void onDelete() {

		Log.d(TAG, "onDelete");
		// Only do anything if id is valid!
		if (mCurId > -1) {
			// if (onDeleteListener != null) {
			// // Tell fragment to delete the current note
			// onDeleteListener.onEditorDelete(mCurId);
			// }
			if (activity.getCurrentContent().equals(
					DualLayoutActivity.CONTENTVIEW.DUAL)) {
				autoOpenNote = true;
			}

			// if (FragmentLayout.LANDSCAPE_MODE) {
			// } else {
			// // Get the id of the currently "selected" note
			// // This matters if we switch to landscape mode
			// reCalculateValidValuesAfterDelete();
			// }
		}
	}

	private void reCalculateValidValuesAfterDelete() {
		int index = mCurCheckPosition;
		if (mSectionAdapter != null) {
			index = index >= mSectionAdapter.getCount() ? mSectionAdapter
					.getCount() - 1 : index;

			Log.d(TAG, "ReCalculate valid index is: " + index);
			if (index == -1) {
				// Completely empty list.
				mCurCheckPosition = 0;
				mCurId = -1;
			} else { // if (index != -1) {
				mCurCheckPosition = index;
				mCurId = mSectionAdapter.getItemId(index);
			}
		}
	}

	/**
	 * Recalculate note to select from id
	 */
	public void reSelectId() {
		int pos = getPosOfId(mCurId);

		Log.d(TAG, "reSelectId id pos: " + mCurId + " " + pos);
		// This happens in a search. Don't destroy id information in selectPos
		// when it is invalid
		if (pos != -1) {
			mCurCheckPosition = pos;
			selectPos(mCurCheckPosition);
		}
	}

	private SimpleCursorAdapter getThemedAdapter(Cursor cursor) {
		// The names of the cursor columns to display in the view,
		// initialized
		// to the title column
		String[] dataColumns = { NotePad.Notes.COLUMN_NAME_INDENTLEVEL,
				NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
				NotePad.Notes.COLUMN_NAME_TITLE,
				NotePad.Notes.COLUMN_NAME_NOTE,
				NotePad.Notes.COLUMN_NAME_DUE_DATE };

		// The view IDs that will display the cursor columns, initialized to
		// the TextView in noteslist_item.xml
		// My hacked adapter allows the boolean to be set if the string matches
		// gtasks string values for them. Needs id as well (set after first)
		int[] viewIDs = { R.id.itemIndent, R.id.itemDone, R.id.itemTitle,
				R.id.itemNote, R.id.itemDate };

		int themed_item = R.layout.noteslist_item;
		// Support two different list items
		if (activity != null) {
			if (PreferenceManager.getDefaultSharedPreferences(activity)
					.getBoolean(MainPrefs.KEY_LISTITEM, true)) {
				themed_item = R.layout.noteslist_item;
			} else {
				themed_item = R.layout.noteslist_item_doublenote;
			}
		}

		// Creates the backing adapter for the ListView.
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity,
				themed_item, cursor, dataColumns, viewIDs, 0);

		final OnCheckedChangeListener listener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean checked) {
				ContentValues values = new ContentValues();
				String status = getText(R.string.gtask_status_uncompleted)
						.toString();
				if (checked)
					status = getText(R.string.gtask_status_completed)
							.toString();
				values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS, status);

				long id = ((NoteCheckBox) buttonView).getNoteId();
				if (id > -1)
					activity.getContentResolver().update(
							NotesEditorFragment.getUriFrom(id), values, null,
							null);
			}
		};

		// In order to set the checked state in the checkbox
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			static final String indent = "      ";

			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				if (columnIndex == cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS)) {
					NoteCheckBox cb = (NoteCheckBox) view;
					cb.setOnCheckedChangeListener(null);
					long id = cursor.getLong(cursor
							.getColumnIndex(BaseColumns._ID));
					cb.setNoteId(id);
					String text = cursor.getString(cursor
							.getColumnIndex(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS));

					if (text != null
							&& text.equals(getText(R.string.gtask_status_completed))) {
						cb.setChecked(true);
					} else {
						cb.setChecked(false);
					}

					// Set a simple on change listener that updates the note on
					// changes.
					cb.setOnCheckedChangeListener(listener);

					return true;
				} else if (columnIndex == cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE)
						|| columnIndex == cursor
								.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE)) {
					TextView tv = (TextView) view;

					// Hide empty note
					if (columnIndex == cursor
							.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE)) {

						LinearLayout.LayoutParams layoutParams;
						String noteText = cursor.getString(cursor
								.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));
						boolean isEmpty = noteText == null
								|| noteText.isEmpty();

						// Set height to zero if it's empty, otherwise wrap
						if (isEmpty)
							layoutParams = new LinearLayout.LayoutParams(0, 0);
						else
							layoutParams = new LinearLayout.LayoutParams(0,
									LinearLayout.LayoutParams.WRAP_CONTENT);
						layoutParams.weight = 1;

						tv.setLayoutParams(layoutParams);
					}

					// Set strike through on completed tasks
					String text = cursor.getString(cursor
							.getColumnIndex(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS));
					if (text != null
							&& text.equals(getText(R.string.gtask_status_completed))) {
						// Set appropriate BITMASK
						tv.setPaintFlags(tv.getPaintFlags()
								| Paint.STRIKE_THRU_TEXT_FLAG);
					} else {
						// Will clear strike-through. Just a BITMASK so do some
						// magic
						if (Paint.STRIKE_THRU_TEXT_FLAG == (tv.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG))
							tv.setPaintFlags(tv.getPaintFlags()
									- Paint.STRIKE_THRU_TEXT_FLAG);
					}

					// Return false so the normal call is used to set the text
					return false;
				} else if (columnIndex == cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE)) {
					String text = cursor.getString(cursor
							.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE));
					TextView tv = (TextView) view;
					LinearLayout.LayoutParams layoutParams;
					if (text == null || text.isEmpty()) {
						// Set height to zero
						layoutParams = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.WRAP_CONTENT, 0);
					} else {
						// Set height to wrap
						layoutParams = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.WRAP_CONTENT,
								LinearLayout.LayoutParams.WRAP_CONTENT);
					}
					tv.setLayoutParams(layoutParams);
					return false;
				} else if (columnIndex == cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_INDENTLEVEL)) {
					// Should only set this on the sort options where it is
					// expected
					TextView indentView = (TextView) view;

					int level = cursor.getInt(cursor
							.getColumnIndex(NotePad.Notes.COLUMN_NAME_INDENTLEVEL));

					// Now set the width
					String width = "";
					if (sortType.equals(NotePad.Notes.POSSUBSORT_SORT_TYPE)) {
						int l;
						for (l = 0; l < level; l++) {
							width += indent;
						}
					}
					indentView.setText(width);
					return true;
				}
				return false;
			}
		});

		return adapter;
	}

	@Override
	public boolean onQueryTextChange(String query) {

		Log.d("NotesListFragment", "onQueryTextChange: " + query);
		if (!currentQuery.equals(query)) {

			Log.d("NotesListFragment", "this is a new query");
			currentQuery = query;

			refreshList(null);

			// hide the clear completed option until search is over
			MenuItem clearCompleted = mOptionsMenu
					.findItem(R.id.menu_clearcompleted);
			if (clearCompleted != null) {
				// Only show this button if there is a list to create notes in
				if ("".equals(query)) {
					clearCompleted.setVisible(true);
				} else {
					clearCompleted.setVisible(false);
				}
			}
		}
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// Just do what we do on text change
		return onQueryTextChange(query);
	}

	private void selectPos(int pos) {
		if (checkMode == CHECK_SINGLE_FUTURE) {
			setSingleCheck();
		}

		Log.d(TAG, "selectPos: " + pos);
		getListView().setItemChecked(pos, true);
		getListView().setSelection(pos);
	}

	public void setSingleCheck() {

		Log.d(TAG, "setSingleCheck");
		checkMode = CHECK_SINGLE;
		ListView lv = getListView();
		if (activity.getCurrentContent().equals(
				DualLayoutActivity.CONTENTVIEW.DUAL)) {
			// Fix the selection before releasing that
			lv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
			// lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
		} else {
			// Not nice to show selected item in list when no editor is showing
			lv.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
		}
		lv.setLongClickable(true);
		lv.setOnItemLongClickListener(this);
	}

	public void setFutureSingleCheck() {
		// REsponsible for disabling the modal selector in the future.
		// can't do it now because it has to destroy itself etc...
		if (checkMode == CHECK_MULTI) {
			checkMode = CHECK_SINGLE_FUTURE;
		}
	}

	public void setMultiCheck(int pos) {

		Log.d(TAG, "setMutliCheck: " + pos + " modeCallback = " + modeCallback);
		// Do this on long press
		checkMode = CHECK_MULTI;
		ListView lv = getListView();
		lv.clearChoices();
		lv.setMultiChoiceModeListener(modeCallback);
		lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		lv.setItemChecked(pos, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
			int position, long id) {

		Log.d(TAG, "onLongClick");
		if (checkMode == CHECK_SINGLE) {
			// get the position which was selected

			Log.d("NotesListFragment", "onLongClick, selected item pos: "
					+ position + ", id: " + id);
			// change to multiselect mode and select that item
			setMultiCheck(position);
		} else {
			// Should never happen
			// Let modal listener handle it
		}
		return true;
	}

	public void setRefreshActionItemState(boolean refreshing) {
		// On Honeycomb, we can set the state of the refresh button by giving it
		// a custom
		// action view.

		Log.d(TAG, "setRefreshActionState");
		if (mOptionsMenu == null) {

			Log.d(TAG, "setRefreshActionState: menu is null, returning");
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_sync);

		Log.d(TAG,
				"setRefreshActionState: refreshItem not null? "
						+ Boolean.toString(refreshItem != null));
		if (refreshItem != null) {
			if (refreshing) {

				Log.d(TAG,
						"setRefreshActionState: refreshing: "
								+ Boolean.toString(refreshing));
				if (mRefreshIndeterminateProgressView == null) {

					Log.d(TAG,
							"setRefreshActionState: mRefreshIndeterminateProgressView was null, inflating one...");
					LayoutInflater inflater = (LayoutInflater) activity
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					mRefreshIndeterminateProgressView = inflater.inflate(
							R.layout.actionbar_indeterminate_progress, null);
				}

				refreshItem.setActionView(mRefreshIndeterminateProgressView);
			} else {

				Log.d(TAG, "setRefreshActionState: setting null actionview");
				refreshItem.setActionView(null);
			}
		}
	}

	private class ModeCallbackHC implements MultiChoiceModeListener {

		protected NotesListFragment list;

		protected HashMap<Long, String> textToShare;

		protected OnModalDeleteListener onDeleteListener;

		protected HashSet<Integer> notesToDelete;

		protected ActionMode mode;

		public ModeCallbackHC(NotesListFragment list) {
			textToShare = new HashMap<Long, String>();
			notesToDelete = new HashSet<Integer>();
			this.list = list;
		}

		public void setDeleteListener(OnModalDeleteListener onDeleteListener) {
			this.onDeleteListener = onDeleteListener;

		}

		protected Intent createShareIntent(String text) {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, text);
			shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

			return shareIntent;
		}

		protected void addTextToShare(long id) {
			// Read note
			Uri uri = NotesEditorFragment.getUriFrom(id);
			Cursor cursor = openNote(uri);

			if (cursor != null && !cursor.isClosed() && cursor.moveToFirst()) {
				// Requery in case something changed while paused (such as the
				// title)
				// cursor.requery();

				/*
				 * Moves to the first record. Always call moveToFirst() before
				 * accessing data in a Cursor for the first time. The semantics
				 * of using a Cursor are that when it is created, its internal
				 * index is pointing to a "place" immediately before the first
				 * record.
				 */
				String note = "";

				int colTitleIndex = cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);

				if (colTitleIndex > -1)
					note = cursor.getString(colTitleIndex) + "\n";

				int colDueIndex = cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
				String due = "";
				if (colDueIndex > -1)
					due = cursor.getString(colDueIndex);

				if (due != null && !due.isEmpty()) {
					Time date = new Time(Time.getCurrentTimezone());
					date.parse3339(due);

					note = note + "due date: " + date.format3339(true) + "\n";
				}

				int colNoteIndex = cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);

				if (colNoteIndex > -1)
					note = note + "\n" + cursor.getString(colNoteIndex);

				// Put in hash
				textToShare.put(id, note);
			}
			if (cursor != null)
				cursor.close();
		}

		protected void delTextToShare(long id) {
			textToShare.remove(id);
		}

		protected String buildTextToShare() {
			String text = "";
			ArrayList<String> notes = new ArrayList<String>(
					textToShare.values());
			if (!notes.isEmpty()) {
				text = text + notes.remove(0);
				while (!notes.isEmpty()) {
					text = text + "\n\n" + notes.remove(0);
				}
			}
			return text;
		}

		@Override
		public boolean onCreateActionMode(android.view.ActionMode mode,
				android.view.Menu menu) {

			Log.d("MODALMAN", "onCreateActionMode mode: " + mode);
			// Clear data!
			this.textToShare.clear();
			this.notesToDelete.clear();

			MenuInflater inflater = activity.getMenuInflater();
			// if (FragmentLayout.lightTheme)
			// inflater.inflate(R.menu.list_select_menu_light, menu);
			// else
			inflater.inflate(R.menu.list_select_menu, menu);
			mode.setTitle("Select Items");

			this.mode = mode;

			return true;
		}

		@Override
		public boolean onPrepareActionMode(android.view.ActionMode mode,
				android.view.Menu menu) {
			return true;
		}

		@Override
		public boolean onActionItemClicked(android.view.ActionMode mode,
				android.view.MenuItem item) {

			Log.d("MODALMAN", "onActionItemClicked mode: " + mode);
			switch (item.getItemId()) {
			case R.id.modal_share:
				shareNote(buildTextToShare());
				mode.finish();
				break;
			case R.id.modal_copy:
				ClipboardManager clipboard = (ClipboardManager) activity
						.getSystemService(Context.CLIPBOARD_SERVICE);
				// ICS style
				// TODO remove hardcoded strings
				clipboard.setPrimaryClip(ClipData.newPlainText("Note",
						buildTextToShare()));
				Toast.makeText(
						activity,
						"Copied " + getListView().getCheckedItemCount()
								+ " notes to clipboard", Toast.LENGTH_SHORT)
						.show();
				mode.finish();
				break;
			case R.id.modal_delete:
				onDeleteAction();
				break;
			default:
				// Toast.makeText(activity, "Clicked " + item.getTitle(),
				// Toast.LENGTH_SHORT).show();
				break;
			}
			return true;
		}

		@Override
		public void onDestroyActionMode(android.view.ActionMode mode) {

			Log.d("modeCallback", "onDestroyActionMode: " + mode.toString()
					+ ", " + mode.getMenu().toString());
			list.setFutureSingleCheck();
		}

		@Override
		public void onItemCheckedStateChanged(android.view.ActionMode mode,
				int position, long id, boolean checked) {
			// Set the share intent with updated text
			if (checked) {
				addTextToShare(id);
				this.notesToDelete.add(position);
			} else {
				delTextToShare(id);
				this.notesToDelete.remove(position);
			}
			final int checkedCount = getListView().getCheckedItemCount();
			switch (checkedCount) {
			case 0:
				mode.setSubtitle(null);
				break;
			case 1:
				// TODO remove hard coded strings
				mode.setSubtitle("One item selected");
				break;
			default:
				mode.setSubtitle("" + checkedCount + " items selected");
				break;
			}
		}

		private void shareNote(String text) {
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType("text/plain");
			share.putExtra(Intent.EXTRA_TEXT, text);
			share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivity(Intent.createChooser(share, "Share note"));
		}

		public Cursor openNote(Uri uri) {
			/*
			 * Using the URI passed in with the triggering Intent, gets the note
			 * or notes in the provider. Note: This is being done on the UI
			 * thread. It will block the thread until the query completes. In a
			 * sample app, going against a simple provider based on a local
			 * database, the block will be momentary, but in a real app you
			 * should use android.content.AsyncQueryHandler or
			 * android.os.AsyncTask.
			 */
			Cursor cursor = activity.managedQuery(uri, // The URI that gets
														// multiple
					// notes from
					// the provider.
					NotesEditorFragment.PROJECTION, // A projection that returns
													// the note ID and
					// note
					// content for each note.
					null, // No "where" clause selection criteria.
					null, // No "where" clause selection values.
					null // Use the default sort order (modification date,
							// descending)
					);
			// Or Honeycomb will crash
			activity.stopManagingCursor(cursor);
			return cursor;
		}

		public void onDeleteAction() {
			int num = notesToDelete.size();
			if (onDeleteListener != null) {
				for (int pos : notesToDelete) {

					Log.d(TAG, "Deleting key: " + pos);
				}
				onDeleteListener.onModalDelete(notesToDelete);
			}
			Toast.makeText(
					activity,
					getString(R.string.deleted) + " " + num + " "
							+ getString(R.string.items), Toast.LENGTH_SHORT)
					.show();
			mode.finish();
		}

	}

	@TargetApi(14)
	private class ModeCallbackICS extends ModeCallbackHC {

		protected ShareActionProvider actionProvider;

		@Override
		public void onItemCheckedStateChanged(android.view.ActionMode mode,
				int position, long id, boolean checked) {
			super.onItemCheckedStateChanged(mode, position, id, checked);

			if (actionProvider != null) {
				actionProvider
						.setShareIntent(createShareIntent(buildTextToShare()));
			}

		}

		@Override
		public boolean onCreateActionMode(android.view.ActionMode mode,
				android.view.Menu menu) {

			Log.d("modeCallBack", "onCreateActionMode " + mode);
			this.textToShare.clear();
			this.notesToDelete.clear();

			MenuInflater inflater = activity.getMenuInflater();
			// if (FragmentLayout.lightTheme)
			// inflater.inflate(R.menu.list_select_menu_light, menu);
			// else
			inflater.inflate(R.menu.list_select_menu, menu);
			mode.setTitle("Select Items");

			this.mode = mode;

			// Set file with share history to the provider and set the share
			// intent.
			android.view.MenuItem actionItem = menu
					.findItem(R.id.modal_item_share_action_provider_action_bar);

			actionProvider = (ShareActionProvider) actionItem
					.getActionProvider();
			actionProvider
					.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
			// Note that you can set/change the intent any time,
			// say when the user has selected an image.
			actionProvider
					.setShareIntent(createShareIntent(buildTextToShare()));

			return true;
		}

		public ModeCallbackICS(NotesListFragment list) {
			super(list);
		}

	}

	@Override
	public void onModalDelete(Collection<Integer> positions) {

		Log.d(TAG, "onModalDelete");
		if (positions.contains(mCurCheckPosition)) {

			Log.d(TAG, "onModalDelete contained setting id invalid");
			idInvalid = true;
		} else {
			// We must recalculate the positions index of the current note
			// This is always done when content changes
		}

		HashSet<Long> ids = new HashSet<Long>();
		for (int pos : positions) {

			Log.d(TAG, "onModalDelete pos: " + pos);
			ids.add(mSectionAdapter.getItemId(pos));
		}
		((MainActivity) activity).onMultiDelete(ids, mCurId);
	}

	public void showList(long id) {

		Log.d(TAG, "showList id " + id);
		mCurListId = id;
		// Will show note if necessary
		Bundle args = new Bundle();
		if (activity.getCurrentContent().equals(
				DualLayoutActivity.CONTENTVIEW.DUAL))
			args.putBoolean(SHOULD_OPEN_NOTE, true);

		refreshList(args);
	}

	private boolean shouldDisplaySections(String sorting) {
		if (mCurListId == MainActivity.ALL_NOTES_ID
				&& PreferenceManager.getDefaultSharedPreferences(activity)
						.getBoolean(MainPrefs.KEY_LISTHEADERS, false)) {
			return true;
		} else if (sorting.equals(MainPrefs.DUEDATESORT)
				|| sorting.equals(MainPrefs.MODIFIEDSORT)) {
			return true;
		} else {
			return false;
		}
	}

	private void refreshList(Bundle args) {
		// We might need to construct a new adapter
		final String sorting = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(MainPrefs.KEY_SORT_TYPE, "");
		if (shouldDisplaySections(sorting)) {
			if (mSectionAdapter == null || !mSectionAdapter.isSectioned()) {
				mSectionAdapter = new SectionAdapter(activity, null);
				// mSectionAdapter.changeState(sorting);
				setListAdapter(mSectionAdapter);
			}
		} else if (mSectionAdapter == null || mSectionAdapter.isSectioned()) {
			mSectionAdapter = new SectionAdapter(activity,
					getThemedAdapter(null));
			setListAdapter(mSectionAdapter);
		}

		if (mSectionAdapter.isSectioned()) {
			// If sort date, fire sorting loaders
			// If mod date, fire modded loaders
			if (mCurListId == MainActivity.ALL_NOTES_ID
					&& PreferenceManager.getDefaultSharedPreferences(activity)
							.getBoolean(MainPrefs.KEY_LISTHEADERS, false)) {
				getLoaderManager().restartLoader(LOADER_LISTNAMES, args, this);
			} else if (sorting.equals(MainPrefs.DUEDATESORT)) {
				Log.d("listproto", "refreshing sectioned date list");
				getLoaderManager().restartLoader(LOADER_DATEFUTURE, args, this);
				getLoaderManager().restartLoader(LOADER_DATENONE, args, this);
				getLoaderManager()
						.restartLoader(LOADER_DATEOVERDUE, args, this);
				getLoaderManager().restartLoader(LOADER_DATETODAY, args, this);
				getLoaderManager().restartLoader(LOADER_DATETOMORROW, args,
						this);
				getLoaderManager().restartLoader(LOADER_DATEWEEK, args, this);
				getLoaderManager().restartLoader(LOADER_DATECOMPLETED, args, this);
			} else if (sorting.equals(MainPrefs.MODIFIEDSORT)) {
				Log.d("listproto", "refreshing sectioned mod list");
				getLoaderManager().restartLoader(LOADER_MODPAST, args, this);
				getLoaderManager().restartLoader(LOADER_MODTODAY, args, this);
				getLoaderManager().restartLoader(LOADER_MODWEEK, args, this);
				getLoaderManager().restartLoader(LOADER_MODYESTERDAY, args,
						this);
			}
		} else {
			Log.d("listproto", "refreshing normal list");
			getLoaderManager().restartLoader(LOADER_REGULARLIST, args, this);
		}

	}

	private CursorLoader getAllNotesLoader(long listId) {
		Uri baseUri = NotePad.Notes.CONTENT_VISIBLE_URI;

		// Get current sort order or assemble the default one.
		String sortChoice = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(MainPrefs.KEY_SORT_TYPE, "");

		String sortOrder = NotePad.Notes.ALPHABETIC_SORT_TYPE;

		if (MainPrefs.DUEDATESORT.equals(sortChoice)) {
			sortOrder = NotePad.Notes.DUEDATE_SORT_TYPE;
		} else if (MainPrefs.TITLESORT.equals(sortChoice)) {
			sortOrder = NotePad.Notes.ALPHABETIC_SORT_TYPE;
		} else if (MainPrefs.MODIFIEDSORT.equals(sortChoice)) {
			sortOrder = NotePad.Notes.MODIFICATION_SORT_TYPE;
		} else if (MainPrefs.POSSUBSORT.equals(sortChoice)) {
			sortOrder = NotePad.Notes.POSSUBSORT_SORT_TYPE;
		}

		NotesListFragment.sortType = sortOrder;

		sortOrder += " "
				+ PreferenceManager.getDefaultSharedPreferences(activity)
						.getString(MainPrefs.KEY_SORT_ORDER,
								NotePad.Notes.DEFAULT_SORT_ORDERING);

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		if (listId == MainActivity.ALL_NOTES_ID) {
			return new CursorLoader(activity, baseUri, PROJECTION, null, null,
					sortOrder);
		} else {
			return new CursorLoader(activity, baseUri, PROJECTION,
					NotePad.Notes.COLUMN_NAME_LIST + " IS ?",
					new String[] { Long.toString(listId) }, sortOrder);
		}
	}

	private CursorLoader getSearchNotesLoader() {
		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		Uri baseUri = NotePad.Notes.CONTENT_VISIBLE_URI;
		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		// Get current sort order or assemble the default one.
		String sortOrder = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(MainPrefs.KEY_SORT_TYPE,
				NotePad.Notes.DEFAULT_SORT_TYPE)
				+ " "
				+ PreferenceManager.getDefaultSharedPreferences(activity)
						.getString(MainPrefs.KEY_SORT_ORDER,
								NotePad.Notes.DEFAULT_SORT_ORDERING);

		// include title field in search
		return new CursorLoader(activity, baseUri, PROJECTION,
				NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?" + " OR "
						+ NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?",
				new String[] { "%" + currentQuery + "%",
						"%" + currentQuery + "%" }, sortOrder);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		Log.d(TAG, "onCreateLoader");

		if (args != null) {
			if (args.containsKey(SHOULD_OPEN_NOTE)
					&& args.getBoolean(SHOULD_OPEN_NOTE)) {
				autoOpenNote = true;
			}
		}

		if (currentQuery != null && !currentQuery.isEmpty()) {
			return getSearchNotesLoader();
		} else {
			// Important that these are in the correct order!
			switch (id) {
			case LOADER_DATEFUTURE:
			case LOADER_DATENONE:
			case LOADER_DATEOVERDUE:
			case LOADER_DATETODAY:
			case LOADER_DATETOMORROW:
			case LOADER_DATEWEEK:
			case LOADER_DATECOMPLETED:
				return getDateLoader(id, mCurListId);
			case LOADER_MODPAST:
			case LOADER_MODTODAY:
			case LOADER_MODWEEK:
			case LOADER_MODYESTERDAY:
				return getModLoader(id, mCurListId);
			case LOADER_REGULARLIST:
				Log.d("listproto", "Getting cursor normal list: " + mCurListId);
				// Regular lists
				return getAllNotesLoader(mCurListId);
			case LOADER_LISTNAMES:
				Log.d("listproto", "Getting cursor for list names");
				// Section names
				return getSectionNameLoader();
			default:
				Log.d("listproto", "Getting cursor for individual list: " + id);
				// Individual lists. ID is actually be the list id
				return getAllNotesLoader(id);
			}
		}
	}

	private CursorLoader getSectionNameLoader() {
		// first check SharedPreferences for what the appropriate loader would
		// be
		// list names, due date, modification
		return new CursorLoader(activity, NotePad.Lists.CONTENT_URI,
				new String[] { NotePad.Lists._ID,
						NotePad.Lists.COLUMN_NAME_TITLE },
				NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1", null,
				NotePad.Lists.SORT_ORDER);
	}

	private CursorLoader getDateLoader(int id, long listId) {
		Log.d("listproto", "getting date loader");
		String sortOrder = NotePad.Notes.DUEDATE_SORT_TYPE;
		NotesListFragment.sortType = sortOrder;
		final String ordering = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(MainPrefs.KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);
		sortOrder += " " + ordering;

		if (dateComparator == null) {
			// Create the comparator
			// Doing it here because I need the context
			// to fetch strings
			dateComparator = new Comparator<String>() {
				@SuppressWarnings("serial")
				private final Map<String, String> orderMap = Collections
						.unmodifiableMap(new HashMap<String, String>() {
							{
								put(activity
										.getString(R.string.date_header_overdue),
										"0");
								put(activity
										.getString(R.string.date_header_today),
										"1");
								put(activity
										.getString(R.string.date_header_tomorrow),
										"2");
								put(activity
										.getString(R.string.date_header_7days),
										"3");
								put(activity
										.getString(R.string.date_header_future),
										"4");
								put(activity
										.getString(R.string.date_header_none),
										"5");
								put(activity
										.getString(R.string.date_header_completed),
										"6");
							}
						});

				public int compare(String object1, String object2) {
					// -1 if object 1 is first, 0 equal, 1 otherwise
					final String m1 = orderMap.get(object1);
					final String m2 = orderMap.get(object2);
					if (m1 == null)
						return 1;
					if (m2 == null)
						return -1;
					if (ordering.equals(NotePad.Notes.ASCENDING_SORT_ORDERING))
						return m1.compareTo(m2);
					else
						return m2.compareTo(m1);
				};
			};
		}

		String[] vars = null;
		String where = NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " IS ? AND ";
		switch (id) {
		case LOADER_DATEFUTURE:
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT NULL AND ";
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT '' AND ";
			where += "date(" + NotePad.Notes.COLUMN_NAME_DUE_DATE + ") >= ?";
			vars = new String[] {
					activity.getString(R.string.gtask_status_uncompleted),
					TimeHelper.dateEightDay() };
			break;
		case LOADER_DATEOVERDUE:
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT NULL AND ";
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT '' AND ";
			where += "date(" + NotePad.Notes.COLUMN_NAME_DUE_DATE + ") < ?";
			vars = new String[] {
					activity.getString(R.string.gtask_status_uncompleted),
					TimeHelper.dateToday() };
			break;
		case LOADER_DATETODAY:
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT NULL AND ";
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT '' AND ";
			where += "date(" + NotePad.Notes.COLUMN_NAME_DUE_DATE + ") IS ?";
			vars = new String[] {
					activity.getString(R.string.gtask_status_uncompleted),
					TimeHelper.dateToday() };
			break;
		case LOADER_DATETOMORROW:
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT NULL AND ";
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT '' AND ";
			where += "date(" + NotePad.Notes.COLUMN_NAME_DUE_DATE + ") IS ?";
			vars = new String[] {
					activity.getString(R.string.gtask_status_uncompleted),
					TimeHelper.dateTomorrow() };
			break;
		case LOADER_DATEWEEK:
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT NULL AND ";
			where += NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NOT '' AND ";
			where += "date(" + NotePad.Notes.COLUMN_NAME_DUE_DATE
					+ ") > ? AND date(" + NotePad.Notes.COLUMN_NAME_DUE_DATE
					+ ") < ?";
			vars = new String[] {
					activity.getString(R.string.gtask_status_uncompleted),
					TimeHelper.dateTomorrow(), TimeHelper.dateEightDay() };
			break;
		case LOADER_DATECOMPLETED:
			where = NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " IS ?";
			vars = new String[] { activity
					.getString(R.string.gtask_status_completed) };
			break;
		case LOADER_DATENONE:
		default:
			where += "(" + NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS NULL OR "
					+ NotePad.Notes.COLUMN_NAME_DUE_DATE + " IS '')";
			vars = new String[] { activity
					.getString(R.string.gtask_status_uncompleted) };
			break;
		}

		// And only for current list
		if (listId != MainActivity.ALL_NOTES_ID) {
			where = NotePad.Notes.COLUMN_NAME_LIST + " IS ? AND (" + where
					+ ")";
			if (vars == null)
				vars = new String[] { Long.toString(listId) };
			else {
				String[] nvars = new String[1 + vars.length];
				nvars[0] = Long.toString(listId);
				for (int i = 0; i < vars.length; i++) {
					nvars[i + 1] = vars[i];
				}

				vars = nvars;
			}
		}

		return new CursorLoader(activity, NotePad.Notes.CONTENT_VISIBLE_URI,
				PROJECTION, where, vars, sortOrder);
	}

	private CursorLoader getModLoader(int id, long listId) {
		Log.d("listproto", "getting mod loader");
		String sortOrder = NotePad.Notes.MODIFICATION_SORT_TYPE;
		NotesListFragment.sortType = sortOrder;
		final String ordering = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(MainPrefs.KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);
		sortOrder += " " + ordering;

		String[] vars = null;
		String where = "";

		if (modComparator == null) {
			// Create the comparator
			// Doing it here because I need the context
			// to fetch strings
			modComparator = new Comparator<String>() {
				@SuppressWarnings("serial")
				private final Map<String, String> orderMap = Collections
						.unmodifiableMap(new HashMap<String, String>() {
							{
								put(activity
										.getString(R.string.mod_header_today),
										"0");
								put(activity
										.getString(R.string.mod_header_yesterday),
										"1");
								put(activity
										.getString(R.string.mod_header_thisweek),
										"2");
								put(activity
										.getString(R.string.mod_header_earlier),
										"3");
							}
						});

				public int compare(String object1, String object2) {
					// -1 if object 1 is first, 0 equal, 1 otherwise
					final String m1 = orderMap.get(object1);
					final String m2 = orderMap.get(object2);
					if (m1 == null)
						return 1;
					if (m2 == null)
						return -1;
					if (ordering.equals(NotePad.Notes.ASCENDING_SORT_ORDERING))
						return m1.compareTo(m2);
					else
						return m2.compareTo(m1);
				};
			};
		}

		switch (id) {
		case LOADER_MODTODAY:
			where = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " > ?";
			vars = new String[] { TimeHelper.milliTodayStart() };
			break;
		case LOADER_MODYESTERDAY:
			where = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " >= ? AND ";
			where += NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " < ?";
			vars = new String[] { TimeHelper.milliYesterdayStart(),
					TimeHelper.milliTodayStart() };
			break;
		case LOADER_MODWEEK:
			where = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " >= ? AND ";
			where += NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " < ?";
			vars = new String[] { TimeHelper.milli7DaysAgo(),
					TimeHelper.milliYesterdayStart() };
			break;
		case LOADER_MODPAST:
		default:
			where = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " < ?";
			vars = new String[] { TimeHelper.milli7DaysAgo() };
			break;
		}

		// And only for current list
		if (listId != MainActivity.ALL_NOTES_ID) {
			where = NotePad.Notes.COLUMN_NAME_LIST + " IS ? AND (" + where
					+ ")";
			String[] nvars = new String[1 + vars.length];
			nvars[0] = Long.toString(listId);
			for (int i = 0; i < vars.length; i++) {
				nvars[i + 1] = vars[i];
			}

			vars = nvars;
		}

		return new CursorLoader(activity, NotePad.Notes.CONTENT_VISIBLE_URI,
				PROJECTION, where, vars, sortOrder);
	}

	private void addSectionToAdapter(String sectionname, Cursor data,
			Comparator<String> comp) {
		// Make sure an adapter exists
		// TODO not add empty cursors?
		SimpleCursorAdapter adapter = mSectionAdapter.sections.get(sectionname);
		if (adapter == null) {
			adapter = getThemedAdapter(null);
			mSectionAdapter.addSection(sectionname, adapter, comp);
		}
		adapter.swapCursor(data);
		// mSectionAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in. (The framework will take care of closing the
		// old cursor once we return.)

		Log.d(TAG, "onLoadFinished");

		Log.d("listproto", "loader id: " + loader.getId());
		Log.d("listproto", "Current list " + mCurListId);
		long listid;
		String sectionname;
		SimpleCursorAdapter adapter;
		switch (loader.getId()) {
		case LOADER_REGULARLIST:
			if (!mSectionAdapter.isSectioned()) {
				mSectionAdapter.swapCursor(data);
			} else
				Log.d("listproto",
						"That's odd... List id invalid: " + loader.getId());
			break;
		case LOADER_LISTNAMES:
			// Section names and starts loaders for individual sections
			Log.d("listproto", "List names");
			while (data != null && data.moveToNext()) {
				listid = data.getLong(data.getColumnIndex(NotePad.Lists._ID));
				sectionname = data.getString(data
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
				Log.d("listproto", "Adding " + sectionname + " to headers");
				listNames.put(listid, sectionname);
				// Start loader for this list
				Log.d("listproto", "Starting loader for " + sectionname
						+ " id " + listid);
				getLoaderManager().restartLoader((int) listid, null, this);
			}
			break;
		case LOADER_DATEFUTURE:
			mSectionAdapter.changeState(SECTION_STATE_DATE);
			sectionname = activity.getString(R.string.date_header_future);
			addSectionToAdapter(sectionname, data, dateComparator);
			break;
		case LOADER_DATECOMPLETED:
			Log.d("listproto", "got completed cursor");
			mSectionAdapter.changeState(SECTION_STATE_DATE);
			sectionname = activity.getString(R.string.date_header_completed);
			addSectionToAdapter(sectionname, data, dateComparator);
			break;
		case LOADER_DATENONE:
			mSectionAdapter.changeState(SECTION_STATE_DATE);
			sectionname = activity.getString(R.string.date_header_none);
			addSectionToAdapter(sectionname, data, dateComparator);
			break;
		case LOADER_DATEOVERDUE:
			mSectionAdapter.changeState(SECTION_STATE_DATE);
			sectionname = activity.getString(R.string.date_header_overdue);
			addSectionToAdapter(sectionname, data, dateComparator);
			break;
		case LOADER_DATETODAY:
			mSectionAdapter.changeState(SECTION_STATE_DATE);
			sectionname = activity.getString(R.string.date_header_today);
			addSectionToAdapter(sectionname, data, dateComparator);
			break;
		case LOADER_DATETOMORROW:
			mSectionAdapter.changeState(SECTION_STATE_DATE);
			sectionname = activity.getString(R.string.date_header_tomorrow);
			addSectionToAdapter(sectionname, data, dateComparator);
			break;
		case LOADER_DATEWEEK:
			mSectionAdapter.changeState(SECTION_STATE_DATE);
			sectionname = activity.getString(R.string.date_header_7days);
			addSectionToAdapter(sectionname, data, dateComparator);
			break;
		case LOADER_MODPAST:
			mSectionAdapter.changeState(SECTION_STATE_MOD);
			sectionname = activity.getString(R.string.mod_header_earlier);
			addSectionToAdapter(sectionname, data, modComparator);
			break;
		case LOADER_MODTODAY:
			mSectionAdapter.changeState(SECTION_STATE_MOD);
			sectionname = activity.getString(R.string.mod_header_today);
			addSectionToAdapter(sectionname, data, modComparator);
			break;
		case LOADER_MODWEEK:
			mSectionAdapter.changeState(SECTION_STATE_MOD);
			sectionname = activity.getString(R.string.mod_header_thisweek);
			addSectionToAdapter(sectionname, data, modComparator);
			break;
		case LOADER_MODYESTERDAY:
			mSectionAdapter.changeState(SECTION_STATE_MOD);
			sectionname = activity.getString(R.string.mod_header_yesterday);
			addSectionToAdapter(sectionname, data, modComparator);
			break;
		default:
			mSectionAdapter.changeState(SECTION_STATE_LISTS);
			// Individual lists have ids that are positive
			if (loader.getId() >= 0) {
				Log.d("listproto", "Sublists");
				// Sublists
				listid = loader.getId();
				sectionname = listNames.get(listid);
				Log.d("listproto", "Loader finished for list id: "
						+ sectionname);

				addSectionToAdapter(sectionname, data, alphaComparator);
			}
			break;
		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}

		// Reselect current note in list, if possible
		// This happens in delete
		if (idInvalid) {
			idInvalid = false;
			// Note is invalid, so recalculate a valid position and index
			reCalculateValidValuesAfterDelete();
			reSelectId();
			if (activity.getCurrentContent().equals(
					DualLayoutActivity.CONTENTVIEW.DUAL))
				autoOpenNote = true;
		}

		// If a note was created, it will be set in this variable
		if (newNoteIdToSelect > -1) {
			selectPos(getPosOfId(newNoteIdToSelect));
			newNoteIdToSelect = -1; // Should only be set to anything else on
									// create
		}
		// Open first note if this is first start
		// or if one was opened previously
		else if (autoOpenNote) {
			autoOpenNote = false;
			showFirstBestNote();
		} else {
			reSelectId();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed. We need to make sure we are no
		// longer using it.

		Log.d(TAG, "onLoaderReset");
		if (mSectionAdapter.isSectioned()) {
			// Sections
			for (SimpleCursorAdapter adapter : mSectionAdapter.sections
					.values()) {
				adapter.swapCursor(null);
			}
			mSectionAdapter.headers.clear();
			mSectionAdapter.sections.clear();
		} else {
			// Single list
			mSectionAdapter.swapCursor(null);
		}
	}

	/**
	 * Re list notes when sorting changes
	 * 
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		try {
			if (activity.isFinishing()) {

				Log.d(TAG, "isFinishing, should not update");
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
			} else {
				if (MainPrefs.KEY_SORT_TYPE.equals(key)
						|| MainPrefs.KEY_SORT_ORDER.equals(key)) {
					// rebuild comparators during refresh
					dateComparator = modComparator = null;
					refreshList(null);
				}
			}
		} catch (IllegalStateException e) {
			// This is just in case the "isFinishing" wouldn't be enough
			// The isFinishing will try to prevent us from doing something
			// stupid
			// This catch prevents the app from crashing if we do something
			// stupid

			Log.d(TAG, "Exception was caught: " + e.getMessage());
		}
	}
}
