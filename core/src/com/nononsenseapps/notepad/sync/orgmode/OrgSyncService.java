package com.nononsenseapps.notepad.sync.orgmode;

import java.util.Random;

import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

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

	// Use a random number to keep track of changes
	private final Random rand;

	private Looper serviceLooper;
	private ServiceHandler serviceHandler;
	private OrgSyncer orgSyncer;
	private FileWatcher fileWatcher;
	private DBWatcher dbWatcher;

	public static void start(Context context) {
		context.startService(new Intent(context, OrgSyncService.class));
	}

	public OrgSyncService() {
		rand = new Random();
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
		// Create a new orgSyncer to refresh the saved dir
		orgSyncer = new OrgSyncer(this);

		if (firstStart) {
			firstStart = false;
			Toast.makeText(this, "First start, call 2 way sync",
					Toast.LENGTH_SHORT).show();
			Message msg = serviceHandler.obtainMessage();
			msg.arg1 = TWO_WAY_SYNC;
			serviceHandler.sendMessage(msg);
		}

		// Setup the file monitor
		// TODO
		if (fileWatcher != null) {
			fileWatcher.stopWatching();
		}
		fileWatcher = new FileWatcher(orgSyncer.getOrgDir(), serviceHandler);
		fileWatcher.startWatching();

		// Setup the database monitor
		if (dbWatcher == null) {
			registerDBWatcher();
		}

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	private void registerDBWatcher() {
		// TODO
		if (dbWatcher == null) {
			dbWatcher = new DBWatcher(serviceHandler);
		}
		// Monitor both lists and tasks
		getContentResolver().registerContentObserver(TaskList.URI, true,
				dbWatcher);
		getContentResolver().registerContentObserver(Task.URI, true, dbWatcher);
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
		// Unregister observers
		getContentResolver().unregisterContentObserver(dbWatcher);
		fileWatcher.stopWatching();
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

			// TODO
			switch (msg.arg1) {
			case DB_TO_FS_QUEUE:
				Log.d(TAG, "DB2FS-Queue: " + msg.arg2);
				lastDBChangeId = msg.arg2;
				break;
			case DB_TO_FS_RUN:
				if (msg.arg2 == lastDBChangeId) {
					Log.d(TAG, "DB2FS-Run: " + msg.arg2);
					Toast.makeText(OrgSyncService.this,
							"DB2FS-Run: " + msg.arg2, Toast.LENGTH_SHORT)
							.show();
				}
				break;
			case FS_TO_DB_QUEUE:
				Log.d(TAG, "FS2DB-Queue: " + msg.arg2);
				lastFSChangeId = msg.arg2;
				break;
			case FS_TO_DB_RUN:
				if (msg.arg2 == lastFSChangeId) {
					Log.d(TAG, "FS2DB-Run: " + msg.arg2);
					Toast.makeText(OrgSyncService.this,
							"FS2DB-Run: " + msg.arg2, Toast.LENGTH_SHORT)
							.show();
				}
				break;
			case TWO_WAY_SYNC:
				Toast.makeText(OrgSyncService.this, "Do 2WAY",
						Toast.LENGTH_SHORT).show();
				break;
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
				Toast.makeText(OrgSyncService.this,
						"OnEvent Exc: " + e.getLocalizedMessage(),
						Toast.LENGTH_SHORT).show();
			}
		}

	}
}
