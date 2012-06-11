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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.RightActivity;

/**
 * Thewidget's AppWidgetProvider.
 */
public class ListWidgetProvider extends AppWidgetProvider {
	// private static final String TAG = "WIDGETPROVIDER";
	public static final String CLICK_ACTION = "com.nononsenseapps.notepad.widget.CLICK";
	public static final String OPEN_ACTION = "com.nononsenseapps.notepad.widget.OPENAPP";
	public static final String CREATE_ACTION = "com.nononsenseapps.notepad.widget.CREATE";
	public static final String EXTRA_NOTE_ID = "com.nononsenseapps.notepad.widget.note_id";
	public static final String EXTRA_LIST_ID = "com.nononsenseapps.notepad.widget.list_id";

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
		String action = intent.getAction();
		Intent appIntent = new Intent();
		appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		if (action.equals(OPEN_ACTION)) {
			appIntent.setClass(context, MainActivity.class);
			appIntent.setAction(Intent.ACTION_VIEW);
			appIntent.setData(Uri.withAppendedPath(
					NotePad.Lists.CONTENT_VISIBLE_ID_URI_BASE, Long
							.toString(intent.getLongExtra(
									NotePad.Notes.COLUMN_NAME_LIST, -1))));
			context.startActivity(appIntent);
		} else if (action.equals(CREATE_ACTION)) {
			appIntent.setClass(context, RightActivity.class);
			appIntent.setData(NotePad.Notes.CONTENT_VISIBLE_URI);
			appIntent.setAction(Intent.ACTION_INSERT);
			appIntent.putExtra(NotePad.Notes.COLUMN_NAME_LIST,
					intent.getLongExtra(NotePad.Notes.COLUMN_NAME_LIST, -1));
			context.startActivity(appIntent);

		} else if (action.equals(CLICK_ACTION)) {
			appIntent.setClass(context, RightActivity.class);
			long noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1);
			
			// This would complete the note
//			if (noteId > -1) {
//				Intent bintent = new Intent();
//				bintent.setAction(context.getText(R.string.complete_note_broadcast_intent).toString());
//				bintent.putExtra(NotePad.Notes._ID, noteId);
//				Log.d("Broadcast", "Sending broadcast");
//				context.sendBroadcast(bintent);
//			}
			
			if (noteId > -1) {
				appIntent.setData(Uri.withAppendedPath(
						NotePad.Notes.CONTENT_VISIBLE_ID_URI_BASE,
						Long.toString(noteId)));
				appIntent
						.putExtra(NotePad.Notes.COLUMN_NAME_LIST, intent
								.getLongExtra(NotePad.Notes.COLUMN_NAME_LIST,
										-1));
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
		// Hack: We must set this widget's id in the URI to prevent the
		// situation
		// where the last widget added will be used for everything
		Uri data = Uri.withAppendedPath(Uri.parse("STUPIDWIDGETS"
				+ "://widget/id/"), String.valueOf(appWidgetId));

		// Specify the service to provide data for the collection widget. Note
		// that we need to
		// embed the appWidgetId via the data otherwise it will be ignored.
		Intent intent = new Intent(context, ListWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		
		final int itemId;
		SharedPreferences settings = context.getSharedPreferences(
				ListWidgetConfigure.getSharedPrefsFile(appWidgetId),
				Context.MODE_PRIVATE);
		if (settings!= null && ListWidgetConfigure.THEME_DARK.equals(settings.getString(ListWidgetConfigure.KEY_THEME, ListWidgetConfigure.THEME_LIGHT))) {
			itemId = R.layout.listwidget_dark;
		} else {
			itemId = R.layout.listwidget;
		}
		
		RemoteViews rv = new RemoteViews(context.getPackageName(),
				itemId);
		rv.setRemoteAdapter(appWidgetId, R.id.notes_list, intent);

		// Set the empty view to be displayed if the collection is empty. It
		// must be a sibling
		// view of the collection view.
		rv.setEmptyView(R.id.notes_list, R.id.empty_view);

		// set list title
		// String listTitle = settings.getString(
		// ListWidgetConfigure.KEY_LIST_TITLE,
		// context.getText(R.string.show_from_all_lists).toString());

		// String listTitle = context.getText(R.string.app_name).toString();

		long listId = Long.parseLong(settings.getString(
				ListWidgetConfigure.KEY_LIST,
				Integer.toString(MainActivity.ALL_NOTES_ID)));

		String listTitle = getListTitle(context, settings, listId);
		rv.setCharSequence(R.id.titleButton, "setText", listTitle);

		// Bind a click listener template for the contents of the list.
		// Note that we
		// need to update the intent's data if we set an extra, since the extras
		// will be
		// ignored otherwise.
		Intent onClickIntent = new Intent(context, ListWidgetProvider.class);
		onClickIntent.setAction(ListWidgetProvider.CLICK_ACTION)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
				.putExtra(NotePad.Notes.COLUMN_NAME_LIST, listId).setData(data);

		PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(
				context, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setPendingIntentTemplate(R.id.notes_list, onClickPendingIntent);

		// Bind the click intent for the button on the widget
		Intent openAppIntent = new Intent(context, ListWidgetProvider.class);
		openAppIntent.setAction(ListWidgetProvider.OPEN_ACTION);
		openAppIntent.putExtra(NotePad.Notes.COLUMN_NAME_LIST, listId);
		openAppIntent.setData(data);
		PendingIntent openAppPendingIntent = PendingIntent.getBroadcast(
				context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.openAppButton, openAppPendingIntent);
		// Bind the click intent for the button on the widget
		rv.setOnClickPendingIntent(R.id.titleButton, openAppPendingIntent);
		// Create button
		Intent createIntent = new Intent(context, ListWidgetProvider.class);
		createIntent.setAction(ListWidgetProvider.CREATE_ACTION);
		createIntent.putExtra(NotePad.Notes.COLUMN_NAME_LIST, listId);
		createIntent.setData(data);
		PendingIntent createPendingIntent = PendingIntent.getBroadcast(context,
				0, createIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.createNoteButton, createPendingIntent);

		return rv;
	}

	// Retrieve the list name from the database and set in shared preference
	// file
	private static String getListTitle(Context mContext,
			SharedPreferences settings, long listId) {
		String title = mContext.getText(R.string.show_from_all_lists)
				.toString();
		if (listId == MainActivity.ALL_NOTES_ID) {
			settings.edit()
					.putString(ListWidgetConfigure.KEY_LIST_TITLE, title)
					.commit();
		} else {
			Cursor c = mContext.getContentResolver().query(
					NotePad.Lists.CONTENT_URI,
					new String[] { NotePad.Lists._ID,
							NotePad.Lists.COLUMN_NAME_TITLE },
					NotePad.Lists._ID + " IS ?",
					new String[] { Long.toString(listId) }, null);

			if (c != null && c.moveToFirst()) {
				title = c.getString(c
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
				settings.edit()
						.putString(ListWidgetConfigure.KEY_LIST_TITLE, title)
						.commit();
			}
			if (c != null)
				c.close();
		}

		return title;
	}
}