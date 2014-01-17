package com.nononsenseapps.notepad.fragments;

import android.app.FragmentManager;
import android.os.Bundle;
import android.widget.Toast;

import com.nononsenseapps.notepad.core.R;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.sync.files.JSONBackup;

public class DialogExportBackup extends DialogConfirmBaseV11 {
	static final String ID = "id";
	static final String TAG = "deletelistok";

	public static void showDialog(final FragmentManager fm, final DialogConfirmedListener listener) {
		DialogExportBackup d = new DialogExportBackup();
		d.setListener(listener);
		d.setArguments(new Bundle());
		d.show(fm, TAG);
	}

	@Override
	public int getTitle() {
		return R.string.backup_export;
	}

	@Override
	public CharSequence getMessage() {
		return getString(R.string.backup_export_msg, JSONBackup.DEFAULT_BACKUP_FILEPATH);
	}

	@Override
	public void onOKClick() {
		if (listener != null) {
			listener.onConfirm();
		}
		getDialog().dismiss();
	}

}
