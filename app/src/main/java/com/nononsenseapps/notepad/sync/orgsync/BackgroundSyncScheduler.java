/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.sync.orgsync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class BackgroundSyncScheduler extends BroadcastReceiver {
	// Unique ID for schedule
	private final static int scheduleCode = 2832;
	private static final String TAG = "BackgroundSyncScheduler";

	public BackgroundSyncScheduler() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final boolean enabled = OrgSyncService.areAnyEnabled(context);
		if (enabled && intent != null && Intent.ACTION_RUN.equals(intent
				.getAction())) {
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
		final AlarmManager alarmManager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		final Intent action = new Intent(context, BackgroundSyncScheduler
				.class);
		action.setAction(Intent.ACTION_RUN);
		final PendingIntent operation = PendingIntent
				.getBroadcast(context, scheduleCode, action,
						PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		if (OrgSyncService.areAnyEnabled(context)) {
			// Schedule syncs
			// Repeat at inexact intervals and do NOT wake the device up.
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
					SystemClock.elapsedRealtime(),
					AlarmManager.INTERVAL_HALF_HOUR, operation);
		} else {
			// Remove schedule
			alarmManager.cancel(operation);
		}
	}
}
