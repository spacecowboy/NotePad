/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.support.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.R;

/**
 * Implementation of {@link androidx.viewpager.widget.PagerAdapter} that
 * represents each page as a {@link Fragment} that is persistently kept in the
 * fragment manager as long as the user can return to the page.
 *
 * <p>
 * This version of the pager is best for use when there are a handful of
 * typically more static fragments to be paged through, such as a set of tabs.
 * The fragment of each page the user visits will be kept in memory, though its
 * view hierarchy may be destroyed when not visible. This can result in using a
 * significant amount of memory since fragment instances can hold on to an
 * arbitrary amount of state. For larger sets of pages, consider
 * {@link FragmentStatePagerAdapter}.
 *
 * <p>
 * When using FragmentPagerAdapter the host ViewPager must have a valid ID set.
 * </p>
 *
 * <p>
 * Subclasses only need to implement {@link #getItem(int)} and
 * {@link #getCount()} to have a working adapter.
 *
 * <p>
 * Here is an example implementation of a pager containing fragments of lists:
 *
 * {@sample
 * development/samples/Support4Demos/src/com/example/android/supportv4/app/
 * FragmentPagerSupport.java complete}
 *
 * <p>
 * The <code>R.layout.fragment_pager</code> resource of the top-level fragment
 * is:
 *
 * {@sample development/samples/Support4Demos/res/layout/fragment_pager.xml
 * complete}
 *
 * <p>
 * The <code>R.layout.fragment_pager_list</code> resource containing each
 * individual fragment's layout is:
 *
 * {@sample development/samples/Support4Demos/res/layout/fragment_pager_list.xml
 * complete}
 */
public abstract class FragmentPagerAdapter extends PagerAdapter {
	private static final String TAG = "FragmentPagerAdapter";

	private final FragmentManager mFragmentManager;
	private FragmentTransaction mCurTransaction = null;
	private Fragment mCurrentPrimaryItem = null;
	private final Context context;

	public Fragment getCurrentPrimaryItem() {
		return mCurrentPrimaryItem;
	}

	public FragmentPagerAdapter(Context context, FragmentManager fm) {
		this.context = context;
		mFragmentManager = fm;
	}

	/**
	 * Return the Fragment associated with a specified position.
	 */
	public abstract Fragment getItem(int position);

//	@Override
//	public void startUpdate(ViewGroup container) {
//		
//	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}

		final long itemId = getItemId(position);

		// Do we already have this fragment?
		String name = makeFragmentName(container.getId(), itemId);
		Fragment fragment = mFragmentManager.findFragmentByTag(name);
		if (fragment != null) {
			Log.d(TAG, "Attaching item #" + itemId + ": f=" + fragment);
			mCurTransaction.attach(fragment);
		} else {
			fragment = getItem(position);
			Log.d(TAG, "Adding item #" + itemId + ": f=" + fragment);
			mCurTransaction.add(container.getId(), fragment,
					makeFragmentName(container.getId(), itemId));
		}
		// if (fragment != mCurrentPrimaryItem) {
		setMenuVisibility(fragment, false);
		setUserVisibleHint(fragment, false);
		// }

		return fragment;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}
		Log.d(TAG, "Detaching item #" + getItemId(position) + ": f=" + object
				+ " v=" + ((Fragment) object).getView());
		mCurTransaction.detach((Fragment) object);
	}

	private void setMenuVisibility(final Fragment fragment, final boolean value) {
		if (fragment != null
				&& context.getResources().getBoolean(R.bool.atLeast14))
			fragment.setMenuVisibility(value);
	}

	private void setUserVisibleHint(final Fragment fragment, final boolean value) {
		if (fragment != null
				&& context.getResources().getBoolean(R.bool.atLeast15))
			fragment.setUserVisibleHint(value);
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		Fragment fragment = (Fragment) object;
		if (fragment != mCurrentPrimaryItem) {
			if (mCurrentPrimaryItem != null) {
				setMenuVisibility(mCurrentPrimaryItem, false);
				setUserVisibleHint(mCurrentPrimaryItem, false);
			}
			if (fragment != null) {
				setMenuVisibility(fragment, true);
				setUserVisibleHint(fragment, true);
			}
			mCurrentPrimaryItem = fragment;
		}
	}

	@Override
	public void finishUpdate(ViewGroup container) {
		if (mCurTransaction != null) {
			mCurTransaction.commitAllowingStateLoss();
			mCurTransaction = null;
			mFragmentManager.executePendingTransactions();
		}
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return ((Fragment) object).getView() == view;
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {
	}

	/**
	 * Return a unique identifier for the item at the given position.
	 *
	 * <p>
	 * The default implementation returns the given position. Subclasses should
	 * override this method if the positions of items can change.
	 * </p>
	 *
	 * @param position Position within this adapter
	 * @return Unique identifier for the item at position
	 */
	public abstract long getItemId(int position);

	private static String makeFragmentName(int viewId, long id) {
		return "android:switcher:" + viewId + ":" + id;
	}
}
