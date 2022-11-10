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

package com.nononsenseapps.util;

import android.text.format.Time;

import com.nononsenseapps.helpers.NnnLogger;

/**
 * Has a few helper functions for dealing with dates. Google Tasks API is crap
 * because it throws away the time part and sets to UTC.
 *
 * @author Jonas
 */
public class TimeHelper {

	// 2012-12-31
	public final static String dateFormat = "%Y-%m-%d";

	/**
	 * Today = 2012-12-30 Returns 2012-12-31
	 */
	public static String dateTomorrow() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday + 1);

		return time.format(dateFormat);
	}

	/**
	 * Today = 2012-01-01 Returns 2012-01-08
	 */
	public static String dateEightDay() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday + 8);

		return time.format(dateFormat);
	}

	/**
	 * Today = 2012-12-30 Returns 2012-12-30
	 */
	public static String dateToday() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();

		return time.format(dateFormat);
	}

	/**
	 * Today = 2012-12-30 Returns 2012-12-30 00:00:00
	 */
	public static String milliTodayStart() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		return Long.toString(time.toMillis(false));
	}

	/**
	 * Today = 2012-12-30 Returns 2012-12-30 00:00:00
	 */
	public static long milliTodayStartLong() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		return time.toMillis(false);
	}

	/**
	 * Today = 2012-12-30 Returns 2012-12-29 00:00:00
	 */
	public static String milliYesterdayStart() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday - 1);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		return Long.toString(time.toMillis(false));
	}

	/**
	 * Today = 2012-12-30 Returns 2012-12-29 00:00:00
	 */
	public static long milliYesterdayStartLong() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday - 1);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		return time.toMillis(false);
	}

	/**
	 * Today = 2012-12-30 Returns 2012-12-23
	 */
	public static String milli7DaysAgo() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday - 7);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		return Long.toString(time.toMillis(false));
	}

	/**
	 * Today = 2012-12-30 Returns 2012-12-23
	 */
	public static long milli7DaysAgoLong() {
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		int julianToday = Time.getJulianDay(time.toMillis(false), time.gmtoff);
		time.setJulianDay(julianToday - 7);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		return time.toMillis(false);
	}

	/**
	 * Given the argument i, will return todays date + i days, formatted as
	 * RFC3339 in UTC time zone
	 */
	public static String get3339DaysFromToday(int i) {
		Time localtime = new Time(Time.getCurrentTimezone());
		localtime.setToNow();
		int julianToday = Time.getJulianDay(localtime.toMillis(false),
				localtime.gmtoff);

		Time time = new Time(Time.TIMEZONE_UTC);
		time.setJulianDay(julianToday + i);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;
		NnnLogger.debugOnly(TimeHelper.class,
				"Dragdate: " + i + " days ago: " + time.format3339(false));

		return time.format3339(false);
	}

	public static boolean dateBefore(final String itemDate,
									 final String referenceDate) {
		// Fix for timezone issue. We don't care about them
		// A date like 2013-03-05T00:00 will be abbreviated
		// to 2013-03-05
		String dbstring = itemDate;
		if (dbstring.contains("T")) {
			dbstring = dbstring.substring(0, dbstring.indexOf("T"));
		}
		String compareString = referenceDate;
		if (compareString.contains("T")) {
			compareString = compareString.substring(0,
					compareString.indexOf("T"));
		}

		Time time = new Time();
		time.parse3339(dbstring);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		Time ctime = new Time();
		ctime.parse3339(compareString);

		return time.before(ctime);
	}

	public static boolean dateIs(final String itemDate,
								 final String referenceDate) {
		// Fix for timezone issue. We don't care about them
		// A date like 2013-03-05T00:00 will be abbreviated
		// to 2013-03-05
		String dbstring = itemDate;
		if (dbstring.contains("T")) {
			dbstring = dbstring.substring(0, dbstring.indexOf("T"));
		}
		String compareString = referenceDate;
		if (compareString.contains("T")) {
			compareString = compareString.substring(0,
					compareString.indexOf("T"));
		}

		Time time = new Time();
		time.parse3339(dbstring);
		time.hour = 0;
		time.minute = 0;
		time.second = 0;

		Time ctime = new Time();
		ctime.parse3339(compareString);

		return (time.year == ctime.year) && (time.month == ctime.month)
				&& (time.monthDay == ctime.monthDay);
	}
}
