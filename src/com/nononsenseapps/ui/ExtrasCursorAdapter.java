package com.nononsenseapps.ui;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ExtrasCursorAdapter extends SimpleCursorAdapter {

	private static final String TAG = "ExtrasCursorAdapter";
	protected Context context;
	protected String[] from;
	protected int[] to;
	protected int layout;
	protected int[] extraIds;
	protected int[] extraLabels;

	//private int currentExtraItem = -1;
	private int numOfItems = 0;


	/**
	 * Same as a simplecursoradapter except two extra arrays are taken. The first is an array of what IDs you want to assign your items
	 * so you can identify them later. Second is an array of ids to the String resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int[] extraIds, int[] extraLabels) {
		super(context, layout, c, from, to);
		this.from = from;
		this.to = to;
		this.layout = layout;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
	}

	/**
	 * Same as a simplecursoradapter except two extra arrays are taken. The first is an array of what IDs you want to assign your items
	 * so you can identify them later. Second is an array of ids to the String resources to use as labels.
	 */
	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags, int[] extraIds, int[] extraLabels) {
		super(context, layout, c, from, to, flags);
		this.from = from;
		this.to = to;
		this.layout = layout;
		this.extraIds = extraIds;
		this.extraLabels = extraLabels;
		this.context = context;
	}
	
	protected View getExtrasView(int extraIndex, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(layout, parent, false);
		TextView text = (TextView) view.findViewById(android.R.id.text1);
		if (text != null) {
			text.setText(context.getText(extraLabels[extraIndex]));
			view.setId(extraIds[extraIndex]);
		}
		return view;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position >= numOfItems) {
			Log.d(TAG, "getView Extra: " + (position - numOfItems));
			return getExtrasView(position - numOfItems, parent);
		}
		else {
			Log.d(TAG, "getView before: " + (position - numOfItems));
			return super.getView(position, convertView, parent);
		}
	}
	
	@Override
	public long getItemId(int position) {
		if (position >= numOfItems) {
			Log.d(TAG, "getItemId: " + (position - numOfItems));
			return extraIds[position - numOfItems];
		}
		return super.getItemId(position);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		Log.d(TAG, "newView: " + cursor.getPosition());
		//return getExtrasView(0, parent);
		return super.newView(context, cursor, parent);
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (position >= numOfItems) {
			Log.d(TAG, "getDropDownView Extra: " + (position - numOfItems));
			return getExtrasView(position - numOfItems, parent);
		}
		else {
			Log.d(TAG, "getDropDownView before: " + (position - numOfItems));
			return super.getDropDownView(position, convertView, parent);
		}
	}

	@Override
	public int getCount() {
		//Log.d(TAG, "Returning false count");
		numOfItems = super.getCount();
		Log.d(TAG, "NumOfItems: " +numOfItems);
		if (numOfItems > 0) {
			return numOfItems + extraIds.length;
		} else {
			return numOfItems;
		}
	}
}
