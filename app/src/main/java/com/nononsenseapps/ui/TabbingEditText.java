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
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * And editText which catches TABs and inserts \t in the text
 */
public class TabbingEditText extends androidx.appcompat.widget.AppCompatEditText {
	// Unfortunately, this doesn't work so well
	//private static final String TAB = "\t";
	// Which is why I use spaces instead
	private static final String TAB = "    ";

	public TabbingEditText(Context context) {
		super(context);
	}

	public TabbingEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TabbingEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_TAB:
				int start = getSelectionStart();
				int end = getSelectionEnd();
				getText().replace(Math.min(start, end), Math.max(start, end),
						TAB, 0, TAB.length());
				return true;
			default:
				return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_TAB:
				// This was handled in key down
				return true;
			default:
				return super.onKeyUp(keyCode, event);
		}
	}

}
