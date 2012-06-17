/*
 * Copyright (C) 2010 Eric Harlow
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

package com.ericharlow.DragNDrop;

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;

public class DragNDropListView extends ListView {

	boolean mDragMode;

	int mStartPosition;
	int mEndPosition;
	int mDragPointOffset; // Used to adjust drag view location

	ImageView mDragView;
	GestureDetector mGestureDetector;

	DropListener mDropListener;
	RemoveListener mRemoveListener;
	DragListener mDragListener;

	public DragNDropListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setDropListener(DropListener l) {
		mDropListener = l;
	}

	public void setRemoveListener(RemoveListener l) {
		mRemoveListener = l;
	}

	public void setDragListener(DragListener l) {
		mDragListener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		if (action == MotionEvent.ACTION_DOWN && onDragHolder(x, y)) {
			// x > this.getWidth() / 2) {
			mDragMode = true;
		}

		if (!mDragMode)
			return super.onTouchEvent(ev);

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mStartPosition = pointToPosition(x, y);
			if (mStartPosition != INVALID_POSITION) {
				int mItemPosition = mStartPosition - getFirstVisiblePosition();
				mDragPointOffset = y - getChildAt(mItemPosition).getTop();
				mDragPointOffset -= ((int) ev.getRawY()) - y;
				startDrag(mItemPosition, y);
				drag(0, y);// replace 0 with x if desired
			}
			break;
		case MotionEvent.ACTION_MOVE:
			drag(0, y);// replace 0 with x if desired
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
		default:
			mDragMode = false;
			mEndPosition = pointToPosition(x, y);
			stopDrag(mStartPosition - getFirstVisiblePosition());
			Log.d("dragndrop", "Start " + mStartPosition + " End "
					+ mEndPosition);
			if (mDropListener != null && mStartPosition != INVALID_POSITION
					&& mEndPosition != INVALID_POSITION)
				mDropListener.onDrop(mStartPosition, mEndPosition);
			break;
		}
		return true;
	}

	/**
	 * Given coords x and y, returns true if they are within a drag holder else
	 * false. Caution: This method makes the assumption that the drag holder is
	 * flush against the right edge of the list and spans the entire height of
	 * the list item.
	 */
	private boolean onDragHolder(int x, int y) {
		Log.d("dragndrop", "Got position: " + pointToPosition(x, y));
		final View item = getChildAt(pointToPosition(x, y)
				- getFirstVisiblePosition());
		if (item == null) {
			Log.d("dragndrop", "item was null");
			return false;
		}
		// final View dragItem = item.findViewById(R.id.dragholder);
		final View dragItem = item.findViewById(R.id.itemDate);
		if (dragItem == null) {
			Log.d("dragndrop", "dragItem was null");
			return false;
		}

		final int left = item.getRight() - dragItem.getWidth();
		Rect frame = new Rect(left, item.getTop(), item.getRight(),
				item.getBottom());
		Log.d("dragndrop", "item left " + frame.left + " right " + frame.right);
		Log.d("dragndrop", "x " + x + " y " + y);
		Log.d("dragndrop",
				"item right " + item.getRight() + " limit "
						+ (item.getRight() - dragItem.getWidth()));
		return frame.contains(x, y);
	}

	// move the drag view
	private void drag(int x, int y) {
		if (mDragView != null) {
			WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView
					.getLayoutParams();
			layoutParams.x = x;
			layoutParams.y = y - mDragPointOffset;
			WindowManager mWindowManager = (WindowManager) getContext()
					.getSystemService(Context.WINDOW_SERVICE);
			mWindowManager.updateViewLayout(mDragView, layoutParams);

			if (mDragListener != null)
				mDragListener.onDrag(x, y, null);// change null to "this" when
													// ready to use

			// If we are close to the edges, scroll the list
			if (y > (getHeight() * 6) / 7) {
				final int last = this.getLastVisiblePosition();
				if (last < this.getCount()) {
					final View v = getChildAt(last - getFirstVisiblePosition());
					if (v != null)
						this.setSelectionFromTop(last, v.getTop() - 24);
				}
			} else if (y < getHeight() / 5) {
				final int first = this.getFirstVisiblePosition();
				if (first > 0) {
					final View v = getChildAt(first - getFirstVisiblePosition());
					if (v != null)
						this.setSelectionFromTop(first, v.getTop() + 24);
				}
			}
		}
	}

	// enable the drag view for dragging
	private void startDrag(int itemIndex, int y) {
		stopDrag(itemIndex);

		View item = getChildAt(itemIndex);
		if (item == null)
			return;
		item.setDrawingCacheEnabled(true);
		if (mDragListener != null)
			mDragListener.onStartDrag(item);

		// Create a copy of the drawing cache so that it does not get recycled
		// by the framework when the list tries to clean up memory
		Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());

		WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP;
		mWindowParams.x = 0;
		mWindowParams.y = y - mDragPointOffset;

		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;

		Context context = getContext();
		ImageView v = new ImageView(context);
		v.setImageBitmap(bitmap);

		WindowManager mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		mWindowManager.addView(v, mWindowParams);
		mDragView = v;
	}

	// destroy drag view
	private void stopDrag(int itemIndex) {
		if (mDragView != null) {
			if (mDragListener != null)
				mDragListener.onStopDrag(getChildAt(itemIndex));
			mDragView.setVisibility(GONE);
			WindowManager wm = (WindowManager) getContext().getSystemService(
					Context.WINDOW_SERVICE);
			wm.removeView(mDragView);
			mDragView.setImageDrawable(null);
			mDragView = null;
		}
	}

	// private GestureDetector createFlingDetector() {
	// return new GestureDetector(getContext(), new SimpleOnGestureListener() {
	// @Override
	// public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
	// float velocityY) {
	// if (mDragView != null) {
	// int deltaX = (int)Math.abs(e1.getX()-e2.getX());
	// int deltaY = (int)Math.abs(e1.getY() - e2.getY());
	//
	// if (deltaX > mDragView.getWidth()/2 && deltaY < mDragView.getHeight()) {
	// mRemoveListener.onRemove(mStartPosition);
	// }
	//
	// stopDrag(mStartPosition - getFirstVisiblePosition());
	//
	// return true;
	// }
	// return false;
	// }
	// });
	// }
}
