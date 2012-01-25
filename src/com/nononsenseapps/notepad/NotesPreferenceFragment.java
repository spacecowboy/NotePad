package com.nononsenseapps.notepad;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class NotesPreferenceFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {
	public static final String KEY_THEME = "key_current_theme";
	public static final String KEY_SORT_ORDER = "key_sort_order";
	public static final String KEY_SORT_TYPE = "key_sort_type";

	public static final String THEME_DARK = "dark";
	public static final String THEME_LIGHT = "light";
	public static final String THEME_LIGHT_ICS_AB = "light_ab";

	private Preference prefSortOrder;
	private Preference prefSortType;
	private Preference prefTheme;

	public String SUMMARY_SORT_TYPE;
	public String SUMMARY_SORT_ORDER;
	public String SUMMARY_THEME;
	private Activity activity;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.main_preferences);

		SUMMARY_SORT_TYPE = getText(
				R.string.settings_summary_sort_type_alphabetic).toString();

		SUMMARY_SORT_ORDER = getText(R.string.settings_summary_sort_order_desc)
				.toString();

		SUMMARY_THEME = getText(R.string.settings_summary_theme_dark)
				.toString();

		prefSortOrder = getPreferenceScreen().findPreference(KEY_SORT_ORDER);
		prefSortType = getPreferenceScreen().findPreference(KEY_SORT_TYPE);
		prefTheme = getPreferenceScreen().findPreference(KEY_THEME);

		SharedPreferences sharedPrefs = getPreferenceScreen()
				.getSharedPreferences();
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Set summaries
		setTypeSummary(sharedPrefs);
		setOrderSummary(sharedPrefs);
		setThemeSummary(sharedPrefs);

		// Set up navigation (adds nice arrow to icon)
		// ActionBar actionBar = getActionBar();
		// actionBar.setDisplayHomeAsUpEnabled(true);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		try {
			if (activity.isFinishing()) {
				Log.d("settings", "isFinishing, should no update summaries");
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
			} else {
				if (KEY_THEME.equals(key)) {
					Log.d("settings", "Theme changed!");
					setThemeSummary(sharedPreferences);
				} else if (KEY_SORT_TYPE.equals(key)) {
					Log.d("settings", "Sort type changed!");
					setTypeSummary(sharedPreferences);
				} else if (KEY_SORT_ORDER.equals(key)) {
					Log.d("settings", "Sort order changed!");
					setOrderSummary(sharedPreferences);
				} else
					Log.d("settings", "Somethign changed!");
			}
		} catch (IllegalStateException e) {
			// This is just in case the "isFinishing" wouldn't be enough
			// The isFinishing will try to prevent us from doing something
			// stupid
			// This catch prevents the app from crashing if we do something
			// stupid
			Log.d("settings", "Exception was caught: " + e.getMessage());
		}
	}

	private void setOrderSummary(SharedPreferences sharedPreferences) {
		String value = sharedPreferences.getString(KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);
		String summary;
		if (NotePad.Notes.DEFAULT_SORT_ORDERING.equals(value))
			summary = getText(R.string.settings_summary_sort_order_desc)
					.toString();
		else
			summary = getText(R.string.settings_summary_sort_order_asc)
					.toString();
		SUMMARY_SORT_ORDER = summary;
		prefSortOrder.setSummary(summary);
	}

	private void setTypeSummary(SharedPreferences sharedPreferences) {
		String value = sharedPreferences.getString(KEY_SORT_TYPE,
				NotePad.Notes.DEFAULT_SORT_TYPE);
		String summary;
		if (NotePad.Notes.DEFAULT_SORT_TYPE.equals(value))
			summary = getText(R.string.settings_summary_sort_type_modified)
					.toString();
		else
			summary = getText(R.string.settings_summary_sort_type_alphabetic)
					.toString();
		SUMMARY_SORT_TYPE = summary;
		prefSortType.setSummary(summary);
	}

	private void setThemeSummary(SharedPreferences sharedPreferences) {
		// Dark theme is default
		String value = sharedPreferences.getString(KEY_THEME, THEME_DARK);
		String summary;
		if (THEME_DARK.equals(value))
			summary = getText(R.string.settings_summary_theme_dark).toString();
		else if (THEME_LIGHT.equals(value))
			summary = getText(R.string.settings_summary_theme_light).toString();
		else
			summary = getText(R.string.settings_summary_theme_light_dark_ab)
					.toString();
		SUMMARY_THEME = summary;
		Log.d("setThemeSummary", "Setting summary now");
		prefTheme.setSummary(summary);
	}

	/*
	 * @Override public boolean onOptionsItemSelected(MenuItem item) { // Handle
	 * all of the possible menu actions. switch (item.getItemId()) { case
	 * android.R.id.home: Log.d("Settings", "Go back!"); finish(); break; }
	 * return super.onOptionsItemSelected(item); }
	 */
}