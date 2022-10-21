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

package com.nononsenseapps.helpers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.orgsync.OrgSyncService;

import java.util.Calendar;

/**
 * This class handles sync logic. No other class should request a sync.
 */
public class SyncHelper {

	// Sync types
	public static final int MANUAL = 0;
	public static final int BACKGROUND = 1;
	public static final int ONCHANGE = 2;
	public static final int ONAPPSTART = 3;

	public static void requestSyncIf(final Context context, final int TYPE) {

		switch (TYPE) {
		case MANUAL:
			if (isGTasksConfigured(context)) {
				requestGTaskSyncNow(context);
			}
			break;
		case BACKGROUND:
			// if (shouldSyncBackground(context)) {
			// //requestSync(context);
			// }
			break;
		case ONCHANGE:
			if (shouldSyncGTasksOnChange(context)) {
				requestDelayedGTasksSync(context);
			}
			break;
		case ONAPPSTART:
			if (shouldSyncGTasksOnAppStart(context)) {
				requestGTaskSyncNow(context);
			}
			break;
		}

	}

    public static boolean isGTasksConfigured(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String accountName = prefs.getString(SyncPrefs.KEY_ACCOUNT, "");
		final boolean syncEnabled = prefs.getBoolean(SyncPrefs.KEY_SYNC_ENABLE,
				false);
		return syncEnabled && !accountName.isEmpty();
	}

	private static void requestGTaskSyncNow(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
        // Do nothing if gtask not enabled
        if (!prefs.getBoolean(SyncPrefs.KEY_SYNC_ENABLE, false)) {
            return;
        }

		final String accountName = prefs.getString(SyncPrefs.KEY_ACCOUNT, "");

		if (!accountName.isEmpty()) {
			Account account = SyncPrefs.getAccount(AccountManager.get(context),
					accountName);
			// Don't start a new sync if one is already going
			if (!ContentResolver.isSyncActive(account, MyContentProvider.AUTHORITY)) {
				Bundle options = new Bundle();
				// This will force a sync regardless of what the setting is
				// in accounts manager. Only use it here where the user has
				// manually desired a sync to happen NOW.
				options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                options.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
				ContentResolver
						.requestSync(account, MyContentProvider.AUTHORITY, options);
				// Set last sync time to now
				prefs.edit()
						.putLong(SyncPrefs.KEY_LAST_SYNC,
								Calendar.getInstance().getTimeInMillis())
						.commit();
			}
		}
	}

	private static boolean shouldSyncGTasksOnChange(final Context context) {
		boolean shouldSync = isGTasksConfigured(context);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		return shouldSync
				& prefs.getBoolean(SyncPrefs.KEY_SYNC_ON_CHANGE, true);
	}

	private static void requestDelayedGTasksSync(final Context context) {
		context.startService(new Intent(context, GTasksSyncDelay.class));
	}

	private static boolean shouldSyncGTasksOnAppStart(final Context context) {
		final boolean shouldSync = isGTasksConfigured(context);

        final boolean enoughTime = enoughTimeSinceLastSync(context);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

		return shouldSync & prefs.getBoolean(SyncPrefs.KEY_SYNC_ON_START, true)
				& enoughTime;
	}

	private static boolean shouldSyncBackground(final Context context) {
		boolean shouldSync = isGTasksConfigured(context);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		return shouldSync
				& prefs.getBoolean(SyncPrefs.KEY_BACKGROUND_SYNC, true);
	}

    public static boolean enoughTimeSinceLastSync(final Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        // Let 5 mins elapse before sync on start again
        final long now = Calendar.getInstance().getTimeInMillis();
        final long lastSync = prefs.getLong(SyncPrefs.KEY_LAST_SYNC, 0);
        final long fivemins = 5 * 60 * 1000;

        return fivemins < (now - lastSync);
    }
}