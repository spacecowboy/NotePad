package com.nononsenseapps.helpers;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
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
	 * A theme for dialogs, Light or dark, according to the user-chosen theme.
	 * Use it in a {@link DatePickerDialog}, {@link TimePickerDialog} and {@link AlertDialog}.
	 *
	 * @return the Id of the style (theme)
	 */
	@StyleRes
	public static int getPickerDialogTheme(@NonNull Context context) {
		// choose a dark or white theme depending on the appearance preferences
		String defaultTheme = context.getString(R.string.const_theme_light_ab);
		String theme = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(AppearancePrefs.KEY_THEME, defaultTheme);

		// The "DeviceDefault" theme adapts to the device's general appearance: on Android 13,
		// it uses the user-picked accent color. We use only 2 themes for dialogs: one light,
		// one dark. There's no need to customize it, let's keep it default-looking
		return theme.contains("light")
				? android.R.style.Theme_DeviceDefault_Light_Dialog
				: android.R.style.Theme_DeviceDefault_Dialog;
	}

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
}
