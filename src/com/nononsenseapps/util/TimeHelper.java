package com.nononsenseapps.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.text.format.Time;
import android.util.Log;

/**
 * Has a few helper functions for dealing with dates.
 * Google Tasks API is crap because it throws away the time part
 * and sets to UTC.
 * @author Jonas
 *
 */
public class TimeHelper {
	
	// 2012-12-31
	public final static String dateFormat = "%Y-%m-%d";
	
	/**
	 * Today = 2012-12-30
	 * Returns 2012-12-31
	 */
	public static String Tomorrow() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday + 1);
		Log.d("listproto", "tomorrow starts: " + time.format(dateFormat));
		return time.format(dateFormat);
	}

	/**
	 * Today = 2012-01-01
	 * Returns 2012-01-08
	 */
	public static String DateEightDay() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday + 8);
		Log.d("listproto", "eigth day starts: " + time.format(dateFormat));
		return time.format(dateFormat);
	}
	
	/**
	 * Today = 2012-12-30
	 * Returns 2012-12-30
	 */
	public static String Today() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		Log.d("listproto", "today starts: " + time.format(dateFormat));
		return time.format(dateFormat);
	}

}
