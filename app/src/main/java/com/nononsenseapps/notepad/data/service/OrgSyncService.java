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

package com.nononsenseapps.notepad.data.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.nononsenseapps.notepad.data.remote.orgmodedropbox.DropboxSynchronizer;
import com.nononsenseapps.notepad.data.local.orgmode.Monitor;
import com.nononsenseapps.notepad.data.local.orgmode.SDSynchronizer;
import com.nononsenseapps.notepad.data.local.orgmode.SynchronizerInterface;
import com.nononsenseapps.notepad.util.Log;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.ui.settings.PrefsActivity;
import com.nononsenseapps.notepad.ui.settings.SyncPrefs;
import com.nononsenseapps.notepad.data.service.gtasks.SyncAdapter;
import com.nononsenseapps.notepad.util.SharedPreferencesHelper;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;

public class OrgSyncService extends Service {

	private static final String TAG = "OrgSyncService";

    public static final String ACTION_START = "com.nononsenseapps.notepad" +
                                              ".sync.START";
    public static final String ACTION_PAUSE = "com.nononsenseapps.notepad" +
                                              ".sync.PAUSE";

	// Msg arguments
	public static final int TWO_WAY_SYNC = 1;
	public static final int SYNC_QUEUE = 2;
	public static final int SYNC_RUN = 3;

	private static final int DELAY_MSECS = 30000;

	private boolean firstStart = true;

	private Looper serviceLooper;
	private SyncHandler serviceHandler;
	// private FileWatcher fileWatcher;
	private DBWatcher dbWatcher;

    private final ArrayList<Monitor> monitors;
    private final ArrayList<SynchronizerInterface> synchronizers;

    public static void start(Context context) {
		context.startService(new Intent(context, OrgSyncService.class)
                                     .setAction(ACTION_START));
	}

    public static void pause(Context context) {
        context.startService(new Intent(context, OrgSyncService.class)
                                     .setAction(ACTION_PAUSE));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, OrgSyncService.class));
    }

    public static boolean areAnyEnabled(Context context) {
        return SharedPreferencesHelper.isSdSyncEnabled(context) ||
               (BuildConfig.DROPBOX_ENABLED &&
               SharedPreferencesHelper.isDropboxSyncEnabled(context));
    }

	public OrgSyncService() {
        monitors = new ArrayList<Monitor>();
        synchronizers = new ArrayList<SynchronizerInterface>();
	}

    /**
     * Will only return Synchronizers which have been configured.
     * @return configured Synchronizers
     */
	public ArrayList<SynchronizerInterface> getSynchronizers() {
		ArrayList<SynchronizerInterface> syncers = new
                ArrayList<SynchronizerInterface>();

        // Try SD
        SynchronizerInterface sd = new SDSynchronizer(this);
        if (sd.isConfigured()) {
            syncers.add(sd);
        }

        // Try Dropbox
        if (BuildConfig.DROPBOX_ENABLED) {
            SynchronizerInterface db = new DropboxSynchronizer(this);
            if (db.isConfigured()) {
                syncers.add(db);
            }
        }

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
		serviceLooper = thread.getLooper();
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
        for (Monitor monitor: monitors) {
            monitor.pauseMonitor();
        }
    }

    private void notifyError() {
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				this)
                // TODO hardcoded
				.setContentTitle("Could not access files")
				.setContentText("Please change directory")
				.setContentIntent(
						PendingIntent.getActivity(this, 0, new Intent(this,
								PrefsActivity.class),
								PendingIntent.FLAG_UPDATE_CURRENT));

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(237388, notBuilder.build());
	}

	@Override
	public void onDestroy() {
		// Unregister observers
        for (Monitor monitor: monitors) {
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
            Log.d(TAG, "OnMonitorChange");
            // Increment the changeId
            changeId++;

            // First queue the operation
            final Message q = obtainMessage();
            q.arg1 = SYNC_QUEUE;
            q.arg2 = changeId;
            sendMessage(q);
            // Next, schedule a run in a short delay.
            // Only the run number matching a queue number will run (last one)
            final Message r =obtainMessage();
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
                for (final SynchronizerInterface syncer: synchronizers) {
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
					Log.d(TAG, "Sync-Queue: " + msg.arg2);
					lastChangeId = msg.arg2;
					break;
				case SYNC_RUN:
                    Log.d(TAG, "Sync-Run: " + msg.arg2);
					if (msg.arg2 != lastChangeId) {
						// Wait...
                        return;
					}
                    // Falling through
				case TWO_WAY_SYNC:
                    Log.d(TAG, "Sync-Two-Way: " + msg.arg2);
                    // Pause monitors
                    for (final Monitor monitor: monitors) {
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
                    for (final Monitor monitor: monitors) {
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

			} catch (IOException ignored) {
                Log.e(TAG, ignored.getMessage());
			} catch (ParseException ignored) {
			}
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
