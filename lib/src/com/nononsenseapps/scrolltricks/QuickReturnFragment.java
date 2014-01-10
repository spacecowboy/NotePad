/*
 * Copyright 2012 Roman Nurik + Nick Butcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.scrolltricks;


import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * A simple fragment that handles the quick return bindings. Just implement the
 * methods.
 * 
 * Make sure to have a layout much like the one seen in layout/quick_return_example.xml
 *
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
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        onScrollChanged();
                        mCachedVerticalScrollRange = mObservableScrollView.computeVerticalScrollRange();
                        mQuickReturnHeight = mQuickReturnView.getHeight();
                    }
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
