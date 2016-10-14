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
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import com.nononsenseapps.notepad.R;

/**
 * A TextView that highlights the first line and makes links clickable. The text
 * is not selectable. This is intended to be used in a ListView where the text
 * on items is not intended to be selectable.
 */
public class TitleNoteTextView extends TextView {

    Object titleStyleSpan;
    Object titleSizeSpan;
    Object titleFamilySpan;
    Object bodyFamilySpan;

    private int primaryColor;
    private int secondaryColor;
    private float mTitleRelativeSize;
    private int mBodyFontFamily;
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
            mBodyFontFamily = a.getInteger(
                    R.styleable.StyledTextView_bodyFontFamily, 0);
            mStyledText = a.getString(R.styleable.StyledTextView_styledText);
            mLinkify = a.getBoolean(R.styleable.StyledTextView_linkify, false);
            primaryColor = super.getCurrentTextColor();
            secondaryColor = a.getColor(
                    R.styleable.StyledTextView_secondaryColor, primaryColor);
        } finally {
            a.recycle();
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
                } else if (link.length != 0) {
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
