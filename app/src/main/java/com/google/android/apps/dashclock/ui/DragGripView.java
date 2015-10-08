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

package com.google.android.apps.dashclock.ui;

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

public class DragGripView extends View {
    private static final int[] ATTRS = new int[]{
            android.R.attr.gravity,
            android.R.attr.color,
    };

    private static final int HORIZ_RIDGES = 2;

    private int mGravity = Gravity.END;
    private int mColor = 0x33333333;

    private Paint mRidgePaint;

    private float mRidgeSize;
    private float mRidgeGap;

    private int mWidth;
    private int mHeight;

    public DragGripView(Context context) {
        this(context, null, 0);
    }

    public DragGripView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragGripView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        mGravity = a.getInteger(0, mGravity);
        mColor = a.getColor(1, mColor);
        a.recycle();

        final Resources res = getResources();
        mRidgeSize = res.getDimensionPixelSize(R.dimen.drag_grip_ridge_size);
        mRidgeGap = res.getDimensionPixelSize(R.dimen.drag_grip_ridge_gap);

        mRidgePaint = new Paint();
        mRidgePaint.setColor(mColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                View.resolveSize(
                        (int) (HORIZ_RIDGES * (mRidgeSize + mRidgeGap) - mRidgeGap)
                                + getPaddingLeft() + getPaddingRight(),
                        widthMeasureSpec),
                View.resolveSize(
                        (int) mRidgeSize,
                        heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float drawWidth = HORIZ_RIDGES * (mRidgeSize + mRidgeGap) - mRidgeGap;
        float drawLeft;

        //getLayoutDirection()
        switch (Gravity.getAbsoluteGravity(mGravity, LAYOUT_DIRECTION_LTR)
                & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                drawLeft = getPaddingLeft()
                        + ((mWidth - getPaddingLeft() - getPaddingRight()) - drawWidth) / 2;
                break;
            case Gravity.RIGHT:
                drawLeft = getWidth() - getPaddingRight() - drawWidth;
                break;
            default:
                drawLeft = getPaddingLeft();
        }

        int vertRidges = (int) ((mHeight - getPaddingTop() - getPaddingBottom() + mRidgeGap)
                / (mRidgeSize + mRidgeGap));
        float drawHeight = vertRidges * (mRidgeSize + mRidgeGap) - mRidgeGap;
        float drawTop = getPaddingTop()
                + ((mHeight - getPaddingTop() - getPaddingBottom()) - drawHeight) / 2;

        for (int y = 0; y < vertRidges; y++) {
            for (int x = 0; x < HORIZ_RIDGES; x++) {
                canvas.drawRect(
                        drawLeft + x * (mRidgeSize + mRidgeGap),
                        drawTop + y * (mRidgeSize + mRidgeGap),
                        drawLeft + x * (mRidgeSize + mRidgeGap) + mRidgeSize,
                        drawTop + y * (mRidgeSize + mRidgeGap) + mRidgeSize,
                        mRidgePaint);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
    }
}