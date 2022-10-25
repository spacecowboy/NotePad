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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * Mimics the SimpleCursorAdapter, but also allows extra items to be injected at
 * the beginning. When asked for id for the extra items, the defined ids are returned.
 * Make sure to set them to negative values (< -1) in order not to confuse them with
 * database IDs.
 * 
 * @author Jonas
 * 
 */
public class ExtrasCursorAdapter extends ResourceCursorAdapter {
	// private static final String TAG = "ExtrasCursorAdapter";

	private Cursor cursor;
	protected Context context;

	protected int[] extraIds;
	protected int[] extraLabels;

	protected String[] from;
	protected int[] to;

	protected int layout;
	protected int dropdownlayout;

	/**
	 * Same as a cursoradapter except two extra arrays are taken (and a layout).
	 * The first is an array of what IDs you want to assign your items so you
	 * can identify them later. Second is an array of ids to the String
	 * resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int[] extraIds, int[] extraLabels, int dropdownlayout) {
		super(context, layout, c, 0);
		this.cursor = c;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
		this.from = from;
		this.to = to;
		this.layout = layout;
		this.dropdownlayout = dropdownlayout;
	}

	/**
	 * Same as a cursoradapter except two extra arrays are taken (and a layout).
	 * The first is an array of what IDs you want to assign your items so you
	 * can identify them later. Second is an array of ids to the String
	 * resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			int flags, String[] from, int[] to, int[] extraIds,
			int[] extraLabels, int dropdownlayout) {
		super(context, layout, c, flags);
		this.cursor = c;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
		this.from = from;
		this.to = to;
		this.layout = layout;
		this.dropdownlayout = dropdownlayout;
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
	 * 
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
		if (cursor != null && position >= extraLabels.length)
			return super.getView(position - extraLabels.length, convertView, parent);

		ViewHolder viewHolder = null;
		if (convertView == null) {
			// Make a new view
			LayoutInflater mInflater = LayoutInflater.from(context);
			convertView = mInflater.inflate(getItemLayout(position), parent, false);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (viewHolder == null) {
			viewHolder = setViewHolder(convertView);
		}
		setExtraText(viewHolder, position);

		return convertView;
	}
	
	/**
	 * Only sets the first field
	 */
	protected void setExtraText(final ViewHolder viewHolder, final int position) {
		viewHolder.texts[0].setText(context.getText(extraLabels[position]));
	}
	
	// TODO method to update extra labels

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (cursor != null && position >= extraLabels.length)
			return super.getDropDownView(position - extraLabels.length, convertView, parent);

		ViewHolder viewHolder = null;
		if (convertView == null) {
			// Make a new view
			LayoutInflater mInflater = LayoutInflater.from(context);
			convertView = mInflater.inflate(dropdownlayout, parent, false);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (viewHolder == null) {
			viewHolder = setViewHolder(convertView);
		}
		setExtraText(viewHolder, position);

		return convertView;
	}
	
	protected int getItemLayout(final int position) {
		return layout;
	}

	@Override
	public long getItemId(int position) {
		if (position < extraIds.length) {
			return extraIds[position];
		}
		else {
			return super.getItemId(position - extraIds.length);
		}
	}
	
	@Override
	public Object getItem(int position) {
		if (position < extraIds.length) {
			return getExtraItem(position);
		}
		else {
			return super.getItem(position - extraIds.length);
		}
	}

	@Override
	public int getCount() {
		if (extraIds != null)
			return super.getCount() + extraIds.length;
		else
			return super.getCount();
	}
	
	/**
	 * Should be a number >= count of the wrapped cursor
	 * @param realPos
	 * @return
	 */
	public CharSequence getExtraItem(int realPos) {
		if (extraLabels.length == 0 || realPos < -1 || realPos > extraLabels.length)
			return null;
		else
			return context.getText(extraLabels[realPos]);
	}

	static class ViewHolder {
		TextView[] texts;
	}
}
