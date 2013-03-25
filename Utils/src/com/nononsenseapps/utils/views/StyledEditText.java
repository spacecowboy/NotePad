package com.nononsenseapps.utils.views;

import com.nononsenseapps.utils.R;

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

/**
 * An EditText field that highlights the first line and makes links clickable in
 * the text. The text is still selectable, movable etc.
 * 
 */
public class StyledEditText extends EditText {

	Object textBoldSpan;
	Object textBigSpan;
	Object textCondensedSpan;

	private float mTitleRelativeSize;
	private int mTitleFontFamily;
	private int mTitleFontStyle;
	private boolean mLinkify;

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
		} finally {
			a.recycle();
		}

		textBigSpan = new RelativeSizeSpan(mTitleRelativeSize);

		switch (mTitleFontFamily) {
		case 1:
			textCondensedSpan = new TypefaceSpan("sans-serif-condensed");
			break;
		case 2:
			textCondensedSpan = new TypefaceSpan("sans-serif-light");
			break;
		default:
			textCondensedSpan = new TypefaceSpan("sans-serif");
			break;
		}

		switch (mTitleFontStyle) {
		case 1:
			textBoldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
			break;
		case 2:
			textBoldSpan = new StyleSpan(android.graphics.Typeface.ITALIC);
			break;
		default:
			textBoldSpan = new StyleSpan(android.graphics.Typeface.NORMAL);
			break;
		}

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

	private void spannify(final Spannable s) {
		int titleEnd = s.toString().indexOf("\n");
		if (titleEnd < 0) {
			titleEnd = s.toString().length();
		}

		if (titleEnd > 0) {
			s.setSpan(textBoldSpan, 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(textBigSpan, 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(textCondensedSpan, 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
					|| action == MotionEvent.ACTION_DOWN
					) {
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

				if (link.length != 0) {
					if (action == MotionEvent.ACTION_UP) {
						link[0].onClick(widget);
					} else if (action == MotionEvent.ACTION_DOWN) {
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
