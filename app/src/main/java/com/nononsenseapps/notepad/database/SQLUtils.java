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

package com.nononsenseapps.notepad.database;

import android.util.Log;

public class SQLUtils {

	public static String[] repeatTwice(final String[] org) {
		if (org == null || org.length == 0) {
			return null;
		}
		
		String[] result = new String[2*org.length];
		for (int i = 0; i < org.length; i++) {
			Log.d("nononsenseapps widget", "twice: " + org[i]);
			result[2*i] = org[i];
			result[2*i + 1] = org[i];
		}
		
		return result;
	}
}
