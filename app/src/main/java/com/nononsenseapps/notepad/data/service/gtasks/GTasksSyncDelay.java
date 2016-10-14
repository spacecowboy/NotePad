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

package com.nononsenseapps.notepad.data.service.gtasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.nononsenseapps.notepad.util.SyncGtaskHelper;

import java.util.Calendar;

public class GTasksSyncDelay extends Service {
    // Delay this long before doing the sync
    private static final int delaySecs = 60;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* Schedule a sync if settings say so */
        if (intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
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

    private void scheduleSync() {
        // Create an offset from the current time in which the alarm will go off.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, delaySecs);
        int id = 38475;

        // Create a new PendingIntent and add it to the AlarmManager
        Intent intent = new Intent(Intent.ACTION_RUN);
        PendingIntent pendingIntent = PendingIntent.getService(this, id, intent, PendingIntent
                .FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
        // Yes, use local time
        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
    }

}
