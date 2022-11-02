package com.nononsenseapps.helpers;

import com.nononsenseapps.build.Config;

public final class Log {

	/**
	 * Logs the given exception with tag "NNN"
	 */
	public static void exception(Exception e) {
		android.util.Log.e("NNN", e.getMessage());

		String stackTrace = android.util.Log.getStackTraceString(e);
		android.util.Log.e("NNN", stackTrace);
	}

	public static void d(String t, String m) {
		if (Config.LOGGING) android.util.Log.d(t, m);
	}

	public static void e(String tAG, String string, Exception e) {
		android.util.Log.e(tAG, string, e);
	}

	public static void e(String tag, String string) {
		android.util.Log.e(tag, string);
	}

}
