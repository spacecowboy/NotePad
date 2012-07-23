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
import android.os.Bundle;

/**
 * Each page in the view pager displays a different list which is handled by a different cursor adapter
 *
 */
public class ListPagerAdapter extends FragmentPagerAdapter {

	private final ExtrasCursorAdapter wrappedAdapter;
	private final int countAdjust;

	public ListPagerAdapter(Context context, final FragmentManager fm, final ExtrasCursorAdapter wrappedAdapter, final int countAdjust) {
		super(context, fm);
		this.wrappedAdapter = wrappedAdapter;
		this.countAdjust = countAdjust;
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
				title = c.getString(c.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
			} else {
				title = wrappedAdapter.getExtraItem(position);
			}
		}
		
		return title;
		}

}
