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

package com.nononsenseapps.ui;

import android.content.Context;
import android.util.AttributeSet;
import com.nononsenseapps.helpers.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.view.View.OnClickListener;

/**
 * This class is designed to act as a simple version of the touch delegate. E.g.
 * it is intended to enlarge the touch area for a specified child view.
 * 
 * Define it entirely in XML as the following example demonstrates:
 * 
 * <com.nononsenseapps.ui.DelegateFrame
        xmlns:nononsenseapps="http://nononsenseapps.com"
        android:id="@+id/datecheckcontainer"
        android:layout_width="wrap_content"
        android:layout_height="fill_parent"
        android:minWidth="44dp"
        android:paddingBottom="4dp"
        android:paddingLeft="8dp"
        android:paddingRight="4dp"
        android:paddingTop="8dp"
        android:clickable="true"
        nononsenseapps:enlargedView="@+id/itemDone" >
        
        It's important to add android:clickable="true" and 
        nononsenseapps:enlargedView="@+id/YOURIDHERE"
 *
 */
public class DelegateFrame extends RelativeLayout implements OnClickListener {
	public static final String NONONSENSEAPPSNS = "http://nononsenseapps.com";
	public static final String ATTR_ENLARGEDVIEW = "enlargedView";
	
	private int enlargedViewId;

	public DelegateFrame(Context context) {
		super(context);
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
		if (enlargedViewId > -1) {
			View enlargedView = findViewById(enlargedViewId);
			Log.d("delegate", "onTouchEvent! view is null?: " + Boolean.toString(enlargedView == null));
			if (enlargedView != null)
				enlargedView.performClick();
		}
	}
}
