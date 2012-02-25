package com.nononsenseapps.ui;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class ExtrasCursorAdapter extends ResourceCursorAdapter {
	private static final String TAG = "ExtrasCursorAdapter";

	private Cursor cursor;
	private LayoutInflater mInflater;

	protected int layout;
	protected int dropDownLayout;
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
			int[] extraIds, int[] extraLabels) {
		super(context, layout, c);
		this.cursor = c;
		mInflater = LayoutInflater.from(context);
		this.layout = layout;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
	}

	/**
	 * Same as a cursoradapter except two extra arrays are taken (and a layout).
	 * The first is an array of what IDs you want to assign your items so you
	 * can identify them later. Second is an array of ids to the String
	 * resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			boolean autoRequery, int[] extraIds, int[] extraLabels) {
		super(context, layout, c, autoRequery);
		this.cursor = c;
		mInflater = LayoutInflater.from(context);
		this.layout = layout;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
	}

	/**
	 * Same as a cursoradapter except two extra arrays are taken (and a layout).
	 * The first is an array of what IDs you want to assign your items so you
	 * can identify them later. Second is an array of ids to the String
	 * resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			int flags, int[] extraIds, int[] extraLabels) {
		super(context, layout, c, flags);
		this.cursor = c;
		mInflater = LayoutInflater.from(context);
		this.layout = layout;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
	}

	@Override
	public void setDropDownViewResource(int dropDownLayout) {
		this.dropDownLayout = dropDownLayout;
		super.setDropDownViewResource(dropDownLayout);
	}

	@Override
	public void setViewResource(int layout) {
		this.layout = layout;
		super.setViewResource(layout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.CursorAdapter#bindView(android.view.View,
	 * android.content.Context, android.database.Cursor)
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Log.d(TAG, "bindView");
		// Dont need to do anything here
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		if (viewHolder == null) {
			viewHolder = new ViewHolder();
			viewHolder.text = (TextView) view.findViewById(android.R.id.text1);
			view.setTag(viewHolder);
		}
		// Fetch from database
		// this.cursor.moveToPosition(position);
		viewHolder.text.setText(cursor.getString(cursor
				.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.CursorAdapter#newView(android.content.Context,
	 * android.database.Cursor, android.view.ViewGroup)
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		Log.d(TAG, "newView");
		// Dont need to do anything here either
		// return null;
		return super.newView(context, cursor, parent);
	}

	@Override
	public Cursor swapCursor(Cursor newCursor) {
		Log.d(TAG, "swapCursor");
		this.cursor = newCursor;
		return super.swapCursor(newCursor);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Log.d(TAG, "getView: " + position);
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
			viewHolder = new ViewHolder();
			viewHolder.text = (TextView) convertView
					.findViewById(android.R.id.text1);
			convertView.setTag(viewHolder);
		}
		viewHolder.text.setText(context.getText(extraLabels[position
				- numOfItems]));

		return convertView;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent);
	}

	@Override
	public long getItemId(int position) {
		Log.d(TAG, "getItemId: " + position);
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
		TextView text;
	}
}
