/*
 * Copyright (c) 2015. Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;
import com.nononsenseapps.notepad.prefs.SyncPrefs;

/**
 * Helper class to save common options to shared preferences.
 */
public final class PreferencesHelper {

	private static SharedPreferences Prefs(@NonNull Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static boolean shouldUseExactAlarms(@NonNull Context context) {
		String key = context.getString(R.string.key_pref_should_use_exact_alarms);
		return Prefs(context).getBoolean(key, false);
	}

	public static boolean isSdSyncEnabled(@NonNull Context context) {
		return Prefs(context).getBoolean(SyncPrefs.KEY_SD_ENABLE, false);
	}

	/**
	 * Disable SD synchronization in the settings
	 */
	public static void disableSdCardSync(@NonNull Context context) {
		Prefs(context).edit().putBoolean(SyncPrefs.KEY_SD_ENABLE, false).apply();
	}

	/**
	 * @return TRUE if synchronization is enabled in the global preference,
	 * regardless of any sync method chosen. This being false should block all sync code
	 * from running at all
	 */
	public static boolean isSincEnabledAtAll(@NonNull Context context) {
		String key = context.getString(R.string.key_pref_sync_enabled_master);
		return Prefs(context).getBoolean(key, false);
	}

	private static String getStr(@NonNull Context c, int id) {
		return c.getResources().getString(id);
	}

	public static void setSortingDue(@NonNull Context context) {
		Prefs(context)
				.edit()
				.putString(getStr(context, R.string.pref_sorttype), getStr(context, R.string.const_duedate))
				.commit();
	}

	public static void setSortingManual(@NonNull Context context) {
		Prefs(context)
				.edit()
				.putString(getStr(context, R.string.pref_sorttype), getStr(context, R.string.const_possubsort))
				.commit();
	}

	public static void setSortingAlphabetic(Context context) {
		Prefs(context)
				.edit()
				.putString(getStr(context, R.string.pref_sorttype), getStr(context, R.string.const_alphabetic))
				.commit();
	}


	private static void put(@NonNull Context context, @NonNull String key, @NonNull String value) {
		Prefs(context).edit().putString(key, value).apply();
	}

	public static void put(@NonNull Context context, @NonNull String key, boolean value) {
		Prefs(context).edit().putBoolean(key, value).apply();
	}

	/**
	 * @return TRUE if the password for locking notes is set, FALSE if it isn't
	 */
	public static boolean isPasswordSet(@NonNull Context context) {
		return !Prefs(context).getString(PasswordPrefs.KEY_PASSWORD, "").isEmpty();
	}

	/**
	 * @return TRUE if animations are enabled in the system settings. Used to choose if animations
	 * should be displayed in the app
	 */
	public static boolean areAnimationsEnabled(@NonNull Context context) {

		// there are 3 redundant system settings that control animations
		String[] sysSettingsToCheck = new String[] {
				Settings.Global.ANIMATOR_DURATION_SCALE,
				Settings.Global.TRANSITION_ANIMATION_SCALE,
				Settings.Global.WINDOW_ANIMATION_SCALE
		};

		// if at least 1 of those is set to "0x", which means "disable animations",
		// we assume that the user wants to disable all animations, also in this app
		for (String option : sysSettingsToCheck) {
			float f = Settings.Global.getFloat(context.getContentResolver(), option, 1.0f);
			if (f == 0) return false;
		}

		// if none of the 3 settings is "0x", we assume that animations are enabled
		return true;
	}

}
