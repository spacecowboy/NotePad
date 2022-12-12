package com.nononsenseapps.helpers;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;

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
	 * @return the Id of the style (theme) to use in a {@link DatePickerDialog}
	 * or {@link TimePickerDialog}
	 */
	@StyleRes
	public static int getPickerDialogTheme(@NonNull Context context) {
		// choose a dark or white theme depending on the appearance preferences
		String defaultTheme = context.getString(R.string.const_theme_light_ab);
		String theme = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(AppearancePrefs.KEY_THEME, defaultTheme);
		int themeResId = theme.contains("light")
				? android.R.style.Theme_Material_Light_Dialog
				: android.R.style.Theme_Material_Dialog;

		return themeResId;
	}

}
