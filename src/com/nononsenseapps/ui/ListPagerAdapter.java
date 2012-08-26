/**
 * 
 */
package com.nononsenseapps.ui;

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

/**
 * Each page in the view pager displays a different list which is handled by a
 * different cursor adapter
 * 
 */
public class ListPagerAdapter extends FragmentPagerAdapter {

	private final ExtrasCursorAdapter wrappedAdapter;
	private final int countAdjust;
	private final DataSetObserver subObserver;

	public ListPagerAdapter(Context context, final FragmentManager fm,
			final ExtrasCursorAdapter wrappedAdapter, final int countAdjust) {
		super(context, fm);
		this.wrappedAdapter = wrappedAdapter;
		this.countAdjust = countAdjust;

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
		long id = wrappedAdapter.getItemId(pos);
		Bundle arguments = new Bundle();
		arguments.putLong(NotesListFragment.LISTID, id);
		NotesListFragment list = new NotesListFragment();
		list.setArguments(arguments);
		return list;
	}

	@Override
	public long getItemId(int position) {
		return wrappedAdapter.getItemId(position);
	}

	@Override
	public int getCount() {
		return wrappedAdapter.getCount() + countAdjust;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		CharSequence title = null;
		if (wrappedAdapter != null) {
			Cursor c = (Cursor) wrappedAdapter.getItem(position);
			if (c != null && !c.isAfterLast() && !c.isBeforeFirst()) {
				title = c.getString(c
						.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
			} else {
				title = wrappedAdapter.getExtraItem(position);
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
		
		int length = wrappedAdapter.getCount();
		int position;
		for (position = 0; position < length; position++) {
			if (listId == wrappedAdapter.getItemId(position)) {
				break;
			}
		}
		if (position == length) {
			// Happens both if list is empty
			// and if id is -1
			position = POSITION_NONE;
		}
		return position;
	}
}
