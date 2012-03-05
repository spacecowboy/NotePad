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

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class Spacer extends TextView {
	private Context context;
	private final static String indent = "      ";

	public Spacer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

	public Spacer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public Spacer(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		Log.d("Spacer", "Indent." + indent + ".");
		Log.d("Spacer", "Got." + text + ".");
		int level = 0;
		if (text != null) {
			try {
			int newLevel = Integer.parseInt(text.toString());
			if (newLevel >= 0) {
				level = newLevel;
			}
			} catch (NumberFormatException e) {
				//if string cannot be parsed as an integer value.  
			}
		}
		// Now set the width
		String width = "";
		int l;
		for(l = 0; l < level; l++) {
			width += indent;
		}
		Log.d("Spacer", "Width." + width + ".");
		super.setText(width, type);
	}
}
