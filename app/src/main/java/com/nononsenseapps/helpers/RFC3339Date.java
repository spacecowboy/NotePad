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

package com.nononsenseapps.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class RFC3339Date {

	// TODO see TimeFormatter & TimeHelper in this package. One of these 3 classes HAS to be redundant

	public static java.util.Date parseRFC3339Date(String datestring) {
		if (datestring == null || datestring.isEmpty()) {
			return null;
		}

		Date d;

		// if there is no time zone, we don't need to do any special parsing.
		if (datestring.endsWith("Z")) {
			try {
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
						Locale.US); // spec for RFC3339
				s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
				d = s.parse(datestring);
			} catch (ParseException pe) { // try again with optional
				// decimals
				SimpleDateFormat s = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
						Locale.US); // spec for RFC3339
				// (with fractional seconds)
				s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
				s.setLenient(true);
				try {
					d = s.parse(datestring);
				} catch (ParseException e) {
					return null;
				}
			}
			return d;
		}

		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
		// spec for RFC3339
		s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));

		try {
			d = s.parse(datestring);
		} catch (java.text.ParseException pe) { // try again with optional decimals
			s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.US);
			s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
			// spec for RFC3339(with fractional seconds)
			s.setLenient(true);
			try {
				d = s.parse(datestring);
			} catch (ParseException e) {
				return null;
			}
		}

		return d;
	}

	/**
	 * Given a UTC date (2013-02-21), and a local time(13:23), will combine them
	 * into 2013-02-21T13:23 local time.
	 *
	 * DateString should be in RFC3339.
	 * If time is null, defaults to 23:59
	 */
	public static Long combineDateAndTime(final String datestring, final Long time) {
		final java.util.Date d = parseRFC3339Date(datestring);
		if (d == null) {
			return null;
		}
		// UTC
		final Calendar utc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
		utc.setTime(d);
		utc.set(Calendar.HOUR_OF_DAY, 0);
		utc.set(Calendar.MINUTE, 0);

		// Local date
		final Calendar local = GregorianCalendar.getInstance();
		local.set(Calendar.YEAR, utc.get(Calendar.YEAR));
		local.set(Calendar.MONTH, utc.get(Calendar.MONTH));
		local.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
		// Default to 23:59
		local.set(Calendar.MINUTE, 59);
		local.set(Calendar.HOUR_OF_DAY, 23);

		// Time
		if (time == null) {
			return local.getTimeInMillis();
		}

		final Calendar localTime = GregorianCalendar.getInstance();
		localTime.setTimeInMillis(time);

		local.set(Calendar.MINUTE, localTime.get(Calendar.MINUTE));
		local.set(Calendar.HOUR_OF_DAY, localTime.get(Calendar.HOUR_OF_DAY));

		return local.getTimeInMillis();
	}

	public static String asRFC3339(final Long time) {
		if (time == null)
			return null;
		return asRFC3339(new Date(time));
	}

	/**
	 * For GTasks syncing. Given a date and time, say 2013-02-21T13:34.
	 * Will return 2013-02-21T00:00Z.
	 */
	public static String asRFC3339ZuluDate(final Long time) {
		if (time == null)
			return null;

		// Local time calendar
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(time);

		// Extract the date
		return String.format(Locale.US, "%d", cal.get(Calendar.YEAR)) +
				"-" +
				String.format(Locale.US, "%02d", (1 + cal.get(Calendar.MONTH))) +
				"-" +
				String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH)) +
				"T00:00:00Z";
	}

	private static String asRFC3339(final java.util.Date date) {
		if (date == null) return null;
		// spec for RFC3339:
		final SimpleDateFormat s =
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
		return s.format(date);
	}
}