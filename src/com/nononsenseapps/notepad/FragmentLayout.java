package com.nononsenseapps.notepad;

import java.util.ArrayList;
import java.util.Collection;

import com.nononsenseapps.notepad.interfaces.DeleteActionListener;
import com.nononsenseapps.notepad.interfaces.OnEditorDeleteListener;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;

/**
 * Showing a single fragment in an activity.
 */
public class FragmentLayout extends Activity implements
		OnSharedPreferenceChangeListener, OnEditorDeleteListener,
		DeleteActionListener, OnNavigationListener,
		LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "FragmentLayout";
	private static final String CURRENT_LIST = "currentlistid";
	private static final int CREATE_LIST = 0;
	private static final int RENAME_LIST = 1;
	private static final int DELETE_LIST = 2;
	// public static boolean lightTheme = false;
	public static String currentTheme = NotesPreferenceFragment.THEME_LIGHT;
	public static boolean shouldRestart = false;
	public static boolean LANDSCAPE_MODE;
	public static boolean AT_LEAST_ICS;
	public static boolean AT_LEAST_HC;
	
	public static boolean UI_DEBUG_PRINTS = false;

	public static OnEditorDeleteListener ONDELETELISTENER = null;

	private NotesListFragment list;

	private SimpleCursorAdapter mSpinnerAdapter;
	private long currentList;
	
	private int prevNumberOfLists = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Must set theme before this
		super.onCreate(savedInstanceState);

		LANDSCAPE_MODE = getResources().getBoolean(R.bool.useLandscapeView);
		AT_LEAST_ICS = getResources()
				.getBoolean(R.bool.atLeastIceCreamSandwich);
		AT_LEAST_HC = getResources().getBoolean(R.bool.atLeastHoneycomb);

		if (savedInstanceState != null) {
			// TODO this will get overwritten in the onNavigationClick callback.
			// We don't want that
			currentList = savedInstanceState.getLong(CURRENT_LIST);
		}

		// Setting theme here
		readAndSetSettings();

		// Set up dropdown navigation
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Will set cursor in Loader
		mSpinnerAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_dropdown_item, null,
				new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 });
		
		// This will listen for navigation callbacks
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
		getLoaderManager().initLoader(0, null, this);

		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("Activity", "onCreate before");
		// XML makes sure notes list is displayed. And editor too in landscape
		// if (lightTheme)
		// setContentView(R.layout.fragment_layout_light);
		// else
		setContentView(R.layout.fragment_layout);
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("Activity", "onCreate after");

		// Set this as delete listener
		NotesListFragment list = (NotesListFragment) getFragmentManager()
				.findFragmentById(R.id.noteslistfragment);

		list.setOnDeleteListener(this);

		this.list = list;
		// So editor can access it
		ONDELETELISTENER = this;
		
		// Get the intent, verify the action and get the query
	    Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      list.onQueryTextSubmit(query);
	    }
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem createNote = menu.findItem(R.id.menu_add);
		if (createNote != null) {
			// Only show this button if there is a list to create it in
			if (mSpinnerAdapter.getCount() == 0) {
				createNote.setVisible(false);
			} else {
				createNote.setVisible(true);
			}
		}
	}
	
	/**
	 * If the user has a search button, ideally he should be able to use it. Expand the search provider in that case
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_SEARCH:
			if (list != null && list.mSearchItem != null) {
				list.mSearchItem.expandActionView();
			}
			else if (list != null) {
				onSearchRequested();
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
		outState.putLong(CURRENT_LIST, currentList);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("FragmentLayout", "On New Intent list: " + list);
		// Get the intent, verify the action and get the query
	    //Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      //list.onQueryTextChange(query);
	      if (list != null && list.mSearchView != null) {
	    	  list.mSearchView.setQuery(query, false);
	      } else if (list!= null) {
	  	      list.onQueryTextSubmit(query);
	      }
	    } else {
		if (this.list != null) {
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("FragmentLayout", "Calling refresh");
			list.refresh();
		}
	    }
	}

	@Override
	protected void onResume() {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("FragmentLayout", "onResume");
		if (shouldRestart) {
			Log.d("FragmentLayout", "Should refresh");
			restartAndRefresh();
		}
		super.onResume();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DELETE_LIST:
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setMessage(R.string.delete_list_warning)
//			       .setCancelable(false)
//			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//			           public void onClick(DialogInterface dialog, int id) {
//			                deleteCurrentList();
//			           }
//			       })
//			       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//			           public void onClick(DialogInterface dialog, int id) {
//			                dialog.cancel();
//			           }
//			       });
//			
//			return builder.create();
			
			final Dialog deleteDialog = new Dialog(this);
			deleteDialog.setContentView(R.layout.delete_list_dialog);
			deleteDialog.setTitle(R.string.menu_deletelist);
			
			Button dYesButton = (Button) deleteDialog.findViewById(R.id.d_dialog_yes);
			dYesButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteCurrentList();
					deleteDialog.dismiss();
				}
			});

			Button dNoButton = (Button) deleteDialog.findViewById(R.id.d_dialog_no);
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
					EditText title = (EditText) dialog.findViewById(R.id.editTitle);
					createList(title.getText().toString());
					dialog.dismiss();
				}
			});

			Button noButton = (Button) dialog.findViewById(R.id.dialog_no);
			noButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			return dialog;
		case RENAME_LIST:
			final Dialog renameDialog = new Dialog(this);
			renameDialog.setContentView(R.layout.rename_list_dialog);
			renameDialog.setTitle(R.string.menu_renamelist);
			
			EditText renameTitle = (EditText) renameDialog.findViewById(R.id.renameTitle);
			renameTitle.setText("");
			
			Button rYesButton = (Button) renameDialog.findViewById(R.id.r_dialog_yes);
			rYesButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					EditText renameTitle = (EditText) renameDialog.findViewById(R.id.renameTitle);
					renameList(renameTitle.getText().toString());
					renameDialog.dismiss();
				}
			});

			Button rNoButton = (Button) renameDialog.findViewById(R.id.r_dialog_no);
			rNoButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					renameDialog.cancel();
				}
			});
			return renameDialog;
			
		default:
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Wanted to create some dialog: " + id);
			return null;
		}
	}

	protected void createList(String title) {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Create list: " + title);
		// I will not allow empty names for lists
		if (!title.equals("")) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Lists.COLUMN_NAME_TITLE, title);
			// Add list
			Uri listUri = getContentResolver().insert(NotePad.Lists.CONTENT_URI, values);
			// Also create an empty note in it
			if (listUri != null) {
				Uri noteUri = createNote(ContentResolver resolver, Long.parseLong(listUri.getPathSegments().get(
						NotePad.List.ID_PATH_POSITION)));
				// TODO open this note
			}
		}
	}
	
	protected void renameList(String title) {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Rename list: " + title);
		// I will not allow empty names for lists
		// Also must have a valid id
		if (!title.equals("") && currentList > -1) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Lists.COLUMN_NAME_TITLE, title);
			// Update list
			getContentResolver().update(Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE, Long.toString(currentList)), values, null, null);
		}
	}

	/**
	 * Marks the current list and all the tasks contained in it as deleted in the database.
	 * Will be deleted on next sync.
	 */
	protected void deleteCurrentList() {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Delete current list");
		// Only if id is valid
		if (currentList > -1) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Lists.COLUMN_NAME_DELETED, 1);
			// Mark list as deleted
			getContentResolver().update(Uri.withAppendedPath(NotePad.Lists.CONTENT_ID_URI_BASE, Long.toString(currentList)), values, null, null);
			// Mark tasks as deleted
			values = new ContentValues();
			values.put(NotePad.Notes.COLUMN_NAME_DELETED, 1);
			getContentResolver().update(NotePad.Notes.CONTENT_URI, values, NotePad.Notes.COLUMN_NAME_LIST + " IS " + currentList, null);
		}
	}

	public void restartAndRefresh() {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("FragmentLayout", "Should restart and refresh");
		shouldRestart = false;
		Intent intent = getIntent();
		overridePendingTransition(0, 0);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intent);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Need to restart to allow themes and such to go into effect
		if (key.equals(NotesPreferenceFragment.KEY_THEME)) {
			shouldRestart = true;
		}
	}

	private void readAndSetSettings() {
		// Read settings and set
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		currentTheme = prefs.getString(NotesPreferenceFragment.KEY_THEME,
				currentTheme);

		setTypeOfTheme();

		String sortType = prefs.getString(
				NotesPreferenceFragment.KEY_SORT_TYPE,
				NotePad.Notes.DEFAULT_SORT_TYPE);
		String sortOrder = prefs.getString(
				NotesPreferenceFragment.KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);

		NotePad.Notes.SORT_ORDER = sortType + " " + sortOrder;
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("ReadingSettings", "sortOrder is: " + NotePad.Notes.SORT_ORDER);

		// We want to be notified of future changes
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	private void setTypeOfTheme() {
		if (NotesPreferenceFragment.THEME_LIGHT_ICS_AB.equals(currentTheme)) {
			setTheme(R.style.ThemeHoloLightDarkActonBar);
		} else if (NotesPreferenceFragment.THEME_LIGHT.equals(currentTheme)) {
			setTheme(R.style.ThemeHoloLight);
		} else {
			setTheme(R.style.ThemeHolo);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			onDeleteAction();
			break;
		case R.id.menu_preferences:
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("NotesListFragment", "onOptionsSelection pref");
			showPrefs();
			return true;
		case R.id.menu_createlist:
			// Create dialog
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG,"menu_createlist");
			showDialog(CREATE_LIST);
			return true;
		case R.id.menu_renamelist:
			// Create dialog
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG,"menu_renamelist");
			showDialog(RENAME_LIST);
			return true;
		case R.id.menu_deletelist:
			// Create dialog
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG,"menu_deletelist");
			showDialog(DELETE_LIST);
			return true;
		case R.id.menu_search:
			if (list != null && list.mSearchItem != null) {
				list.mSearchItem.expandActionView();
			}
			else if (list != null) {
				// Launches the search window
				onSearchRequested();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showPrefs() {
		// launch a new activity to display the dialog
		Intent intent = new Intent();
		intent.setClass(this, NotesPreferencesDialog.class);
		startActivity(intent);
	}

	/**
	 * This is a secondary activity, to show what the user has selected when the
	 * screen is not large enough to show it all in one activity.
	 */
	public static class NotesEditorActivity extends Activity implements
			DeleteActionListener {
		private static final String TAG = "NotesEditorActivity";
		private NotesEditorFragment editorFragment;
		private long currentId = -1;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// Make sure to set themes before this
			super.onCreate(savedInstanceState);

			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("NotesEditorActivity", "onCreate");

			if (NotesPreferenceFragment.THEME_LIGHT_ICS_AB
					.equals(FragmentLayout.currentTheme)) {
				setTheme(R.style.ThemeHoloLightDarkActonBar);
			} else if (NotesPreferenceFragment.THEME_LIGHT
					.equals(FragmentLayout.currentTheme)) {
				setTheme(R.style.ThemeHoloLight);
			} else {
				setTheme(R.style.ThemeHolo);
			}

			// if (FragmentLayout.lightTheme) {
			// setTheme(R.style.ThemeHoloLightDarkActonBar);
			// } else {
			// setTheme(R.style.ThemeHolo);
			// }

			// Set up navigation (adds nice arrow to icon)
			ActionBar actionBar = getActionBar();
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setDisplayShowTitleEnabled(false);
			}

			if (getResources().getBoolean(R.bool.useLandscapeView)) {
				// If the screen is now in landscape mode, we can show the
				// dialog in-line with the list so we don't need this activity.
				Log.d("NotesEditorActivity",
						"Landscape mode detected, killing myself");
				finish();
				return;
			}

			setContentView(R.layout.note_editor_activity);

			this.currentId = getIntent().getExtras().getLong(
					NotesEditorFragment.KEYID);
			long listId = getIntent().getExtras().getLong(
					NotesEditorFragment.LISTID);

			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("NotesEditorActivity", "Time to show the note!");
			// if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			// Set this as delete listener
			editorFragment = (NotesEditorFragment) getFragmentManager()
					.findFragmentById(R.id.portrait_editor);
			if (editorFragment != null) {
				editorFragment.setValues(currentId, listId);
			}
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
			case R.id.menu_delete:
				onDeleteAction();
				return true;
			case R.id.menu_revert:
				setResult(Activity.RESULT_CANCELED);
				finish();
				break;
			}
			return super.onOptionsItemSelected(item);
		}

		@Override
		public void onPause() {
			super.onPause();
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("NotesEditorActivity", "onPause");
			if (isFinishing()) {
				// Log.d("NotesEditorActivity",
				// "onPause, telling list to display list");
				// SharedPreferences.Editor prefEditor = PreferenceManager
				// .getDefaultSharedPreferences(this).edit();
				// prefEditor.putBoolean(NotesListFragment.SHOWLISTKEY, true);
				// prefEditor.commit();
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("NotesEditorActivity", "onResume");
			if (getResources().getBoolean(R.bool.useLandscapeView)) {
				if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("NotesEditorActivity", "onResume, killing myself");
				// Log.d("NotesEditorActivity",
				// "onResume telling list to display me");
				// SharedPreferences.Editor prefEditor = PreferenceManager
				// .getDefaultSharedPreferences(this).edit();
				// prefEditor.putBoolean(NotesListFragment.SHOWLISTKEY, false);
				// prefEditor.commit();
				finish();
			}
		}

		@Override
		public void onDeleteAction() {
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "onDeleteAction");
			editorFragment.setSelfAction(); // Don't try to reload the deleted
											// note
			FragmentLayout.deleteNote(getContentResolver(), editorFragment.getCurrentNoteId());
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
	}

	@Override
	public void onEditorDelete(long id) {
		deleteNote(getContentResolver(), id);
	}

	/**
	 * Calls deleteNotes wrapped in ArrayList
	 * 
	 * @param id
	 */
	public static void deleteNote(ContentResolver resolver, long id) {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "deleteNote: " + id);
		// Only do this for valid id
		if (id > -1) {
			ArrayList<Long> idList = new ArrayList<Long>();
			idList.add(id);
			deleteNotes(resolver, idList);
		}
	}

	/**
	 * Delete all notes given from database
	 * Only marks them as deleted actually
	 * 
	 * @param ids
	 */
	public static void deleteNotes(ContentResolver resolver, Iterable<Long> ids) {
		for (long id : ids) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Notes.COLUMN_NAME_DELETED, "1");
			resolver.update(NotesEditorFragment.getUriFrom(id), values, null,
					null);
			// resolver.delete(NotesEditorFragment.getUriFrom(id), null, null);
		}
	}
	
	/**
	 * Inserts a new note in the designated list
	 * @param resolver
	 * @param listId
	 * @return
	 */
	public static Uri createNote(ContentResolver resolver, long listId) {
		if (listId > -1) {
			ContentValues values = new ContentValues();
			// Must always include list
			values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);
			try {
				return resolver.insert(
					NotePad.Notes.CONTENT_URI, values);
			} catch (SQLException e) {
				if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Failed to insert note. Sure there is a list to insert into?");
				return null;
			}
		}
		else {
			return null;
		}
	}

	@Override
	public void onMultiDelete(Collection<Long> ids, long curId) {
		if (ids.contains(curId)) {
			Log.d("FragmentLayout",
					"id was contained in multidelete, setting no save first");
			NotesEditorFragment editor = (NotesEditorFragment) getFragmentManager()
					.findFragmentById(R.id.editor_container);
			if (editor != null) {
				editor.setSelfAction();
			}
		}
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("FragmentLayout", "deleting notes...");
		deleteNotes(getContentResolver(), ids);
	}

	public static class NotesPreferencesDialog extends Activity {
		public static final int DIALOG_ACCOUNTS = 23;
		private NotesPreferenceFragment prefFragment;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			if (NotesPreferenceFragment.THEME_DARK
					.equals(FragmentLayout.currentTheme)) {
				setTheme(R.style.ThemeHoloDialogNoActionBar);
			} else {
				setTheme(R.style.ThemeHoloLightDialogNoActionBar);
			}

			// Display the fragment as the main content.
			prefFragment = new NotesPreferenceFragment();
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(android.R.id.content, prefFragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
			}
			return super.onOptionsItemSelected(item);
		}

		@Override
		protected Dialog onCreateDialog(int id) {
			switch (id) {
			case DIALOG_ACCOUNTS:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Select a Google account");
				final Account[] accounts = AccountManager.get(this)
						.getAccountsByType("com.google");
				final int size = accounts.length;
				String[] names = new String[size];
				for (int i = 0; i < size; i++) {
					names[i] = accounts[i].name;
				}
				// TODO
				// Could add a clear alternative here
				builder.setItems(names, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Stuff to do when the account is selected by the user
						prefFragment.accountSelected(accounts[which]);
					}
				});
				return builder.create();
			}
			return null;
		}
	}

	@Override
	public void onDeleteAction() {
		// both list and editor should be notified
		NotesListFragment list = (NotesListFragment) getFragmentManager()
				.findFragmentById(R.id.noteslistfragment);
		NotesEditorFragment editor = (NotesEditorFragment) getFragmentManager()
				.findFragmentById(R.id.editor_container);
		if (editor != null)
			editor.setSelfAction();
		// delete note
		if (list != null)
			list.onDelete();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (UI_DEBUG_PRINTS) Log.d(TAG, "onNavigationItemSelected pos: " + itemPosition + " id: "
				+ itemId);

		// Change the active list
		currentList = itemId;
		// Display list'
		list.showList(itemId);
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		Uri baseUri = NotePad.Lists.CONTENT_URI;
		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		return new CursorLoader(this, baseUri, new String[] {
				NotePad.Lists._ID, NotePad.Lists.COLUMN_NAME_TITLE },
				NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1", // un-deleted
																	// records.
				null, NotePad.Lists.SORT_ORDER // Use the default sort order.
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mSpinnerAdapter.swapCursor(data);
		
		if (prevNumberOfLists == -1) {
//			prevNumberOfLists = mSpinnerAdapter.getCount();
		}
		else if (prevNumberOfLists < mSpinnerAdapter.getCount()) {
			// User created a list, we want to display it
			prevNumberOfLists = mSpinnerAdapter.getCount();
			// Now select it. Using modified desc, will always be first item.
			getActionBar().setSelectedNavigationItem(0);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mSpinnerAdapter.swapCursor(null);
	}
}
