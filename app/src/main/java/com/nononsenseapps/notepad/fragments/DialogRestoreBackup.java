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

package com.nononsenseapps.notepad.fragments;


import android.net.Uri;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;

import com.nononsenseapps.helpers.DocumentFileHelper;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.BackupPrefs;

public class DialogRestoreBackup extends DialogConfirmBaseV11 {
	static final String ID = "id";
	static final String TAG = "deletelistok";

	public static void showDialog(final FragmentManager fm,
								  final DialogConfirmedListener listener) {
		DialogRestoreBackup d = new DialogRestoreBackup();
		d.setListener(listener);
		d.setArguments(new Bundle());
		d.show(fm, TAG);
	}

	@Override
	public int getTitle() {
		return R.string.backup_import;
	}

	@Override
	public CharSequence getMessage() {
		Uri dir = BackupPrefs.getSelectedBackupDirUri(this.getContext());
		if (dir == null) return getString(R.string.unavailable_chose_directory);

		DocumentFile file = DocumentFileHelper.getSelectedBackupJsonFile(this.getContext());
		if (file == null) return getString(R.string.backup_file_not_found);

		// file found. Ask users if they want to import from it
		return getString(R.string.backup_import_msg,
				"\n" + file.getUri().getLastPathSegment());
	}

	@Override
	public void onOKClick() {
		if (listener != null) {
			listener.onConfirm();
		}
		getDialog().dismiss();
	}

}
