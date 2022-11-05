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

package com.nononsenseapps.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.nononsenseapps.notepad.R;

import java.util.Locale;

/**
 * This class contains some helper methods for activities.
 */
public class ActivityHelper {

	/**
	 * Set configured locale on current context
	 */
	public static void setSelectedLanguage(@NonNull Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Configuration config = context.getResources().getConfiguration();

		String lang = prefs.getString(context.getString(R.string.pref_locale), "");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if ("".equals(lang))
				locale = Locale.getDefault();
			else if (lang.length() == 5) {
				locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			} else {
				locale = new Locale(lang.substring(0, 2));
			}
			// Locale.setDefault(locale);
			config.locale = locale;
			context.getResources().updateConfiguration(config, context.getResources()
					.getDisplayMetrics());
		}
	}
}
