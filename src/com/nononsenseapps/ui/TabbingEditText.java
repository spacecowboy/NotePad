package com.nononsenseapps.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * And editText which catches TABs and inserts \t in the text
 *
 */
public class TabbingEditText extends EditText {
	private static final String TAB = "\t";
	
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
