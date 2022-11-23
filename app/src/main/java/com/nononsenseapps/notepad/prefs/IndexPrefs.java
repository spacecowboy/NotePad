package com.nononsenseapps.notepad.prefs;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.nononsenseapps.notepad.R;

/**
 * Holds all preference categories, it's the "main" settings page
 */
public class IndexPrefs extends PreferenceFragmentCompat {

	@Override
	public void onCreatePreferences(@Nullable Bundle savInstState, String rootKey) {
		addPreferencesFromResource(R.xml.app_pref_headers);
	}
}
