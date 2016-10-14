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

package com.nononsenseapps.notepad.ui.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.ui.settings.MainPrefs;

import java.util.Locale;

public class ActivityHelper {
// can be deleted, moved to new activity helper
	public static void readAndSetSettings(Activity activity) {
		// Read settings and set
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(activity);

		final String theme = prefs.getString(MainPrefs.KEY_THEME,
				activity.getString(R.string.const_theme_light_ab));
		if (activity.getString(R.string.const_theme_light_ab).equals(theme)) {
			activity.setTheme(R.style.ThemeHoloLightDarkActonBar);
		}
		else if (activity.getString(R.string.const_theme_black).equals(theme)) {
			activity.setTheme(R.style.ThemeHoloBlack);
		}
		else if (activity.getString(R.string.const_theme_classic).equals(theme)) {
			activity.setTheme(R.style.ThemeHoloLightClassic);
		}
		else // if (theme.equals(getResources().getString(
				// R.string.const_theme_googlenow_dark)))
		{
			activity.setTheme(R.style.ThemeGoogleNowDark);
		}

		// Set language
		Configuration config = activity.getResources().getConfiguration();

		String lang = prefs.getString(activity.getString(R.string.const_preference_locale_key),
				"");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if (lang == null || lang.isEmpty())
				locale = Locale.getDefault();
			else if (lang.length() == 5) {
				locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			}
			else {
				locale = new Locale(lang.substring(0, 2));
			}
			// Locale.setDefault(locale);
			config.locale = locale;
			activity.getResources().updateConfiguration(config,
					activity.getResources().getDisplayMetrics());
		}
		
		if (activity instanceof OnSharedPreferenceChangeListener) {
			prefs.registerOnSharedPreferenceChangeListener((OnSharedPreferenceChangeListener) activity);
		}
	}

}
