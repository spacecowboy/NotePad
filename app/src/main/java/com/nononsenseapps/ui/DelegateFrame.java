/*
 * Copyright (C) 2012 Jonas Kalderstam
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

package com.nononsenseapps.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.nononsenseapps.helpers.Log;

/**
 * This class is designed to act as a simple version of the touch delegate. E.g.
 * it is intended to enlarge the touch area for a specified child view.
 *
 * Define it entirely in XML as the following example demonstrates:
 *
 * <com.nononsenseapps.ui.DelegateFrame
 * xmlns:nononsenseapps="http://nononsenseapps.com"
 * android:id="@+id/datecheckcontainer"
 * android:layout_width="wrap_content"
 * android:layout_height="fill_parent"
 * android:minWidth="44dp"
 * android:paddingBottom="4dp"
 * android:paddingLeft="8dp"
 * android:paddingRight="4dp"
 * android:paddingTop="8dp"
 * android:clickable="true"
 * nononsenseapps:enlargedView="@+id/itemDone" >
 *
 * It's important to add android:clickable="true" and
 * nononsenseapps:enlargedView="@+id/YOURIDHERE"
 */
public class DelegateFrame extends RelativeLayout implements OnClickListener {
	public static final String NONONSENSEAPPSNS = "http://nononsenseapps.com";
	public static final String ATTR_ENLARGEDVIEW = "enlargedView";

	private int enlargedViewId;
	private View cachedView;

	public DelegateFrame(Context context) {
		super(context, null);
	}

	public DelegateFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		setValuesFromXML(attrs);
	}

	public DelegateFrame(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setValuesFromXML(attrs);
	}

	private void setValuesFromXML(AttributeSet attrs) {
		enlargedViewId = attrs.getAttributeResourceValue(NONONSENSEAPPSNS, ATTR_ENLARGEDVIEW, -1);
		Log.d("delegate", "setting xml values! view: " + enlargedViewId);
		setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int UNDEFINED = -1;
		if (cachedView == null && enlargedViewId != UNDEFINED) {
			cachedView = findViewById(enlargedViewId);
		}
		Log.d("delegate", "onTouchEvent! view is null?: " + Boolean.toString(cachedView == null));
		if (cachedView != null) {
			cachedView.performClick();
		}
	}
}
