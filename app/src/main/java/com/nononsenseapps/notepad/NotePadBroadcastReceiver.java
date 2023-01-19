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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;

import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.widget.list.ListWidgetProvider;

/**
 * Used by {@link ListWidgetProvider} to receive the signal
 * that a note was completed in the widget
 */
public class NotePadBroadcastReceiver extends BroadcastReceiver {

	// TODO but at this point can't you just embed this in ListWidgetProvider.onReceive() ?

	// if you edit these, see also AndroidManifest.xml
	public static final String SET_NOTE_COMPLETE = "com.nononsenseapps.SetNoteComplete";
	public static final String SET_NOTE_INCOMPLETE = "com.nononsenseapps.SetNoteIncomplete";

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras == null || context == null) return;

		long id = extras.getLong(BaseColumns._ID, -1);
		if (id <= 0) return;

		String action = intent.getAction();
		switch (action) {
			case SET_NOTE_COMPLETE:
				Task.setCompleted(context, true, id);
				// Toast.makeText(context, R.string.completed, Toast.LENGTH_SHORT).show();
				// Broadcast that it has been completed, primarily for AndroidAgendaWidget
				Intent i = new Intent(context.getString(R.string.note_completed_broadcast_intent));
				context.sendBroadcast(i);
				break;
			case SET_NOTE_INCOMPLETE:
				Task.setCompleted(context, false, id);
				break;
			default:
				break;
		}
	}
}
