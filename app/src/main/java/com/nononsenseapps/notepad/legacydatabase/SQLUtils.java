package com.nononsenseapps.notepad.legacydatabase;

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
