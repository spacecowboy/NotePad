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

package com.nononsenseapps.scrolltricks;


import androidx.fragment.app.Fragment;

import android.view.View;

/**
 * A simple fragment that handles the quick return bindings. Just implement the
 * methods.
 *
 * Make sure to have a layout much like the one seen in layout/quick_return_example.xml
 */
public abstract class QuickReturnFragment extends Fragment implements ObservableScrollView.Callbacks {
	private static final int STATE_ONSCREEN = 0;
	private static final int STATE_OFFSCREEN = 1;
	private static final int STATE_RETURNING = 2;

	private View mQuickReturnView;
	private View mPlaceholderView;
	private ObservableScrollView mObservableScrollView;
	private int mMinRawY = 0;
	private int mState = STATE_ONSCREEN;
	private int mQuickReturnHeight;
	private int mCachedVerticalScrollRange;

	public void setupQuickReturnView() {
		mObservableScrollView = getScrollView();
		mObservableScrollView.setCallbacks(this);

		mQuickReturnView = getQuickReturnView();
		mPlaceholderView = getPlaceHolderView();

		mObservableScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
				() -> {
					onScrollChanged();
					mCachedVerticalScrollRange = mObservableScrollView.computeVerticalScrollRange();
					mQuickReturnHeight = mQuickReturnView.getHeight();
				});
	}

	protected abstract ObservableScrollView getScrollView();

	protected abstract View getQuickReturnView();

	protected abstract View getPlaceHolderView();

	@Override
	public void onScrollChanged() {
		int rawY = mPlaceholderView.getTop() - Math.min(
				mCachedVerticalScrollRange - mObservableScrollView.getHeight(),
				mObservableScrollView.getScrollY());
		int translationY = 0;

		switch (mState) {
			case STATE_OFFSCREEN:
				if (rawY <= mMinRawY) {
					mMinRawY = rawY;
				} else {
					mState = STATE_RETURNING;
				}
				translationY = rawY;
				break;

			case STATE_ONSCREEN:
				if (rawY < -mQuickReturnHeight) {
					mState = STATE_OFFSCREEN;
					mMinRawY = rawY;
				}
				translationY = rawY;
				break;

			case STATE_RETURNING:
				translationY = (rawY - mMinRawY) - mQuickReturnHeight;
				if (translationY > 0) {
					translationY = 0;
					mMinRawY = rawY - mQuickReturnHeight;
				}

				if (rawY > 0) {
					mState = STATE_ONSCREEN;
					translationY = rawY;
				}

				if (translationY < -mQuickReturnHeight) {
					mState = STATE_OFFSCREEN;
					mMinRawY = rawY;
				}
				break;
		}

		mQuickReturnView.setTranslationY(translationY);
	}
}
