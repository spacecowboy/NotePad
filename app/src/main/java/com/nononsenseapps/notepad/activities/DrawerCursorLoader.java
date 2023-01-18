package com.nononsenseapps.notepad.activities;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.TaskListFragment;
import com.nononsenseapps.ui.ExtraTypesCursorAdapter;

import java.util.ArrayList;

/**
 * Used only in {@link ActivityMain#loadLeftDrawer()}, so it's a package-private class
 */
class DrawerCursorLoader implements LoaderManager.LoaderCallbacks<Cursor> {

	final String[] COUNTROWS = new String[] { "COUNT(1)" };
	final String NOTCOMPLETED = Task.Columns.COMPLETED + " IS NULL ";

	/**
	 * the instance of {@link ActivityMain} that hosts this loader object
	 */
	Context mContext;

	/**
	 * this and {@link #mAdapter} are references to the variables in
	 * {@link ActivityMain#loadLeftDrawer()}, so it's exactly as if this code
	 * was copypasted in that function
	 */
	ArrayList<ArrayList<Object>> mExtraData;
	ExtraTypesCursorAdapter mAdapter;

	public DrawerCursorLoader(ActivityMain drawerHost, ArrayList<ArrayList<Object>> extraData,
							  ExtraTypesCursorAdapter adapter) {
		mContext = drawerHost;
		mExtraData = extraData;
		mAdapter = adapter;
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		// Normal lists
		switch (id) {
			case TaskListFragment.LIST_ID_OVERDUE:
				return new CursorLoader(mContext, Task.URI, COUNTROWS,
						NOTCOMPLETED + TaskListFragment.andWhereOverdue(),
						null, null);
			case TaskListFragment.LIST_ID_TODAY:
				return new CursorLoader(mContext, Task.URI, COUNTROWS,
						NOTCOMPLETED + TaskListFragment.andWhereToday(),
						null, null);
			case TaskListFragment.LIST_ID_WEEK:
				return new CursorLoader(mContext, Task.URI, COUNTROWS,
						NOTCOMPLETED + TaskListFragment.andWhereWeek(),
						null, null);
			case 0:
			default:
				return new CursorLoader(mContext,
						TaskList.URI_WITH_COUNT,
						new String[] { TaskList.Columns._ID, TaskList.Columns.TITLE,
								TaskList.Columns.VIEW_COUNT },
						null, null,
						mContext.getResources()
								.getString(R.string.const_as_alphabetic, TaskList.Columns.TITLE));
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> l, Cursor c) {
		switch (l.getId()) {
			case TaskListFragment.LIST_ID_OVERDUE:
				if (c.moveToFirst()) {
					updateExtra(1, c.getInt(0));
				}
				break;
			case TaskListFragment.LIST_ID_TODAY:
				if (c.moveToFirst()) {
					updateExtra(2, c.getInt(0));
				}
				break;
			case TaskListFragment.LIST_ID_WEEK:
				if (c.moveToFirst()) {
					updateExtra(3, c.getInt(0));
				}
				break;
			case 0:
			default:
				mAdapter.swapCursor(c);
		}
	}

	private void updateExtra(final int pos, final int count) {
		while (mExtraData.get(pos).size() < 2) {
			// To avoid crashes
			mExtraData.get(pos).add("0");
		}
		mExtraData.get(pos).set(1, Integer.toString(count));
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> l) {
		switch (l.getId()) {
			case TaskListFragment.LIST_ID_OVERDUE:
			case TaskListFragment.LIST_ID_TODAY:
			case TaskListFragment.LIST_ID_WEEK:
				break;
			case 0:
			default:
				mAdapter.swapCursor(null);
		}
	}

}
