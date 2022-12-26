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

package com.nononsenseapps.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.Calendar;

public final class GTasksSyncDelay extends Service {

	// Delay this long before doing the sync
	private static final int delaySecs = 60;

	// TODO useless. delete by ~summer 2023

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/* Schedule a sync if settings say so */
		if (intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
			NnnLogger.debug(GTasksSyncDelay.class, "Requesting sync NOW");
			//SyncHelper.requestSyncIf(this, SyncHelper.MANUAL);
			SyncGtaskHelper.requestSyncIf(this, SyncGtaskHelper.MANUAL);
		} else {
			scheduleSync();
		}

		// Not needed any more, stop us
		super.stopSelf(startId);
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Uses {@link AlarmManager} to schedule the synchronization. Be careful:
	 * https://developer.android.com/training/scheduling/alarms#exact
	 * describes the differences between {@link AlarmManager#set} all
	 * the way up to {@link AlarmManager#setAlarmClock}
	 */
	private void scheduleSync() {
		// Create an offset from the current time in which the alarm will go off.
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, delaySecs);
		int id = 38475;

		// must use an EXPLICIT intent!
		Intent intent = new Intent(this, GTasksSyncDelay.class)
				.setAction(Intent.ACTION_RUN);
		// Create a new PendingIntent and add it to the AlarmManager
		var pendingIntent = PendingIntent.getService(this, id, intent,
				PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.cancel(pendingIntent);

		// it used to be am.set(), which after API 19 became too vague.
		// setExactAndAllowWhileIdle() would be ideal, but it requires a permission since API 32.
		// setAndAllowWhileIdle() is better than set() because:
		// * it fights doze mode a bit
		// * it does not require a permission
		// keep in mind that this code is for background sync, so delays are tolerable
		am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
				cal.getTimeInMillis(), pendingIntent); // Yes, use local time

		NnnLogger.debug(GTasksSyncDelay.class, "Scheduled sync");
	}
}