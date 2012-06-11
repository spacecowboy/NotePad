package com.nononsenseapps.notepad;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class NotePadBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Broadcast", "Broadcast on Receive");
		Bundle extras = intent.getExtras();
		if (extras != null) {
			completeNote(context, extras.getLong(NotePad.Notes._ID, -1));
		}

	}

	private void completeNote(Context context, long id) {
		Log.d("Broadcast", "Id is: " + id);
		if (id > -1 && context != null) {
			ContentValues values = new ContentValues();
			String status = context.getText(R.string.gtask_status_completed)
					.toString();
			values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS, status);

			Log.d("Broadcast", "Sending update...");
			context.getContentResolver().update(
					NotesEditorFragment.getUriFrom(id), values, null, null);
		}
	}

}
