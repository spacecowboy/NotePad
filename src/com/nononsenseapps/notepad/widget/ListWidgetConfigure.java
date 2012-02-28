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
	private Spinner listSpinner;
	private Spinner sortTypeSpinner;
	private Spinner sortOrderSpinner;

	private SharedPreferences settings;

	public static final String SHARED_PREFS_BASE = "prefs_widget_";
	public static final String LIST_ID = "listId";

	public static final String KEY_LIST = "widget_key_list";
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
		settings.registerOnSharedPreferenceChangeListener(this);

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

		// /////////////////
		// Inflate UI
		// setContentView(R.layout.list_widget_configure);
		//
		// // Get spinners
		// listSpinner = (Spinner) findViewById(R.id.list_widget_config_list);
		// sortTypeSpinner = (Spinner)
		// findViewById(R.id.list_widget_config_sort_type);
		// sortOrderSpinner = (Spinner)
		// findViewById(R.id.list_widget_config_sort_order);
		//
		// // Populate list spinner with the list titles
		// populateListSpinner();
		//
		// // Set button listeners
		//
		// findViewById(R.id.list_widget_config_cancel).setOnClickListener(
		// new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// configCancel();
		// }
		// });
		//
		// findViewById(R.id.list_widget_config_ok).setOnClickListener(
		// new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// configDone();
		// }
		// });
	}

	/**
	 * Populate the activity with the top-level headers.
	 */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.widget_pref_headers, target);
	}

	@Override
	public void switchToHeader(PreferenceActivity.Header header) {
		Log.d("prefsActivity", "Wrong SwitchToHeader called");
		super.switchToHeader(header);
	}

	@Override
	public void switchToHeader(String fragmentName, Bundle args) {
		Log.d("prefsActivity", "Correct SwitchToHeader called");
		if (args == null)
			args = new Bundle();
		if (!args.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID))
			args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		super.switchToHeader(fragmentName, args);
	}

	@Override
	public void startWithFragment(String fragmentName, Bundle args,
			Fragment resultTo, int resultRequestCode) {
		if (args == null)
			args = new Bundle();
		if (!args.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID))
			args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		super.startWithFragment(fragmentName, args, resultTo, resultRequestCode);
	}

	@Override
	public void startWithFragment(String fragmentName, Bundle args,
			Fragment resultTo, int resultRequestCode, int titleRes,
			int shortTitleRes) {
		Log.d("PrefsActivity", "startWithFragment");
		if (args == null)
			args = new Bundle();
		if (!args.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID))
			args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		super.startWithFragment(fragmentName, args, resultTo,
				resultRequestCode, titleRes, shortTitleRes);
	}

	/**
	 * This fragment shows the preferences for the first header.
	 */
	public static class ListFragment extends PreferenceFragment {
		private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		private SharedPreferences settings;

		@Override
		public void onCreate(Bundle saves) {
			super.onCreate(saves);

			Bundle args = getArguments();
			if (args != null) {
				appWidgetId = args.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);

				// Set our custom prefs file. This doesn't work because Android
				// is retarded sometimes...
				// getPreferenceManager().setSharedPreferencesName(
				// getSharedPrefsFile(appWidgetId));

				Log.d("prefsList", "Got it from bundle!! yeah: " + appWidgetId);
			}
			Log.d("prefsList", "List appWidgetId: " + appWidgetId);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.widget_pref_list);

			// Populate list options
			ListPreference listSpinner = (ListPreference) findPreference(KEY_LIST);
			if (listSpinner != null) {
				setEntries(listSpinner);
			}
		}

		/**
		 * Reads the lists from database. Also adds "All lists" as the first
		 * item.
		 * 
		 * @return
		 */
		private void setEntries(ListPreference listSpinner) {
			Log.d("prefsList", "appWidgetId: " + appWidgetId);

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

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (settings != null) {
			settings.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
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
		getSharedPreferences(getSharedPrefsFile(appWidgetId), MODE_PRIVATE)
				.edit().putString(key, sharedPreferences.getString(key, null))
				.apply();

		if (key.equals(KEY_LIST)) {
			Log.d("prefsList", "List changed");
		} else if (key.equals(KEY_SORT_ORDER)) {
			Log.d("prefsList", "KEY_SORT_ORDER changed");
		} else if (key.equals(KEY_SORT_TYPE)) {
			Log.d("prefsList", "KEY_SORT_TYPE changed");
		} else if (key.equals(KEY_THEME)) {
			Log.d("prefsList", "KEY_THEME changed");
		}
	}
}
