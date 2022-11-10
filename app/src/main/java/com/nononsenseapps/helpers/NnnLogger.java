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

import android.util.Log;

import androidx.annotation.NonNull;

import com.nononsenseapps.build.Config;

/**
 * Our own No Nonsense Notes Logger
 */
public final class NnnLogger {

	/**
	 * Logs the given exception with tag "NNN"
	 */
	public static void exception(@NonNull Exception e) {
		Log.e("NNN", e.getMessage());

		String stackTrace = android.util.Log.getStackTraceString(e);
		Log.e("NNN", stackTrace);
	}

	/**
	 * Logs the given error message with tag "NNN". If you have an {@link Exception} object,
	 * please use {@link NnnLogger#exception(Exception)} instead
	 *
	 * @param caller the class who's calling this function. Its name is added to the message
	 * @param message the additional message sent to logcat
	 */
	public static void error(@NonNull Class caller, @NonNull String message) {

		Log.e("NNN", message);
	}

	/**
	 * Logs the given message with tag "NNN", but only in debug mode and if
	 * {@link Config#LOGGING} is set
	 *
	 * @param caller the class who's calling this function. Its name is added to the message
	 * @param message the additional message sent to logcat
	 */
	public static void debugOnly(@NonNull Class caller, @NonNull String message) {
		if (Config.LOGGING) Log.d("NNN", caller.getSimpleName() + ": " + message);
	}
}
