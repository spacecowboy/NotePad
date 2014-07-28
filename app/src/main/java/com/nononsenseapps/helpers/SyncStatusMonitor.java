package com.nononsenseapps.helpers;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.SyncAdapter;

public class SyncStatusMonitor extends BroadcastReceiver {
	public static interface OnSyncStartStopListener {
		/**
		 * This is always called on the activity's UI thread.
		 */
		public void onSyncStartStop(final boolean ongoing);
	}

	Activity activity;

	/**
	 * Call this in the activity's onResume
	 */
	public void startMonitoring(final Activity activity) {
		this.activity = activity;

		activity.registerReceiver(this, new IntentFilter(
				SyncAdapter.SYNC_FINISHED));
		activity.registerReceiver(this, new IntentFilter(
				SyncAdapter.SYNC_STARTED));

		String accountName = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(SyncPrefs.KEY_ACCOUNT, "");
		// Sync state might have changed, make sure we're spinning when
		// we should
		if (accountName != null
				&& !accountName.isEmpty()
				&& ContentResolver.isSyncActive(SyncPrefs.getAccount(
						AccountManager.get(activity), accountName),
						MyContentProvider.AUTHORITY)) {
			try {
				((OnSyncStartStopListener) activity).onSyncStartStop(true);
			}
			catch (Exception e) {
			}
		}
		else {
			try {
				((OnSyncStartStopListener) activity).onSyncStartStop(false);
			}
			catch (Exception e) {
			}
		}
	}

	/**
	 * Call this in the activity's onPause
	 */
	public void stopMonitoring() {
		try {
			activity.unregisterReceiver(this);
		}
		catch (Exception e) {
			
		}
		try {
			((OnSyncStartStopListener) activity).onSyncStartStop(false);
		}
		catch (Exception e) {
		}
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(SyncAdapter.SYNC_STARTED)) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						((OnSyncStartStopListener) activity)
								.onSyncStartStop(true);
					}
					catch (Exception e) {
					}
				}
			});
		}
		else { //if (intent.getAction().equals(SyncAdapter.SYNC_FINISHED)) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						((OnSyncStartStopListener) activity)
								.onSyncStartStop(false);
					}
					catch (Exception e) {
					}
				}
			});
            Bundle b = intent.getExtras();
            if (b == null) {
                b = new Bundle();
            }
			tellUser(context, b.getInt(SyncAdapter.SYNC_RESULT,
                                       SyncAdapter.SUCCESS));
		}
	}

	private void tellUser(Context context, int result) {
		int text = R.string.sync_failed;
		switch (result) {
		case SyncAdapter.ERROR:
			text = R.string.sync_failed;
			break;
		case SyncAdapter.LOGIN_FAIL:
			text = R.string.sync_login_failed;
			break;
		case SyncAdapter.SUCCESS:
		default:
			return;
		}

		Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		toast.show();
	}
}