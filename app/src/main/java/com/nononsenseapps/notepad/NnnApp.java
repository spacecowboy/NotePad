package com.nononsenseapps.notepad;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

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
		super.onCreate();
		// use dynamic colors for android >= 13
		DynamicColors.applyToActivitiesIfAvailable(this);
	}

}
