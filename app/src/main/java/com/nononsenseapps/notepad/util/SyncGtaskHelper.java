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

package com.nononsenseapps.notepad.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.nononsenseapps.notepad.data.service.gtasks.GTasksSyncDelay;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.local.sql.MyContentProvider;

import java.util.Calendar;

/**
 * Helper methods related to Google Tasks synchronization. This should be the only place where
 * sync is requested/managed.
 */
public class SyncGtaskHelper {
    // Sync types
    public static final int MANUAL = 0;
    public static final int ONCHANGE = 2;

    public static final String KEY_LAST_SYNC = "lastSync";

    public static void requestSyncIf(final Context context, final int TYPE) {
        if (!isGTasksConfigured(context)) {
            return;
        }

        switch (TYPE) {
            case MANUAL:
                forceGTaskSyncNow(context);
                break;
            case ONCHANGE:
                requestDelayedGTasksSync(context);
                break;
        }

    }

    public static boolean isGTasksConfigured(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String accountName = prefs.getString(context.getString(R.string
                .const_preference_gtask_account_key), "");
        final boolean syncEnabled = prefs.getBoolean(context.getString(R.string
                .const_preference_gtask_enabled_key), false);
        return syncEnabled & !accountName.isEmpty();
    }

    /**
     * Finds and returns the account of the name given
     *
     * @param accountName email of google account
     * @return a Google Account
     */
    public static Account getAccount(@NonNull AccountManager manager, @NonNull String accountName) {
        Account[] accounts = manager.getAccountsByType("com.google");
        for (Account account : accounts) {
            if (account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Removes account name from sharedpreferences if one is saved.
     */
    @SuppressLint("CommitPrefEdits")
    public static void disableSync(@NonNull Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                (context);
        sharedPreferences.edit().putBoolean(context.getString(R.string
                .const_preference_gtask_enabled_key), false).commit();
        toggleSync(context, sharedPreferences);
    }

    /**
     * If the toggle is not successful in setting sync to on, removes account name from
     * sharedpreferences.
     *
     * @return the status of the sync after enabling/disabling it.
     */
    public static boolean toggleSync(@NonNull Context context, @NonNull SharedPreferences
            sharedPreferences) {
        final boolean enabled = sharedPreferences.getBoolean(context.getString(R.string
                .const_preference_gtask_enabled_key), false);
        String accountName = sharedPreferences.getString(context.getString(R.string
                .const_preference_gtask_account_key), "");

        boolean currentlyEnabled = false;

        if (!accountName.isEmpty()) {
            Account account = getAccount(AccountManager.get(context), accountName);
            if (account != null) {
                if (enabled) {
                    // set syncable
                    ContentResolver.setSyncAutomatically(account, MyContentProvider.AUTHORITY,
                            true);
                    ContentResolver.setIsSyncable(account, MyContentProvider.AUTHORITY, 1);
                    // Also set sync frequency
                    long pollFrequency = 3600;
                    // Set periodic syncing
                    ContentResolver.addPeriodicSync(account, MyContentProvider.AUTHORITY, Bundle
                            .EMPTY, pollFrequency);
                    currentlyEnabled = true;
                } else {
                    ContentResolver.setSyncAutomatically(account, MyContentProvider.AUTHORITY,
                            false);
                }
            }
        }
        if (!currentlyEnabled) {
            forgetAccountOnce(context, sharedPreferences);
            disableSyncOnce(context, sharedPreferences);
        }
        return currentlyEnabled;
    }

    /**
     * Returns true if at least 5 minutes have passed since last sync.
     */
    public static boolean enoughTimeSinceLastSync(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Let 5 mins elapse before sync on start again
        final long now = Calendar.getInstance().getTimeInMillis();
        final long lastSync = prefs.getLong(KEY_LAST_SYNC, 0);
        final long fivemins = 5 * 60 * 1000;

        return fivemins < (now - lastSync);
    }

    /**
     * Disables gtask sync, but only if it's not already disabled (so
     * we don't call listeners on removal of already removed values)
     */
    private static void disableSyncOnce(@NonNull Context context, @NonNull SharedPreferences
            sharedPreferences) {
        if (sharedPreferences.getBoolean(context.getString(R.string
                .const_preference_gtask_enabled_key), false)) {
            sharedPreferences.edit().putBoolean(context.getString(R.string
                    .const_preference_gtask_enabled_key), false).apply();

        }
    }

    /**
     * Removes the account name from shared preferences, but only if we have an account name (so
     * we don't call listeners on removal of already removed values)
     */
    private static void forgetAccountOnce(@NonNull Context context, @NonNull SharedPreferences
            sharedPreferences) {
        if (sharedPreferences.contains(context.getString(R.string
                .const_preference_gtask_account_key))) {
            sharedPreferences.edit().remove(context.getString(R.string
                    .const_preference_gtask_account_key)).apply();

        }
    }

    /**
     * Request sync right now.
     */
    private static void forceGTaskSyncNow(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Do nothing if gtask not enabled
        if (!isGTasksConfigured(context)) {
            return;
        }

        final String accountName = prefs.getString(context.getString(R.string
                .const_preference_gtask_account_key), "");

        if (!accountName.isEmpty()) {
            Account account = getAccount(AccountManager.get(context), accountName);
            // Don't start a new sync if one is already going
            if (!ContentResolver.isSyncActive(account, MyContentProvider.AUTHORITY)) {
                Bundle options = new Bundle();
                // This will force a sync regardless of what the setting is
                // in accounts manager. Only use it here where the user has
                // manually desired a sync to happen NOW.
                options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                options.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(account, MyContentProvider.AUTHORITY, options);
                // Set last sync time to now
                prefs.edit().putLong(KEY_LAST_SYNC, Calendar.getInstance().getTimeInMillis())
                        .apply();
            }
        }
    }

    private static void requestDelayedGTasksSync(final Context context) {
        context.startService(new Intent(context, GTasksSyncDelay.class));
    }
}
