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

package com.nononsenseapps.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;

import java.util.Arrays;
import java.util.Locale;

/**
 * Contains helper methods for activities
 */
public final class ActivityHelper {

	// TODO everything in this "helpers" namespace could be moved to its own
	//  gradle module. This would speed up builds, but maybe it's harder to manage?

	// forbid instances: it's a static class
	private ActivityHelper() {}

	/**
	 * @return the users's default or selected locale
	 */
	public static Locale getUserLocale(Context context) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String lang = prefs.getString(context.getString(R.string.pref_locale), "");
		final Locale locale;
		if (lang.isEmpty())
			locale = Locale.getDefault();
		else if (lang.length() == 5) {
			locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
		} else if (lang.length() == 3) {
			// for example: "vec"
			locale = new Locale(lang);
		} else {
			locale = new Locale(lang.substring(0, 2));
		}

		return locale;
	}

	/**
	 * Set configured locale on the given activity. Call it before Activity.onCreate()
	 */
	public static void setSelectedLanguage(@NonNull AppCompatActivity context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Configuration config = context.getResources().getConfiguration();

		String lang = prefs.getString(context.getString(R.string.pref_locale), "");
		boolean localeExists = Arrays.asList(Locale.getISOLanguages()).contains(lang);
		if (!localeExists) {
			NnnLogger.warning(ActivityHelper.class,
					"Trying to set a locale that does not exist on this device: " + lang);
		}
		if (!config.locale.toString().equals(lang)) {
			config.locale = getUserLocale(context);
			context.getResources()
					.updateConfiguration(config, context.getResources().getDisplayMetrics());
		}
		if (context instanceof OnSharedPreferenceChangeListener) {
			prefs.registerOnSharedPreferenceChangeListener(
					(OnSharedPreferenceChangeListener) context);
		}
	}
}
