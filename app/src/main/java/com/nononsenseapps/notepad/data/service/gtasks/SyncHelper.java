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

import android.content.Context;

import com.nononsenseapps.notepad.data.service.OrgSyncService;
import com.nononsenseapps.notepad.util.SyncGtaskHelper;

/**
 * This class handles sync logic for all configured syncers and calls through to each individual
 * service.
 */
public class SyncHelper {

    public static boolean onManualSyncRequest(final Context context) {
        boolean syncing = false;

        // GTasks
        if (SyncGtaskHelper.isGTasksConfigured(context)) {
            syncing = true;
            SyncGtaskHelper.requestSyncIf(context, SyncGtaskHelper.MANUAL);
        }

        // Others
        if (OrgSyncService.areAnyEnabled(context)) {
            syncing = true;
            OrgSyncService.start(context);
        }

        return syncing;
    }
}
