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

package com.nononsenseapps.notepad.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;

import java.util.Locale;

/**
 * Locale related helper functions.
 */
public class LocaleHelper {
    /**
     * @return the users's default or selected locale
     */
    public static Locale getUserLocale(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lang = prefs.getString(context.getString(R.string.const_preference_locale_key), "");
        final Locale locale;
        if (lang == null || lang.isEmpty())
            locale = Locale.getDefault();
        else if (lang.length() == 5) {
            locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
        } else {
            locale = new Locale(lang.substring(0, 2));
        }

        return locale;
    }
}
