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

package com.nononsenseapps.notepad.sync.googleapi;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class GoogleTaskSync {
    public static final String AUTH_TOKEN_TYPE = "Manage your tasks";
    public static final boolean NOTIFY_AUTH_FAILURE = true;
    public static final String PREFS_LAST_SYNC_ETAG = "lastserveretag";
    public static final String PREFS_GTASK_LAST_SYNC_TIME = "gtasklastsync";
    static final String TAG = "nononsenseapps gtasksync";

    /**
     * Returns true if sync was successful, false otherwise
     */
    public static boolean fullSync(final Context context,
                                   final Account account, final Bundle extras, final String authority,
                                   final ContentProviderClient provider, final SyncResult syncResult) {
        return false;
    }
}
