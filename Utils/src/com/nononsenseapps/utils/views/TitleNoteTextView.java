package com.nononsenseapps.utils.views;

import com.nononsenseapps.utils.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * A TextView that highlights the first line and makes links clickable. The text
 * is not selectable. This is intended to be used in a ListView where the text
 * on items is not intended to be selectable.
 * 
 */
public class TitleNoteTextView extends TextView {

	Object textBoldSpan;
	Object textBigSpan;
	Object textCondensedSpan;

	private float mTitleRelativeSize;
	private int mTitleFontFamily;
	private int mTitleFontStyle;
	private boolean mLinkify;
	private String mStyledText;
	private String mTitle = "";
	private String mRest = "";

	public TitleNoteTextView(Context context, AttributeSet attrs) {
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
			mStyledText = a.getString(R.styleable.StyledTextView_styledText);
			mLinkify = a.getBoolean(R.styleable.StyledTextView_linkify, false);
		}
		finally {
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
		case 3:
			textCondensedSpan = new TypefaceSpan("sans-serif-thin");
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
	}

	public String getStyledText() {
		return mStyledText;
	}

	public void setStyledText(final String styledText) {
		if (styledText != null) {
			this.mStyledText = styledText;

			int titleEnd = mStyledText.indexOf("\n");
			if (titleEnd < 0) {
				titleEnd = mStyledText.length();
			}

			SpannableString ss = new SpannableString(mStyledText);
			if (titleEnd > 0) {
				ss.setSpan(textBoldSpan, 0, titleEnd,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ss.setSpan(textBigSpan, 0, titleEnd,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ss.setSpan(textCondensedSpan, 0, titleEnd,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}

			setText(ss, BufferType.SPANNABLE);

			if (mLinkify) {
				Linkify.addLinks(this, Linkify.ALL);
				// Make sure links dont steal click focus everywhere
				setMovementMethod(null);
			}
		}
	}
	
	/**
	 * Useful method for widgets where only default textviews may be used. Use
	 * this method in your widget adapter to get the functionality this view
	 * would have offered.
	 * 
	 * First argument is a relative size of the first line, like 1.3
	 * 
	 * Second argument is of type: android.graphics.Typeface.BOLD, ITALIC etc
	 * 
	 * Third is: 0 (normal), 1 (condensed), 2 (light), 3 (thin)
	 */
	public static CharSequence getStyledText(final String title, final String rest, final float titleRelSize, final int face, final int font) {
		final StringBuilder textBuilder = new StringBuilder(title);
		if (rest.length() > 0) {
			textBuilder.append("\n").append(rest);
		}
		return getStyledText(textBuilder.toString(), titleRelSize, face, font);
	}

	/**
	 * Useful method for widgets where only default textviews may be used. Use
	 * this method in your widget adapter to get the functionality this view
	 * would have offered.
	 * 
	 * First argument is a relative size of the first line, like 1.3
	 * 
	 * Second argument is of type: android.graphics.Typeface.BOLD, ITALIC etc
	 * 
	 * Third is: 0 (normal), 1 (condensed), 2 (light), 3 (thin)
	 */
	public static CharSequence getStyledText(final String text, final float titleRelSize, final int face, final int font) {
		if (text == null)
			return null;
		
		int titleEnd = text.indexOf("\n");
		if (titleEnd < 0) {
			titleEnd = text.length();
		}
		
		TypefaceSpan fontSpan;
		switch (font) {
		case 1:
			fontSpan = new TypefaceSpan("sans-serif-condensed");
			break;
		case 2:
			fontSpan = new TypefaceSpan("sans-serif-light");
			break;
		case 3:
			fontSpan = new TypefaceSpan("sans-serif-thin");
			break;
		default:
			fontSpan = new TypefaceSpan("sans-serif");
			break;
		}

		SpannableString ss = new SpannableString(text);
		if (titleEnd > 0) {
			ss.setSpan(new StyleSpan(face), 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			ss.setSpan(new RelativeSizeSpan(titleRelSize), 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			ss.setSpan(fontSpan, 0, titleEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		return ss;
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

				if (link.length != 0) {
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

		return false;
	}

	public String getTextRest() {
		return mRest;
	}

	public void setTextRest(final String rest) {
		if (rest != null) {
			this.mRest = rest;
			// Make sure it starts with a new line
			if (mRest.length() > 0) {
				mRest = (rest.startsWith("\n") ? "" : "\n") + rest;
			}

			setStyledText(mTitle + mRest);
		}
	}

	public String getTextTitle() {
		return mTitle;
	}

	public void setTextTitle(final String title) {
		if (title != null) {
			// Make sure it does not end with a newline
			this.mTitle = (title.endsWith("\n") ? title.substring(0,
					title.length() - 1) : title);

			setStyledText(mTitle + mRest);
		}
	}
}
