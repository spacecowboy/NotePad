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

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotePadBroadcastReceiver;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.ui.DateView;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.audiofx.BassBoost.Settings;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;

/**
 * This is the service that provides the factory to be bound to the collection
 * service.
 */
public class ListWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
	}
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private Context mContext;
	private HeaderCursor mCursor;
	private int mAppWidgetId;

	private static final String TAG = "WidgetService";

	private static final String indent = "    ";

	private long listId = -1;

	private static final String[] PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_LIST,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
			NotePad.Notes.COLUMN_NAME_GTASKS_STATUS };

	public ListRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	public void onCreate() {
	}

	public void onDestroy() {
		if (mCursor != null && mCursor.getCursor() != null) {
			mCursor.getCursor().close();
		}
	}

	public int getCount() {
		if (mCursor != null)
			return mCursor.getCount();
		else
			return 0;
	}

	public RemoteViews getViewAt(int position) {
		Log.d(TAG, "getViewAt");
		// Get widget settings
		WidgetPrefs settings = new WidgetPrefs(mContext, mAppWidgetId);

		if (!settings.isPresent()) {
			Log.d(TAG, "Is not present!");
			return null;
		}

		boolean dark = false;
		if (mContext.getString(R.string.const_theme_dark).equals(
				settings.getString(ListWidgetConfig.KEY_THEME,
						mContext.getString(R.string.const_theme_light)))) {
			dark = true;
		}

		// Get the data for this position from the content provider
		String title = "";
		String note = "";
		// String space = "";
		CharSequence dueDate = "";
		long noteId = -1;
		// long localListId = -1;
		RemoteViews rv = null;
		if (mCursor.moveToPosition(position)) {
			if (mCursor.getViewType() == HeaderCursor.headerType) {
				final int itemId;
				if (dark)
					itemId = R.layout.widgetlist_header_dark;
				else
					itemId = R.layout.widgetlist_header;
				rv = new RemoteViews(mContext.getPackageName(), itemId);
				rv.setTextViewText(R.id.widget_itemHeader,
						mCursor.getHeaderText());
				rv.setBoolean(itemId, "setClickable", false);
				if (!dark
						&& settings.getBoolean(
								ListWidgetConfig.KEY_TRANSPARENT, false)) {
					rv.setTextColor(
							R.id.widget_itemHeader,
							mContext.getResources().getColor(
									android.R.color.primary_text_light));
				}
			} else {
				final int titleIndex = mCursor.getCursor().getColumnIndex(
						NotePad.Notes.COLUMN_NAME_TITLE);
				final int dateIndex = mCursor.getCursor().getColumnIndex(
						NotePad.Notes.COLUMN_NAME_DUE_DATE);
				final int noteIndex = mCursor.getCursor().getColumnIndex(
						NotePad.Notes.COLUMN_NAME_NOTE);
				// final int listIndex = mCursor
				// .getColumnIndex(NotePad.Notes.COLUMN_NAME_LIST);
				// final int indentIndex = mCursor
				// .getColumnIndex(NotePad.Notes.COLUMN_NAME_INDENTLEVEL);
				final int idIndex = mCursor.getCursor().getColumnIndex(
						NotePad.Notes._ID);
				title = mCursor.getCursor().getString(titleIndex);
				note = mCursor.getCursor().getString(noteIndex);
				noteId = mCursor.getCursor().getLong(idIndex);
				// localListId = mCursor.getLong(listIndex);
				String date = mCursor.getCursor().getString(dateIndex);

				// if (settings != null) {
				// String sortChoice = settings.getString(
				// ListWidgetConfig.KEY_SORT_TYPE,
				// MainPrefs.DUEDATESORT);
				// if (sortChoice.equals(MainPrefs.POSSUBSORT)) {
				// int indentLevel = mCursor.getInt(indentIndex);
				// int l;
				// for (l = 0; l < indentLevel; l++) {
				// space += indent;
				// }
				// }
				// }

				if (date == null || date.isEmpty())
					dueDate = "";
				else {
					dueDate = DateView.toDate(date);
				}

				final int itemId;
				if (dark) {
					itemId = R.layout.widgetlist_item_dark;
				} else {
					itemId = R.layout.widgetlist_item;
				}
				rv = new RemoteViews(mContext.getPackageName(), itemId);

				if (settings
						.getBoolean(ListWidgetConfig.KEY_TRANSPARENT, false)) {
					rv.setInt(R.id.widget_item_container,
							"setBackgroundResource", 0);
				}

				rv.setViewVisibility(
						R.id.widget_complete_task,
						settings.getBoolean(
								ListWidgetConfig.KEY_HIDDENCHECKBOX, false) ? View.GONE
								: View.VISIBLE);
				if (settings.getBoolean(ListWidgetConfig.KEY_HIDDENCHECKBOX,
						false)) {
					rv.setViewVisibility(R.id.item_spacer, View.VISIBLE);
				} else {
					rv.setViewVisibility(R.id.item_spacer, View.GONE);
				}

				if (note == null
						|| note.isEmpty()
						|| settings.getBoolean(ListWidgetConfig.KEY_HIDDENNOTE,
								false)) {
					rv.setViewVisibility(R.id.widget_itemNote, View.GONE);
					rv.setViewVisibility(R.id.widget_itemDateNote, View.GONE);
					rv.setViewVisibility(R.id.widget_itemDateTitle,
							View.VISIBLE);
				} else {
					rv.setViewVisibility(R.id.widget_itemNote, View.VISIBLE);
					rv.setViewVisibility(R.id.widget_itemDateNote, View.VISIBLE);
					rv.setViewVisibility(R.id.widget_itemDateTitle, View.GONE);
				}

				if (dueDate == null
						|| dueDate.length() == 0
						|| settings.getBoolean(ListWidgetConfig.KEY_HIDDENDATE,
								false)) {
					rv.setViewVisibility(R.id.widget_itemDateNote, View.GONE);
					rv.setViewVisibility(R.id.widget_itemDateTitle, View.GONE);
				}

				String lines = settings.getString(
						ListWidgetConfig.KEY_TITLEROWS, "2");
				rv.setInt(R.id.widget_itemTitle, "setMaxLines",
						Integer.parseInt(lines));

				rv.setTextViewText(R.id.widget_itemTitle, title);
				rv.setTextViewText(R.id.widget_itemNote, note);
				rv.setTextViewText(R.id.widget_itemDateNote, dueDate);
				rv.setTextViewText(R.id.widget_itemDateTitle, dueDate);
				// rv.setTextViewText(R.id.widget_itemIndent, space);

				// Set the click intent so that we can handle it and show a
				// toast
				// message

				long listId = Long.parseLong(settings.getString(
						ListWidgetConfig.KEY_LIST, "-1"));

				if (mContext.getResources().getBoolean(R.bool.atLeast16)
						&& settings.getBoolean(ListWidgetConfig.KEY_LOCKSCREEN,
								false)) {
					final Intent fillInIntent = new Intent();
					fillInIntent
							.setAction(Intent.ACTION_EDIT)
							.setData(
									Uri.withAppendedPath(
											NotePad.Notes.CONTENT_VISIBLE_ID_URI_BASE,
											Long.toString(noteId)))
							.putExtra(NotePad.Notes.COLUMN_NAME_LIST, listId);

					rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
				} else {
					final Intent fillInIntent = new Intent();
					fillInIntent.setAction(ListWidgetProvider.CLICK_ACTION);
					fillInIntent.putExtra(ListWidgetProvider.EXTRA_NOTE_ID,
							noteId);
					fillInIntent.putExtra(ListWidgetProvider.EXTRA_LIST_ID,
							listId);
					rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
				}

				if (mContext.getResources().getBoolean(R.bool.atLeast16)
						&& settings.getBoolean(ListWidgetConfig.KEY_LOCKSCREEN,
								false)) {
					final Intent fillInIntent = new Intent();
					fillInIntent
							.setAction(
									mContext.getString(R.string.complete_note_broadcast_intent))
							.setData(
									Uri.withAppendedPath(
											NotePad.Notes.CONTENT_VISIBLE_ID_URI_BASE,
											Long.toString(noteId)))
							.putExtra(NotePad.Notes.COLUMN_NAME_LIST, listId);

					rv.setOnClickFillInIntent(R.id.widget_complete_task, fillInIntent);
				} else {
					final Intent completeFillIntent = new Intent();
					completeFillIntent
							.setAction(ListWidgetProvider.COMPLETE_ACTION)
							.putExtra(ListWidgetProvider.EXTRA_NOTE_ID, noteId);
					rv.setOnClickFillInIntent(R.id.widget_complete_task,
							completeFillIntent);
				}

			}
		}

		return rv;
	}

	public RemoteViews getLoadingView() {
		// We aren't going to return a default loading view in this sample
		return null;
	}

	public int getViewTypeCount() {
		return 4;
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
		if (mCursor != null && mCursor.getCursor() != null) {
			mCursor.getCursor().close();
		}

		// Get widget settings
		WidgetPrefs settings = new WidgetPrefs(mContext, mAppWidgetId);
		if (settings != null) {
			listId = Long.parseLong(settings.getString(
					ListWidgetConfig.KEY_LIST, "-1"));

			// getListTitle(settings, listId);

			String sortChoice = settings.getString(
					ListWidgetConfig.KEY_SORT_TYPE, MainPrefs.DUEDATESORT);
			String sortOrder = NotePad.Notes.DUEDATE_SORT_TYPE;

			if (MainPrefs.DUEDATESORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.DUEDATE_SORT_TYPE;
			} else if (MainPrefs.TITLESORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.ALPHABETIC_SORT_TYPE;
			} else if (MainPrefs.MODIFIEDSORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.MODIFICATION_SORT_TYPE;
			} else if (MainPrefs.POSSUBSORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.POSSUBSORT_SORT_TYPE;
			}

			sortOrder += " "
					+ settings.getString(ListWidgetConfig.KEY_SORT_ORDER,
							NotePad.Notes.DEFAULT_SORT_ORDERING);

			String listWhere = null;
			String[] listArg = null;
			if (listId > -1) {
				listWhere = NotePad.Notes.COLUMN_NAME_LIST + " IS ? AND "
						+ NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " IS ?";
				listArg = new String[] {
						Long.toString(listId),
						mContext.getText(R.string.gtask_status_uncompleted)
								.toString() };
			} else {
				listWhere = NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " IS ?";
				listArg = new String[] { mContext.getText(
						R.string.gtask_status_uncompleted).toString() };
			}

			Cursor cursor = mContext.getContentResolver().query(
					NotePad.Notes.CONTENT_VISIBLE_URI, PROJECTION, listWhere,
					listArg, sortOrder);
			mCursor = new HeaderCursor(mContext, cursor, sortChoice,
					settings.getString(ListWidgetConfig.KEY_SORT_ORDER,
							NotePad.Notes.DEFAULT_SORT_ORDERING));

		}
	}
}