/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.nononsenseapps.notepad.FragmentLayout;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;

/**
 * The weather widget's AppWidgetProvider.
 */
public class ListWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "WIDGETPROVIDER";
	public static String CLICK_ACTION = "com.nononsenseapps.notepad.widget.CLICK";
	public static String OPEN_ACTION = "com.nononsenseapps.notepad.widget.OPENAPP";
	public static String CREATE_ACTION = "com.nononsenseapps.notepad.widget.CREATE";
	public static String EXTRA_NOTE_ID = "com.nononsenseapps.notepad.widget.note_id";
	public static String EXTRA_LIST_ID = "com.nononsenseapps.notepad.widget.list_id";

	public ListWidgetProvider() {
	}

	@Override
	public void onEnabled(Context context) {
		// Register for external updates to the data to trigger an update of the
		// widget. When using
		// content providers, the data is often updated via a background
		// service, or in response to
		// user interaction in the main app. To ensure that the widget always
		// reflects the current
		// state of the data, we must listen for changes and update ourselves
		// accordingly.
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Intent appIntent = new Intent();
		appIntent.setClass(context, FragmentLayout.class);
		appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (action.equals(OPEN_ACTION)) {
			context.startActivity(appIntent);
		} else if (action.equals(CREATE_ACTION)) {
			appIntent.setData(NotePad.Notes.CONTENT_VISIBLE_URI);
			appIntent.setAction(Intent.ACTION_INSERT);
			// TODO set list ID
			// appIntent.addExtras(NotePad.Notes.COLUMN_NAME_LIST, listId);
			context.startActivity(appIntent);

		} else if (action.equals(CLICK_ACTION)) {
			final long noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1);
			final long listId = intent.getLongExtra(EXTRA_LIST_ID, -1);
			if (noteId > -1 && listId > -1) {
				appIntent.setData(Uri.withAppendedPath(
						NotePad.Notes.CONTENT_VISIBLE_ID_URI_BASE,
						Long.toString(noteId)));
				appIntent.putExtra(NotePad.Notes.COLUMN_NAME_LIST, listId);
				appIntent.setAction(Intent.ACTION_EDIT);
				context.startActivity(appIntent);
			}
		}

		super.onReceive(context, intent);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// This is not called on start up if we are using a configuration
		// activity

		// Update each of the widgets with the remote adapter
		for (int i = 0; i < appWidgetIds.length; ++i) {
			appWidgetManager.updateAppWidget(appWidgetIds[i],
					buildRemoteViews(context, appWidgetIds[i]));
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public static RemoteViews buildRemoteViews(Context context, int appWidgetId) {
		// Specify the service to provide data for the collection widget. Note
		// that we need to
		// embed the appWidgetId via the data otherwise it will be ignored.
		final Intent intent = new Intent(context, ListWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		final RemoteViews rv = new RemoteViews(context.getPackageName(),
				R.layout.listwidget);
		rv.setRemoteAdapter(appWidgetId, R.id.notes_list, intent);

		// Set the empty view to be displayed if the collection is empty. It
		// must be a sibling
		// view of the collection view.
		rv.setEmptyView(R.id.notes_list, R.id.empty_view);

		// Bind a click listener template for the contents of the weather list.
		// Note that we
		// need to update the intent's data if we set an extra, since the extras
		// will be
		// ignored otherwise.
		final Intent onClickIntent = new Intent(context,
				ListWidgetProvider.class);
		onClickIntent.setAction(ListWidgetProvider.CLICK_ACTION);
		onClickIntent
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		onClickIntent.setData(Uri.parse(onClickIntent
				.toUri(Intent.URI_INTENT_SCHEME)));
		final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(
				context, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setPendingIntentTemplate(R.id.notes_list, onClickPendingIntent);

		// Bind the click intent for the button on the widget
		final Intent openAppIntent = new Intent(context,
				ListWidgetProvider.class);
		openAppIntent.setAction(ListWidgetProvider.OPEN_ACTION);
		final PendingIntent openAppPendingIntent = PendingIntent.getBroadcast(
				context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.openAppButton, openAppPendingIntent);
		// Bind the click intent for the button on the widget
		rv.setOnClickPendingIntent(R.id.titleButton, openAppPendingIntent);
		// Create button
		final Intent createIntent = new Intent(context,
				ListWidgetProvider.class);
		createIntent.setAction(ListWidgetProvider.CREATE_ACTION);
		final PendingIntent createPendingIntent = PendingIntent.getBroadcast(
				context, 0, createIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.createNoteButton, createPendingIntent);
		
		return rv;
	}
}