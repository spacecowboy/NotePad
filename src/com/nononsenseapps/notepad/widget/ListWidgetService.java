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

import java.util.ArrayList;
import java.util.List;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotePadProvider;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.NotePad.Notes;
import com.nononsenseapps.notepad.R.id;
import com.nononsenseapps.notepad.R.layout;
import com.nononsenseapps.ui.DateView;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.ContentUris;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class ListWidgetService extends RemoteViewsService {
	
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;
    private int mAppWidgetId;
    
    private ListChecker observer;
    
    private static final String[] PROJECTION = new String[] {
		NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
		NotePad.Notes.COLUMN_NAME_NOTE,
		NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
		NotePad.Notes.COLUMN_NAME_DUE_DATE,
		NotePad.Notes.COLUMN_NAME_GTASKS_STATUS };
	private static final String TAG = "FACTORY";

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        observer = new ListChecker(null);
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
    	mContext.getContentResolver().registerContentObserver(NotePad.Notes.CONTENT_URI, true, observer);
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
    }

    public void onDestroy() {
    	mContext.getContentResolver().unregisterContentObserver(observer);
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider
        String title = "";
        String note = "";
        CharSequence dueDate = "";
        long noteId = -1;
        if (mCursor.moveToPosition(position)) {
            final int titleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            final int dateIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
            final int noteIndex = mCursor.getColumnIndex(
            		NotePad.Notes.COLUMN_NAME_NOTE);
            final int idIndex = mCursor.getColumnIndex(
            		NotePad.Notes._ID);
            title = mCursor.getString(titleIndex);
            note = mCursor.getString(noteIndex);
            noteId = mCursor.getLong(idIndex);
            String date = mCursor.getString(dateIndex);
            if (date == null || date.length() == 0)
            	dueDate = "";
    		else {
    			dueDate = DateView.toDate(date);
    		}
        }

        final int itemId =  R.layout.widgetlist_item;
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), itemId);
        rv.setTextViewText(R.id.widget_itemTitle, title);
        rv.setTextViewText(R.id.widget_itemNote, note);
        rv.setTextViewText(R.id.widget_itemDate, dueDate);

        // Set the click intent so that we can handle it and show a toast message
        final Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putLong(ListWidgetProvider.EXTRA_NOTE_ID, noteId);
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

        return rv;
    }
    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
    	Log.d(TAG, "onDataSetChanged");
        // Refresh the cursor
        if (mCursor != null) {
            mCursor.close();
        }
        
        mCursor = mContext.getContentResolver().query(ListDBProvider.CONTENT_VISIBLE_URI, PROJECTION, null,
                null, NotePad.Notes.ALPHABETIC_ASC_ORDER);
    }
    
    private class ListChecker extends ContentObserver {

		public ListChecker(Handler handler) {
			super(handler);
		}
    	
		@Override
		public void onChange(boolean selfchange) {
			Log.d("Observer", "onChange");
			// Refresh the widget
			AppWidgetManager.getInstance(mContext).notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.notes_list);
		}
    }
}