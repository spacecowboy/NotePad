package com.nononsenseapps.utils.time;

/*
 * I was working on an Atom (http://www.w3.org/2005/Atom) parser and discovered
 * that I could not parse dates in the format defined by RFC 3339 using the
 * SimpleDateFormat class. The reason was the ':' in the time zone. This code
 * strips out the colon if it's there and tries four different formats on the
 * resulting string depending on if it has a time zone, or if it has a
 * fractional second part. There is a probably a better way to do this, and a
 * more proper way. But this is a really small addition to a codebase (You don't
 * need a jar, just throw this function in some static Utility class if you have
 * one).
 * 
 * Feel free to use this in your code, but I'd appreciate it if you keep this
 * note in the code if you distribute it. Thanks!
 * 
 * For people who might be googling: The date format parsed by this goes by:
 * atomDateConstruct, xsd:dateTime, RFC3339 and is compatable with:
 * ISO.8601.1988, W3C.NOTE-datetime-19980827 and W3C.REC-xmlschema-2-20041028
 * (that I know of)
 * 
 * 
 * Copyright 2007, Chad Okere (ceothrow1 at gmail dotcom) OMG NO WARRENTY
 * EXPRESSED OR IMPLIED!!!1
 */

// package org.doubango.imsdroid.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class RFC3339Date {
	public static java.util.Date parseRFC3339Date(String datestring)
			throws java.text.ParseException, IndexOutOfBoundsException {
		Date d = new Date();

		// if there is no time zone, we don't need to do any special parsing.
		if (datestring.endsWith("Z")) {
			try {
				SimpleDateFormat s = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss'Z'"); // spec for RFC3339
				s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
				d = s.parse(datestring);
			}
			catch (java.text.ParseException pe) {// try again with optional
													// decimals
				SimpleDateFormat s = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");// spec for RFC3339
															// (with fractional
															// seconds)
				s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
				s.setLenient(true);
				d = s.parse(datestring);
			}
			return d;
		}

		// step one, split off the timezone.
//		String firstpart = datestring.substring(0, datestring.lastIndexOf('-'));
//		String secondpart = datestring.substring(datestring.lastIndexOf('-'));
//
//		// step two, remove the colon from the timezone offset
//		secondpart = secondpart.substring(0, secondpart.indexOf(':'))
//				+ secondpart.substring(secondpart.indexOf(':') + 1);
//		datestring = firstpart + secondpart;
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		// spec for RFC3339
		s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));

		try {
			d = s.parse(datestring);
		}
		catch (java.text.ParseException pe) {// try again with optional decimals
			s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
			s.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
			// spec for RFC3339(with fractional seconds)
			s.setLenient(true);
			d = s.parse(datestring);
		}

		return d;
	}
	
	public static String asRFC3339(final Long time) {
		if (time == null)
			return null;
		return asRFC3339(new Date(time));
	}

	private static String asRFC3339(final java.util.Date date) {
		if (date == null)
			return null;
		final SimpleDateFormat s = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ssZ");// spec for RFC3339
		return s.format(date);
	}
}