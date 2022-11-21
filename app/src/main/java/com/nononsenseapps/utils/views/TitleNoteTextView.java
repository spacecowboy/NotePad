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

package com.nononsenseapps.utils.views;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.R;

/**
 * A TextView that highlights the first line and makes links clickable. The text
 * is not selectable. This is intended to be used in a ListView where the text
 * on items is not intended to be selectable.
 */
public class TitleNoteTextView extends androidx.appcompat.widget.AppCompatTextView {

	Object titleStyleSpan;
	Object titleSizeSpan;
	Object titleFamilySpan;
	Object bodyFamilySpan;

	private final int primaryColor;
	private final int secondaryColor;
	private int mBodyFontFamily;
	private int mTitleFontFamily;
	private int mTitleFontStyle;
	private boolean mLinkify;
	private String mStyledText;
	private String mTitle = "";
	private String mRest = "";

	public TitleNoteTextView(Context context, AttributeSet attrs) {
		// TODO it crashes here in the editor. check activity_task_history.xml in design mode
		super(context, attrs);

		TypedArray attributes = context
				.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.StyledTextView, 0, 0);

		float mTitleRelativeSize;
		try {
			mTitleRelativeSize = attributes
					.getFloat(R.styleable.StyledTextView_titleRelativeSize, 1.0f);
			mTitleFontFamily = attributes
					.getInteger(R.styleable.StyledTextView_titleFontFamily, 0);
			mTitleFontStyle = attributes
					.getInteger(R.styleable.StyledTextView_titleFontStyle, 0);
			mBodyFontFamily = attributes
					.getInteger(R.styleable.StyledTextView_bodyFontFamily, 0);
			mStyledText = attributes.getString(R.styleable.StyledTextView_styledText);
			mLinkify = attributes.getBoolean(R.styleable.StyledTextView_linkify, false);

			primaryColor = super.getCurrentTextColor();
			secondaryColor = attributes.getColor(R.styleable.StyledTextView_secondaryColor, primaryColor);
		} finally {
			attributes.recycle();
		}

		titleSizeSpan = new RelativeSizeSpan(mTitleRelativeSize);

		setTitleFontFamily(mTitleFontFamily);

		setTitleFontStyle(mTitleFontStyle);

