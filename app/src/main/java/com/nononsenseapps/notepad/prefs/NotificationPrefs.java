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

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.UnusedAppRestrictionsConstants;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.R;

public class NotificationPrefs extends PreferenceFragmentCompat {

	private static final int REQUEST_CODE_ALERT_RINGTONE = 1;

	@Override
	public void onCreatePreferences(@Nullable Bundle savInstState, String rootKey) {

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_notifications);

		PrefsActivity.bindPreferenceSummaryToValue(
				findPreference(getString(R.string.key_pref_prio)));

		// show the initial value of the selected ringtone
		updateRingtonePrefSummary(
				findPreference(getString(R.string.key_pref_ringtone)),
				this.getContext());

		// the "Preferences for older Android devices" category
		PreferenceCategory prefCat = findPreference(getString(R.string.key_pref_cat_notif_old));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// newer androids have a dedicated settings page for the notification channel
			// => use that, also because the channel overwrites the individual notifications
			prefCat.setEnabled(false);
		} else {
			// older androids don't have the notification channel => we keep using our
			// notification preferences => expand their category
			prefCat.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
		}

	}

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {
		final String key = preference.getKey();
		final String ringtonePrefKey = getString(R.string.key_pref_ringtone);
		final String allowExactRemindersKey = getString(R.string.key_pref_allow_exact_reminders);
		final String ignoreBatteryOptimizationKey = getString(R.string.key_pref_ignore_battery_optimizations);
		final String openNotifChannelKey = getString(R.string.key_pref_notif_channel_settings);
		final String disableHibernation = getString(R.string.key_pref_disable_hibernation);
		final String notificVisibility = getString(R.string.key_pref_notif_visibility);

		if (key.equals(ringtonePrefKey)) {
			// the pseudo-ringtonePreference was clicked => open a system page to pick a ringtone
			Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
							RingtoneManager.TYPE_NOTIFICATION)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
							Settings.System.DEFAULT_NOTIFICATION_URI);

			String existingValue = PreferenceManager
					.getDefaultSharedPreferences(this.getContext())
					.getString(ringtonePrefKey, null);
			if (existingValue != null) {
				Uri existing = existingValue.length() == 0
						? null // Select "Silent"
						: Uri.parse(existingValue);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
			} else {
				// No ringtone has been selected, set to the default
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
						Settings.System.DEFAULT_NOTIFICATION_URI);
			}

			startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);
			return true;
		} else if (key.equals(allowExactRemindersKey)) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
				// open a settings page to enable exact reminders for this app.
				// they're enabled by default in the Android 12 emulator
				Intent i = new Intent()
						.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
						.setData(Uri.parse("package:" + getContext().getPackageName()));
				startActivity(i);
			} else {
				// not needed before android S
			}
			// we don't care about the value
			return false;
		} else if (key.equals(ignoreBatteryOptimizationKey)) {
			// open the battery settings when clicked
			Intent i = new Intent()
					.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
			startActivity(i);

			// the value of this preference is never used,
			// it's just something the user can click to open a settings page
			return false;
		} else if (key.equals(openNotifChannelKey)) {
			openNotificationSettings(this.getContext());
			return false;
		} else if (key.equals(notificVisibility)) {
			// open the app settings to let the user change app permissions
			Intent i = new Intent(
					android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
					Uri.parse("package:" + BuildConfig.APPLICATION_ID));
			startActivity(i);
			return false;
		} else if (key.equals(disableHibernation)) {
			showHibernationPageIfNeeded(this);
			return false;
		} else {
			return super.onPreferenceTreeClick(preference);
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_OK || data == null) {
			// canceled by the user
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}

		if (requestCode == REQUEST_CODE_ALERT_RINGTONE) {
			// the user picked a ringtone => save it
			Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			String ringtonePrefKey = getString(R.string.key_pref_ringtone);
			Preference pref = findPreference(ringtonePrefKey);

			// ringtone == null means that "Silent" was selected in the picker
			String newPrefVal = ringtone == null ? null : ringtone.toString();

			// save the new value
			PreferenceManager
					.getDefaultSharedPreferences(this.getContext())
					.edit()
					.putString(ringtonePrefKey, newPrefVal)
					.commit();
			// show it
			updateRingtonePrefSummary(pref, this.getContext());
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	/**
	 * Get the ringtone name and show it in the summary
	 *
	 * @param theRingtonePref a reference to the {@link Preference} object of the ringtone
	 */
	private static void updateRingtonePrefSummary(Preference theRingtonePref, Context con) {

		// get the URI saved in the preferences
		String ringtonePrefKey = con.getString(R.string.key_pref_ringtone);
		final String ringtonePrefVal = PreferenceManager
				.getDefaultSharedPreferences(con)
				.getString(ringtonePrefKey, null);
		final Uri newVal = ringtonePrefVal == null ? null : Uri.parse(ringtonePrefVal);

		// look up the correct display value using RingtoneManager
		if (newVal == null) {
			// Empty values correspond to 'silent' (no ringtone)
			theRingtonePref.setSummary(R.string.silent);
		} else {
			Ringtone ringtone = RingtoneManager.getRingtone(con, newVal);
			if (ringtone == null) {
				// Clear the summary if there was a lookup error
				theRingtonePref.setSummary(null);
			} else {
				// Set the summary to reflect the new ringtone display name
				String name = ringtone.getTitle(con);
				theRingtonePref.setSummary(name);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// check if battery optimizations are enabled and show it in the summary
		var pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
		int summaryResId1 = pm.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
				? R.string.battery_optimizations_inactive
				: R.string.battery_optimizations_active;
		findPreference(getString(R.string.key_pref_ignore_battery_optimizations))
				.setSummary(summaryResId1);

		var nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
		int summaryResId2 = NotificationHelper.areNotificationsVisible(nm)
				? R.string.notifications_enabled
				: R.string.notifications_blocked;
		findPreference(getString(R.string.key_pref_notif_visibility))
				.setSummary(summaryResId2);
	}

	/**
	 * opens a system settings page dedicated to notification preferences for <br/>
	 * - our only notification channel (only devices on Oreo or newer) <br/>
	 * - the app as a whole (only devices on API 23, 24 or 25) <br/>
	 * In android Oreo and newer, these settings overwrite those of the old preferences,
	 * which now are in the {@link PreferenceCategory} "key_pref_cat_notif_old"
	 */
	private static void openNotificationSettings(Context context) {
		Intent intent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
					.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
					.putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID);
		} else {
			// it works on a tablet with API 23. But it's not as complete as the
			// notification channel preference page on API 32 devices, for example
			intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS")
					.putExtra("app_package", context.getPackageName())
					.putExtra("app_uid", context.getApplicationInfo().uid);
		}
		context.startActivity(intent);
	}

	// TODO test the app in doze mode: see
	//  https://developer.android.com/training/monitoring-device-state/doze-standby#testing_doze
	//  command: $ adb shell dumpsys alarm
	//  in particular, ensure that the notification arrive at a reasonable time


	/**
	 * If the user doesn't start the app for a few months, the system will
	 * place restrictions on it. See the {@link UnusedAppRestrictionsConstants} for details.
	 * This function shows the settings page where the user can disable this behavior
	 */
	static void showHibernationPageIfNeeded(@NonNull PreferenceFragmentCompat owner) {
		var context = owner.getContext();
		ListenableFuture<Integer> lfi = PackageManagerCompat
				.getUnusedAppRestrictionsStatus(context);
		lfi.addListener(() -> {
			// if we're going to show the settings page to disable hibernation
			boolean showPage;
			try {
				int appRestrictionsStatus = lfi.get();
				switch (appRestrictionsStatus) {
					case UnusedAppRestrictionsConstants.API_30_BACKPORT:
					case UnusedAppRestrictionsConstants.API_30:
					case UnusedAppRestrictionsConstants.API_31:
						// restriction enabled => show settings page to let users disable it
						showPage = true;
						break;
					case UnusedAppRestrictionsConstants.ERROR:
					case UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE:
					case UnusedAppRestrictionsConstants.DISABLED:
					default:
						// restriction not enabled => don't show settings page
						showPage = false;
						break;
				}
			} catch (Exception ex) {
				NnnLogger.exception(ex);
				return;
			}
			if (showPage) {
				// ask the user to disable these restrictions: redirect the user to
				// the page in system settings to disable the feature.
				String pkgName = context.getPackageName();
				Intent i = IntentCompat.createManageUnusedAppRestrictionsIntent(context, pkgName);

				// You must use startActivityForResult(), not startActivity(), even if
				// you don't use the result code returned in onActivityResult().
				owner.startActivityForResult(i, 12345);
			} else {
				// tell the user that hibernation is already OFF
				Toast.makeText(context, R.string.msg_hibernation_already_off, Toast.LENGTH_SHORT)
						.show();
			}
		}, ContextCompat.getMainExecutor(context));
	}

}
