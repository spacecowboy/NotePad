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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * A custom ScrollView that can accept a scroll listener.
 */
public class ObservableScrollView extends ScrollView {
	private Callbacks mCallbacks;

	public ObservableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if (mCallbacks != null) {
			mCallbacks.onScrollChanged();
		}
	}

	@Override
	public int computeVerticalScrollRange() {
		return super.computeVerticalScrollRange();
	}

	public void setCallbacks(Callbacks listener) {
		mCallbacks = listener;
	}

	public interface Callbacks {
		void onScrollChanged();
	}
}
