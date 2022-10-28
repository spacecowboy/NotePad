/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.helpers;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.widget.ListWidgetProvider;
import com.nononsenseapps.notepad.widget.WidgetPrefs;

/**
 * The purpose here is to make it easy for other classes to notify that
 * something has changed in the database. Will also call update on the widgets
 * appropriately.
 */
public class UpdateNotifier {

	/**
	 * Will update all notes and specific uri if present
	 */
	public static void notifyChangeNote(Context context) {
		notifyChange(context, Task.URI);
		updateWidgets(context);
	}

	/**
	 * Will update all notes and specific uri if present
	 *
	 * @param uri optional uri
	 */
	public static void notifyChangeNote(Context context, Uri uri) {
		notifyChange(context, uri);
		notifyChangeNote(context);
	}

	/**
	 * Will update all notes
	 *
	 * @param context
	 */
	public static void notifyChangeList(Context context) {
		notifyChange(context, TaskList.URI);
		updateWidgets(context);
	}

	/**
	 * Will update all lists and specific uri if present
	 *
	 * @param uri optional uri
	 */
	public static void notifyChangeList(Context context, Uri uri) {
		notifyChange(context, uri);
		notifyChangeList(context);
	}

	/**
	 * Will update all lists and specific uri if present
	 * Always updates notifications
	 *
	 * @param uri optional uri
	 */
	private static void notifyChange(Context context, Uri uri) {
		if (uri != null) {
			context.getContentResolver().notifyChange(uri, null, false);
			SyncHelper.requestSyncIf(context, SyncHelper.ONCHANGE);
			context.getContentResolver().notifyChange(Notification.URI, null);
		}
	}

	/**
	 * Instead of doing this in a service which might be killed, simply call
	 * this whenever something is changed in here
	 *
	 * Update all widgets's views as this database has changed somehow
	 */
	public static void updateWidgets(Context context) {
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(context,
						ListWidgetProvider.class));
		if (appWidgetIds.length > 0) {
			/*
			 * Tell the widgets that the list items should be invalidated and
			 * refreshed! Will call onDatasetChanged in ListWidgetService, doing
			 * a new requery
			 */
			// appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds,
			// R.id.notes_list);

			// Only update widgets that exist
			for (int widgetId : appWidgetIds) {
				final WidgetPrefs prefs = new WidgetPrefs(context, widgetId);
				if (prefs.isPresent()) {
					appWidgetManager.notifyAppWidgetViewDataChanged(widgetId,
							R.id.notesList);
				}
			}
		}
	}
}
