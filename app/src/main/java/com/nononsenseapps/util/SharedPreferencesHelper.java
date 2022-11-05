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

package com.nononsenseapps.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.orgsync.SDSynchronizer;

/**
 * Helper class to save common options to shared preferences.
 */
public class SharedPreferencesHelper {
	private static SharedPreferences Prefs(@NonNull Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static boolean isSdSyncEnabled(@NonNull Context context) {
		return Prefs(context).getBoolean(SyncPrefs.KEY_SD_ENABLE, false);
	}

	/** Disable SD synchronization in the settings */
	public static void disableSdCardSync(@NonNull Context context) {
		Prefs(context).edit().putBoolean(SyncPrefs.KEY_SD_ENABLE, false).apply();
	}

	public static Uri getSdDirUri(@NonNull Context context) {
		return Uri.parse(
				Prefs(context).getString(
						SyncPrefs.KEY_SD_DIR_URI,
						SDSynchronizer.getDefaultOrgDir(context)));
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


	public static void put(@NonNull Context context, @NonNull String key, @NonNull String value) {
		Prefs(context).edit().putString(key, value).apply();
	}

	public static void put(@NonNull Context context, @NonNull String key, boolean value) {
		Prefs(context).edit().putBoolean(key, value).apply();
	}


	public static @NonNull
	String getGoogleAccount(@NonNull Context context) {
		return Prefs(context).getString(SyncPrefs.KEY_ACCOUNT, "");
	}

	public static boolean isPasswordSet(@NonNull Context context) {
		return !Prefs(context).getString(PasswordPrefs.KEY_PASSWORD, "").isEmpty();
	}

}
