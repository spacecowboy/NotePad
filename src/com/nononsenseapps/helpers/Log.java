package com.nononsenseapps.helpers;

import com.nononsenseapps.build.Config;

public final class Log {

	public static void d(String t, String m) {
		if (Config.LOGGING || true)
			android.util.Log.d(t, m);
	}

	public static void e(String tAG, String string, Exception e) {
		android.util.Log.e(tAG, string, e);
	}

	public static void e(String tag, String string) {
		android.util.Log.e(tag, string);
	}

}
