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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ExtrasCursorAdapter extends SimpleCursorAdapter {

	private static final String TAG = "ExtrasCursorAdapter";

	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.from = from;
		this.to = to;
		this.layout = layout;
	}

	public ExtrasCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.from = from;
		this.to = to;
		this.layout = layout;
	}

	protected String[] from;
	protected int[] to;
	protected int layout;

	boolean lastItem = false;
	private int numOfItems = 0;

	@Override
	public Cursor swapCursor(Cursor newCursor) {
		return super.swapCursor(newCursor);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TextView viewName = (TextView) view.findViewById(UI_TEXT_ID);
		// if (viewName != null && cursor != null)
		// viewName.setText(cursor.getString(nameColumnIndex));
		if (lastItem) {
			Log.d(TAG, "last item!");
			TextView text = (TextView) view.findViewById(android.R.id.text1);
			if (text != null) {
				text.setText("This is my final item");
				view.setId(-2);
			}
			lastItem = false;
		} else {
			Log.d(TAG, "Not last item");
			super.bindView(view, context, cursor);
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == numOfItems - 1) {
			Log.d(TAG, "getView Last");
			lastItem = true;
			position--; // Return one before, we'll handle it internally
		}
		else {
			Log.d(TAG, "getView before");
			lastItem = false;
		}
		return super.getView(position, convertView, parent);
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (position == numOfItems - 1) {
			Log.d(TAG, "getDropDownView Last");
			lastItem = true;
			position--; // Return one before, we'll handle it internally
		}
		else {
			Log.d(TAG, "getDropDownView before");
			lastItem = false;
		}
		return super.getDropDownView(position, convertView, parent);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// return inflater.inflate(R.layout.actionbar_dropdown_item, parent,
		// true);
		return super.newView(context, cursor, parent);
	}

	@Override
	public int getCount() {
		Log.d(TAG, "Returning false count");
		int count = super.getCount();
		
		if (count > 0) {
			numOfItems = count + 1;
			return count + 1;
		} else {
			numOfItems = count;
			return count;
		}
	}
}
