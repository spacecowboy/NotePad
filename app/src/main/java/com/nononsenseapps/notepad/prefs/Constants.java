package com.nononsenseapps.notepad.prefs;

/**
 * Key names of the preferences.
 */
public final class Constants {

	// TODO replace calls to SyncPrefs.KEY_SYNC_ENABLE with calls to Constants.KEY_SYNC_ENABLE,
	//  which makes more sense. Also ensure that these actually correspond to the values in the
	//  XML files

	public static final String KEY_SYNC_ENABLE = "syncEnablePref";
	public static final String KEY_ACCOUNT = "accountPref";
	// public static final String KEY_SYNC_FREQ = "syncFreq";
	public static final String KEY_FULLSYNC = "syncFull";
	public static final String KEY_SYNC_ON_START = "syncOnStart";
	public static final String KEY_SYNC_ON_CHANGE = "syncOnChange";
	public static final String KEY_BACKGROUND_SYNC = "syncInBackground";
	// Used for sync on start and on change
	public static final String KEY_LAST_SYNC = "lastSync";
	// SD sync
	public static final String KEY_SD_ENABLE = "pref_sync_sd_enabled";

	// Dropbox sync
	public static final String KEY_DROPBOX_ENABLE = "pref_sync_dropbox_enabled";
	public static final String KEY_DROPBOX_DIR = "pref_sync_dropbox_dir";
	public static final String KEY_THEME = "preference_theme";

	/**
	 * Location of the app tutorial web page
	 */
	public static final String TUTORIAL_URL =
			"https://github.com/spacecowboy/NotePad/blob/master/documents/TUTORIAL.md";
}
