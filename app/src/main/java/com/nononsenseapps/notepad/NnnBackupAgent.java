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

package com.nononsenseapps.notepad;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInputStream;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.prefs.SyncPrefs;


/**
 * Backs up the user's preferences
 */
public class NnnBackupAgent extends BackupAgentHelper {

	private static final String PREFS_BACKUP_KEY = "prefs";

	@Override
	public void onCreate() {
		// Compute the default preferences filename.
		String defaultPrefsFilename = getPackageName() + "_preferences";
		// Allocate a helper and add it to the backup agent
		addHelper(PREFS_BACKUP_KEY, new PrefBackupHelper(this, defaultPrefsFilename));
	}

	public static class PrefBackupHelper extends SharedPreferencesBackupHelper {

		final Context mContext;

		public PrefBackupHelper(Context context, String... prefGroups) {
			super(context, prefGroups);
			mContext = context;
		}

		/**
		 * Restore as usual, but when complete, delete information about sync times!
		 */
		@Override
		public void restoreEntity(BackupDataInputStream data) {
			super.restoreEntity(data);
			// TODO reflect on what to add/delete on the preferences, then write it here
			/* PreferenceManager
					.getDefaultSharedPreferences(mContext)
					.edit()
					.remove(GoogleTaskSync.PREFS_GTASK_LAST_SYNC_TIME)
					.remove(SyncPrefs.KEY_LAST_SYNC)
					.commit(); */
		}

	}
}