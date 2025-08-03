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

package com.nononsenseapps.notepad.prefs;

import android.app.backup.BackupManager;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.helpers.ThemeHelper;
import com.nononsenseapps.notepad.R;

/**
 * The preferences page, holds a list of all preference categories
 */
public class PrefsActivity extends AppCompatActivity implements
		PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	private boolean isTabletInLandscape = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ThemeHelper.setTheme(this);
		ActivityHelper.setSelectedLanguage(this);
		super.onCreate(savedInstanceState);

		// Add the arrow to go back
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			// the title updates when the user chooses a new language setting,
			// but ONLY if we set it here
			getSupportActionBar().setTitle(R.string.menu_preferences);
		}

		// inflates a layout with a fragmentcontainerview, which will
		// automatically start an instance of IndexPrefs
		setContentView(R.layout.activity_settings);

		// this exists only in the tablet-landscape layout file
		FragmentContainerView fragmentSpot2 = this.findViewById(R.id.fragmentRightForTablets);
		isTabletInLandscape = fragmentSpot2 != null;

		getSupportFragmentManager().addOnBackStackChangedListener(() -> {
			int numActiveFrags = getSupportFragmentManager().getBackStackEntryCount();
			if (numActiveFrags == 1) {
				// it's opening a settings category => there is nothing to do
			} else if (numActiveFrags == 0) {
				// it's going back to the "main menu" => remove the subtitle
				if (getSupportActionBar() != null) {
					getSupportActionBar().setSubtitle(null);
				}
			} else {
				NnnLogger.warning(PrefsActivity.class,
						"unexpected numActiveFrags = " + numActiveFrags);
			}
		});

		// when pressing the physical back button, navigate between fragments by removing the
		// subtitle
		getOnBackPressedDispatcher().addCallback(this,
				new OnBackPressedCallback(true) {
					@Override
					public void handleOnBackPressed() {
						// replicate super.onBackPressed() behavior
						if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
							getSupportFragmentManager().popBackStack();
						} else {
							finish();
						}
					}
				});
	}

	/**
	 * called when a settings category is clicked. It opens the appropriate
	 * preference fragment. From:
	 * <a href="https://developer.android.com/develop/ui/views/components/settings/organize-your-settings">here</a>.
	 */
	@Override
	public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller,
											 Preference pref) {
		// Instantiate the new Fragment
		final Bundle args = pref.getExtras();
		final Fragment fragment = getSupportFragmentManager()
				.getFragmentFactory()
				.instantiate(getClassLoader(), pref.getFragment());
		fragment.setArguments(args);
		fragment.setTargetFragment(caller, 0);

		if (isTabletInLandscape) {
			// for tablets in landscape mode, 2 fragments are shown (=> 2 pane view):
			// the "main menu" remains on the left, the preference page list opens on the right
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragmentRightForTablets, fragment)
					// don't call .addToBackStack(null), so the back button will immediately exit
					.commit();

		} else {
			// for phones & tablets in portrait mode, there is only 1 fragment shown:
			// Replace the existing "main menu" Fragment with the new "category" Fragment
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragment, fragment)
					.addToBackStack(null)
					.commit();
		}

		if (getSupportActionBar() != null) {
			getSupportActionBar().setSubtitle(pref.getTitle());
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		// Request a backup in case prefs changed. Safe to call multiple times
		new BackupManager(this).dataChanged();
		// show reminders notifications. Useful when the user re-enables notifications
		// permissions: in this case, any overdue notification should be shown immediately
		NotificationHelper.schedule(this);
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// This ID represents the Home or Up button. In this activity, the Up button is shown.
			// To get a consistent behavior, both pressing "back" and clicking the Up arrow
			// will navigate back, so if a preference category is shown, pressing the Up
			// button won't close the settings, it will go back to the Index
			getOnBackPressedDispatcher().onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value. Handles the {@link ListPreference} specially.
	 */
	private static final Preference.OnPreferenceChangeListener
			sBindPreferenceSummaryToValueListener = (preference, value) -> {
		final String stringValue = value.toString();

		if (preference instanceof ListPreference listPreference) {
			// For list preferences, look up the correct display value in
			// the preference's 'entries' list.
			int index = listPreference.findIndexOfValue(stringValue);

			// Set the summary to reflect the new value, if possible
			preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

		} else {
			// For all other preferences, set the summary to the value's
			// simple string representation.
			preference.setSummary(stringValue);
		}
		return true;
	};

	/**
	 * Binds a preference's summary to its value. When the preference's value is changed,
	 * its summary (text below the preference title) is updated to reflect the value.
	 * The summary is also updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	public static void bindSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
				PreferenceManager
						.getDefaultSharedPreferences(preference.getContext())
						.getString(preference.getKey(), ""));
	}
}
