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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatToggleButton;

import com.nononsenseapps.notepad.R;

/**
 * A Checkbox like textview. It displays its 2 states by toggling between 2 text
 * colors: the primary text color and grey. Used in {@link WeekDaysView}
 */
public class GreyableToggleButton extends AppCompatToggleButton {

	// text colors for checked & unchecked status
	private final ColorStateList primaryColor;
	private final int secondaryColor;

	/**
	 * See also "GreyableButtonToggle" in styles.xml
	 */
	public GreyableToggleButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		primaryColor = getTextColorPrimary(context);
		secondaryColor = getResources().getColor(R.color.uncheckedGrey);
	}

	/**
	 * Correct and mandatory way to get the text color for the "selected" state
	 */
	private static ColorStateList getTextColorPrimary(Context context) {
		TypedArray a = context
				.obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary });
		ColorStateList color = a.getColorStateList(a.getIndex(0));
		a.recycle();
		return color;
	}

	@Override
	public void setChecked(boolean checked) {
		super.setChecked(checked);
		// Set correct text color
		if (checked) {
			super.setTextColor(primaryColor);
		} else {
			super.setTextColor(secondaryColor);
		}
		// note that the code that reacts to days being pressed in the reminder view is
		// not here, it's in NotificationItemHelper.java and WeekDaysView.java
	}

}
