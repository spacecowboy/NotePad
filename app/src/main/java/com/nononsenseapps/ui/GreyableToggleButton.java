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

import androidx.appcompat.widget.AppCompatToggleButton;

import com.nononsenseapps.notepad.R;

/**
 * A Checkbox like textview. It displays its 2 states by toggling between two
 * textcolors. Secondary color is grey. Used in {@link WeekDaysView}
 */
public class GreyableToggleButton extends AppCompatToggleButton {

	// text colors for checked & unchecked status
	private final int primaryColor;
	private final int secondaryColor;

	public GreyableToggleButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		primaryColor = super.getCurrentTextColor();
		// defTextSize = super.getTextSize();
		secondaryColor = getResources().getColor(R.color.uncheckedGrey);
		// max size defaults to the initially specified text size unless it is too small
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
	}

}
