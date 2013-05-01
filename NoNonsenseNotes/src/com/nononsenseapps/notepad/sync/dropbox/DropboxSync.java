package com.nononsenseapps.notepad.sync.dropbox;

import java.util.ArrayList;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFileSystem;
import com.googlecode.androidannotations.annotations.EService;
import com.nononsenseapps.build.Config;

import android.app.IntentService;
import android.content.Intent;

/**
 * Synchronizes the database with the linked dropbox account. For task specific
 * URIs, will synchronize only those files.
 * 
 */
@EService
public class DropboxSync extends IntentService {

	public DropboxSync() {
		super("DropboxSyncService");
	}

	/**
	 * Call with intent ACTION_SYNC to update the dropbox cache.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals(Intent.ACTION_SYNC)) {
			syncCache();
		}
	}

	void syncDB() {
		final ArrayList local = new ArrayList();
		final ArrayList remote = new ArrayList();
		
		// get local stuff
		
		// If local version does not have remote id it should be created
		
		// get remote stuff
		
		// a local task with a remote id, which does not exist remotely, should be deleted locally
		
		// a remote task which does not exist locally, should be created
		
		// else, check updated timestamp. higher wins
	}
	
	/**
	 * Simply calls SyncAndWait. Makes sure the dropbox cache is up to date.
	 * Suitable for periodic checks
	 */
	void syncCache() {
		try {
			if (Config.DROPBOX_APP_SECRET.contains(" ")) {
				// NO valid key
				return;
			}
			final DbxAccount account = DbxAccountManager.getInstance(
					getApplicationContext(), Config.DROPBOX_APP_KEY,
					Config.DROPBOX_APP_SECRET).getLinkedAccount();
			if (account == null) {
				// Not linked
				return;
			}
			DbxFileSystem.forAccount(account).syncNowAndWait();
		}
		catch (Unauthorized e) {
		}
		catch (DbxException e) {
		}
	}

}
