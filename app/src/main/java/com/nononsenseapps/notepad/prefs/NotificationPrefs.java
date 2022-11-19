/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.prefs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;

import com.nononsenseapps.notepad.R;

public class NotificationPrefs extends PreferenceFragment {

	private Preference disableBatteryOptimizationPref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_notifications);

		PrefsActivity.bindPreferenceSummaryToValue(
				findPreference(getString(R.string.key_pref_prio)));
		PrefsActivity.bindPreferenceSummaryToValue(
				findPreference(getString(R.string.key_pref_ringtone)));

		disableBatteryOptimizationPref
				= findPreference(getString(R.string.key_pref_ignore_battery_optimizations));

		findPreference(getString(R.string.key_pref_allow_exact_reminders))
				.setOnPreferenceClickListener(p -> {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
				// open a settings page to enable exact reminders for this app.
				// they're enabled by default in the Android 12 emulator
				Intent i = new Intent()
						.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
						.setData(Uri.parse("package:"+getContext().getPackageName()));
				startActivity(i);
			} else {
				// not needed before android S
			}
			// we don't care about the value
			return false;
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		// check if battery optimizations are enabled and show it in the summary
		PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
		int summaryResId = pm.isIgnoringBatteryOptimizations(getContext().getPackageName())
				? R.string.battery_optimizations_inactive
				: R.string.battery_optimizations_active;
		disableBatteryOptimizationPref.setSummary(summaryResId);

		// (if needed) add a listener to open the settings when clicked
		if (disableBatteryOptimizationPref.getOnPreferenceClickListener() == null)
			disableBatteryOptimizationPref.setOnPreferenceClickListener(y -> {
				Intent i = new Intent()
						.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
				startActivity(i);

				// the value of this preference is never used,
				// it's just something the user can click to open a settings page
				return false;
			});
	}

	// TODO test the app in doze mode: see
	//  https://developer.android.com/training/monitoring-device-state/doze-standby#testing_doze

}
