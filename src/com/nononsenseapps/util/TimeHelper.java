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
	public static String dateTomorrow() {
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
	public static String dateEightDay() {
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
	public static String dateToday() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		Log.d("listproto", "today starts: " + time.format(dateFormat));
		return time.format(dateFormat);
	}

	/**
	 * Today = 2012-12-30
	 * Returns 2012-12-30 00:00:00
	 */
	public static String milliTodayStart() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		time.hour = 0;
		time.minute = 0;
		time.second = 0;
		Log.d("listproto", "today starts: " + time.toMillis(false));
		return Long.toString(time.toMillis(false));
	}
	
	/**
	 * Today = 2012-12-30
	 * Returns 2012-12-29 00:00:00
	 */
	public static String milliYesterdayStart() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday - 1);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;
		Log.d("listproto", "yesterday starts: " + time.toMillis(false));
		return Long.toString(time.toMillis(false));
	}
	
	/**
	 * Today = 2012-12-30
	 * Returns 2012-12-23
	 */
	public static String milli7DaysAgo() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday - 7);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;
		Log.d("listproto", "7 days ago starts: " + time.toMillis(false));
		return Long.toString(time.toMillis(false));
	}

	/**
	 * Given the argument i, will return todays date + i days, formatted as
	 * RFC3339
	 */
	public static String get3339DaysFromToday(int i) {
		Time localtime = new Time(Time.getCurrentTimezone());
		localtime.setToNow();
		int julianToday = Time.getJulianDay(localtime.toMillis(false), localtime.gmtoff);
		
		Time time = new Time(Time.TIMEZONE_UTC);
		time.setJulianDay(julianToday + i);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;
		Log.d("dragdate", "" + i + " days ago: " + time.format3339(false));
		
		return time.format3339(false);
	}
}
