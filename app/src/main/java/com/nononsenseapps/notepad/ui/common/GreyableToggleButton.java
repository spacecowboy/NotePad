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
import android.util.AttributeSet;
import android.widget.ToggleButton;

import com.nononsenseapps.notepad.R;

/**
 * A Checkbox like textview. It displays it's two states by toggling between two
 * textcolors.
 * 
 * Default secondary color is grey.
 * 
 */
public class GreyableToggleButton extends ToggleButton {

	private int primaryColor;
	private int secondaryColor;

//	private float defTextSize = 16.0f;
//	private int currentWidth = 0;
	
	//Attributes
    //private Paint mTestPaint;

	public GreyableToggleButton(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.GreyableToggleButton, 0, 0);

		try {
			primaryColor = super.getCurrentTextColor();
			// defTextSize = super.getTextSize();

			secondaryColor = a.getColor(
					R.styleable.GreyableToggleButton_secondaryColor,
					getResources().getColor(R.color.uncheckedGrey));
		}
		finally {
			a.recycle();
		}
		
//		mTestPaint = new Paint();
//        mTestPaint.set(this.getPaint());
        //max size defaults to the initially specified text size unless it is too small
	}

	@Override
	public void setChecked(boolean checked) {
		super.setChecked(checked);

		// Set correct color
		if (secondaryColor != primaryColor) {
			if (checked) {
				super.setTextColor(primaryColor);
			}
			else {
				super.setTextColor(secondaryColor);
			}
		}
	}

//	@Override
//	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//		super.onSizeChanged(w, h, oldw, oldh);
//		
//		if (w != 0) {
//			currentWidth  = w;
//			shrinkTextToFit();
//		}
//	}
//
//
//	private void shrinkTextToFit() {
//		if (currentWidth == 0)
//			return;
//
//		final float densityMultiplier = getResources().getDisplayMetrics().density;
//		final CharSequence text = getText();
//		float textSize = defTextSize; //* densityMultiplier;
//		setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
//
//		final float minimumTextSize = 12;// * densityMultiplier;
//		while (text != (TextUtils.ellipsize(text, getPaint(),
//				currentWidth, TextUtils.TruncateAt.END))) {
//			textSize -= 1;
//			if (textSize < minimumTextSize) {
//				break;
//			}
//			else {
//				setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
//			}
//		}
//	}
//	
//	/* Re size the font so the specified text fits in the text box
//     * assuming the text box is the specified width.
//     */
//    private void refitText(String text, int textWidth) 
//    { 
//        if (textWidth <= 0)
//            return;
//        int targetWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
//        float hi = 100;
//        float lo = 2;
//        final float threshold = 0.5f; // How close we have to be
//
//        mTestPaint.set(this.getPaint());
//
//        while((hi - lo) > threshold) {
//            float size = (hi+lo)/2;
//            mTestPaint.setTextSize(size);
//            if(mTestPaint.measureText(text) >= targetWidth) 
//                hi = size; // too big
//            else
//                lo = size; // too small
//        }
//        // Use lo so that we undershoot rather than overshoot
//        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, lo);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
//    {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
//        int height = getMeasuredHeight();
//        refitText(this.getText().toString(), parentWidth);
//        this.setMeasuredDimension(parentWidth, height);
//    }
//
//    @Override
//    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
//        refitText(text.toString(), this.getWidth());
//    }
//
//    @Override
//    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
//        if (w != oldw) {
//            refitText(this.getText().toString(), w);
//        }
//    }
}
