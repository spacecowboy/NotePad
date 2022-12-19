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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.helpers.FilePickerHelper;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.fragments.DialogExportBackup;
import com.nononsenseapps.notepad.fragments.DialogRestoreBackup;
import com.nononsenseapps.notepad.sync.files.JSONBackup;

import java.io.FileNotFoundException;
import java.util.concurrent.Executors;

public class BackupPrefs extends PreferenceFragmentCompat {

	// settings IDs from app_pref_backup.xml
	private static final String KEY_IMPORT = "backup_import";
	private static final String KEY_EXPORT = "backup_export";
	private static final String KEY_BACKUP_DIR_URI = "key_backup_dir_uri";

	private JSONBackup mTool;

	/**
	 * the folder that contains the backup json file
	 */
	Preference dirUriPref;

	@Override
	public void onCreatePreferences(@Nullable Bundle savInstState, String rootKey) {
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_pref_backup);

		mTool = new JSONBackup(getActivity());

		findPreference(KEY_IMPORT).setOnPreferenceClickListener(pref -> {
			DialogRestoreBackup.showDialog(getFragmentManager(),
					/*callback when confirmed:*/ () -> runBackupOrRestore(true));
			return true;
		});

		findPreference(KEY_EXPORT).setOnPreferenceClickListener(pref -> {
			DialogExportBackup.showDialog(getFragmentManager(),
					() -> runBackupOrRestore(false));
			return true;
		});

		dirUriPref = findPreference(KEY_BACKUP_DIR_URI);
		dirUriPref.setOnPreferenceClickListener(pref -> {
			// open the file picker on click
			Uri initialDir = getSelectedBackupDirUri(this.getContext());
			FilePickerHelper.showFolderPickerActivity(this, initialDir);
			// tell android to update the preference value
			return true;
		});
		// initialize
		onUriDirPrefChange(dirUriPref);
	}

	/**
	 * Updates the description of "directoryUriPreference"
	 * with the newly selected backup directory Uri
	 */
	private static void onUriDirPrefChange(Preference directoryUriPreference) {
		Uri uri = getSelectedBackupDirUri(directoryUriPreference.getContext());
		String summary = uri != null
				? uri.getPath() // shows a pretty representation of the URI's destination
				: directoryUriPreference.getContext().getString(R.string.not_selected_yet);
		directoryUriPreference.setSummary(summary);
	}

	/**
	 * @return the Uri of the folder that the user chose for saving Json backups,
	 * or NULL if none is chosen
	 */
	@Nullable
	public static Uri getSelectedBackupDirUri(Context context) {
		var sharPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		String uriVal = sharPrefs.getString(KEY_BACKUP_DIR_URI, null);
		if (uriVal == null) return null;
		return Uri.parse(uriVal);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		// it was cancelled by the user. Let's ignore it
		if (resultCode != Activity.RESULT_OK) return;

		if (requestCode == FilePickerHelper.REQ_CODE) {
			// "data" contains the URI for the user-selected directory, A.K.A. the "document tree"
			FilePickerHelper.onUriPicked(data, this.getContext(), KEY_BACKUP_DIR_URI);
			onUriDirPrefChange(dirUriPref);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Run the backup (or restore) in the background. Locking the UI-thread for up to a few
	 * seconds is not nice...
	 */
	private void runBackupOrRestore(boolean isRestoring) {
		// get them in this thread
		Handler handler = new Handler(Looper.getMainLooper());
		Context context = this.getContext();

		if (getSelectedBackupDirUri(this.getContext()) == null) {
			// the user tried to make a backup without having selected
			// a folder first. The dialogs warn of this. Here, we just
			// have to cancel the operation
			return;
		}

		// replacement for AsyncTask<,,>
		Executors.newSingleThreadExecutor().execute(() -> {
			// Background work here
			int result = asyncTask_doInBackground(isRestoring, mTool);

			handler.post(() -> {
				// UI Thread work here
				asyncTask_onPostExecute(context, isRestoring, result);
			});
		});
	}

	/**
	 * the backup/restore work for the background thread
	 *
	 * @param isRestoring TRUE if this task should RESTORE a backup from a file,
	 *                    FALSE if it should CREATE a backup file
	 * @return a result code used by {@link #asyncTask_onPostExecute(Context, boolean, int)}
	 */
	private static int asyncTask_doInBackground(boolean isRestoring, JSONBackup backupMaker) {
		try {
			if (isRestoring) backupMaker.restoreBackup();
			else backupMaker.writeBackup();
			return 0;
		} catch (FileNotFoundException e) {
			return 1;
		} catch (SecurityException e) {
			// can't read from that folder: missing permission ?
			return 2;
		} catch (Exception e) {
			NnnLogger.exception(e);
			return 3;
		}
	}

	/**
	 * after the backup/restore is finished, show a toast on the UI thread
	 *
	 * @param isRestoring FALSE if it is "save backup" operation
	 * @param result      from {@link #asyncTask_doInBackground(boolean, JSONBackup)}
	 */
	private static void asyncTask_onPostExecute(@NonNull Context mContext,
												boolean isRestoring, int result) {
		int msgId;
		switch (result) {
			case 0:
				msgId = isRestoring
						? R.string.backup_import_success
						: R.string.backup_export_success;
				break;
			case 1:
				msgId = R.string.backup_file_not_found;
				break;
			case 2:
				// can't read from / write to that folder: missing permission ?
				msgId = R.string.permission_denied;
				break;
			case 3:
				msgId = isRestoring
						? R.string.backup_import_failed
						: R.string.backup_export_failed;
				break;
			default:
				// won't happen, anyway
				return;
		}
		Toast.makeText(mContext, msgId, Toast.LENGTH_SHORT).show();
	}


}