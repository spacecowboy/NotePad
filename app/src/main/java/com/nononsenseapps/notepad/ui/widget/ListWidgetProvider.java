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

package com.nononsenseapps.notepad.ui.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.data.receiver.NotePadBroadcastReceiver;
import com.nononsenseapps.notepad.ui.editor.ActivityEditor;
import com.nononsenseapps.notepad.ui.editor.TaskDetailFragment;
import com.nononsenseapps.notepad.ui.list.ActivityList;
import com.nononsenseapps.notepad.util.Log;

/**
 * Thewidget's AppWidgetProvider.
 */
public class ListWidgetProvider extends AppWidgetProvider {
	// private static final String TAG = "WIDGETPROVIDER";
	public static final String COMPLETE_ACTION = "com.nononsenseapps.notepad.widget.COMPLETE";
	public static final String CLICK_ACTION = "com.nononsenseapps.notepad.widget.CLICK";
	public static final String OPEN_ACTION = "com.nononsenseapps.notepad.widget.OPENAPP";
	public static final String CONFIG_ACTION = "com.nononsenseapps.notepad.widget.CONFIG";
	public static final String CREATE_ACTION = "com.nononsenseapps.notepad.widget.CREATE";
	public static final String EXTRA_NOTE_ID = "com.nononsenseapps.notepad.widget.note_id";
	public static final String EXTRA_LIST_ID = "com.nononsenseapps.notepad.widget.list_id";

	public ListWidgetProvider() {

	}

	/**
	 * Complete note calls go here
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Intent appIntent = new Intent();
		appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);

		if (action.equals(CLICK_ACTION)) {
			Log.d("widgetwork", "CLICK ACTION RECEIVED");
			long noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1);
			if (noteId > -1) {
				appIntent
						.setAction(Intent.ACTION_EDIT)
						.setData(Task.getUri(noteId))
						.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID,
								intent.getLongExtra(EXTRA_LIST_ID, -1));

				context.startActivity(appIntent);
			}
		}
		else if (action.equals(COMPLETE_ACTION)) {
			// Should send broadcast here
			Log.d("widgetwork", "COMPLETE ACTION RECEIVED");
			long noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1);
			// This will complete the note
			if (noteId > -1) {
				Intent bintent = new Intent(context,
						NotePadBroadcastReceiver.class);
				bintent.setAction(context
						.getString(R.string.complete_note_broadcast_intent));
				bintent.putExtra(Task.Columns._ID, noteId);
				context.sendBroadcast(bintent);
			}
		}

		super.onReceive(context, intent);
	}

	@Override
	public void onEnabled(Context context) {
		/*
		 * Register for external updates to the data to trigger an update of the
		 * widget. When using content providers, the data is often updated via a
		 * background service, or in response to user interaction in the main
		 * app. To ensure that the widget always reflects the current state of
		 * the data, we must listen for changes and update ourselves
		 * accordingly.
		 */
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.d("ListWidgetProvider", "onDeleted, appWidgetIds.length = "
				+ String.valueOf(appWidgetIds.length));

