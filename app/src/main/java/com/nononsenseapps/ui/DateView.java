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

package com.nononsenseapps.ui;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.nononsenseapps.helpers.TimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * A simple textview that can display time.
 */
public class DateView extends AppCompatTextView {

	// TODO everything in this "ui" namespace could be moved to its own
	//  gradle module. This would speed up builds, but maybe it's harder to manage?

	private static final int SECONDS_PER_DAY = 3600;

	SimpleDateFormat mDateFormatter;

	public DateView(Context context) {
		super(context);

		// TODO if you want to also show a "due time" on the note, use this instead:
		//  mDateFormatter = TimeFormatter.getLocalFormatterShort(context);
		//  as of now we only show the date, which for me is good enough.
		//  (note that this line is called many times in this file)
		mDateFormatter = TimeFormatter.getLocalFormatterShortDateOnly(context);
	}

	public DateView(Context context, AttributeSet attrs) {
		super(context, attrs);

		try {
			mDateFormatter = TimeFormatter.getLocalFormatterShortDateOnly(context);
		} catch (Exception e) {
			// Just to function in view
			mDateFormatter = new SimpleDateFormat();
		}
	}

	public DateView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mDateFormatter = TimeFormatter.getLocalFormatterShortDateOnly(context);
	}

	public void setTimeText(final long time) {
		super.setText(mDateFormatter.format(new Date(time)));
	}

	public static CharSequence toDate(String format, long msecs) {
		// TODO remove this
		// String format = day;
		try {
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.setTimeInMillis(msecs);

			return DateFormat.format(format, c);
		} catch (Exception e) {
			return "";
		}
	}
}
