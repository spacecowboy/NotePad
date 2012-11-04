/**
 * 
 */
package com.nononsenseapps.ui;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotesListFragment;
import com.nononsenseapps.support.app.FragmentPagerAdapter;
import com.nononsenseapps.support.app.FragmentStatePagerAdapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.widget.CursorAdapter;

/**
 * Each page in the view pager displays a different list which is handled by a
 * different cursor adapter
 * 
 */
public class ListPagerAdapter extends FragmentPagerAdapter {

	private final CursorAdapter wrappedAdapter;
	private final DataSetObserver subObserver;

	public ListPagerAdapter(Context context, final FragmentManager fm,
			final CursorAdapter wrappedAdapter) {
		super(context, fm);
		this.wrappedAdapter = wrappedAdapter;

		subObserver = new DataSetObserver() {
			@Override
			public void onChanged() {
				notifyDataSetChanged();
			}

			@Override
			public void onInvalidated() {
				// Probably destroying the loader
			}
		};

		if (wrappedAdapter != null)
			wrappedAdapter.registerDataSetObserver(subObserver);

	}

	@Override
	public Fragment getItem(int pos) {
		long id = getItemId(pos);
		Log.d("pagebug", "getitem: " + pos + " : " + id);
		if (id < 0)
			return null;
		Bundle arguments = new Bundle();
		arguments.putLong(NotesListFragment.LISTID, id);
		NotesListFragment list = new NotesListFragment();
		list.setArguments(arguments);
		return list;
	}

	@Override
	public long getItemId(int position) {
		long id = -1;
		if (wrappedAdapter != null) {
			Cursor c = (Cursor) wrappedAdapter.getItem(position);
			if (c != null && !c.isAfterLast() && !c.isBeforeFirst()) {
				id = c
						.getLong((c
								.getColumnIndex(NotePad.Lists._ID)));
			}
		}
		Log.d("pagebug", "getItemId: " + position + " = " + id);
		return id;
	}

	@Override
	public int getCount() {
		return wrappedAdapter.getCount();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		if (position >= getCount())
			return null;
		CharSequence title = null;
		if (wrappedAdapter != null) {
			Cursor c = (Cursor) wrappedAdapter.getItem(position);
			if (c != null && !c.isAfterLast() && !c.isBeforeFirst()) {
				title = c.getString(c
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
			}
		}

		return title;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Called when the host view is attempting to determine if an item's
	 * position has changed. Returns POSITION_UNCHANGED if the position of the
	 * given item has not changed or POSITION_NONE if the item is no longer
	 * present in the adapter.
	 * 
	 * Argument is the object previously returned by instantiateItem
	 */
	@Override
	public int getItemPosition(Object object) {
		Fragment f = (Fragment) object;

		long listId = f.getArguments().getLong(NotesListFragment.LISTID);
		int deleted = 1;

		int length = getCount();
		int result = POSITION_NONE;
		int position;
		for (position = 0; position < length; position++) {
			if (listId == getItemId(position)) {
				if (wrappedAdapter != null) {
					Cursor c = (Cursor) wrappedAdapter.getItem(position);
					if (c != null && !c.isAfterLast() && !c.isBeforeFirst()) {
						deleted = c
								.getInt((c
										.getColumnIndex(NotePad.Lists.COLUMN_NAME_DELETED)));
					}
				}
				if (deleted == 0)
					result = position;
				break;
			}
		}
		
		Log.d("pagebug", "itemposition: " + + listId + " = " + result);
		return result;
	}
}
