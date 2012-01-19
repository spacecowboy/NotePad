package com.nononsenseapps.notepad;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

/**
 * Showing a single fragment in an activity.
 */
public class FragmentLayout extends Activity implements
		OnSharedPreferenceChangeListener {
	public static boolean lightTheme = false;
	public static boolean shouldRestart = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		readAndSetSettings();

		// XML makes sure notes list is displayed. And editor too in landscape
		if (lightTheme)
			setContentView(R.layout.fragment_layout_light);
		else
			setContentView(R.layout.fragment_layout_dark);
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
		if (key.equals(NotesPreferenceFragment.KEY_SORT_ORDER)
				|| key.equals(NotesPreferenceFragment.KEY_SORT_TYPE)
				|| key.equals(NotesPreferenceFragment.KEY_THEME)) {
			shouldRestart = true;
		}
	}

	private void readAndSetSettings() {
		// Read settings and set
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		lightTheme = prefs.getBoolean(NotesPreferenceFragment.KEY_THEME, false);
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
		if (lightTheme) {
			setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
		} else {
			setTheme(android.R.style.Theme_Holo);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// both list and editor should be notified
		NotesListFragment list = (NotesListFragment) getFragmentManager()
				.findFragmentById(R.id.noteslistfragment);
		NotesEditorFragment editor = (NotesEditorFragment) getFragmentManager()
				.findFragmentById(R.id.editor);
		switch (item.getItemId()) {
		case R.id.menu_delete:
		case R.id.menu_add:
			if (editor != null) {
				// Order is important. Always editor before list
				// Will be null in singlepane mode
				Log.d("FragmentLayout", "onShared, passing on to editor!");
				editor.onSharedItemSelected(item);
			}

			if (list != null) {
				Log.d("FragmentLayout", "onShared, passing on to list!");
				// Order is important. Always editor before list
				list.onSharedItemSelected(item);
			} else {
				Log.d("FragmentLayout", "onShared, list was null!");
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * This is a secondary activity, to show what the user has selected when the
	 * screen is not large enough to show it all in one activity.
	 */
	public static class NotesEditorActivity extends Activity {
		private NotesEditorFragment editorFragment;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			Log.d("NotesEditorActivity", "onCreate");

			if (FragmentLayout.lightTheme) {
				setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
			} else {
				setTheme(android.R.style.Theme_Holo);
			}

			// Set up navigation (adds nice arrow to icon)
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);

			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				// If the screen is now in landscape mode, we can show the
				// dialog in-line with the list so we don't need this activity.
				Log.d("NotesEditorActivity",
						"Landscape mode detected, killing myself");
				finish();
				return;
			}

			Log.d("NotesEditorActivity", "Time to show the note!");
			// if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			editorFragment = new NotesEditorFragment();
			editorFragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction()
					.add(android.R.id.content, editorFragment).commit();
			// }

		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
			case R.id.menu_delete:
				editorFragment.onSharedItemSelected(item);
				setResult(Activity.RESULT_CANCELED);
				finish();
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
				Log.d("NotesEditorActivity",
						"onPause, telling list to display list");
				SharedPreferences.Editor prefEditor = PreferenceManager
						.getDefaultSharedPreferences(this).edit();
				prefEditor.putBoolean(NotesListFragment.SHOWLISTKEY, true);
				prefEditor.commit();
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			Log.d("NotesEditorActivity", "onResume telling list to display me");
			SharedPreferences.Editor prefEditor = PreferenceManager
					.getDefaultSharedPreferences(this).edit();
			prefEditor.putBoolean(NotesListFragment.SHOWLISTKEY, false);
			prefEditor.commit();
		}
	}

	public static class NotesPreferencesDialog extends Activity {
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			if (FragmentLayout.lightTheme) {
				setTheme(android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
			} else {
				setTheme(android.R.style.Theme_Holo_Dialog_NoActionBar);
			}

			// Display the fragment as the main content.
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(android.R.id.content, new NotesPreferenceFragment());
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
	}
}
