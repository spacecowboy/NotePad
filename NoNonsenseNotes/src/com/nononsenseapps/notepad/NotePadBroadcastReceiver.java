package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad.database.Task;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.Toast;

public class NotePadBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			if (extras.getLong(BaseColumns._ID, -1) > 0 && context != null) {
				Task.setCompleted(context, true, extras.getLong(BaseColumns._ID, -1));
				//UpdateNotifier.notifyChangeNote(context, NotesEditorFragment.getUriFrom(id));
				
				Toast.makeText(context, context.getString(R.string.completed),
						Toast.LENGTH_SHORT).show();
			}
		}

	}
}
