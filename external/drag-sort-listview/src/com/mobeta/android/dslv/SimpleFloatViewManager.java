package com.mobeta.android.dslv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Simple implementation of the FloatViewManager class. Uses list
 * items as they appear in the ListView to create the floating View.
 */
public class SimpleFloatViewManager implements DragSortListView.FloatViewManager {

	private Bitmap mFloatBitmap;

	private ImageView mImageView;

	private int mFloatBGColor = Color.BLACK;

	private ListView mListView;

	public SimpleFloatViewManager(ListView lv) {
		mListView = lv;
	}

	public void setBackgroundColor(int color) {
		mFloatBGColor = color;
	}

	/**
	 * This simple implementation creates a Bitmap copy of the
	 * list item currently shown at ListView <code>position</code>.
	 */
	@Override
	public View onCreateFloatView(int position) {
		// Guaranteed that this will not be null? I think so. Nope, got
		// a NullPointerException once...
		View v = mListView.getChildAt(position + mListView.getHeaderViewsCount() - mListView.getFirstVisiblePosition());

		if (v == null) {
			return null;
		}

		v.setPressed(false);

		// Create a copy of the drawing cache so that it does not get
		// recycled by the framework when the list tries to clean up memory
		//v.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		v.setDrawingCacheEnabled(true);

		// EDITED by CampelloManuel @ github
		if (v.getDrawingCache() == null) {
			// big views (for us, notes with ~90 lines) are too big to fit into the cache,
			// so getDrawingCache() will always return null, which causes a crash.
			// So we have to build a shorter "thumbnail" view for dragging the listitem

			// To me this seems about as high as a note with 3 lines, which is good enough
			int maxHeightInDp = 120;
			// convert to pixels
			int maxHeightEquivPixels = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, maxHeightInDp,
					mListView.getContext().getResources().getDisplayMetrics());

			// if needed, truncate the height to something that will make the view small
			// enough to fit into the cache
			int h = Math.min(v.getMeasuredHeight(), maxHeightEquivPixels);
			int w = v.getMeasuredWidth(); // the width is always OK, it seems

			Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			Canvas bitmapHolder = new Canvas(bitmap);
			v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
			v.draw(bitmapHolder);

			// we drew a smaller tumbnail of the view on this bitmap: set it as cache
			mFloatBitmap = bitmap;
		} else {
			// normal behavior of the library
			mFloatBitmap = Bitmap.createBitmap(v.getDrawingCache());
		}
		v.setDrawingCacheEnabled(false);

		if (mImageView == null) {
			mImageView = new ImageView(mListView.getContext());
		}
		mImageView.setBackgroundColor(mFloatBGColor);
		mImageView.setPadding(0, 0, 0, 0);
		mImageView.setImageBitmap(mFloatBitmap);
		mImageView.setLayoutParams(new ViewGroup.LayoutParams(v.getWidth(), v.getHeight()));

		return mImageView;
	}

	/**
	 * This does nothing
	 */
	@Override
	public void onDragFloatView(View floatView, Point position, Point touch) {
		// do nothing
	}

	/**
	 * Removes the Bitmap from the ImageView created in
	 * onCreateFloatView() and tells the system to recycle it.
	 */
	@Override
	public void onDestroyFloatView(View floatView) {
		((ImageView) floatView).setImageDrawable(null);

		mFloatBitmap.recycle();
		mFloatBitmap = null;
	}

}

