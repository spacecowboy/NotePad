package com.nononsenseapps.notepad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.nononsenseapps.notepad.FragmentLayout.NotesEditorActivity;
import com.nononsenseapps.notepad.FragmentLayout.NotesPreferencesDialog;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class NotesListFragment extends ListFragment implements
		SearchView.OnQueryTextListener, OnItemLongClickListener {
	boolean mDualPane;
	int mCurCheckPosition = 0;

	public static final String SELECTEDIDKEY = "selectedid";
	public static final String SEARCHQUERYKEY = "searchqueryid";

	// For logging and debugging
	private static final String TAG = "NotesListFragment";

	/**
	 * The columns needed by the cursor adapter
	 */
	private static final String[] PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_NOTE,
			NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
	public static final String SHOWLISTKEY = "showList";
	private static final int CHECK_SINGLE = 1;
	private static final int CHECK_MULTI = 2;
	private static final int CHECK_SINGLE_FUTURE = 3;

	private long mCurId;

	private SearchView mSearchView;

	private static String SAVEDPOS = "curPos";
	private static String SAVEDID = "curId";
	private String currentQuery = "";
	private int checkMode = CHECK_SINGLE;

	private ModeCallback modeCallback;

	private ListView lv;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mSearchView = (SearchView) getActivity().findViewById(R.id.search_view);
		setupSearchView();

		lv = getListView();
		// Populate list
		listAllNotes();

		// Check to see if we have a frame in which to embed the details
		// fragment directly in the containing UI.
		View editorFrame = getActivity().findViewById(R.id.editor);
		mDualPane = editorFrame != null
				&& editorFrame.getVisibility() == View.VISIBLE;

		boolean showList = PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getBoolean(SHOWLISTKEY, false);

		if (getListAdapter().isEmpty()) {
			// -1 will display a new note
			mCurCheckPosition = -1;
			mCurId = -1;
			Log.d("NotesListFragment", "Setting data: " + mCurCheckPosition
					+ ", " + mCurId);
		} else {
			mCurId = PreferenceManager.getDefaultSharedPreferences(
					getActivity()).getLong(SELECTEDIDKEY, -1);
			String query = PreferenceManager.getDefaultSharedPreferences(
					getActivity()).getString(SEARCHQUERYKEY, "");
			mSearchView.setQuery(query, false); // "true" should call the
												// listener
			onQueryTextChange(query);

			// If there was a query
			if (mCurId == -1) {
				mCurCheckPosition = 0;
				mCurId = getListAdapter().getItemId(0);
			} else {
				mCurCheckPosition = getPosOfId(mCurId);
			}

			Log.d("NotesListFragment", "Setting data not empty: "
					+ mCurCheckPosition + ", " + mCurId + ", query = " + query);
		}

		// if (mDualPane) {
		// In dual-pane mode, the list view highlights the selected item.
		setSingleCheck(mCurCheckPosition);
		// }
		if (!getListAdapter().isEmpty() && (mDualPane || !showList)) {
			// Only display note in singlepane if user had one showing
			showNote(mCurCheckPosition, mCurId);
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
			// Just in case
			position = 0;
		}
		return position;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate menu from XML resource
		// if (FragmentLayout.lightTheme)
		// inflater.inflate(R.menu.list_options_menu_light, menu);
		// else
		inflater.inflate(R.menu.list_options_menu_dark, menu);

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

	public void onSharedItemSelected(MenuItem item) {
		int pos = mCurCheckPosition;
		// Handle all of the possible menu actions.

		switch (item.getItemId()) {
		case R.id.menu_delete:
			if (mDualPane) {
				// Otherwise only the list should update, but that happens
				// automatically
				if (currentQuery.isEmpty())
					listAllNotes();
				else
					showResults(currentQuery);
				Log.d("NotesListFragment", "onSharedSelection del");
				if (!currentQuery.isEmpty() && getListAdapter().isEmpty()) {
					// Do not create new note in this case. Reset position to 0
					// and id to null
					Log.d("NotesListFragment",
							"Setting pos to zero since search list is empty");
					mCurCheckPosition = 0;
					mCurId = -1;
				} else {
					while (pos >= getListAdapter().getCount()) {
						pos = pos - 1;
					}
					showNote(pos, -1);
				}
			}
			break;
		case R.id.menu_add:
			Log.d("NotesListFragment", "onSharedSelection add");
			showNote(-1, -1);
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("NotesListFragment", "onOptionsSelection ");
		switch (item.getItemId()) {
		case R.id.menu_add:
			// Handled in shared elsewhere
			// Log.d("NotesListFragment", "onOptionsSelection add");
			// showNote(-1, -1);
			return false;
		case R.id.menu_preferences:
			Log.d("NotesListFragment", "onOptionsSelection pref");
			showPrefs();
			return true;
		case R.id.menu_delete:
			// Handled in shared elsewhere
			return false;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// To get the call back to add items to the menu
		setHasOptionsMenu(true);

		modeCallback = new ModeCallback(this);

		if (savedInstanceState != null) {
			Log.d("NotesListFragment", "onCreate saved not null");
			mCurCheckPosition = savedInstanceState.getInt(SAVEDPOS);
			mCurId = savedInstanceState.getLong(SAVEDID);
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
				.getDefaultSharedPreferences(getActivity()).edit();
		prefEditor.putLong(SELECTEDIDKEY, mCurId);
		prefEditor.putString(SEARCHQUERYKEY, mSearchView.getQuery().toString());
		if (mDualPane) {
			// No editor activity to do this
			prefEditor.putBoolean(NotesListFragment.SHOWLISTKEY, false);
		}
		prefEditor.commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("NotesListFragment", "onResume");
		// Remove focus from search window
		getActivity().findViewById(R.id.search_view).clearFocus();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.d("NotesListFragment", "Clicked: " + position + ", " + id);
		showNote(position, id);
		// Remove focus from search window
		getActivity().findViewById(R.id.search_view).clearFocus();
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
		Cursor cursor = getActivity().managedQuery(contentUri, // Use the
																// default
				// content URI for
				// the provider.
				PROJECTION, // Return the note ID and title for each note.
				null, // No where clause, return all records.
				null, // No where clause, therefore no where column values.
				NotePad.Notes.SORT_ORDER // Use the default sort order.
				);

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
		Cursor cursor = getActivity().managedQuery(NotePad.Notes.CONTENT_URI, // Use
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
	 * Helper function to show the details of a selected item, either by
	 * displaying a fragment in-place in the current UI, or starting a whole new
	 * activity in which it is displayed.
	 * 
	 * If id is not -1, index is ignored! if id is -1, index is used. If both
	 * are -1, then a new note is created.
	 */
	void showNote(int index, long id) {
		if (id != -1) {
			mCurCheckPosition = getPosOfId(id);
			selectPos(mCurCheckPosition);
			mCurId = id;
		} else if (index != -1) {
			mCurCheckPosition = index;
			selectPos(mCurCheckPosition);
			mCurId = getListAdapter().getItemId(index);
		} else {
			// Both are -1, show new note. Editor will tell when it's time to
			// select item
			mCurCheckPosition = index;
			mCurId = id;
		}

		if (mDualPane) {
			Log.d("NotesLIstFragmenT", "It is dualPane!");
			// We can display everything in-place with fragments, so update
			// the list to highlight the selected item and show the data.
			Log.d("NotesListFragment", "Showing note: " + mCurId + ", "
					+ mCurCheckPosition);

			// Check what fragment is currently shown, replace if needed.
			NotesEditorFragment editor = (NotesEditorFragment) getFragmentManager()
					.findFragmentById(R.id.editor);
			if (editor != null)
				Log.d("NotesListFragment",
						"Current note: " + editor.getShownId());
			if (editor == null || editor.getShownId() != mCurId) {
				// Make new fragment to show this selection.
				editor = NotesEditorFragment.newInstance(mCurCheckPosition,
						mCurId);

				// Execute a transaction, replacing any existing fragment
				// with this one inside the frame.
				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				ft.replace(R.id.editor, editor);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				Log.d("NotesListFragment",
						"Commiting transaction, opening fragment now");
				ft.commit();
			}
		} else {
			Log.d("NotesListFragment", "Showing note in SinglePane: id "
					+ mCurId + ", pos: " + mCurCheckPosition);
			// Otherwise we need to launch a new activity to display
			// the dialog fragment with selected text.
			Intent intent = new Intent();
			intent.setClass(getActivity(), NotesEditorActivity.class);
			intent.putExtra(NotesEditorFragment.KEYPOS, mCurCheckPosition);
			intent.putExtra(NotesEditorFragment.KEYID, mCurId);
			startActivity(intent);
		}
	}

	private void showPrefs() {
		// launch a new activity to display the dialog
		Intent intent = new Intent();
		intent.setClass(getActivity(), NotesPreferencesDialog.class);
		startActivity(intent);
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
		if (FragmentLayout.lightTheme)
			themed_item = R.layout.noteslist_item_light;
		else
			themed_item = R.layout.noteslist_item_dark;

		// Creates the backing adapter for the ListView.
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
				themed_item, cursor, dataColumns, viewIDs);

		return adapter;
	}

	public boolean onQueryTextChange(String query) {
		Log.d("NotesListFragment", "onQueryTextChange: " + query);
		if (!currentQuery.equals(query)) {
			Log.d("NotesListFragment", "this is a new query");
			currentQuery = query;
			if (query.equals("")) {
				Log.d("NotesListFragment", "empty query even");
				listAllNotes();
				if (mDualPane && mCurCheckPosition > -1) {
					if (getListAdapter().isEmpty()) {
						mCurCheckPosition = -1;
						mCurId = -1; // Just in case here
					}
					if (mCurId == -1) {
						Log.d("NotesListFragment",
								"We are returning from an empty search, display the first note in editor window");
						// We are returning from an empty search, display the
						// first note in editor window
						// A specific example is if you have a search goign, and
						// the user deletes all notes in the search (editor
						// displays nothing now)
						// On returning here, select and show the first note (or
						// a new note if list is empty)
						showNote(mCurCheckPosition, mCurId);
					} else {
						// A note must be showing, select it in the list
						selectCurrentId(mCurId);
					}
				}
			} else {
				showResults(query);
				// Doing this might be a bad idea, but it looks nicer
				selectCurrentId(mCurId);
			}
		}
		return true;
	}

	public boolean onQueryTextSubmit(String query) {
		Log.d("NotesListFragment", "onQueryTextChange: " + query);
		return false;
	}

	public void selectNewId(long newId) {
		if (currentQuery.isEmpty())
			listAllNotes();
		else
			showResults(currentQuery);

		selectCurrentId(newId);
	}

	private void selectCurrentId(long id) {
		int pos = getPosOfId(id);
		Log.d("NotesListFragment", "Selecting the current note: id " + id
				+ ", pos " + pos);
		selectPos(pos);
	}

	private void selectPos(int pos) {
		if (checkMode == CHECK_SINGLE_FUTURE) {
			setSingleCheck(pos); // Will call me back
		} else {
			getListView().setItemChecked(pos, true);
			mCurCheckPosition = pos;
			// O(1) perfomance on getID from pos
			mCurId = getListAdapter().getItemId(mCurCheckPosition);
			Log.d("NotesListFragment", "Selected the new note: mCurId "
					+ mCurId + ", mCurCheckPosition " + mCurCheckPosition);
		}
	}

	public void setSingleCheck() {
		setSingleCheck(mCurCheckPosition);
	}

	public void setSingleCheck(int pos) {
		Log.d(TAG, "setSingleCheck: " + pos);
		checkMode = CHECK_SINGLE;
		// ListView lv = getListView();
		lv.setLongClickable(true);
		lv.setOnItemLongClickListener(this);
		if (mDualPane) {
			// Fix the selection before releasing that
			//lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
		} else {
			// Not nice to show selected item in list when no editor is showing
			lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
		}
		// lv.setMultiChoiceModeListener(null);

		// Select the note
		if (pos != -1) {
			// -1, new note, handled by editor callback
			selectPos(pos);
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

	private class ModeCallback implements ListView.MultiChoiceModeListener,
			OnShareTargetSelectedListener {

		private NotesListFragment list;

		private HashMap<Long, String> textToShare;

		public ModeCallback(NotesListFragment list) {
			textToShare = new HashMap<Long, String>();
			this.list = list;
		}

		private Intent createShareIntent(String text) {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, text);

			return shareIntent;
		}

		private Intent createShareIntent() {
			return createShareIntent("");
		}

		private void addTextToShare(long id) {
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

				// Modifies the window title for the Activity according to the
				// current Activity state.
				// Set the title of the Activity to include the note title
				int colNoteIndex = cursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
				String note = cursor.getString(colNoteIndex);
				// Put in hash
				textToShare.put(id, note);
			}
		}

		private void delTextToShare(long id) {
			textToShare.remove(id);
		}

		private String buildTextToShare() {
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

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = getActivity().getMenuInflater();
			inflater.inflate(R.menu.list_select_menu_dark, menu);
			mode.setTitle("Select Items");

			// Set file with share history to the provider and set the share
			// intent.
			MenuItem actionItem = menu
					.findItem(R.id.menu_item_share_action_provider_action_bar);
			ShareActionProvider actionProvider = (ShareActionProvider) actionItem
					.getActionProvider();
			actionProvider
					.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
			// Note that you can set/change the intent any time,
			// say when the user has selected an image.
			actionProvider.setShareIntent(createShareIntent());

			actionProvider.setOnShareTargetSelectedListener(this);

			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.modal_share:
				Toast.makeText(
						getActivity(),
						"Shared " + getListView().getCheckedItemCount()
								+ " items\n\n" + buildTextToShare(),
						Toast.LENGTH_SHORT).show();
				mode.finish();
				break;
			case R.id.modal_copy:
				ClipboardManager clipboard = (ClipboardManager) getActivity()
						.getSystemService(Context.CLIPBOARD_SERVICE);
				// ICS style
				clipboard.setPrimaryClip(ClipData.newPlainText("Note",
						buildTextToShare()));
				Toast.makeText(
						getActivity(),
						"Copied " + getListView().getCheckedItemCount()
								+ " notes to clipboard",
						Toast.LENGTH_SHORT).show();
				mode.finish();
				break;
			default:
				Toast.makeText(getActivity(), "Clicked " + item.getTitle(),
						Toast.LENGTH_SHORT).show();
				break;
			}
			return true;
		}

		public void onDestroyActionMode(ActionMode mode) {
			Log.d("modeCallback", "onDestroyActionMode: " + mode.toString()
					+ ", " + mode.getMenu().toString());
			list.setFutureSingleCheck();
		}

		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			// Set the share intent with updated text
			if (checked) {
				addTextToShare(id);
			} else {
				delTextToShare(id);
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
			return getActivity().managedQuery(uri, // The URI that gets multiple
													// notes from
					// the provider.
					PROJECTION, // A projection that returns the note ID and
								// note
								// content for each note.
					null, // No "where" clause selection criteria.
					null, // No "where" clause selection values.
					null // Use the default sort order (modification date,
							// descending)
					);
		}

		public boolean onShareTargetSelected(ShareActionProvider source,
				Intent intent) {
			// Just add the text
			intent.putExtra(Intent.EXTRA_TEXT, buildTextToShare());
			return false;
		}

	}
}
