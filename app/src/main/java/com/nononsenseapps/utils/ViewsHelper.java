package com.nononsenseapps.utils;

import android.content.Context;
import android.util.TypedValue;

public class ViewsHelper {

	/**
	 * Convert DPs to Pixels for the current resolution
	 *
	 * @param context
	 * @param dip
	 * @return
	 */
	public static int convertDip2Pixels(Context context, int dip) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				dip, context.getResources().getDisplayMetrics());
	}
}