		for (int widgetId : appWidgetIds) {
			WidgetPrefs.delete(context, widgetId);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// This is not called on start up if we are using a configuration
		// activity

		// Update each of the widgets with the remote adapter
		for (int i = 0; i < appWidgetIds.length; ++i) {
			int widgetId = appWidgetIds[i];
			// Load widget prefs
			final WidgetPrefs prefs = new WidgetPrefs(context, widgetId);

			// Build view update
			RemoteViews updateViews = buildRemoteViews(context,
					appWidgetManager, widgetId, prefs);

			// Tell the AppWidgetManager to perform an update
			appWidgetManager.updateAppWidget(widgetId, updateViews);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static RemoteViews buildRemoteViews(final Context context,
			final AppWidgetManager appWidgetManager, final int appWidgetId,
			final WidgetPrefs settings) {
		// Hack: We must set this widget's id in the URI to prevent the
		// situation
		// where the last widget added will be used for everything
		final Uri data = Uri.withAppendedPath(Uri.parse("STUPIDWIDGETS"
				+ "://widget/id/"), String.valueOf(appWidgetId));

		boolean isKeyguard = false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			Bundle myOptions = appWidgetManager
					.getAppWidgetOptions(appWidgetId);

			// Get the value of OPTION_APPWIDGET_HOST_CATEGORY
			int category = myOptions.getInt(
					AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);

			// If the value is WIDGET_CATEGORY_KEYGUARD, it's a lockscreen
			// widget
			isKeyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
		}
		if (isKeyguard) {
			settings.putBoolean(ListWidgetConfig.KEY_LOCKSCREEN, true);
		}

		// Specify the service to provide data for the collection widget. Note
		// that we need to
		// embed the appWidgetId via the data otherwise it will be ignored.
		final Intent intent = new Intent(context, ListWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

		RemoteViews rv = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout);

		rv.setRemoteAdapter(R.id.notesList, intent);

		// Set the empty view to be displayed if the collection is empty. It
		// must be a sibling
		// view of the collection view.
		rv.setEmptyView(R.id.notesList, R.id.empty_view);

		final long listId = settings.getLong(ListWidgetConfig.KEY_LIST, -1);
		final String listTitle = settings.getString(
				ListWidgetConfig.KEY_LIST_TITLE,
				context.getString(R.string.app_name));
		rv.setTextViewText(R.id.titleButton, listTitle);

		// Hide header if we should
		rv.setViewVisibility(R.id.widgetHeader, settings.getBoolean(
				ListWidgetConfig.KEY_HIDDENHEADER, false) ? View.GONE
				: View.VISIBLE);

		// Set background color
		final int color = settings.getInt(ListWidgetConfig.KEY_SHADE_COLOR,
				ListWidgetConfig.DEFAULT_SHADE);
		rv.setInt(R.id.shade, "setBackgroundColor", color);
		rv.setViewVisibility(R.id.shade, (color & 0xff000000) == 0 ? View.GONE
				: View.VISIBLE);

		/*
		 * Bind a click listener template for the contents of the list. Note
		 * that we need to update the intent's data if we set an extra, since
		 * the extras will be ignored otherwise.
		 */
		if (isKeyguard) {
			final Intent itemIntent = new Intent(context, ActivityList.class);
			itemIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);

			PendingIntent onClickPendingIntent = PendingIntent.getActivity(
					context, 0, itemIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setPendingIntentTemplate(R.id.notesList, onClickPendingIntent);
		}
		else {
			// To handle complete, we use broadcasts
			Intent onClickIntent = new Intent(context, ListWidgetProvider.class);
			onClickIntent
					.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
					.putExtra(Task.Columns.DBLIST, listId).setData(data);

			PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(
					context, 0, onClickIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setPendingIntentTemplate(R.id.notesList, onClickPendingIntent);
		}

		final Intent appIntent = new Intent();
		appIntent
				.setFlags(
						Intent.FLAG_ACTIVITY_NEW_TASK
								| Intent.FLAG_ACTIVITY_CLEAR_TASK)
				.setClass(context, ActivityList.class)
				.setAction(Intent.ACTION_VIEW).setData(TaskList.getUri(listId));
		PendingIntent openAppPendingIntent = PendingIntent.getActivity(context,
				0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.titleButton, openAppPendingIntent);

		final Intent configIntent = new Intent();
		configIntent
				.setFlags(
						Intent.FLAG_ACTIVITY_NEW_TASK
								| Intent.FLAG_ACTIVITY_CLEAR_TASK)
				.setClass(context, ListWidgetConfig.class).setData(data)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent openConfigPendingIntent = PendingIntent.getActivity(
				context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.widgetConfigButton,
				openConfigPendingIntent);

		final Intent createIntent = new Intent();
		createIntent
				.setFlags(
						Intent.FLAG_ACTIVITY_NEW_TASK
								| Intent.FLAG_ACTIVITY_CLEAR_TASK)
				.setClass(context, ActivityEditor.class)
                // Append a dummy path so we don't override this intent on 2nd, 3rd, etc, widgets.
				.setAction(Intent.ACTION_INSERT).setData(Uri.withAppendedPath(Task.URI,
                "/widget/" + appWidgetId + "/-1"))
				.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);

		PendingIntent createPendingIntent = PendingIntent.getActivity(context,
				0, createIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.createNoteButton, createPendingIntent);

		return rv;
	}
}
