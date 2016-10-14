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

package com.nononsenseapps.notepad.data.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Task;

public class NotePadBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			if (extras.getLong(BaseColumns._ID, -1) > 0 && context != null) {
				Task.setCompleted(context, true, extras.getLong(BaseColumns._ID, -1));

				Toast.makeText(context, context.getString(R.string.completed), Toast.LENGTH_SHORT)
												.show();

				// Broadcast that it has been completed, primarily for
				// AndroidAgendaWidget
				context.sendBroadcast(new Intent(
												context.getString(R.string.note_completed_broadcast_intent)));
			}
		}

	}
}
