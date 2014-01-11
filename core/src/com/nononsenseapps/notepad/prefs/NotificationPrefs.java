package com.nononsenseapps.notepad.prefs;

import com.nononsenseapps.notepad.core.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class NotificationPrefs extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_notifications);

		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_prio)));
		PrefsActivity
				.bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_ringtone)));
	}
}
