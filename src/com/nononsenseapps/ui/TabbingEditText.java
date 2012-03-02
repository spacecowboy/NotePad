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
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * And editText which catches TABs and inserts \t in the text
 *
 */
public class TabbingEditText extends EditText {
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
