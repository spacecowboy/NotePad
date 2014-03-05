package com.nononsenseapps.notepad.sync.orgmode;

import com.nononsenseapps.helpers.Log;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class OrgSyncServiceOld extends IntentService {
	private static final String ACTION_READ = "com.nononsenseapps.notepad.core.action.READ";
	private static final String ACTION_WRITE = "com.nononsenseapps.notepad.core.action.WRITE";

	/**
	 * Starts this service to perform action Read with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 * 
	 * @see IntentService
	 */
	public static void startRead(Context context) {
		Intent intent = new Intent(context, OrgSyncServiceOld.class);
		intent.setAction(ACTION_READ);
		context.startService(intent);
	}

	/**
	 * Starts this service to perform action Write with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 * 
	 * @see IntentService
	 */
	public static void startWrite(Context context) {
		Intent intent = new Intent(context, OrgSyncServiceOld.class);
		intent.setAction(ACTION_WRITE);
		context.startService(intent);
	}

	public OrgSyncServiceOld() {
		super("OrgSyncService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_READ.equals(action)) {
				handleRead();
			} else if (ACTION_WRITE.equals(action)) {
				handleWrite();
			}
		}
	}

	/**
	 * Handle action Foo in the provided background thread with the provided
	 * parameters.
	 */
	private void handleRead() {
		// TODO: Handle action Foo
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Handle action Baz in the provided background thread with the provided
	 * parameters.
	 */
	private void handleWrite() {
		try {
			Log.d(OrgSyncer.TAG, "Starting write process");
			final OrgSyncer orgSyncer = new OrgSyncer(this);
			Log.d(OrgSyncer.TAG, "Have syncer, starting write");
			orgSyncer.writeChanges(this);
			Log.d(OrgSyncer.TAG, "Finished write");
		} catch (Exception e) {
			Log.d(OrgSyncer.TAG, e.getLocalizedMessage());
			for (StackTraceElement s : e.getStackTrace()) {
				Log.d(OrgSyncer.TAG, s.toString());
			}
		}
	}
}
