package com.nononsenseapps.helpers;

import java.util.Locale;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;

public class ActivityHelper {

	public static void readAndSetSettings(Activity activity) {
		// Read settings and set
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(activity);

		final String theme = prefs.getString(MainPrefs.KEY_THEME,
				MainPrefs.THEME_LIGHT_ICS_AB);
		if (MainPrefs.THEME_LIGHT_ICS_AB.equals(theme)) {
			activity.setTheme(R.style.ThemeHoloLightDarkActonBar);
		}
		else if (MainPrefs.THEME_BLACK.equals(theme)) {
			activity.setTheme(R.style.ThemeHoloBlack);
		}
		else // if (theme.equals(getResources().getString(
				// R.string.const_theme_googlenow_dark)))
		{
			activity.setTheme(R.style.ThemeGoogleNowDark);
		}

		// Set language
		Configuration config = activity.getResources().getConfiguration();

		String lang = prefs.getString(activity.getString(R.string.pref_locale),
				"");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if ("".equals(lang))
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
	}
}
