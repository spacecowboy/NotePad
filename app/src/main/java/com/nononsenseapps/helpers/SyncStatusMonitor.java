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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.sync.SyncAdapter;

public final class SyncStatusMonitor extends BroadcastReceiver {

	AppCompatActivity activity;
	OnSyncStartStopListener listener;

	/**
	 * Call this in the activity's onResume
	 */
	public void startMonitoring(AppCompatActivity activity, OnSyncStartStopListener listener) {
		// in the caller, the activity acts also as the listener, anyway
		this.activity = activity;
		this.listener = listener;

		ContextCompat.registerReceiver(activity, this, new IntentFilter(SyncAdapter.SYNC_FINISHED), ContextCompat.RECEIVER_NOT_EXPORTED);
		ContextCompat.registerReceiver(activity, this, new IntentFilter(SyncAdapter.SYNC_STARTED), ContextCompat.RECEIVER_NOT_EXPORTED);

		if (!PreferencesHelper.isSincEnabledAtAll(activity)) {
			// not starting: sync is disabled in the prefs
			return;
		}

		// get the selected account and verify if it is valid
		boolean isAccountValid = false;
		// Sync state might have changed, make sure we're spinning when we should
		try {
			listener.onSyncStartStop(isAccountValid);
		} catch (Exception e) {
			NnnLogger.debug(SyncStatusMonitor.class, e.getMessage());
		}
	}

	/**
	 * Call this in the activity's onPause
	 */
	public void stopMonitoring() {
		try {
			activity.unregisterReceiver(this);
		} catch (Exception e) {
			NnnLogger.exception(e);
		}
		try {
			listener.onSyncStartStop(false);
		} catch (Exception e) {
			NnnLogger.exception(e);
		}
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {

		if (!PreferencesHelper.isSincEnabledAtAll(context)) {
			NnnLogger.debug(SyncStatusMonitor.class,
					"ignore onReceive(): sync is disabled in the prefs");
			return;
		}

		if (intent.getAction().equals(SyncAdapter.SYNC_STARTED)) {
			activity.runOnUiThread(() -> {
				try {
					listener.onSyncStartStop(true);
				} catch (Exception e) {
					NnnLogger.exception(e);
				}
			});
		} else { //if (intent.getAction().equals(SyncAdapter.SYNC_FINISHED)) {
			activity.runOnUiThread(() -> {
				try {
					listener.onSyncStartStop(false);
				} catch (Exception e) {
					NnnLogger.exception(e);
				}
			});
			Bundle b = intent.getExtras();
			if (b == null) {
				b = Bundle.EMPTY;
			}
			tellUser(context, b.getInt(SyncAdapter.SYNC_RESULT, SyncAdapter.SUCCESS));
		}
	}

	private void tellUser(Context context, int result) {
		int text;
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

		NnnLogger.debug(SyncStatusMonitor.class, "SYNC: " + result);
		Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		toast.show();
	}

	public interface OnSyncStartStopListener {
		/**
		 * This is always called on the activity's UI thread.
		 */
		void onSyncStartStop(final boolean ongoing);
	}
}