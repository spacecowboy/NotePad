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
package com.nononsenseapps.notepad.prefs;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.fragments.DialogExportBackup;
import com.nononsenseapps.notepad.fragments.DialogRestoreBackup;
import com.nononsenseapps.notepad.sync.files.JSONBackup;

import java.io.FileNotFoundException;

public class BackupPrefs extends PreferenceFragment {

	public static final String KEY_IMPORT = "backup_import";
	public static final String KEY_EXPORT = "backup_export";

	private JSONBackup backupMaker;
	private RestoreBackupTask bgTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_backup);

		backupMaker = new JSONBackup(getActivity());

		findPreference(KEY_IMPORT).setOnPreferenceClickListener(
				preference -> {
					DialogRestoreBackup.showDialog(getFragmentManager(),
							() -> {
								bgTask = new RestoreBackupTask(super.getContext(), backupMaker);
								bgTask.execute();
							});
					return true;
				});

		findPreference(KEY_EXPORT).setOnPreferenceClickListener(
				preference -> {
					DialogExportBackup.showDialog(getFragmentManager(), () -> {
						try {
							backupMaker.writeBackup();
							Toast.makeText(getActivity(), R.string.backup_export_success, Toast.LENGTH_SHORT)
									.show();
						} catch (Exception e) {
							NnnLogger.exception(e);
							Toast.makeText(getActivity(), R.string.backup_export_failed, Toast.LENGTH_SHORT)
									.show();
						}
					});

					return true;
				});
	}

	/**
	 * Run the backup in the background. Locking the UI-thread for up to a few
	 * seconds is not nice...
	 */
	private static class RestoreBackupTask extends AsyncTask<Void, Void, Integer> {
		// TODO move this class into its own java file
		private final JSONBackup backupMaker;
		private final Context mContext;

		/**
		 * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
		 */
		public RestoreBackupTask(@NonNull Context context, @NonNull JSONBackup backupMaker) {
			super();
			this.backupMaker = backupMaker;
			mContext = context;
		}

		protected Integer doInBackground(Void... params) {
			try {
				backupMaker.restoreBackup();
				return 0;
			} catch (FileNotFoundException e) {
				return 1;
			} catch (Exception e) {
				return 2;
			}
		}

		protected void onPostExecute(final Integer result) {
			if (mContext == null) {
				return;
			}
			switch (result) {
				case 0:
					Toast.makeText(mContext, R.string.backup_import_success,
							Toast.LENGTH_SHORT).show();
					break;
				case 1:
					Toast.makeText(mContext, R.string.backup_file_not_found,
							Toast.LENGTH_SHORT).show();
					break;
				case 2:
					Toast.makeText(mContext, R.string.backup_import_failed,
							Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}
}