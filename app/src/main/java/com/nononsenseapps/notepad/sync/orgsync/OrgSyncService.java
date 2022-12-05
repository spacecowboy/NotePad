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

package com.nononsenseapps.notepad.sync.orgsync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.preference.PreferenceManager;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.SyncAdapter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;

public class OrgSyncService extends Service {

	public static final String ACTION_START = BuildConfig.APPLICATION_ID + ".sync.START";
	public static final String ACTION_PAUSE = BuildConfig.APPLICATION_ID + ".sync.PAUSE";

	// Msg arguments
	public static final int TWO_WAY_SYNC = 1;
	public static final int SYNC_QUEUE = 2;
	public static final int SYNC_RUN = 3;

	private static final int DELAY_MSECS = 30000;

	private SyncHandler serviceHandler;

	private final ArrayList<Monitor> monitors;
	private final ArrayList<SynchronizerInterface> synchronizers;

	public static void start(Context context) {
		NnnLogger.debug(OrgSyncService.class, "got here #1");
		context.startService(new Intent(context, OrgSyncService.class) // TODO startservice. does this work correctly in newer android versions ?
				.setAction(ACTION_START));
		NnnLogger.debug(OrgSyncService.class, "got here #2");
	}

	// TODO this service crashes in API 23 - default image on github

	public static void pause(Context context) {
		NnnLogger.debug(OrgSyncService.class, "got here #3");
		context.startService(new Intent(context, OrgSyncService.class) // TODO startservice. does this work correctly in newer android versions ?
				.setAction(ACTION_PAUSE));
		NnnLogger.debug(OrgSyncService.class, "got here #4");
	}

	public static void stop(Context context) {
		NnnLogger.debug(OrgSyncService.class, "got here #5");
		context.stopService(new Intent(context, OrgSyncService.class));
		NnnLogger.debug(OrgSyncService.class, "got here #6");
	}

	public static boolean areAnyEnabled(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(SyncPrefs.KEY_SD_ENABLE, false);
	}

	public OrgSyncService() {
		monitors = new ArrayList<>();
		synchronizers = new ArrayList<>();
	}

	/**
	 * Will only return Synchronizers which have been configured.
	 *
	 * @return configured Synchronizers
	 */
	public ArrayList<SynchronizerInterface> getSynchronizers() {
		ArrayList<SynchronizerInterface> syncers = new ArrayList<>();

		// Try SD
		SynchronizerInterface sd = new SDSynchronizer(this);
		if (sd.isConfigured()) {
			syncers.add(sd);
		}

		// if we add another synchronization service, add code here

		return syncers;
	}

	@Override
	public void onCreate() {
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		Looper serviceLooper = thread.getLooper();
		serviceHandler = new SyncHandler(serviceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getAction() != null &&
				ACTION_PAUSE.equals(intent.getAction())) {
			pause();
		} else {
			final Message msg = serviceHandler.obtainMessage();
			msg.arg1 = TWO_WAY_SYNC;
			serviceHandler.sendMessage(msg);
		}

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	private void pause() {
		// Pause monitors
		for (Monitor monitor : monitors) {
			monitor.pauseMonitor();
		}
	}

	@Override
	public void onDestroy() {
		// Unregister observers
		for (Monitor monitor : monitors) {
			monitor.terminate();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// Handler that receives messages from the thread
	public final class SyncHandler extends Handler {

		private int changeId = 0;
		private int lastChangeId;

		public SyncHandler(Looper looper) {
			super(looper);
		}

		public void onMonitorChange() {
			NnnLogger.debug(OrgSyncService.class, "OnMonitorChange");
			// Increment the changeId
			changeId++;

			// First queue the operation
			final Message q = obtainMessage();
			q.arg1 = SYNC_QUEUE;
			q.arg2 = changeId;
			sendMessage(q);
			// Next, schedule a run in a short delay.
			// Only the run number matching a queue number will run (last one)
			final Message r = obtainMessage();
			r.arg1 = SYNC_RUN;
			r.arg2 = changeId;
			sendMessageDelayed(r, DELAY_MSECS);
		}

		@Override
		public void handleMessage(Message msg) {

			if (synchronizers.isEmpty()) {
				synchronizers.addAll(getSynchronizers());
			}

			// Get monitors if empty
			if (monitors.isEmpty()) {
				// First db watcher
				monitors.add(new DBWatcher(this));
				// Then remote sources
				for (final SynchronizerInterface syncer : synchronizers) {
					final Monitor monitor = syncer.getMonitor();
					if (monitor != null) {
						monitors.add(monitor);
					}
				}
			}

			try {

				/*
				 * Queues are used to delay operations until subsequent updates
				 * are complete.
				 */
				switch (msg.arg1) {
					case SYNC_QUEUE:
						NnnLogger.debug(OrgSyncService.class, "Sync-Queue: " + msg.arg2);
						lastChangeId = msg.arg2;
						break;
					case SYNC_RUN:
						NnnLogger.debug(OrgSyncService.class, "Sync-Run: " + msg.arg2);
						if (msg.arg2 != lastChangeId) {
							// Wait...
							return;
						}
						// Falling through
					case TWO_WAY_SYNC:
						NnnLogger.debug(OrgSyncService.class, "Sync-Two-Way: " + msg.arg2);
						// Pause monitors
						for (final Monitor monitor : monitors) {
							monitor.pauseMonitor();
						}
						// Sync each
						for (final SynchronizerInterface syncer : synchronizers) {
							sendBroadcast(new Intent(SyncAdapter.SYNC_STARTED));
							syncer.fullSync();
							syncer.postSynchronize();
						}
						sendBroadcast(new Intent(SyncAdapter.SYNC_FINISHED));
						// Restart monitors
						for (final Monitor monitor : monitors) {
							monitor.startMonitor(this);
						}
						// Save last sync time
						PreferenceManager
								.getDefaultSharedPreferences(OrgSyncService.this)
								.edit().putLong(SyncPrefs.KEY_LAST_SYNC,
										Calendar.getInstance()
												.getTimeInMillis())
								.commit();
						break;
				}

			} catch (IOException e) {
				NnnLogger.exception(e);
			} catch (ParseException ignored) {}
		}
	}

	private final class DBWatcher extends ContentObserver implements Monitor {

		private final SyncHandler handler;

		// Giving it the service handler, onChange will run on that thread
		public DBWatcher(SyncHandler handler) {
			super(handler);
			this.handler = handler;
		}

		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			handler.onMonitorChange();
		}

		@Override
		public void startMonitor(final SyncHandler handler) {
			// Monitor both lists and tasks
			getContentResolver().registerContentObserver(TaskList.URI, true,
					this);
			getContentResolver().registerContentObserver(Task.URI, true, this);
		}

		@Override
		public void pauseMonitor() {
			getContentResolver().unregisterContentObserver(this);
		}

		@Override
		public void terminate() {
			pauseMonitor();
		}
	}
}
