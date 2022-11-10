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
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceFragment;
import android.provider.Settings;

import com.nononsenseapps.notepad.R;

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

	// TODO add a settings button to carry the user to the right page in the device's settings app
	public static void showRequestIgnoreBatteryOptimizations(Context context) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

		boolean ok = pm.isIgnoringBatteryOptimizations(context.getPackageName());
		if (ok) return;

		Intent i = new Intent();
		i.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
		context.startActivity(i);
	}

}
