package com.nononsenseapps.notepad;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import androidx.annotation.RequiresApi;

import com.google.android.material.color.DynamicColors;
import com.nononsenseapps.notepad.activities.main.ActivityMain;

/**
 * Represents this app. The application object is not guaranteed to stay
 * in memory forever, it WILL get killed.
 */
public class NnnApp extends Application {

	/**
	 * Called when the application is starting, before any other application
	 * objects have been created. {@link ActivityMain} is a better place to
	 * put initialization logic
	 */
	@Override
	public void onCreate() {
		// enableStrictModeAnalysis();
		super.onCreate();
		// use dynamic colors for android >= 13
		DynamicColors.applyToActivitiesIfAvailable(this);
	}

	/**
	 * Detects every disk read/write operation, and every time a cursor is not closed.
	 * Useful for tests during development. Remember that disk activity is core app
	 * functionality!
	 */
	@RequiresApi(api = Build.VERSION_CODES.P)
	private static void enableStrictModeAnalysis() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.detectDiskWrites()
				.detectNetwork()
				.detectAll()
				.penaltyLog()
				.penaltyFlashScreen()
				.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects()
				.detectLeakedClosableObjects()
				.detectNonSdkApiUsage()
				.detectAll()
				.penaltyLog()
				.build());
	}

}
