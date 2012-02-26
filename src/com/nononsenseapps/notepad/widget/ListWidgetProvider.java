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

    public ListWidgetProvider() {
    }

    @Override
    public void onEnabled(Context context) {
        // Register for external updates to the data to trigger an update of the widget.  When using
        // content providers, the data is often updated via a background service, or in response to
        // user interaction in the main app.  To ensure that the widget always reflects the current
        // state of the data, we must listen for changes and update ourselves accordingly.
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        //if (action.equals(REFRESH_ACTION)) {
        if (action.equals(OPEN_ACTION)) {
        	Intent appIntent = new Intent();
        	appIntent.setClass(context, FragmentLayout.class);
        	appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	context.startActivity(appIntent);
        } else if (action.equals(CREATE_ACTION)) {
        	Toast.makeText(context, "CreateAction", Toast.LENGTH_SHORT).show();
        	
            // BroadcastReceivers have a limited amount of time to do work, so for this sample, we
            // are triggering an update of the data on another thread.  In practice, this update
            // can be triggered from a background service, or perhaps as a result of user actions
            // inside the main application.
//            final Context context = ctx;
//            sWorkerQueue.removeMessages(0);
//            sWorkerQueue.post(new Runnable() {
//                @Override
//                public void run() {
//                    final ContentResolver r = context.getContentResolver();
//                    final Cursor c = r.query(NotePad.Notes.CONTENT_URI, null, null, null, 
//                            null);
//                    final int count = c.getCount();
//                    final int maxDegrees = 96;
//
//                    // We disable the data changed observer temporarily since each of the updates
//                    // will trigger an onChange() in our data observer.
//                    r.unregisterContentObserver(sDataObserver);
//                    for (int i = 0; i < count; ++i) {
//                        final Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, i);
//                        final ContentValues values = new ContentValues();
//                        values.put(WeatherDataProvider.Columns.TEMPERATURE,
//                                new Random().nextInt(maxDegrees));
//                        r.update(uri, values, null, null);
//                    }
//                    r.registerContentObserver(NotePad.Notes.CONTENT_URI, true, sDataObserver);
//
//                    final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
//                    final ComponentName cn = new ComponentName(context, ListWidgetProvider.class);
//                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.weather_list);
//                }
//            });
        } else if (action.equals(CLICK_ACTION)) {
            // Show a toast
            final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            final long noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1);
            Toast.makeText(context, "NoteId = " + noteId, Toast.LENGTH_SHORT).show();
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	// This is not called on start up if we are using a configuration activity
    	
        // Update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            // Specify the service to provide data for the collection widget.  Note that we need to
            // embed the appWidgetId via the data otherwise it will be ignored.
            final Intent intent = new Intent(context, ListWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.listwidget);
            rv.setRemoteAdapter(appWidgetIds[i], R.id.notes_list, intent);

            // Set the empty view to be displayed if the collection is empty.  It must be a sibling
            // view of the collection view.
            rv.setEmptyView(R.id.notes_list, R.id.empty_view);

            // Bind a click listener template for the contents of the weather list.  Note that we
            // need to update the intent's data if we set an extra, since the extras will be
            // ignored otherwise.
            final Intent onClickIntent = new Intent(context, ListWidgetProvider.class);
            onClickIntent.setAction(ListWidgetProvider.CLICK_ACTION);
            onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                    onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.notes_list, onClickPendingIntent);

            // Bind the click intent for the button on the widget
            final Intent openAppIntent = new Intent(context, ListWidgetProvider.class);
            openAppIntent.setAction(ListWidgetProvider.OPEN_ACTION);
            final PendingIntent openAppPendingIntent = PendingIntent.getBroadcast(context, 0,
            		openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.openAppButton, openAppPendingIntent);
         // Bind the click intent for the button on the widget
            rv.setOnClickPendingIntent(R.id.titleButton, openAppPendingIntent);
            // Create button
            final Intent createIntent = new Intent(context, ListWidgetProvider.class);
            createIntent.setAction(ListWidgetProvider.CREATE_ACTION);
            final PendingIntent createPendingIntent = PendingIntent.getBroadcast(context, 0,
            		createIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.createNoteButton, createPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}