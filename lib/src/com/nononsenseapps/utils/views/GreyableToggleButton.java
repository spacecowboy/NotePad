package com.nononsenseapps.utils.views;

import com.nononsenseapps.utils.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.ToggleButton;

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
