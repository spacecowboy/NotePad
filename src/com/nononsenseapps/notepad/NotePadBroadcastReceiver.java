package com.nononsenseapps.notepad;

import com.nononsenseapps.helpers.UpdateNotifier;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class NotePadBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			completeNote(context, extras.getLong(NotePad.Notes._ID, -1));
		}

	}

	private void completeNote(Context context, long id) {
		if (id > -1 && context != null) {
			ContentValues values = new ContentValues();
			String status = context.getText(R.string.gtask_status_completed)
					.toString();
			values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS, status);

			context.getContentResolver().update(
					NotesEditorFragment.getUriFrom(id), values, null, null);
			UpdateNotifier.notifyChangeNote(context, NotesEditorFragment.getUriFrom(id));
		}
	}

}
