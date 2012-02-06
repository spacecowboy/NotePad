package com.nononsenseapps.notepad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.nononsenseapps.notepad.FragmentLayout.NotesEditorActivity;
import com.nononsenseapps.notepad.FragmentLayout.NotesPreferencesDialog;
import com.nononsenseapps.notepad.interfaces.DeleteActionListener;
import com.nononsenseapps.notepad.interfaces.OnEditorDeleteListener;
import com.nononsenseapps.notepad.interfaces.OnModalDeleteListener;
import com.nononsenseapps.notepad.interfaces.Refresher;
import com.nononsenseapps.notepad.interfaces.OnNoteOpenedListener;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.LoaderManager;
import android.view.Menu;
import android.view.MenuItem;
import android.app.ListFragment;
import android.widget.SimpleCursorAdapter;

import android.text.format.Time;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.ShareActionProvider;
import android.widget.Toast;

public class NotesListFragment extends ListFragment implements
		SearchView.OnQueryTextListener, OnItemLongClickListener,
		OnNoteOpenedListener, OnModalDeleteListener, Refresher,
		LoaderManager.LoaderCallbacks<Cursor> {
	int mCurCheckPosition = 0;

	private static final String[] PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_NOTE,
			NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };

	// public static final String SELECTEDPOS = "selectedpos";
	// public static final String SELECTEDID = "selectedid";

	// For logging and debugging
	private static final String TAG = "NotesListFragment";

	private static final int CHECK_SINGLE = 1;
	private static final int CHECK_MULTI = 2;
	private static final int CHECK_SINGLE_FUTURE = 3;

	private static final int STATE_NEW_NOTE = 1;
	private static final int STATE_EXISTING_NOTE = 2;
	private static final int STATE_LIST = 3;
	private int currentState = STATE_LIST;

	private static final String SAVEDPOS = "savedpos";
	private static final String SAVEDID = "savedid";
	private static final String SAVEDSTATE = "savedstate";

	private long mCurId;

	private boolean idInvalid = false;
	private boolean posInvalid = false;

	private SearchView mSearchView;

	private String currentQuery = "";
	private int checkMode = CHECK_SINGLE;

	private ModeCallbackHC modeCallback;

	private ListView lv;

	private Activity activity;

	private OnEditorDeleteListener onDeleteListener;

	private SimpleCursorAdapter mAdapter;

	private boolean autoOpenNote = false;
	private NotesEditorFragment landscapeEditor;

	private ListWatcher watcher;

	private Menu mOptionsMenu;
	private View mRefreshIndeterminateProgressView = null;

	protected boolean refreshing = false;

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "onAttach");
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (FragmentLayout.LANDSCAPE_MODE) {
			autoOpenNote = true;
			landscapeEditor = (NotesEditorFragment) getFragmentManager()
					.findFragmentById(R.id.editor_container);
			landscapeEditor.setOnNewNoteCreatedListener(this);
			/*
			 * // Make new fragment to show this selection. NotesEditorFragment
			 * editor = new NotesEditorFragment();
			 * editor.setOnNewNoteCreatedListener(this);
			 * 
			 * // Execute a transaction, replacing any existing fragment // with
			 * this one inside the frame. FragmentTransaction ft =
			 * getFragmentManager().beginTransaction();
			 * ft.replace(R.id.editor_container, editor); //
			 * ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			 * Log.d("NotesListFragment",
			 * "Commiting transaction, opening fragment now");
			 * 
			 * ft.commit();
			 */
		} else {
			landscapeEditor = null;
		}

		mSearchView = (SearchView) activity.findViewById(R.id.search_view);
		setupSearchView();

		lv = getListView();
		// Populate list
		mAdapter = getThemedAdapter(null);
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		// Set list preferences
		setSingleCheck();

		if (savedInstanceState != null) {
			currentState = savedInstanceState.getInt(SAVEDSTATE, STATE_LIST);
			mCurCheckPosition = savedInstanceState.getInt(SAVEDPOS, 0);
			mCurId = savedInstanceState.getLong(SAVEDID, -1);
			// If in portrait and we were editing a note, open it
			if (currentState == STATE_EXISTING_NOTE)
				autoOpenNote = true;
		} else {
			// Only display note in landscape
			if (FragmentLayout.LANDSCAPE_MODE)
				currentState = STATE_EXISTING_NOTE;
			else
				currentState = STATE_LIST;

			mCurCheckPosition = 0;
			mCurId = -1;
		}

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one. Will list all notes
		getLoaderManager().initLoader(0, null, this);
		

        ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, new SyncStatusObserver() {
			@Override
			public void onStatusChanged(int which) {
				refreshing = !refreshing;
				Log.d("SyncObserver", "Sync status changed");
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setRefreshActionItemState(refreshing);
					}
				});
			}
        });
	}

	private void showFirstBestNote() {
		if (getListAdapter().isEmpty()) {
			currentState = STATE_NEW_NOTE;
			Log.d("NotesListFragment", "Setting data: " + mCurCheckPosition
					+ ", " + mCurId);
		} else {
			currentState = STATE_EXISTING_NOTE;
			Log.d("NotesListFragment", "Setting data not empty first: "
					+ mCurCheckPosition + ", " + mCurId);
		}

		// Create new note if necessary
		showNote(mCurCheckPosition, true);
	}

	private void setupSearchView() {
		Log.d("NotesListFragment", "setup search view");
		mSearchView.setIconifiedByDefault(false);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setSubmitButtonEnabled(false);
		mSearchView.setQueryHint(getString(R.string.search_hint));
	}

	private int getPosOfId(long id) {
		int length = getListAdapter().getCount();
		int position;
		for (position = 0; position < length; position++) {
			if (id == getListAdapter().getItemId(position)) {
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
		// SearchManager searchManager = (SearchManager)
		// getSystemService(Context.SEARCH_SERVICE);
		// searchView = (SearchView) menu.findItem(R.id.list_search)
		// .getActionView();
		// searchView.setSearchableInfo(searchManager
		// .getSearchableInfo(getComponentName()));
		// searchView.setIconifiedByDefault(true); // Do iconify the widget;
		// Don't
		// // expand by default
		// searchView.setSubmitButtonEnabled(false);
		// searchView.setOnCloseListener(this);
		// searchView.setOnQueryTextListener(this);

		// Generate any additional actions that can be performed on the
		// overall list. In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("NotesListFragment", "onOptionsSelection ");
		switch (item.getItemId()) {
		case R.id.menu_add:
			// Log.d("NotesListFragment", "onOptionsSelection add");
			showNote(-1, true);
			// Open a fragment with a new note
			// Fragment should report back when it has added it to the database
			// with the interface: onNewNoteListener
			// onNewNote will here update the list, mCurId and mCurPos in turn.
			return true;
		case R.id.menu_sync:
			Log.d("NotesListFragment", "Sync");
			String accountName = PreferenceManager.getDefaultSharedPreferences(
					activity)
					.getString(NotesPreferenceFragment.KEY_ACCOUNT, "");
			boolean syncEnabled = PreferenceManager
					.getDefaultSharedPreferences(activity).getBoolean(
							NotesPreferenceFragment.KEY_SYNC_ENABLE, false);
			if (accountName != null && !accountName.equals("") && syncEnabled) {
				Account account = NotesPreferenceFragment.getAccount(
						AccountManager.get(activity), accountName);
				// Don't start a new sync if one is already going
				if (!ContentResolver.isSyncActive(account, NotePad.AUTHORITY)) {
					ContentResolver.requestSync(account, NotePad.AUTHORITY,
							new Bundle());
				}
			}
			else {
				// The user might want to enable syncing. Open preferences
				Intent intent = new Intent();
				intent.setClass(activity, NotesPreferencesDialog.class);
				startActivity(intent);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		watcher = new ListWatcher(new Handler());

		// To get the call back to add items to the menu
		setHasOptionsMenu(true);

		if (FragmentLayout.AT_LEAST_ICS) {
			// Share action provider
			modeCallback = new ModeCallbackICS(this);
		} else if (FragmentLayout.AT_LEAST_HC) {
			// Share button
			modeCallback = new ModeCallbackHC(this);
		}

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
		outState.putInt(SAVEDSTATE, currentState);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("NotesListFragment", "onPause");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("NotesListFragment", "onResume");
		// Remove focus from search window
		activity.findViewById(R.id.search_view).clearFocus();

		if (!FragmentLayout.LANDSCAPE_MODE) {
			currentState = STATE_LIST;
		}

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.d("NotesListFragment", "Clicked: " + position + ", " + id);
		currentState = STATE_EXISTING_NOTE;
		if (position != mCurCheckPosition || !FragmentLayout.LANDSCAPE_MODE) {
			showNote(position, false);
		}
		// Remove focus from search window
		activity.findViewById(R.id.search_view).clearFocus();
	}

	/**
	 * Larger values than the list contains are re-calculated to valid positions
	 * -1 will create new note.
	 */
	private void showNote(int index, boolean createIfEmpty) {
		while (index >= getListAdapter().getCount()) {
			index = index - 1;
		}
		Log.d(TAG, "showNote valid index to show is: " + index);
		if (index == -1 && !currentQuery.isEmpty()) {
			// Empty search, do NOT display new note.
			mCurCheckPosition = 0;
			mCurId = -1;
			// Default show first note when search is cancelled.
		} else {
			if (index != -1) {
				mCurCheckPosition = index;
				selectPos(mCurCheckPosition);
				mCurId = getListAdapter().getItemId(index);
			} else {
				// Both are -1, show new note. Editor will tell when it's time
				// to
				// select item
				mCurCheckPosition = index;
				mCurId = -1;
			}

			if (mCurId > -1 || createIfEmpty) {
				// Just make sure
				if (mCurId < 0)
					currentState = STATE_NEW_NOTE;
				else
					currentState = STATE_EXISTING_NOTE;

				if (FragmentLayout.LANDSCAPE_MODE) {
					Log.d("NotesLIstFragmenT", "It is dualPane!");
					// We can display everything in-place with fragments, so
					// update
					// the list to highlight the selected item and show the
					// data.
					Log.d("NotesListFragment", "Showing note: " + mCurId + ", "
							+ mCurCheckPosition);

					// Check what fragment is currently shown, replace if
					// needed.
					// NotesEditorFragment editor = (NotesEditorFragment)
					// getSupportFragmentManager()
					// .findFragmentById(R.id.editor);
					if (landscapeEditor != null) {
						// We want to know about changes here
						Log.d("NotesListFragment", "Would open note here: "
								+ mCurId);
						landscapeEditor.displayNote(mCurId);
					}

					// TODO delete this

					// Make new fragment to show this selection.
					/*
					 * NotesEditorFragment editor = NotesEditorFragment
					 * .newInstance(mCurId);
					 * editor.setOnNewNoteCreatedListener(this);
					 * 
					 * // Execute a transaction, replacing any existing fragment
					 * // with this one inside the frame. FragmentTransaction ft
					 * = getFragmentManager() .beginTransaction();
					 * ft.replace(R.id.editor_container, editor);
					 * ft.setTransition
					 * (FragmentTransaction.TRANSIT_FRAGMENT_FADE);
					 * Log.d("NotesListFragment",
					 * "Commiting transaction, opening fragment now");
					 * 
					 * ft.commit();
					 */
				} else {
					Log.d("NotesListFragment",
							"Showing note in SinglePane: id " + mCurId
									+ ", pos: " + mCurCheckPosition);
					// Otherwise we need to launch a new activity to display
					// the dialog fragment with selected text.
					Intent intent = new Intent();
					intent.setClass(activity, NotesEditorActivity.class);
					intent.putExtra(NotesEditorFragment.KEYID, mCurId);
					startActivity(intent);
				}
			}
		}
	}

	private void reListNotes() {
		getLoaderManager().restartLoader(0, null, this);
		// if (currentQuery.isEmpty())
		// listAllNotes();
		// else
		// showResults(currentQuery);
	}

	/**
	 * Will re-list all notes, and show the note with closest position to
	 * original
	 */
	public void onDelete() {
		Log.d(TAG, "onDelete");
		if (onDeleteListener != null) {
			// Tell fragment to delete the current note
			onDeleteListener.onEditorDelete(mCurId);
		}
		if (FragmentLayout.LANDSCAPE_MODE) {
			autoOpenNote = true;
		}
		currentState = STATE_LIST;

		reListNotes();

		// TODO consider the recalculation bit
		if (FragmentLayout.LANDSCAPE_MODE) {
			// showNote(mCurCheckPosition, true);
		} else {
			// Get the id of the currently "selected" note
			// This matters if we switch to landscape mode
			reCalculateValidValuesAfterDelete();
		}
	}

	private void reCalculateValidValuesAfterDelete() {
		int index = mCurCheckPosition;
		while (index >= getListAdapter().getCount()) {
			index = index - 1;
		}

		Log.d(TAG, "ReCalculate valid index is: " + index);
		if (index == -1 && !currentQuery.isEmpty()) {
			// Empty search, do NOT display new note.
			// Instead display the first note when search is cancelled (or
			// changed)
			mCurCheckPosition = 0;
			mCurId = -1;
		} else if (index == -1) {
			// Completely empty list. We should display a new note
			mCurCheckPosition = 0;
			mCurId = -1;
		} else { // if (index != -1) {
			mCurCheckPosition = index;
			mCurId = getListAdapter().getItemId(index);
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
		String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE,
				NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };

		// The view IDs that will display the cursor columns, initialized to
		// the
		// TextView in
		// noteslist_item.xml
		int[] viewIDs = { R.id.itemTitle, R.id.itemDate };

		int themed_item;
		// if (FragmentLayout.lightTheme)
		// themed_item = R.layout.noteslist_item_light;
		// else
		themed_item = R.layout.noteslist_item;

		// Creates the backing adapter for the ListView.
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity,
				themed_item, cursor, dataColumns, viewIDs);

		return adapter;
	}

	public boolean onQueryTextChange(String query) {
		Log.d("NotesListFragment", "onQueryTextChange: " + query);
		if (!currentQuery.equals(query)) {
			Log.d("NotesListFragment", "this is a new query");
			currentQuery = query;

			getLoaderManager().restartLoader(0, null, this);
		}
		return true;
	}

	public boolean onQueryTextSubmit(String query) {
		// Don't care
		return true;
	}

	private void selectPos(int pos) {
		if (checkMode == CHECK_SINGLE_FUTURE) {
			setSingleCheck();
		}
		Log.d(TAG, "selectPos: " + pos);
		getListView().setItemChecked(pos, true);
	}

	public void setSingleCheck() {
		checkMode = CHECK_SINGLE;
		// ListView lv = getListView();
		if (FragmentLayout.LANDSCAPE_MODE) {
			// Fix the selection before releasing that
			lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			// lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
		} else {
			// Not nice to show selected item in list when no editor is showing
			lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
		}
		lv.setLongClickable(true);
		lv.setOnItemLongClickListener(this);
	}

	public void setFutureSingleCheck() {
		// REsponsible for disabling the modal selector in the future.
		// can't do it now because it has to destroy itself etc...
		if (checkMode == CHECK_MULTI) {
			checkMode = CHECK_SINGLE_FUTURE;

			Intent intent = new Intent(activity, FragmentLayout.class);

			// the mother activity will refresh the list for us
			Log.d(TAG, "Launching intent: " + intent);
			// SingleTop, so will not launch a new instance
			startActivity(intent);
		}
	}

	public void setMultiCheck(int pos) {
		Log.d(TAG, "setMutliCheck: " + pos);
		// Do this on long press
		checkMode = CHECK_MULTI;
		// ListView lv = getListView();
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		lv.clearChoices();
		lv.setMultiChoiceModeListener(modeCallback);
		lv.setItemChecked(pos, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
			int position, long id) {
		Log.d(TAG, "onLongClick");
		if (checkMode == CHECK_SINGLE) {
			// Disable long-clicking temporarliy
			getListView().setLongClickable(false);
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

	@Override
	public void onNoteOpened(long id, boolean created) {
		Log.d(TAG, "onNoteOpened: id " + id + "created " + created);
		// Re list if a new note was created.
		// TODO is it necessary to relist them?

		mCurId = id;
		if (created)
			reListNotes();
		else {
			mCurCheckPosition = getPosOfId(id);
			selectPos(mCurCheckPosition);
		}
	}
	
    public void setRefreshActionItemState(boolean refreshing) {
        // On Honeycomb, we can set the state of the refresh button by giving it a custom
        // action view.
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_sync);
        if (refreshItem != null) {
            if (refreshing) {
                if (mRefreshIndeterminateProgressView == null) {
                    LayoutInflater inflater = (LayoutInflater)
                            activity.getSystemService(
                                    Context.LAYOUT_INFLATER_SERVICE);
                    mRefreshIndeterminateProgressView = inflater.inflate(
                            R.layout.actionbar_indeterminate_progress, null);
                }

                refreshItem.setActionView(mRefreshIndeterminateProgressView);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

	private class ModeCallbackHC implements MultiChoiceModeListener,
			DeleteActionListener {

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

			return shareIntent;
		}

		protected void addTextToShare(long id) {
			// Read note
			Uri uri = NotesEditorFragment.getUriFrom(id);
			Cursor cursor = openNote(uri);

			if (cursor != null) {
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
				cursor.moveToFirst();
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
					note = note
							+ "due date: "
							+ date.format(NotesEditorFragment.ANDROIDTIME_FORMAT)
							+ "\n";
				}

				int colNoteIndex = cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);

				if (colNoteIndex > -1)
					note = note + "\n" + cursor.getString(colNoteIndex);

				// Put in hash
				textToShare.put(id, note);
			}
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

		@Override
		public void onDeleteAction() {
			int num = notesToDelete.size();
			if (onDeleteListener != null) {
				for (int pos : notesToDelete) {
					Log.d(TAG, "Deleting key: " + pos);
				}
				onDeleteListener.onModalDelete(notesToDelete);
			}
			Toast.makeText(activity, "Deleted " + num + " items",
					Toast.LENGTH_SHORT).show();
			mode.finish();
		}

	}

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

			// Now the delete action provider
			// Set delete listener to this
			actionItem = menu.findItem(R.id.modal_action_delete);

			DeleteActionProvider deleteProvider = (DeleteActionProvider) actionItem
					.getActionProvider();

			// Make sure containing activity implements listener interface
			deleteProvider.setDeleteActionListener(this);

			return true;
		}

		public ModeCallbackICS(NotesListFragment list) {
			super(list);
		}

	}

	public void setOnDeleteListener(OnEditorDeleteListener fragmentLayout) {
		this.onDeleteListener = fragmentLayout;
		if (modeCallback != null)
			modeCallback.setDeleteListener(this);
	}

	@Override
	public void onNewNoteDeleted(long id) {
		Log.d(TAG, "onNewNoteDeleted");
		// reListNotes();
		// reSelectId();
	}

	@Override
	public void onModalDelete(Collection<Integer> positions) {
		Log.d(TAG, "onModalDelete");
		if (positions.contains(mCurCheckPosition)) {
			Log.d(TAG, "onModalDelete contained setting id invalid");
			idInvalid = true;
		} else {
			// We must recalculate the positions index of the current note
			Log.d(TAG, "onModalDelete not contained, setting pos invalid");
			posInvalid = true;
		}

		if (onDeleteListener != null) {
			HashSet<Long> ids = new HashSet<Long>();
			for (int pos : positions) {
				Log.d(TAG, "onModalDelete pos: " + pos);
				ids.add(getListAdapter().getItemId(pos));
			}
			onDeleteListener.onMultiDelete(ids, mCurId);
		}

		// Need to refresh
		// Intent intent = activity.getIntent();
		// intent.addFlags(activity.FLAG_ACTIVITY_SINGLE_TOP);

		// overridePendingTransition(0, 0);
		// intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		// finish();
		// overridePendingTransition(0, 0);
		// startActivity(intent);

		// This is called in onDestroyActionMode instead so it happens for all
		// events
		// and only once
		// Intent intent = new Intent(activity, FragmentLayout.class);
		// Log.d(TAG, "Launching intent: " + intent);
		// startActivity(intent);
	}

	@Override
	public void refresh() {
		Log.d(TAG, "refresh time!. Is list updated?");
		// reList first so we don't select deletid ids
		reListNotes();

		if (posInvalid) {
			posInvalid = false;
			// Position is invalid, but editor is showing a valid note still.
			// Recalculate its position
			reSelectId();
		} else if (idInvalid) {
			idInvalid = false;
			// Note is invalid, so recalculate a valid position and index
			reCalculateValidValuesAfterDelete();
		}

		// Now both position and id are valid.
		// Only open the "current" note if we are in landscape mode.
		if (FragmentLayout.LANDSCAPE_MODE) {
			showNote(mCurCheckPosition, true);
		}
	}

	private CursorLoader getAllNotesLoader() {
		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		Uri baseUri = NotePad.Notes.CONTENT_URI;
		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		return new CursorLoader(getActivity(), baseUri, PROJECTION, // Return
																	// the note
																	// ID and
																	// title for
																	// each
																	// note.
				null, // No where clause, return all records.
				null, // No where clause, therefore no where column values.
				NotePad.Notes.SORT_ORDER // Use the default sort order.
		);
	}

	private CursorLoader getSearchNotesLoader() {
		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		Uri baseUri = NotePad.Notes.CONTENT_URI;
		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		// TODO include title field in search
		return new CursorLoader(getActivity(), baseUri, PROJECTION,
				NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?", // Where the note
															// contains the
															// query
				new String[] { "%" + currentQuery + "%" }, // We don't care how
															// it occurs in the
															// note
				NotePad.Notes.SORT_ORDER // Use the default sort order.
		);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (currentQuery != null && !currentQuery.isEmpty()) {
			return getSearchNotesLoader();
		} else {
			return getAllNotesLoader();
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in. (The framework will take care of closing the
		// old cursor once we return.)

		mAdapter.swapCursor(data);

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
		// Open first note if this is first start
		// or if one was opened previously
		if (autoOpenNote) {
			autoOpenNote = false;
			showFirstBestNote();
		}

		// Reselect current note in list, if possible
		reSelectId();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed. We need to make sure we are no
		// longer using it.
		mAdapter.swapCursor(null);
	}

	private void registerObserverOpenedNote(long id) {
		Log.d(TAG, "registering observer");
		// First unregister for any previous uri
		activity.getContentResolver().unregisterContentObserver(watcher);
		// Register for current
		activity.getContentResolver().registerContentObserver(
				NotesEditorFragment.getUriFrom(id), false, watcher);
	}

	// TODO delete if not needed
	private static class ListWatcher extends ContentObserver {

		private static final String TAG = "ListWatcher";

		public ListWatcher(Handler handler) {
			super(handler);
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "MyContentObserver.onChange( " + selfChange + ")");
			super.onChange(selfChange);
			// Only two things can happen
			// 1. Note was modified. It must have been changed on server and
			// synced.

			// 2. Note was deleted. This could have been done on server, or
			// simply by the user.
		}

	}
}
