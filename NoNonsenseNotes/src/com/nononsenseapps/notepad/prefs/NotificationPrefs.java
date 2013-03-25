package com.nononsenseapps.notepad.prefs;

import com.nononsenseapps.notepad.R;

import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class NotificationPrefs extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_notifications);

		final ListPreference prio = (ListPreference) findPreference(getString(R.string.key_pref_prio));
		// Set current prio as summary
		prio.setSummary(prio.getEntry());
		// Set summary on change
		prio.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				// Have to find index of the new value first
				int index = -1;
				for (CharSequence val : ((ListPreference) preference)
						.getEntryValues()) {
					index += 1;
					if (val.toString().equals(newValue.toString())) {
						// we're done
						break;
					}
				}
				if (index > -1)
					preference.setSummary(((ListPreference) preference)
							.getEntries()[index]);

				return true;
			}
		});

		final Preference ringtoneChooser = findPreference(getString(R.string.key_pref_ringtone));

		final Context context = getActivity();
		if (context != null) {
			// Set current ringtone as summary
			setRingtoneSummary(
					ringtoneChooser,
					PreferenceManager.getDefaultSharedPreferences(context)
							.getString(
									getString(R.string.key_pref_ringtone),
									RingtoneManager.getDefaultUri(
											RingtoneManager.TYPE_NOTIFICATION)
											.toString()));
		}

		// Change summary on preference change
		ringtoneChooser
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object stringUri) {
						// Object actually is a string but toString avoids
						// casting
						setRingtoneSummary(preference, stringUri.toString());

						return true;
					}
				});
	}

	private void setRingtoneSummary(final Preference pref,
			final String stringUri) {
		final Context context = getActivity();

		if (stringUri == null || stringUri.isEmpty()) {
			// Silent is an empty string
			pref.setSummary(R.string.silent);
		} else if (context != null) {
			// Got crash report, activity seemed to be null. Guard against it.
			Uri ringtoneUri = Uri.parse(stringUri);
			if (ringtoneUri != null) {
				Ringtone ringtone = RingtoneManager.getRingtone(context,
						ringtoneUri);
				if (ringtone != null)
					pref.setSummary(ringtone.getTitle(context));
			}
		}
	}
}
