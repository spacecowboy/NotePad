package com.nononsenseapps.helpers;

import java.util.Calendar;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.prefs.SyncPrefs;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class SyncDelay extends Service {

	private static final String TAG = "NoNonsenseSyncDelay";

	// Delay this log before doing the sync
	private static final int delaySecs = 60;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/* Schedule a sync if settings say so */
		if (intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
			requestSync();
		} else {
			scheduleSync();
		}

		// Not needed any more, stop us
		super.stopSelf(startId);
		return Service.START_NOT_STICKY;
	}

	private void requestSync() {
		Log.d(TAG, "Requesting sync");
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String accountName = prefs.getString(SyncPrefs.KEY_ACCOUNT, "");
		if (accountName != null && !accountName.equals("")) {
			Account account = SyncPrefs.getAccount(AccountManager.get(this),
					accountName);
			// Don't start a new sync if one is already going
			if (!ContentResolver.isSyncActive(account, NotePad.AUTHORITY)) {
				Bundle options = new Bundle();
				// options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
				ContentResolver
						.requestSync(account, NotePad.AUTHORITY, options);
				Log.d(TAG, "Requested sync");
			}
		}
	}

	private void scheduleSync() {
		// Create an offset from the current time in which the alarm will go
		// off.
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, delaySecs);
		int id = 38475;

		// Create a new PendingIntent and add it to the AlarmManager
		Intent intent = new Intent(Intent.ACTION_RUN);
		PendingIntent pendingIntent = PendingIntent.getService(this, id,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.cancel(pendingIntent);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
		Log.d(TAG, "Scheduled sync");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
