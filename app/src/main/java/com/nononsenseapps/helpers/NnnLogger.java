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

/**
 * Our own No Nonsense Notes Logger
 */
public final class NnnLogger {

	/**
	 * Logs the given exception with tag "NNN"
	 */
	public static void exception(@NonNull Exception e) {
		Log.e("NNN", e.getMessage());

		String stackTrace = Log.getStackTraceString(e);
		Log.e("NNN", stackTrace);
	}

	/**
	 * Logs the given error message with tag "NNN". If you have an {@link Exception} object,
	 * please use {@link NnnLogger#exception(Exception)} instead
	 *
	 * @param caller  the class who's calling this function. Its name is added to the message
	 * @param message the additional message sent to logcat
	 */
	public static <T> void error(@NonNull Class<T> caller, @NonNull String message) {
		try {
			String tag2 = caller.getSimpleName();
			Log.e("NNN", tag2 + ": " + message);
		} catch (Exception ignored) {
			Log.e("NNN", message);
		}
	}

	/**
	 * Logs the given warning message with tag "NNN".
	 *
	 * @param caller  the class who's calling this function. Its name is added to the message
	 * @param message the additional message sent to logcat
	 */
	public static <T> void warning(@NonNull Class<T> caller, @NonNull String message) {
		try {
			String tag2 = caller.getSimpleName();
			Log.w("NNN", tag2 + ": " + message);
		} catch (Exception ignored) {
			Log.w("NNN", message);
		}
	}

	/**
	 * Logs the given message with tag "NNN", but only in debug mode
	 *
	 * @param caller  the class who's calling this function. Its name is added to the message
	 * @param message the additional message sent to logcat. Mostly {@link String}, but it
	 *                will try to write everything
	 */
	public static <T> void debug(@NonNull Class<T> caller, @NonNull Object message) {
		try {
			String tag2 = caller.getSimpleName();
			Log.d("NNN", tag2 + ": " + message);
		} catch (Exception ex) {
			Log.d("NNN", "Can't write LOG line: " + ex.getMessage());
		}
	}

	// TODO this file is in com.nononsenseapps.helpers => move it to its own gradle module.
	//  Having many of these (one per namespace makes sense) will speed up parallel builds.
	//  you can have at least 3 gradle modules: drag-sort-listview,
	//  nononsenseapps-helpers, nononsenseapps-utils, nononsenseapps-ui
	//  and stuff in com.nononsenseapps.notepad remains in the "app" project

}
