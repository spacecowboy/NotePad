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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.nononsenseapps.notepad.R;

/**
 * Helper class to save common options to shared preferences.
 */
public class SharedPreferencesHelper {
    private static SharedPreferences Prefs(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String S(@NonNull Context context, @StringRes int id) {
        return context.getString(id);
    }

    public static void setSortingDue(@NonNull Context context) {
        putStringString(context, R.string.pref_sorttype, R.string.const_duedate);
    }

    public static void setSortingManual(@NonNull Context context) {
        putStringString(context, R.string.pref_sorttype, R.string.const_possubsort);
    }
    public static void setSortingAlphabetic(Context context) {
        putStringString(context, R.string.pref_sorttype, R.string.const_alphabetic);
    }

    public static void putStringString(@NonNull Context context, @StringRes int key, @StringRes int value) {
        put(context, S(context, key), S(context, value));
    }

    public static void put(@NonNull Context context, @NonNull String key, @NonNull String value) {
        Prefs(context).edit().putString(key, value).apply();
    }
}
