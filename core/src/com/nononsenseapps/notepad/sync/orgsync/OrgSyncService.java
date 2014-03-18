package com.nononsenseapps.notepad.sync.orgsync;

import java.io.IOException;
import java.text.ParseException;

import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.prefs.PrefsActivity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class OrgSyncService extends Service {

	private static final String TAG = "OrgSyncService";

	// Msg arguments
	public static final int TWO_WAY_SYNC = 1;
	public static final int DB_TO_FS_QUEUE = 2;
	public static final int DB_TO_FS_RUN = 3;
	public static final int FS_TO_DB_QUEUE = 4;
	public static final int FS_TO_DB_RUN = 5;

	private static final int DELAY_MSECS = 5000;

	private boolean firstStart = true;

	private Looper serviceLooper;
	private ServiceHandler serviceHandler;
	// private FileWatcher fileWatcher;
	private DBWatcher dbWatcher;

	public static void start(Context context) {
		context.startService(new Intent(context, OrgSyncService.class));
	}

	public OrgSyncService() {
	}

	public SynchronizerInterface getSynchronizer() {
		// TODO do something else
		return new SDSynchronizer(this);
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
		serviceHandler = new ServiceHandler(serviceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
		SynchronizerInterface syncer = getSynchronizer();

		// TODO testing
		syncer.isConfigured();
		// if (!syncer.isConfigured()) {
		// notifyError();
		// stopSelf();
		// }

		// Setup the file monitor. Restart it to catch new directories.
		// stopFileWatcher();
		// fileWatcher = null;
		// startFileWatcher();

		// Setup the database monitor, if not already present.
		if (dbWatcher == null) {
			// TODO testing...
			// startDBWatcher();
		}

		// On first start do a 2-way sync.
		if (firstStart) {
			firstStart = false;
			Message msg = serviceHandler.obtainMessage();
			msg.arg1 = TWO_WAY_SYNC;
			serviceHandler.sendMessage(msg);
		} else {
			// TODO testing
			Message msg = serviceHandler.obtainMessage();
			msg.arg1 = TWO_WAY_SYNC;
			serviceHandler.sendMessage(msg);
		}

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	private void notifyError() {
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				this)
				.setContentTitle("Could not access files")
				.setContentText("Please change the export directory")
				.setContentIntent(
						PendingIntent.getActivity(this, 0, new Intent(this,
								PrefsActivity.class),
								PendingIntent.FLAG_UPDATE_CURRENT));

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(237388, notBuilder.build());
	}

	private void stopDBWatcher() {
		if (dbWatcher != null) {
			getContentResolver().unregisterContentObserver(dbWatcher);
		}
	}

	private void startDBWatcher() {
		if (dbWatcher == null) {
			//dbWatcher = new DBWatcher(serviceHandler);
		}
		// Monitor both lists and tasks
		//getContentResolver().registerContentObserver(TaskList.URI, true,
		//		dbWatcher);
		//getContentResolver().registerContentObserver(Task.URI, true, dbWatcher);
	}

	// private void stopFileWatcher() {
	// if (fileWatcher != null) {
	// fileWatcher.stopWatching();
	// }
	// }
	//
	// private void startFileWatcher() {
	// if (fileWatcher == null) {
	// fileWatcher = new FileWatcher(orgSyncer.getOrgDir(), serviceHandler);
	// }
	// fileWatcher.startWatching();
	// }

	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
		// Unregister observers
		// stopFileWatcher();
		stopDBWatcher();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {

		private int lastDBChangeId;
		private int lastFSChangeId;

		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {

			try {

				/*
				 * Queues are used to delay operations until subsequent updates
				 * are complete.
				 */
				switch (msg.arg1) {
				case DB_TO_FS_QUEUE:
					Log.d(TAG, "DB2FS-Queue: " + msg.arg2);
					lastDBChangeId = msg.arg2;
					break;
				case DB_TO_FS_RUN:
					if (msg.arg2 == lastDBChangeId) {
						// stopFileWatcher();
						Log.d(TAG, "DB2FS-Run: " + msg.arg2);
						Toast.makeText(OrgSyncService.this,
								"DB2FS-Run: " + msg.arg2, Toast.LENGTH_SHORT)
								.show();

						// TODO actual work
						getSynchronizer().fullSync();

						// startFileWatcher();
					}
					break;
				case FS_TO_DB_QUEUE:
					Log.d(TAG, "FS2DB-Queue: " + msg.arg2);
					lastFSChangeId = msg.arg2;
					break;
				case FS_TO_DB_RUN:
					if (msg.arg2 == lastFSChangeId) {
						stopDBWatcher();
						Log.d(TAG, "FS2DB-Run: " + msg.arg2);
						Toast.makeText(OrgSyncService.this,
								"FS2DB-Run: " + msg.arg2, Toast.LENGTH_SHORT)
								.show();

						// TODO actual work
						getSynchronizer().fullSync();

						startDBWatcher();
					}
					break;
				case TWO_WAY_SYNC:
					stopDBWatcher();
					// stopFileWatcher();
					Toast.makeText(OrgSyncService.this, "Do 2WAY",
							Toast.LENGTH_SHORT).show();
					// TODO actual work
					getSynchronizer().fullSync();
					startDBWatcher();
					// startFileWatcher();
					break;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private final class DBWatcher extends ContentObserver {

		private final Handler handler;
		private int changeId = 0;

		// Giving it the service handler, onChange will run on that thread
		public DBWatcher(Handler handler) {
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
			// Increment the changeId
			changeId++;

			// First queue the operation
			final Message q = handler.obtainMessage();
			q.arg1 = DB_TO_FS_QUEUE;
			q.arg2 = changeId;
			handler.sendMessage(q);
			// Next, schedule a run in a short delay.
			// Only the run number matching a queue number will run (last one)
			final Message r = handler.obtainMessage();
			r.arg1 = DB_TO_FS_RUN;
			r.arg2 = changeId;
			handler.sendMessageDelayed(r, DELAY_MSECS);
		}
	}

	private final class FileWatcher extends FileObserver {

		private final Handler handler;
		private int changeId = 0;

		public FileWatcher(String path, Handler handler) {
			super(path, FileObserver.CREATE | FileObserver.DELETE
					| FileObserver.DELETE_SELF | FileObserver.MODIFY
					| FileObserver.MOVE_SELF | FileObserver.MOVED_FROM
					| FileObserver.MOVED_TO);
			this.handler = handler;
		}

		@Override
		public void onEvent(int event, String path) {
			try {
				// Increment change id
				changeId++;

				// First queue the operation
				final Message q = handler.obtainMessage();
				q.arg1 = FS_TO_DB_QUEUE;
				q.arg2 = changeId;
				handler.sendMessage(q);
				// Next, schedule a run in a short delay.
				// Only the run number matching a queue number will run (last
				// one)
				final Message r = handler.obtainMessage();
				r.arg1 = FS_TO_DB_RUN;
				r.arg2 = changeId;
				handler.sendMessageDelayed(r, DELAY_MSECS);
			} catch (Exception e) {
				Log.e(TAG, e.getLocalizedMessage());
			}
		}

	}
}
