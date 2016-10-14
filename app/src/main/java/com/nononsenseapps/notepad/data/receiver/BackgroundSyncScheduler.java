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

package com.nononsenseapps.notepad.data.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.nononsenseapps.notepad.data.service.OrgSyncService;

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
                        PendingIntent.FLAG_UPDATE_CURRENT);
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
