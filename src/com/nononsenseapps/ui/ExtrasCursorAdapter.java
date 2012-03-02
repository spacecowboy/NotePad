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

package com.nononsenseapps.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * Mimics the SimpleCursorAdapter, but also allows extra items to be injected at
 * the end. When asked for id for the extra items, the defined ids are returned.
 * Make sure to set them to negative values in order not to confuse them with
 * database IDs.
 * 
 * @author Jonas
 * 
 */
public class ExtrasCursorAdapter extends ResourceCursorAdapter {
	//private static final String TAG = "ExtrasCursorAdapter";

	private Cursor cursor;
	protected Context context;

	protected int[] extraIds;
	protected int[] extraLabels;

	protected String[] from;
	protected int[] to;

	private int numOfItems = 0;

	/**
	 * Same as a cursoradapter except two extra arrays are taken (and a layout).
	 * The first is an array of what IDs you want to assign your items so you
	 * can identify them later. Second is an array of ids to the String
	 * resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int[] extraIds, int[] extraLabels) {
		super(context, layout, c);
		this.cursor = c;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
		this.from = from;
		this.to = to;
	}

	/**
	 * Same as a cursoradapter except two extra arrays are taken (and a layout).
	 * The first is an array of what IDs you want to assign your items so you
	 * can identify them later. Second is an array of ids to the String
	 * resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			boolean autoRequery, String[] from, int[] to, int[] extraIds,
			int[] extraLabels) {
		super(context, layout, c, autoRequery);
		this.cursor = c;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
		this.from = from;
		this.to = to;
	}

	/**
	 * Same as a cursoradapter except two extra arrays are taken (and a layout).
	 * The first is an array of what IDs you want to assign your items so you
	 * can identify them later. Second is an array of ids to the String
	 * resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			int flags, String[] from, int[] to, int[] extraIds,
			int[] extraLabels) {
		super(context, layout, c, flags);
		this.cursor = c;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
		this.from = from;
		this.to = to;
	}

	/**
	 * 
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		int i;
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		if (viewHolder == null) {
			viewHolder = setViewHolder(view);
		}
		// Fetch from database
		for (i = 0; i < from.length; i++) {
			viewHolder.texts[i].setText(cursor.getString(cursor
					.getColumnIndex(from[i])));
		}
	}
	
	/**
	 * Initializes the viewholder according to the specified from/to arrays.
	 * @param view
	 * @return
	 */
	private ViewHolder setViewHolder(View view) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.texts = new TextView[from.length];
		int i;
		for (i = 0; i < from.length; i++) {
			viewHolder.texts[i] = (TextView) view.findViewById(to[i]);
		}
		view.setTag(viewHolder);
		return viewHolder;
	}

	@Override
	public Cursor swapCursor(Cursor newCursor) {
		this.cursor = newCursor;
		return super.swapCursor(newCursor);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (cursor == null || position < numOfItems)
			return super.getView(position, convertView, parent);

		ViewHolder viewHolder = null;
		if (convertView == null) {
			// Make a new view
			cursor.moveToFirst();
			convertView = super.newView(parent.getContext(), cursor, parent);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (viewHolder == null) {
			viewHolder = setViewHolder(convertView);
		}
		viewHolder.texts[0].setText(context.getText(extraLabels[position
				- numOfItems]));

		return convertView;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (cursor == null || position < numOfItems)
			return super.getDropDownView(position, convertView, parent);

		ViewHolder viewHolder = null;
		if (convertView == null) {
			// Make a new view
			cursor.moveToFirst();
			convertView = super.newDropDownView(parent.getContext(), cursor,
					parent);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (viewHolder == null) {
			viewHolder = setViewHolder(convertView);
		}
		viewHolder.texts[0].setText(context.getText(extraLabels[position
				- numOfItems]));

		return convertView;
	}

	@Override
	public long getItemId(int position) {
		if (numOfItems < 1 || position < numOfItems) {
			return super.getItemId(position);
		} else {
			return extraIds[position - numOfItems];
		}
	}

	@Override
	public int getCount() {
		numOfItems = super.getCount();
		if (numOfItems > 0 && extraIds != null)
			return numOfItems + extraIds.length;
		else
			return numOfItems;
	}

	static class ViewHolder {
		TextView[] texts;
	}
}
