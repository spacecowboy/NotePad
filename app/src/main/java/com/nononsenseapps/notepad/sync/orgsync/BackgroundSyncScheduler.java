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

package com.nononsenseapps.notepad.sync.orgsync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.nononsenseapps.helpers.NnnLogger;

public class BackgroundSyncScheduler extends BroadcastReceiver {
	// Unique ID for schedule
	private final static int scheduleCode = 2832;

	public BackgroundSyncScheduler() {}

	@Override
	public void onReceive(Context context, @NonNull Intent intent) {
		NnnLogger.debug(BackgroundSyncScheduler.class,
				"Received intent with action = " + intent.getAction());

		final boolean enabled = OrgSyncService.areAnyEnabled(context);
		if (enabled && Intent.ACTION_RUN.equals(intent.getAction())) {
			// Run sync
			OrgSyncService.start(context);
		} else {
			scheduleSync(context);
		}
	}

	/**
	 * Schedule a synchronization for later.
	 */
	public static void scheduleSync(final Context context) {
		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		final Intent action = new Intent(context, BackgroundSyncScheduler.class) // EXPLICIT intent!
				.setAction(Intent.ACTION_RUN);
		final PendingIntent operation = PendingIntent.getBroadcast(context, scheduleCode, action,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		if (OrgSyncService.areAnyEnabled(context)) {
			// Schedule syncs
			// Repeat at inexact intervals and do NOT wake the device up.
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
					SystemClock.elapsedRealtime(),
					AlarmManager.INTERVAL_HALF_HOUR, // gets ignored anyway
					operation);
		} else {
			// Remove schedule
			alarmManager.cancel(operation);
		}
	}
}
