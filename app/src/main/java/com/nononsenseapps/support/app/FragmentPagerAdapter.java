/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.nononsenseapps.support.app;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.R;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

/**
 * Implementation of {@link android.support.v4.view.PagerAdapter} that
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

	@TargetApi(14)
	private void setMenuVisibility(final Fragment fragment, final boolean value) {
		if (fragment != null
				&& context.getResources().getBoolean(R.bool.atLeast14))
			fragment.setMenuVisibility(value);
	}

	@TargetApi(15)
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
