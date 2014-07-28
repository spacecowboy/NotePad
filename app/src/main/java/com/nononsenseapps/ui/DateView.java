/*
 * Copyright (C) 2013 Jonas Kalderstam
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nononsenseapps.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.widget.TextView;

import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;

/**
 * A simple textview that can display time.
 * 
 */
public class DateView extends TextView {
	private static final int SECONDS_PER_DAY = 3600;
	// private String day = "E, d MMM";
	//public static final String day = "MMM d";
	//public static final String time = "kk:mm";

	private final Context mContext;
	
	//private final Calendar mCalendar;
	
	SimpleDateFormat mDateFormatter;

	public DateView(Context context) {
		super(context);
		this.mContext = context;
		mDateFormatter = TimeFormatter.getLocalFormatterShortDateOnly(context);
	}

	@SuppressLint("SimpleDateFormat") public DateView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
		try {
			mDateFormatter = TimeFormatter.getLocalFormatterShortDateOnly(context);
		} catch(Exception e) {
			// Just to function in view
			mDateFormatter = new SimpleDateFormat();
		}
	}

	public DateView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.mContext = context;
		mDateFormatter = TimeFormatter.getLocalFormatterShortDateOnly(context);
	}
	
	public void setTimeText(final long time) {
		super.setText(mDateFormatter.format(new Date(time)));
	}

	public static CharSequence toDate(final Context context, String time3339) {
		// TODO remove this
		Time time = new Time(Time.getCurrentTimezone());
		time.parse3339(time3339);

		// Ugg... Not beautiful... :-(
		String dateFormatMicro = "MMM d";
		String dateFormatForRes = context.getString(R.string.dateformat_micro);

		if (dateFormatForRes != null) {
			dateFormatMicro = dateFormatForRes;
		}

		return toDate(dateFormatMicro, time.toMillis(false));
	}

	public static CharSequence toDate(String format, long msecs) {
		// TODO remove this
		// String format = day;
		try {
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.setTimeInMillis(msecs);

			return DateFormat.format(format, c);
		}
		catch (Exception e) {
			return "";
		}
	}

	/**
	 * Returns the number of days between two dates. The time part of the days
	 * is ignored in this calculation, so 2007-01-01 13:00 and 2007-01-02 05:00
	 * have one day inbetween.
	 */
	public static long daysBetween(Date firstDate, Date secondDate) {
		// TODO change this
		// We only use the date part of the given dates
		long firstSeconds = truncateToDate(firstDate).getTime() / 1000;
		long secondSeconds = truncateToDate(secondDate).getTime() / 1000;
		// Just taking the difference of the millis.
		// These will not be exactly multiples of 24*60*60, since there
		// might be daylight saving time somewhere inbetween. However, we can
		// say that by adding a half day and rounding down afterwards, we always
		// get the full days.
		long difference = secondSeconds - firstSeconds;
		// Adding half a day
		if (difference >= 0) {
			difference += SECONDS_PER_DAY / 2; // plus half a day in seconds
		}
		else {
			difference -= SECONDS_PER_DAY / 2; // minus half a day in seconds
		}
		// Rounding down to days
		difference /= SECONDS_PER_DAY;

		return difference;
	}

	/**
	 * Truncates a date to the date part alone.
	 */
	public static Date truncateToDate(Date d) {
		// TODO change this
		if (d instanceof java.sql.Date) {
			return d; // java.sql.Date is already truncated to date. And raises
						// an
						// Exception if we try to set hours, minutes or seconds.
		}
		d = (Date) d.clone();
		d.setHours(0);
		d.setMinutes(0);
		d.setSeconds(0);
		d.setTime(((d.getTime() / 1000) * 1000));
		return d;
	}

}
