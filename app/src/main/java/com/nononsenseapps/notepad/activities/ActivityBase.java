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

package com.nononsenseapps.notepad.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.nononsenseapps.notepad.R;

/**
 * A base activity which sets the user's configured language and theme.
 */
public final /* used to be abstract! */ class ActivityBase extends AppCompatActivity {

	// TODO check all activities and see if they need any of these functions, this class should be redundant

	private boolean shouldRestart = false;
	private final SharedPreferences.OnSharedPreferenceChangeListener mThemeLocaleChangeListener =
			new SharedPreferences.OnSharedPreferenceChangeListener() {
				@Override
				public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
					if (key.equals(getString(R.string.const_preference_theme_key)) || key.equals
							(getString(R.string.const_preference_locale_key))) {
						onThemeOrLocaleChange();
					}
				}
			};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(mThemeLocaleChangeListener);
	}

	@Override
	protected void onDestroy() {
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(mThemeLocaleChangeListener);

		super.onDestroy();
	}

	@Override
	protected void onResume() {
		if (shouldRestart) {
			shouldRestart = false;
			restartActivity();
		}
		super.onResume();
	}

	/**
	 * Restarts the activity using the same intent that started it. Disables animations to get a
	 * seamless restart.
	 */
	void restartActivity() {
		Intent intent = getIntent();
		overridePendingTransition(0, 0);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intent);
	}

	/**
	 * Default implementation is to restart activity on onResume. Override if that is not desired.
	 */
	void onThemeOrLocaleChange() {
		shouldRestart = true;
	}
}
