package com.nononsenseapps.notepad;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import com.nononsenseapps.notepad.interfaces.DeleteActionListener;
import com.nononsenseapps.notepad.interfaces.OnEditorDeleteListener;
import com.nononsenseapps.notepad.interfaces.Refresher;
import com.nononsenseapps.notepad.interfaces.OnNoteOpenedListener;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

/**
 * Showing a single fragment in an activity.
 */
public class FragmentLayout extends Activity implements
		OnSharedPreferenceChangeListener, OnEditorDeleteListener,
		DeleteActionListener {
	// public static boolean lightTheme = false;
	public static String currentTheme = NotesPreferenceFragment.THEME_LIGHT;
	public static boolean shouldRestart = false;
	public static boolean LANDSCAPE_MODE;
	public static boolean AT_LEAST_ICS;
	public static boolean AT_LEAST_HC;

	public static OnEditorDeleteListener ONDELETELISTENER = null;

	private Refresher list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Must set theme before this
		super.onCreate(savedInstanceState);

		LANDSCAPE_MODE = getResources().getBoolean(R.bool.useLandscapeView);
		AT_LEAST_ICS = getResources()
				.getBoolean(R.bool.atLeastIceCreamSandwich);
		AT_LEAST_HC = getResources().getBoolean(R.bool.atLeastHoneycomb);

		// Setting theme here
		readAndSetSettings();

		Log.d("Activity", "onCreate before");
		// XML makes sure notes list is displayed. And editor too in landscape
		// if (lightTheme)
		// setContentView(R.layout.fragment_layout_light);
		// else
		setContentView(R.layout.fragment_layout);
		Log.d("Activity", "onCreate after");

		// Set this as delete listener
		NotesListFragment list = (NotesListFragment) getFragmentManager()
				.findFragmentById(R.id.noteslistfragment);

		list.setOnDeleteListener(this);

		this.list = list;
		// So editor can access it
		ONDELETELISTENER = this;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d("FragmentLayout", "On New Intent list: " + list);
		if (this.list != null) {
			Log.d("FragmentLayout", "Calling refresh");
			list.refresh();
		}
	}

	@Override
	protected void onResume() {
		Log.d("FragmentLayout", "onResume");
		if (shouldRestart) {
			Log.d("FragmentLayout", "Should refresh");
			restartAndRefresh();
		}
		super.onResume();
	}

	public void restartAndRefresh() {
		Log.d("FragmentLayout", "Should restart and refresh");
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
		if (key.equals(NotesPreferenceFragment.KEY_THEME)
				|| key.equals(NotesPreferenceFragment.KEY_SORT_TYPE)
				|| key.equals(NotesPreferenceFragment.KEY_SORT_ORDER)) {
			shouldRestart = true;
		}
	}

	private void readAndSetSettings() {
		// Read settings and set
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		// lightTheme = prefs.getBoolean(NotesPreferenceFragment.KEY_THEME,
		// false);

		currentTheme = prefs.getString(NotesPreferenceFragment.KEY_THEME,
				NotesPreferenceFragment.THEME_LIGHT);

		setTypeOfTheme();

		String sortType = prefs.getString(
				NotesPreferenceFragment.KEY_SORT_TYPE,
				NotePad.Notes.DEFAULT_SORT_TYPE);
		String sortOrder = prefs.getString(
				NotesPreferenceFragment.KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);

		NotePad.Notes.SORT_ORDER = sortType + " " + sortOrder;
		Log.d("ReadingSettings", "sortOrder is: " + NotePad.Notes.SORT_ORDER);

		// We want to be notified of future changes
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	private void setTypeOfTheme() {
		if (NotesPreferenceFragment.THEME_LIGHT_ICS_AB.equals(currentTheme)) {
			setTheme(R.style.ThemeHoloLightDarkActonBar);
			// lightTheme = true;
		} else if (NotesPreferenceFragment.THEME_LIGHT.equals(currentTheme)) {
			setTheme(R.style.ThemeHoloLight);
			// lightTheme = true;
		} else {
			setTheme(R.style.ThemeHolo);
			// lightTheme = false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			onDeleteAction();
			break;
		case R.id.menu_preferences:
			Log.d("NotesListFragment", "onOptionsSelection pref");
			showPrefs();
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
			OnNoteOpenedListener, DeleteActionListener {
		private NotesEditorFragment editorFragment;
		private long currentId = -1;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// Make sure to set themes before this
			super.onCreate(savedInstanceState);

			Log.d("NotesEditorActivity", "onCreate");

			if (NotesPreferenceFragment.THEME_LIGHT_ICS_AB
					.equals(FragmentLayout.currentTheme)) {
				setTheme(R.style.ThemeHoloLightDarkActonBar);
				// lightTheme = true;
			} else if (NotesPreferenceFragment.THEME_LIGHT
					.equals(FragmentLayout.currentTheme)) {
				setTheme(R.style.ThemeHoloLight);
				// lightTheme = true;
			} else {
				setTheme(R.style.ThemeHolo);
				// lightTheme = false;
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
			}

			if (getResources().getBoolean(R.bool.useLandscapeView)) {
				// If the screen is now in landscape mode, we can show the
				// dialog in-line with the list so we don't need this activity.
				Log.d("NotesEditorActivity",
						"Landscape mode detected, killing myself");
				finish();
				return;
			}

			this.currentId = getIntent().getExtras().getLong(
					NotesEditorFragment.KEYID);

			Log.d("NotesEditorActivity", "Time to show the note!");
			// if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			editorFragment = new NotesEditorFragment();
			editorFragment.setOnNewNoteCreatedListener(this);
			editorFragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, editorFragment).commit();
			// }
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
			Log.d("NotesEditorActivity", "onPause");
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
			if (getResources().getBoolean(R.bool.useLandscapeView)) {
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
		public void onNoteOpened(long id, boolean created) {
			this.currentId = id;
		}

		@Override
		public void onNewNoteDeleted(long id) {
			// We don't care about this here
		}

		@Override
		public void onDeleteAction() {
			Log.d("NotesEditorActivity", "onDeleteAction");
			// TODO delete setNoSave as it serves no purpose if fragment listens to changes on note itself
			//editorFragment.setNoSave();
			FragmentLayout.deleteNote(getContentResolver(), currentId);
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
		ArrayList<Long> idList = new ArrayList<Long>();
		idList.add(id);
		deleteNotes(resolver, idList);
	}

	/**
	 * Delete all notes given from database
	 * 
	 * @param ids
	 */
	public static void deleteNotes(ContentResolver resolver, Iterable<Long> ids) {
		for (long id : ids) {
			ContentValues values = new ContentValues();
			values.put(NotePad.Notes.COLUMN_NAME_DELETED, "1");
			resolver.update(NotesEditorFragment.getUriFrom(id), values, null, null);
			//resolver.delete(NotesEditorFragment.getUriFrom(id), null, null);
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
				editor.setNoSave();
			}
		}
		Log.d("FragmentLayout", "deleting notes...");
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

			// if (FragmentLayout.lightTheme) {
			// setTheme(android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
			// } else {
			// setTheme(android.R.style.Theme_Holo_Dialog_NoActionBar);
			// }

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
		      final Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
		      final int size = accounts.length;
		      String[] names = new String[size];
		      for (int i = 0; i < size; i++) {
		        names[i] = accounts[i].name;
		      }
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
		//NotesEditorFragment editor = (NotesEditorFragment) getFragmentManager()
		//		.findFragmentById(R.id.editor_container);
		// TODO unnecessary to tell editor this, it already knows when it is deleted.
		//if (editor != null)
		//	editor.setNoSave();
		// delete note
		if (list != null)
			list.onDelete();
	}
}
