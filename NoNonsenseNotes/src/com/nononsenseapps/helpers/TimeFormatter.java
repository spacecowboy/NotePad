package com.nononsenseapps.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.preference.PreferenceManager;

public class TimeFormatter {

	public static Locale getLocale(final String lang) {
		final Locale locale;
		if (lang == null || lang.isEmpty()) {
			locale = Locale.getDefault();
		}
		else if (lang.length() == 5) {
			locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
		}
		else {
			locale = new Locale(lang.substring(0, 2));
		}
		return locale;
	}

	/**
	 * Formats date according to the designated locale
	 */
	public static String getLocalDateString(final Context context,
			final String lang, final String format, final long timeInMillis) {
		final Locale locale = getLocale(lang);
		// Format values
		final SimpleDateFormat formatter = new SimpleDateFormat(format, locale);
		return formatter.format(new Date(timeInMillis));
	}

	/**
	 * Formats the date according to the locale the user has defined in settings
	 */
	public static String getLocalDateString(final Context context,
			final String format, final long timeInMillis) {
		return getLocalDateString(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				format, timeInMillis);
	}
}
