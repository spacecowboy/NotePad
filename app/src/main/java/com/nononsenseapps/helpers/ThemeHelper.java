package com.nononsenseapps.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.AppearancePrefs;

/**
 * contains functions that handle themes and related resources
 */
public final class ThemeHelper {

	// forbid instances: it's a static class
	private ThemeHelper() {}

	/**
	 * It is different from {@link R.color#accent} if the user sets a custom Material3
	 * theme in android 13 or greater
	 *
	 * @return This theme's accent color, in the form 0xAARRGGBB
	 */
	public static int getThemeAccentColor(@NonNull Context context) {
		var outValue = new TypedValue();
		context.getTheme()
				.resolveAttribute(android.R.attr.colorAccent, outValue, true);
		return outValue.data;
	}

	/**
	 * Set the theme chosen by the user in {@link AppearancePrefs} for this activity
	 */
	public static void setTheme(@NonNull AppCompatActivity activity) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		final String theme = prefs.getString(AppearancePrefs.KEY_THEME,
				activity.getString(R.string.const_theme_light_ab));
		if (activity.getString(R.string.const_theme_light_ab).equals(theme)) {
			activity.setTheme(R.style.ThemeNnnLight);
		} else if (activity.getString(R.string.const_theme_black).equals(theme)) {
			activity.setTheme(R.style.ThemeNnnPitchBlack);
		} else if (activity.getString(R.string.const_theme_classic).equals(theme)) {
			activity.setTheme(R.style.ThemeNnnClassicLight);
		} else if (theme.equals(activity.getResources().getString(R.string.const_theme_googlenow_dark))) {
			activity.setTheme(R.style.ThemeNnnDark);
		} else {
			// any theme you want to add should go in a new if block ~here
		}
	}
}
