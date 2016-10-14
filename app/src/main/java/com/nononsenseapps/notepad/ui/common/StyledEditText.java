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

package com.nononsenseapps.notepad.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

import com.nononsenseapps.notepad.R;

/**
 * An EditText field that highlights the first line and makes links clickable in
 * the text. The text is still selectable, movable etc.
 * 
 */
public class StyledEditText extends EditText {

	Object titleStyleSpan;
	Object titleSizeSpan;
	Object titleFamilySpan;
	Object bodyFamilySpan;

	private float mTitleRelativeSize;
	private int mTitleFontFamily;
	private int mBodyFontFamily;
	private int mTitleFontStyle;
	private boolean mLinkify;
	private boolean mTitleLarger = true;

	public StyledEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.StyledTextView, 0, 0);

		try {
			mTitleRelativeSize = a.getFloat(
					R.styleable.StyledTextView_titleRelativeSize, 1.0f);
			mTitleFontFamily = a.getInteger(
					R.styleable.StyledTextView_titleFontFamily, 0);
			mTitleFontStyle = a.getInteger(
					R.styleable.StyledTextView_titleFontStyle, 0);
			mLinkify = a.getBoolean(R.styleable.StyledTextView_linkify, false);
			mBodyFontFamily = a.getInteger(
					R.styleable.StyledTextView_bodyFontFamily, 0);
		}
		finally {
			a.recycle();
		}

		setTitleRelativeLarger(mTitleLarger);

		setTitleFontFamily(mTitleFontFamily);

		setTitleFontStyle(mTitleFontStyle);
		
		setBodyFontFamily(mBodyFontFamily);

		// Style on change
		addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				spannify(s);
				if (mLinkify) {
					Linkify.addLinks(StyledEditText.this, Linkify.ALL);
					// Links shouldnt steal click focus
					// But text must still be selectable etc
					setMovementMethod(new ArrowKeyMovementMethod());
				}
			}
		});
	}

	/**
	 *
	 * @param larger true will use layout defined size, else same size
	 */
	public void setTitleRelativeLarger(final boolean larger) {
		mTitleLarger = larger;
		titleSizeSpan = new RelativeSizeSpan(larger ? mTitleRelativeSize : 1.0f);
	}
	
	/**
	 * 
	 * @param family matches order defined in xml
	 */
	public void setTitleFontFamily(final int family) {
		switch (family) {
		case 1:
			titleFamilySpan = new TypefaceSpan("sans-serif-condensed");
			break;
		case 2:
			titleFamilySpan = new TypefaceSpan("sans-serif-light");
			break;
		case 3:
			titleFamilySpan = new TypefaceSpan("sans-serif-thin");
			break;
		default:
			titleFamilySpan = new TypefaceSpan("sans-serif");
			break;
		}
	}

	/**
	 * 
	 * @param style
	 *        matches order defined in xml
	 */
	public void setTitleFontStyle(final int style) {
		switch (style) {
		case 1:
			titleStyleSpan = new StyleSpan(android.graphics.Typeface.BOLD);
			break;
		case 2:
			titleStyleSpan = new StyleSpan(android.graphics.Typeface.ITALIC);
			break;
		default:
			titleStyleSpan = new StyleSpan(android.graphics.Typeface.NORMAL);
			break;
		}
	}
	
	/**
	 * 
	 * @param family matches order defined in xml
	 */
	public void setBodyFontFamily(final int family) {
		switch (family) {
		case 1:
			bodyFamilySpan = new TypefaceSpan("sans-serif-condensed");
			break;
		case 2:
			bodyFamilySpan = new TypefaceSpan("sans-serif-light");
			break;
		case 3:
			bodyFamilySpan = new TypefaceSpan("sans-serif-thin");
			break;
		default:
			bodyFamilySpan = new TypefaceSpan("sans-serif");
			break;
		}
	}
	
	/**
	 * 
	 * @param size 0, 1 or 2 representing small/medium/large
	 */
	public void setTheTextSize(final int size) {
		switch (size) {
		case 0:
			// small
			super.setTextSize(14.0f);
			break;
		case 2:
			// large
			super.setTextSize(22.0f);
			break;
		case 1:
		default:
			// medium
			super.setTextSize(18.0f);
			break;
		}
	}
	
	/**
	 * 
	 * @param clickable if links should be clickable
	 */
	public void setLinkify(final boolean clickable) {
		this.mLinkify = clickable;
	}

	private void spannify(final Spannable s) {
		// Clear this first, or it will multiply!
		for (RelativeSizeSpan rs : s.getSpans(0, s.length(),
				RelativeSizeSpan.class)) {
			s.removeSpan(rs);
		}

		int titleEnd = s.toString().indexOf("\n");
		if (titleEnd < 0) {
			titleEnd = s.toString().length();
		}

		if (titleEnd > 0) {
			s.setSpan(titleStyleSpan, 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(titleSizeSpan, 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(titleFamilySpan, 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			if (titleEnd < s.toString().length()) {
				s.setSpan(bodyFamilySpan, titleEnd, s.toString().length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		TextView widget = (TextView) this;
		Object text = widget.getText();
		if (text instanceof Spanned) {
			Spannable buffer = (Spannable) text;

			int action = event.getAction();

			if (action == MotionEvent.ACTION_UP
					|| action == MotionEvent.ACTION_DOWN) {
				int x = (int) event.getX();
				int y = (int) event.getY();

				x -= widget.getTotalPaddingLeft();
				y -= widget.getTotalPaddingTop();

				x += widget.getScrollX();
				y += widget.getScrollY();

				Layout layout = widget.getLayout();
				int line = layout.getLineForVertical(y);
				int off = layout.getOffsetForHorizontal(line, x);

				ClickableSpan[] link = buffer.getSpans(off, off,
						ClickableSpan.class);

				// Cant click to the right of a span, if the line ends with the span!
				if (x > layout.getLineRight(line)) {
					// Don't call the span
				}
				else if (link.length != 0) {
					if (action == MotionEvent.ACTION_UP) {
						link[0].onClick(widget);
					}
					else if (action == MotionEvent.ACTION_DOWN) {
						Selection.setSelection(buffer,
								buffer.getSpanStart(link[0]),
								buffer.getSpanEnd(link[0]));
					}
					return true;
				}
			}

		}

		return super.onTouchEvent(event);
	}
	
}
