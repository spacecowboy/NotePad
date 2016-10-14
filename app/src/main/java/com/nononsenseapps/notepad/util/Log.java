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

package com.nononsenseapps.notepad.util;

import com.nononsenseapps.notepad.BuildConfig;

public final class Log {

	public static void d(String t, String m) {
		if (BuildConfig.DEBUG)
			android.util.Log.d(t, m);
	}

	public static void e(String tAG, String string, Exception e) {
		android.util.Log.e(tAG, string, e);
	}

	public static void e(String tag, String string) {
		android.util.Log.e(tag, string);
	}

}
