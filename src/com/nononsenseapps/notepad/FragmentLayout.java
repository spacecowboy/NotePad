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
public class FragmentLayout extends Activity implements OnSharedPreferenceChangeListener {
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
		if (!key.equals(NotesListFragment.SELECTEDIDKEY))
			shouldRestart = true;
	}

	private void readAndSetSettings() {
		// Read settings and set
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		lightTheme = prefs.getBoolean(NotesPreferenceFragment.KEY_THEME, false);
		setTypeOfTheme();

		String sortType = prefs.getString(NotesPreferenceFragment.KEY_SORT_TYPE,
				NotePad.Notes.DEFAULT_SORT_TYPE);
		String sortOrder = prefs.getString(NotesPreferenceFragment.KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);

		NotePad.Notes.SORT_ORDER = sortType + " " + sortOrder;
		Log.d("ReadingSettings", "sortOrder is: " + NotePad.Notes.SORT_ORDER);
		
		// We want to be notified of future changes
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	private void setTypeOfTheme() {
		if (lightTheme) {
			setTheme(android.R.style.Theme_Holo_Light);
		} else {
			setTheme(android.R.style.Theme_Holo);
		}
	}

	/**
	 * This is a secondary activity, to show what the user has selected when the
	 * screen is not large enough to show it all in one activity.
	 */
	public static class NotesEditorActivity extends Activity {
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			if (FragmentLayout.lightTheme) {
				setTheme(android.R.style.Theme_Holo_Light);
			} else {
				setTheme(android.R.style.Theme_Holo);
			}

			// Set up navigation (adds nice arrow to icon)
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);

			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				// If the screen is now in landscape mode, we can show the
				// dialog in-line with the list so we don't need this activity.
				finish();
				return;
			}

			if (savedInstanceState == null) {
				// During initial setup, plug in the details fragment.
				NotesEditorFragment editorFragment = new NotesEditorFragment();
				editorFragment.setArguments(getIntent().getExtras());
				getFragmentManager().beginTransaction()
						.add(android.R.id.content, editorFragment).commit();
			}

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
