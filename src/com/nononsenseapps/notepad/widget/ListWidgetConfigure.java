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

package com.nononsenseapps.notepad.widget;

import java.util.ArrayList;
import java.util.List;

import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;

import android.app.ActionBar;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

public class ListWidgetConfigure extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public int appWidgetId;
	private SharedPreferences settings;

	public static final String SHARED_PREFS_BASE = "prefs_widget_";
	public static final String LIST_ID = "listId";

	public static final String KEY_LIST = "widget_key_list";
	public static final String KEY_LIST_TITLE = "widget_key_list_title";
	public static final String KEY_SORT_TYPE = "widget_key_sort_type";
	public static final String KEY_SORT_ORDER = "widget_key_sort_order";
	public static final String KEY_THEME = "widget_key_current_theme";

	public static final String THEME_LIGHT = "widget_light";
	public static final String THEME_DARK = "widget_dark";

	public static String getSharedPrefsFile(int widgetId) {
		return SHARED_PREFS_BASE + widgetId;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		} else {
			appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		}

		// Set up navigation (adds nice arrow to icon)
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			// actionBar.setDisplayShowTitleEnabled(false);
		}

		// Set valid result from start
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * The goal of this method is to set the default values of the widget
	 * preferences. The only reason we do this, and the reason we do it on this
	 * preference object, is that the UI will otherwise reflect the settings of
	 * the last widget that was added which most likely is different from the
	 * default values, meaning possible nonconsistency between UI and internal
	 * values.
	 * 
	 * We do not need to set the default values on the widget's own shared
	 * preferences because they are implicitly set since we defined them when we
	 * try to retrieve the values later.
	 */
	private void setDefaultSharedPreferenceValues() {
		SharedPreferences.Editor edit = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		edit.putString(KEY_LIST, Integer.toString(MainActivity.ALL_NOTES_ID))
				.putString(KEY_SORT_ORDER,
						NotePad.Notes.ASCENDING_SORT_ORDERING)
				.putString(KEY_SORT_TYPE, MainPrefs.DUEDATESORT).commit();
	}

	/**
	 * Populate the activity with the top-level headers.
	 */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.widget_pref_headers, target);
		setDefaultSharedPreferenceValues();
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
	protected void onDestroy() {
		if (settings != null) {
			settings.unregisterOnSharedPreferenceChangeListener(this);
		}
		// Initiate the widget!
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		appWidgetManager.updateAppWidget(appWidgetId,
				ListWidgetProvider.buildRemoteViews(this, appWidgetId));
		// Then kill the config
		super.onDestroy();
	}

	/**
	 * We need to set the changes done in the fragments on the global file in
	 * the widget specific file.
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			// Log.d("prefsActivity", "changed key: " + key);
			// Log.d("prefsActivity","commiting: " +
			// sharedPreferences.getString(key, null));
			Log.d("config", "setting real values: " + key);
			getSharedPreferences(getSharedPrefsFile(appWidgetId), MODE_PRIVATE)
					.edit()
					.putString(key, sharedPreferences.getString(key, null))
					.commit();
		}
	}

	/**
	 * This fragment shows the preferences for list options.
	 */
	public static class ListFragment extends PreferenceFragment implements
			OnSharedPreferenceChangeListener {
		private SharedPreferences lSettings;
		private ListPreference listSpinner;
		private ListPreference sortType;
		private ListPreference sortOrder;
		private Activity activity;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			this.activity = activity;
			lSettings = PreferenceManager.getDefaultSharedPreferences(activity);
		}

		@Override
		public void onCreate(Bundle saves) {
			super.onCreate(saves);
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.widget_pref_list);

			// Populate list options
			listSpinner = (ListPreference) findPreference(KEY_LIST);
			if (listSpinner != null) {
				setEntries(listSpinner);
			}

			sortType = (ListPreference) findPreference(KEY_SORT_TYPE);
			sortOrder = (ListPreference) findPreference(KEY_SORT_ORDER);

			// Also set the summaries
			if (sortOrder.getValue() == null)
				sortOrder.setValue(NotePad.Notes.ASCENDING_SORT_ORDERING);
			sortOrder.setSummary(sortOrder.getEntry());
			if (sortType.getValue() == null)
				sortType.setValue(MainPrefs.DUEDATESORT);
			sortType.setSummary(sortType.getEntry());
		}

		@Override
		public void onResume() {
			super.onResume();
			if (lSettings != null)
				lSettings.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			super.onPause();
			if (lSettings != null)
				lSettings.unregisterOnSharedPreferenceChangeListener(this);
		}

		/**
		 * Reads the lists from database. Also adds "All lists" as the first
		 * item.
		 * 
		 * @return
		 */
		private void setEntries(ListPreference listSpinner) {

			ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
			ArrayList<CharSequence> values = new ArrayList<CharSequence>();

			// Start with all lists
			entries.add(getText(R.string.show_from_all_lists));
			values.add(Long.toString(MainActivity.ALL_NOTES_ID));
			// Set it as the default value also
			listSpinner.setDefaultValue(Long
					.toString(MainActivity.ALL_NOTES_ID));

			Cursor cursor = getActivity().getContentResolver().query(
					NotePad.Lists.CONTENT_VISIBLE_URI,
					new String[] { NotePad.Lists._ID,
							NotePad.Lists.COLUMN_NAME_TITLE }, null, null,
					NotePad.Lists.SORT_ORDER);
			if (cursor != null) {
				if (!cursor.isClosed() && !cursor.isAfterLast()) {
					while (cursor.moveToNext()) {
						entries.add(cursor.getString(cursor
								.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE)));
						values.add(Long.toString(cursor.getLong(cursor
								.getColumnIndex(NotePad.Lists._ID))));
					}
				}

				cursor.close();
			}

			// Set the values
			if (listSpinner != null) {
				listSpinner.setEntries(entries.toArray(new CharSequence[entries
						.size()]));
				listSpinner.setEntryValues(values
						.toArray(new CharSequence[values.size()]));

				listSpinner.setSummary(listSpinner.getEntry());
			}
		}

		/**
		 * Sets the list name in the preferences as well but also sets the list
		 * summaries depending on the selected value.
		 */
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (!activity.isFinishing()) {
				if (key.equals(KEY_LIST)) {
					// Also set the summary to this text
					listSpinner.setSummary(listSpinner.getEntry());
				} else if (key.equals(KEY_SORT_ORDER)) {
					sortOrder.setSummary(sortOrder.getEntry());
				} else if (key.equals(KEY_SORT_TYPE)) {
					sortType.setSummary(sortType.getEntry());
				}
			}
		}
	}

	/**
	 * This fragment shows the preferences for the theme settings.
	 */
	public static class ThemeFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Log.d("Prefs", "List appWidgetId: " + appWidgetId);
			//
			// Set our custom prefs file
			// getPreferenceManager().setSharedPreferencesName(
			// getSharedPrefsFile(appWidgetId));

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.widget_pref_theme);
		}
	}
}
