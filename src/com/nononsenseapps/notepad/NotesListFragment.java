package com.nononsenseapps.notepad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.nononsenseapps.notepad.FragmentLayout.NotesEditorActivity;
import com.nononsenseapps.notepad.interfaces.DeleteActionListener;
import com.nononsenseapps.notepad.interfaces.OnEditorDeleteListener;
import com.nononsenseapps.notepad.interfaces.OnModalDeleteListener;
import com.nononsenseapps.notepad.interfaces.Refresher;
import com.nononsenseapps.notepad.interfaces.onNewNoteCreatedListener;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.app.ListFragment;
import android.widget.SimpleCursorAdapter;

import android.text.format.Time;
import android.util.Log;
import android.view.ActionMode;
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
		onNewNoteCreatedListener, OnModalDeleteListener, Refresher {
	int mCurCheckPosition = 0;
	
	private static final String[] PROJECTION = new String[] {
		NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
		NotePad.Notes.COLUMN_NAME_NOTE,
		NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };

	public static final String SELECTEDPOS = "selectedpos";
	public static final String SELECTEDID = "selectedid";

	// For logging and debugging
	private static final String TAG = "NotesListFragment";

	private static final int CHECK_SINGLE = 1;
	private static final int CHECK_MULTI = 2;
	private static final int CHECK_SINGLE_FUTURE = 3;

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

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "onAttach");
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mSearchView = (SearchView) activity.findViewById(R.id.search_view);
		setupSearchView();

		lv = getListView();
		// Populate list
		listAllNotes();

		// boolean showList = PreferenceManager.getDefaultSharedPreferences(
		// activity).getBoolean(SHOWLISTKEY, false);

		if (getListAdapter().isEmpty()) {
			// -1 will display a new note
			mCurCheckPosition = 0;
			mCurId = -1;
			Log.d("NotesListFragment", "Setting data: " + mCurCheckPosition
					+ ", " + mCurId);
		} else {
			Log.d("NotesListFragment", "Setting data not empty first: "
					+ mCurCheckPosition + ", " + mCurId);
			mCurCheckPosition = PreferenceManager.getDefaultSharedPreferences(
					activity).getInt(SELECTEDPOS, -1);
			mCurId = PreferenceManager.getDefaultSharedPreferences(activity)
					.getLong(SELECTEDID, -1);

			setSingleCheck();

			Log.d("NotesListFragment", "Setting data not empty: "
					+ mCurCheckPosition + ", " + mCurId);
		}

		// Now we have listed the notes we should have
		// Position is valid for this list and might be -1 in case we had an
		// empty search in portrait.
		// Id might not be valid if we are coming from portrait to landscape
		if (FragmentLayout.LANDSCAPE_MODE) {
			// Always display note in landscape mode
			// Opens a new note if necessary
			showNote(mCurCheckPosition, true);
		} else if (mCurId > -1) {
			// In portrait mode, only display an existing note. And that could
			// have been selected during a search
			// so recacalculate just in case
			mCurCheckPosition = getPosOfId(mCurId);
			// don't open a new note if none exists
			showNote(mCurCheckPosition, false);
		}
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
//		if (FragmentLayout.lightTheme)
//			inflater.inflate(R.menu.list_options_menu_light, menu);
//		else
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		// outState.putInt(SAVEDPOS, mCurCheckPosition);
		// outState.putLong(SAVEDID, mCurId);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("NotesListFragment", "onPause");
		SharedPreferences.Editor prefEditor = PreferenceManager
				.getDefaultSharedPreferences(activity).edit();
		prefEditor.putInt(SELECTEDPOS, mCurCheckPosition);
		prefEditor.putLong(SELECTEDID, mCurId);
		prefEditor.commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("NotesListFragment", "onResume");
		// Remove focus from search window
		activity.findViewById(R.id.search_view).clearFocus();
		// Titles might have changed so re-list and select notes

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.d("NotesListFragment", "Clicked: " + position + ", " + id);
		if (position != mCurCheckPosition || !FragmentLayout.LANDSCAPE_MODE) {
			showNote(position, false);
		}
		// Remove focus from search window
		activity.findViewById(R.id.search_view).clearFocus();
	}

	private void listAllNotes() {
		Uri contentUri = NotePad.Notes.CONTENT_URI;

		/*
		 * Performs a managed query. The Activity handles closing and requerying
		 * the cursor when needed.
		 * 
		 * Please see the introductory note about performing provider operations
		 * on the UI thread.
		 */
		Cursor cursor = activity.managedQuery(contentUri, // Use the
															// default
				// content URI for
				// the provider.
				PROJECTION, // Return the note ID and title for each note.
				null, // No where clause, return all records.
				null, // No where clause, therefore no where column values.
				NotePad.Notes.SORT_ORDER // Use the default sort order.
				);
		// Or Honeycomb will crash
		activity.stopManagingCursor(cursor);

		/*
		 * The following two arrays create a "map" between columns in the cursor
		 * and view IDs for items in the ListView. Each element in the
		 * dataColumns array represents a column name; each element in the
		 * viewID array represents the ID of a View. The SimpleCursorAdapter
		 * maps them in ascending order to determine where each column value
		 * will appear in the ListView.
		 */

		SimpleCursorAdapter adapter = getThemedAdapter(cursor);

		// Sets the ListView's adapter to be the cursor adapter that was just
		// created.
		setListAdapter(adapter);
	}

	private void showResults(String query) {

		// Cursor cursors = managedQuery(NotePad.Notes.CONTENT_URI, null, null,
		// new String[] { query }, null);
		// Log.d(TAG, ("query : " + query));
		Cursor cursor = activity.managedQuery(NotePad.Notes.CONTENT_URI, // Use
																			// the
				// default
				// content URI
				// for the
				// provider.
				PROJECTION, // Return the note ID, title and text for each note.
				null, // No where clause, return all records.
				new String[] { query }, // Only these column values
				NotePad.Notes.SORT_ORDER); // Use the default sort
											// order.
		// Or Honeycomb will crash
		activity.stopManagingCursor(cursor);

		if (cursor == null) {
			// There are no results
		} else {
			SimpleCursorAdapter adapter = getThemedAdapter(cursor);
			// Sets the ListView's adapter to be the cursor adapter that was
			// just
			// created.
			setListAdapter(adapter);
		}
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

					// Make new fragment to show this selection.
					NotesEditorFragment editor = NotesEditorFragment
							.newInstance(mCurId);
					editor.setOnNewNoteCreatedListener(this);

					// Execute a transaction, replacing any existing fragment
					// with this one inside the frame.
					FragmentTransaction ft = getFragmentManager()
							.beginTransaction();
					ft.replace(R.id.editor_container, editor);
					ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
					Log.d("NotesListFragment",
							"Commiting transaction, opening fragment now");
					
					ft.commit();

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
		if (currentQuery.isEmpty())
			listAllNotes();
		else
			showResults(currentQuery);
	}

	/**
	 * Will re-list all notes, and show the note with closest position to
	 * original
	 */
	public void onDelete() {
		Log.d(TAG, "onDelete");
		if (onDeleteListener != null) {
			onDeleteListener.onEditorDelete(mCurId);
		}
		reListNotes();
		if (FragmentLayout.LANDSCAPE_MODE) {
			showNote(mCurCheckPosition, true);
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
//		if (FragmentLayout.lightTheme)
//			themed_item = R.layout.noteslist_item_light;
//		else
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

			if (query.equals("")) {
				listAllNotes();
			} else {
				showResults(query);
			}
			// Reselect current note in list, if possible
			// Will be -1 if list is empty or id is -1
			reSelectId();
			// Now both id and position are valid for this list
			// (they might both be -1)

			// If search is over, and no valid note is showing. display one.
			if (query.isEmpty() && mCurId == -1
					&& FragmentLayout.LANDSCAPE_MODE) {
				showNote(mCurCheckPosition, true);
			}
		}
		return true;
	}

	public boolean onQueryTextSubmit(String query) {
		Log.d("NotesListFragment", "onQueryTextChange: " + query);
		return false;
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
		lv.setLongClickable(true);
		lv.setOnItemLongClickListener(this);
		if (FragmentLayout.LANDSCAPE_MODE) {
			// Fix the selection before releasing that
			lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			// lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
		} else {
			// Not nice to show selected item in list when no editor is showing
			lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
		}
	}

	public void setFutureSingleCheck() {
		// REsponsible for disabling the modal selector in the future.
		// can't do it now because it has to destroy itself etc...
		checkMode = CHECK_SINGLE_FUTURE;
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
	public void onNewNoteCreated(long id) {
		reListNotes();

		mCurId = id;
		mCurCheckPosition = getPosOfId(id);

		selectPos(mCurCheckPosition);
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
					note = note + "due date: " + date.format(NotesEditorFragment.ANDROIDTIME_FORMAT) + "\n";
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
//			if (FragmentLayout.lightTheme)
//				inflater.inflate(R.menu.list_select_menu_light, menu);
//			else
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
//				Toast.makeText(activity, "Clicked " + item.getTitle(),
//						Toast.LENGTH_SHORT).show();
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
					NotesEditorFragment.PROJECTION, // A projection that returns the note ID and
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
				actionProvider.setShareIntent(createShareIntent(buildTextToShare()));
			}
			
		}

		@Override
		public boolean onCreateActionMode(android.view.ActionMode mode,
				android.view.Menu menu) {
			this.textToShare.clear();
			this.notesToDelete.clear();

			MenuInflater inflater = activity.getMenuInflater();
//			if (FragmentLayout.lightTheme)
//				inflater.inflate(R.menu.list_select_menu_light, menu);
//			else
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
			actionProvider.setShareIntent(createShareIntent(buildTextToShare()));
			
			// Now the delete action provider
			// Set delete listener to this
			actionItem = menu.findItem(R.id.modal_action_delete);

			DeleteActionProvider deleteProvider = (DeleteActionProvider) actionItem
					.getActionProvider();

			// Make sure containing activity implements listener interface
			deleteProvider
					.setDeleteActionListener(this);

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

		Intent intent = new Intent(activity, FragmentLayout.class);

		// intent.setData(data);
		Log.d(TAG, "Launching intent: " + intent);
		startActivity(intent);
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
}
