package com.nononsenseapps.helpers;

import java.util.Calendar;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.prefs.SyncPrefs.AccountDialog;
import com.nononsenseapps.notepad.sync.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

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
			if (shouldSyncAtAll(context)) {
				requestSyncNow(context);
			}
			break;
		case BACKGROUND:
			// if (shouldSyncBackground(context)) {
			// //requestSync(context);
			// }
			break;
		case ONCHANGE:
			if (shouldSyncOnChange(context)) {
				requestDelayedSync(context);
			}
			break;
		case ONAPPSTART:
			if (shouldSyncOnAppStart(context)) {
				requestSyncNow(context);
			}
			break;
		}

	}

	public static void setBackgroundSync(final Context context) {

	}

	private static void requestSyncNow(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String accountName = prefs.getString(SyncPrefs.KEY_ACCOUNT, "");

		if (accountName != null && !"".equals(accountName)) {
			Account account = SyncPrefs.getAccount(AccountManager.get(context),
					accountName);
			// Don't start a new sync if one is already going
			if (!ContentResolver.isSyncActive(account, MyContentProvider.AUTHORITY)) {
				Bundle options = new Bundle();
				// This will force a sync regardless of what the setting is
				// in accounts manager. Only use it here where the user has
				// manually desired a sync to happen NOW.
				options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
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

	private static void requestDelayedSync(final Context context) {
		context.startService(new Intent(context, SyncDelay.class));
	}

	public static boolean shouldSyncAtAll(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String accountName = prefs.getString(SyncPrefs.KEY_ACCOUNT, "");
		final boolean syncEnabled = prefs.getBoolean(SyncPrefs.KEY_SYNC_ENABLE,
				false);
		return syncEnabled & accountName != null & !accountName.equals("");
	}

	private static boolean shouldSyncOnChange(final Context context) {
		boolean shouldSync = shouldSyncAtAll(context);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		return shouldSync
				& prefs.getBoolean(SyncPrefs.KEY_SYNC_ON_CHANGE, true);
	}

	private static boolean shouldSyncBackground(final Context context) {
		boolean shouldSync = shouldSyncAtAll(context);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		return shouldSync
				& prefs.getBoolean(SyncPrefs.KEY_BACKGROUND_SYNC, true);
	}

	private static boolean shouldSyncOnAppStart(final Context context) {
		boolean shouldSync = shouldSyncAtAll(context);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		// Let 5 mins elapse before sync on start again
		final long now = Calendar.getInstance().getTimeInMillis();
		final long lastSync = prefs.getLong(SyncPrefs.KEY_LAST_SYNC, 0);
		final long fivemins = 5 * 60 * 1000;

		return shouldSync & prefs.getBoolean(SyncPrefs.KEY_SYNC_ON_START, true)
				& (fivemins < now - lastSync);
	}
}