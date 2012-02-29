package com.nononsenseapps.notepad.widget;

import java.util.ArrayList;
import java.util.List;

import com.nononsenseapps.notepad.FragmentLayout;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotesPreferenceFragment;
import com.nononsenseapps.notepad.R;

import android.app.Activity;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.media.audiofx.BassBoost.Settings;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Spinner;

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

		// Do this in case users backs out
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_CANCELED, resultValue);

		Log.d("PrefsActivity", "appWidgetId: " + appWidgetId);

		settings = PreferenceManager.getDefaultSharedPreferences(this);

		// Create OK button
		// Button okButton = new Button(context, attrs, defStyle)
		/*
		 * android:id="@+id/list_widget_config_ok"
		 * style="?android:attr/buttonBarButtonStyle" android:layout_width="0dp"
		 * android:layout_height="wrap_content" android:layout_weight="1"
		 */
		Button okButton = new Button(this);
		okButton.setText(R.string.ok);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				configDone();
			}
		});

		setListFooter(okButton);
	}

	/**
	 * Populate the activity with the top-level headers.
	 */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.widget_pref_headers, target);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (settings != null) {
			settings.registerOnSharedPreferenceChangeListener(this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (settings != null) {
			settings.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	private void configCancel() {
		// Not much else to do
		finish();
	}

	private void configDone() {
		// Save values to preferences
		// getSharedPreferences(getSharedPrefsFile(appWidgetId), MODE_PRIVATE)
		// .edit()
		// .putString(LIST_WHERE, "")
		// .putString(NotesPreferenceFragment.KEY_SORT_TYPE,
		// NotesPreferenceFragment.DUEDATESORT)
		// .putString(NotesPreferenceFragment.KEY_SORT_ORDER,
		// NotePad.Notes.DEFAULT_SORT_ORDERING).commit();

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		appWidgetManager.updateAppWidget(appWidgetId,
				ListWidgetProvider.buildRemoteViews(this, appWidgetId));
		// AppWidgetManager.getInstance(mContext)
		// .notifyAppWidgetViewDataChanged(mAppWidgetId,
		// R.id.notes_list);
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d("prefsActivity", "onSharedChanged!: " + key);
		Log.d("prefsActivity", "onSharedChanged, app_WidgetId: " + appWidgetId);

		if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
				&& key.equals(KEY_LIST)) {
			getSharedPreferences(getSharedPrefsFile(appWidgetId), MODE_PRIVATE)
					.edit()
					.putString(key, sharedPreferences.getString(key, null))
					.apply();
		}

		if (key.equals(KEY_LIST)) {
			Log.d("prefsList", "KEY_LIST changed");
		} else if (key.equals(KEY_LIST_TITLE)) {
			Log.d("prefsList", "KEY_LIST_TITLE changed");
		} else if (key.equals(KEY_SORT_ORDER)) {
			Log.d("prefsList", "KEY_SORT_ORDER changed");
		} else if (key.equals(KEY_SORT_TYPE)) {
			Log.d("prefsList", "KEY_SORT_TYPE changed");
		} else if (key.equals(KEY_THEME)) {
			Log.d("prefsList", "KEY_THEME changed");
		}
	}
	
	/**
	 * This fragment shows the preferences for the first header.
	 */
	public static class ListFragment extends PreferenceFragment implements
			OnSharedPreferenceChangeListener {
		private SharedPreferences lSettings;
		private ListPreference listSpinner;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
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
			values.add(Long.toString(FragmentLayout.ALL_NOTES_ID));
			// Set it as the default value also
			listSpinner.setDefaultValue(Long
					.toString(FragmentLayout.ALL_NOTES_ID));

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
			}
		}

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (key.equals(KEY_LIST)) {
				// Must also write the list Name to the prefs
				sharedPreferences.edit()
						.putString(KEY_LIST_TITLE,
								listSpinner.getEntry().toString()).apply();
			}
		}
	}

	/**
	 * This fragment shows the preferences for the first header.
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
