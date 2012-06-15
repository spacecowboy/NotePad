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
 * @author Jonas
 *
 */
public class TimeHelper {
	
	/**
	 * Returns tomorrow at 00:00:00
	 */
	public static String DateTomorrowStart() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday + 1);
		Log.d("listproto", "tomorrow starts: " + time.format3339(false));
		return time.format3339(false);
	}
	
	/**
	 * Returns tomorrow at 23:59:59
	 */
	public static String DateTomorrowEnd() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday + 1);
		time.hour = 23;
		time.minute = 59;
		time.second = 59;
		Log.d("listproto", "tomorrow ends: " + time.format3339(false));
		return time.format3339(false);
	}

	/**
	 * Returns the date of the first day that is beyond the next seven days
	 * @return
	 */
	public static String DateEightDay() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday + 8);
		Log.d("listproto", "eigth day starts: " + time.format3339(false));
		return time.format3339(false);
	}
	
	/**
	 * Get the datetime when today started (00:00:00) in UTC as RFC3339 formatted string
	 * @return
	 */
	public static String TodayStart() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		time.hour = 0;
		time.minute = 0;
		time.second = 0;
		Log.d("listproto", "today starts: " + time.format3339(false));
		return time.format3339(false);
	}
	
	/**
	 * Get the datetime when today ends (23:59:59) in UTC as RFC3339 formatted string
	 * @return
	 */
	public static String TodayEnd() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		time.hour = 23;
		time.minute = 59;
		time.second = 59;
		Log.d("listproto", "today ends: " + time.format3339(false));
		return time.format3339(false);
	}

}