		setBodyFontFamily(mBodyFontFamily);

	}

	/**
	 * Useful method for widgets where only default textviews may be used. Use
	 * this method in your widget adapter to get the functionality this view
	 * would have offered.
	 * <p/>
	 * First argument is a relative size of the first line, like 1.3
	 * <p/>
	 * Second argument is of type: android.graphics.Typeface.BOLD, ITALIC etc
	 * <p/>
	 * Third is: 0 (normal), 1 (condensed), 2 (light), 3 (thin)
	 */
	public static CharSequence getStyledText(final String title,
											 final String rest, final float titleRelSize, final int face,
											 final int font) {
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
	 * <p/>
	 * First argument is a relative size of the first line, like 1.3
	 * <p/>
	 * Second argument is of type: android.graphics.Typeface.BOLD, ITALIC etc
	 * <p/>
	 * Third is: 0 (normal), 1 (condensed), 2 (light), 3 (thin)
	 */
	public static CharSequence getStyledText(final String text,
											 final float titleRelSize, final int face, final int font) {
		if (text == null) return null;

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

	/**
	 * @param family matches order defined in xml
	 */
	public void setTitleFontFamily(final int family) {
		if (mTitleFontFamily == family)
			return;

		mTitleFontFamily = family;

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
	 * @param style matches order defined in xml
	 */
	public void setTitleFontStyle(final int style) {
		if (mTitleFontStyle == style)
			return;

		mTitleFontStyle = style;

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
	 * @param family matches order defined in xml
	 */
	public void setBodyFontFamily(final int family) {
		if (mBodyFontFamily == family)
			return;

		mBodyFontFamily = family;

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

	public void useSecondaryColor(final boolean useSecondary) {
		if (secondaryColor != primaryColor) {
			if (useSecondary) {
				super.setTextColor(secondaryColor);
			} else {
				super.setTextColor(primaryColor);
			}
		}
	}

	public String getStyledText() {
		return mStyledText;
	}

	public void setStyledText(final String styledText) {
		if (styledText != null) {
			this.mStyledText = styledText;

			try {
				int titleEnd = mStyledText.indexOf("\n");
				if (titleEnd < 0) {
					titleEnd = mStyledText.length();
				}

				// Need to link first so we can avoid the title
				if (titleEnd > 0) {
					SpannableString text = new SpannableString(mStyledText);
					text.setSpan(titleStyleSpan, 0, titleEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					text.setSpan(titleSizeSpan, 0, titleEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					text.setSpan(titleFamilySpan, 0, titleEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					if (titleEnd < mStyledText.length()) {
						text.setSpan(bodyFamilySpan, titleEnd, mStyledText.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					setText(text, BufferType.SPANNABLE);

					if (mLinkify) {
						// this makes the links clickable in the notes in the lists.
						Linkify.addLinks(this, Linkify.ALL);

						// Make sure links dont steal click focus everywhere
						setMovementMethod(null);
					}
				} else {
					// Emtpy string
					setText(new SpannableString(mStyledText), BufferType.SPANNABLE);
				}
			} catch (NullPointerException miuibug) {
				/*
				 * A bug reported on Miui Android 4.4. NullPointerException inside setText method
				 * due to some nullpointer in android.text.SpannableStringInternal.equals method.
				 * See crashlog in issue #291
				 */
				setText(mStyledText);
			}
		}
	}

	/**
	 * either opens the note or opens a link in the browser. It does not call
	 * {@link View#performClick()} because there's no need to, the default
	 * behavior is fine.
	 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!(this.getText() instanceof Spanned)) {
			return false;
		}

		int action = event.getAction();
		if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_DOWN) {
			return false;
		}

		int x = (int) event.getX();
		int y = (int) event.getY();

		x -= this.getTotalPaddingLeft();
		y -= this.getTotalPaddingTop();

		x += this.getScrollX();
		y += this.getScrollY();

		Layout layout = this.getLayout();
		int line = layout.getLineForVertical(y);
		int off = layout.getOffsetForHorizontal(line, x);

		Spannable buffer = (Spannable) this.getText();
		ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

		// Cant click to the right of a span, if the line ends with the span!
		if (x > layout.getLineRight(line)) {
			// Don't call the span
		} else if (link.length != 0) {
			switch (action) {
				case MotionEvent.ACTION_UP:
					// the user touched a link => fire a custom onClick() method
					onClickableSpanClicked(link[0]);
					break;
				case MotionEvent.ACTION_DOWN:
					// select text
					Selection.setSelection(buffer,
							buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
					break;
				default:
					// 100% impossible to reach this.
					break;
			}

			// ONLY in this case we can say that the event was handled
			return true;
		}

		return false;
	}


	/**
	 * When the user clicks on a link (in a {@link ClickableSpan}) this view will fire an intent
	 * to open the browser, using this function
	 *
	 * @param cs the {@link ClickableSpan} that the user clicked
	 */
	private void onClickableSpanClicked(ClickableSpan cs) {
		if (cs instanceof URLSpan) {
			// it's 99% similar to the code on URLSpan.java in the Android SDK, but they set
			// Browser.EXTRA_APPLICATION_ID, which tells the browser to open links always on
			// the same tab. By NOT setting it, every clicked link will be opened in a new
			// browser tab, which I prefer.
			// TODO If anyone reading this dislikes this behavior, we can add a setting to the
			//  preferences page, just open an issue on github and explain
			Uri uri = Uri.parse(((URLSpan) cs).getURL());
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);

			try {
				this.getContext().startActivity(intent);
			} catch (ActivityNotFoundException e) {
				NnnLogger.warning(TitleNoteTextView.class,
						"Could not find a browser to open the url: " + uri.toString());
			}
		} else {
			NnnLogger.warning(TitleNoteTextView.class, "ClickableSpan was not an UrlSpan");
			// it should not happen. Anyway, just call the old code from version 5.7.1
			cs.onClick(this);
		}
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

	/**
	 * @param clickable if links should be clickable
	 */
	public void setLinkify(final boolean clickable) {
		this.mLinkify = clickable;
	}

	/**
	 * @param size 0, 1 or 2 representing small/medium/large
	 */
	public void setTheTextSize(final int size) {
		switch (size) {
			case 0:
				// small
				super.setTextSize(12.0f);
				break;
			case 2:
				// large
				super.setTextSize(18.0f);
				break;
			case 1:
			default:
				// medium
				super.setTextSize(14.0f);
				break;
		}
	}
}
