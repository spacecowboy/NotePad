/*
 * Copyright 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskSync;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInputStream;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Backs up the user's preferences
 */
public class BackupAgent extends BackupAgentHelper {
	private static final String PREFS_BACKUP_KEY = "prefs";

	// Allocate a helper and add it to the backup agent
	@Override
	public void onCreate() {
		// Compute the default preferences filename.
		String defaultPrefsFilename = getPackageName() + "_preferences";
		addHelper(PREFS_BACKUP_KEY, new PrefBackupHelper(this,
				defaultPrefsFilename));
	}

	public static class PrefBackupHelper extends SharedPreferencesBackupHelper {

		final Context mContext;

		public PrefBackupHelper(Context context, String... prefGroups) {
			super(context, prefGroups);
			mContext = context;
		}

		/**
		 * Restore as usual, but when complete, delete information about sync
		 * times!
		 */
		@Override
		public void restoreEntity(BackupDataInputStream data) {
			super.restoreEntity(data);
			PreferenceManager.getDefaultSharedPreferences(mContext).edit()
					.remove(GoogleTaskSync.PREFS_GTASK_LAST_SYNC_TIME)
					.remove(SyncPrefs.KEY_LAST_SYNC).commit();
		}

	}
}