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

package com.nononsenseapps.notepad.widget.list;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * An helper class to interact with shared preferences related to the "list widget"
 */
public class WidgetPrefs {

	public final static String PREFS_KEY = "NotesListWidget";
	public final static String WIDGET_PRESENT_KEY = "WidgetPresent";
	public final static boolean WIDGET_PRESENT_DEFAULT = false;

	private final int widgetId;
	private final SharedPreferences prefs;
	private SharedPreferences.Editor prefsEditor = null;

	public static void delete(final Context context, final int widgetId) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
		if (prefs != null) {
			SharedPreferences.Editor edit = prefs.edit();
			if (edit != null) {
				edit.remove(keyWrap(WIDGET_PRESENT_KEY, widgetId)).commit();
			}
		}
	}

	public WidgetPrefs(final Context context, final int widgetId) {
		this.widgetId = widgetId;
		prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
	}

	public String keyWrap(final String originalKey) {
		return keyWrap(originalKey, widgetId);
	}

	public static String keyWrap(final String originalKey, final int widgetId) {
		return originalKey + widgetId;
	}

	public boolean isPresent() {
		if (prefs != null) {
			return prefs.getBoolean(keyWrap(WIDGET_PRESENT_KEY), WIDGET_PRESENT_DEFAULT);
		}
		return false;
	}

	public void setPresent() {
		putBoolean(WIDGET_PRESENT_KEY, true);
	}

	public boolean putBoolean(String key, boolean value) {
		if (prefs != null && prefsEditor == null) {
			prefsEditor = prefs.edit();
		}
		if (prefsEditor != null) {
			prefsEditor.putBoolean(keyWrap(key), value).commit();
			return true;
		}
		return false;
	}

	public boolean getBoolean(String key, boolean defValue) {
		if (prefs != null) {
			try {
				return prefs.getBoolean(keyWrap(key), defValue);
			} catch (ClassCastException e) {
				// Return default value
			}
		}
		return false;
	}

	public void putString(String key, String value) {
		if (prefs != null && prefsEditor == null) {
			prefsEditor = prefs.edit();
		}
		if (prefsEditor != null) {
			prefsEditor.putString(keyWrap(key), value).commit();
		}
	}

	public String getString(String key, String defValue) {
		if (prefs != null) {
			try {
				return prefs.getString(keyWrap(key), defValue);
			} catch (ClassCastException e) {
				// Return default value
			}
		}
		return defValue;
	}

	public int getInt(String key, int defValue) {
		if (prefs != null) {
			try {
				return prefs.getInt(keyWrap(key), defValue);
			} catch (ClassCastException e) {
				// Return default value
			}
		}
		return defValue;
	}

	public void putInt(String key, int value) {
		if (prefs != null && prefsEditor == null) {
			prefsEditor = prefs.edit();
		}
		if (prefsEditor != null) {
			prefsEditor.putInt(keyWrap(key), value).commit();
		}
	}

	public void putLong(String key, long value) {
		if (prefs != null && prefsEditor == null) {
			prefsEditor = prefs.edit();
		}
		if (prefsEditor != null) {
			prefsEditor.putLong(keyWrap(key), value).commit();
		}
	}

	public long getLong(String key, long defValue) {
		if (prefs != null) {
			try {
				return prefs.getLong(keyWrap(key), defValue);
			} catch (ClassCastException e) {
				// Return default value
			}
		}
		return defValue;
	}
}