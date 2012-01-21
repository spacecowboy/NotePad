package com.nononsenseapps.notepad;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.MenuItem;

public class NotesPreferenceActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_THEME = "key_theme";
	public static final String KEY_SORT_ORDER = "key_sort_order";
	public static final String KEY_SORT_TYPE = "key_sort_type";

	private Preference prefSortOrder;
	private Preference prefSortType;

	public String SUMMARY_SORT_TYPE;
	public String SUMMARY_SORT_ORDER;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (FragmentLayout.lightTheme) {
			setTheme(R.style.ThemeHoloDialogNoActionBar);
		} else {
			setTheme(R.style.ThemeHoloDialogNoActionBar);
		}

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.main_preferences);

		SUMMARY_SORT_TYPE = getText(
				R.string.settings_summary_sort_type_alphabetic).toString();

		SUMMARY_SORT_ORDER = getText(R.string.settings_summary_sort_order_desc)
				.toString();

		prefSortOrder = getPreferenceScreen().findPreference(KEY_SORT_ORDER);
		prefSortType = getPreferenceScreen().findPreference(KEY_SORT_TYPE);

		SharedPreferences sharedPrefs = getPreferenceScreen()
				.getSharedPreferences();
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Set summaries
		setTypeSummary(sharedPrefs);
		setOrderSummary(sharedPrefs);
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

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (KEY_THEME.equals(key)) {
			Log.d("settings", "Theme changed!");
		} else if (KEY_SORT_TYPE.equals(key)) {
			Log.d("settings", "Sort type changed!");
			setTypeSummary(sharedPreferences);
		} else if (KEY_SORT_ORDER.equals(key)) {
			Log.d("settings", "Sort order changed!");
			setOrderSummary(sharedPreferences);
		} else
			Log.d("settings", "Somethign changed!");
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

}
