package com.nononsenseapps.notepad.test;

import java.util.Calendar;

import android.test.AndroidTestCase;
import android.util.Log;

import com.nononsenseapps.notepad.util.RFC3339Date;

public class RFCDateTest extends AndroidTestCase {

	static final String TAG = "nononsenseapps rfctest";
	
	// Sun May  5 23:53:10 2013
	static final long atime = 1367790790000L;
	// 2 Hours in milli seconds, for MY TIMEZONE
	static final long twohours = 7200000L;
	
//	public void testCalendar() {
//		Calendar c = Calendar.getInstance();
//		c.setTimeInMillis(RFC3339Date.localAsRFC3339(atime));
//		
//		assertEquals("GMT would show as 21:53", 21, c.get(Calendar.HOUR_OF_DAY));
//		
//		//Log.d(TAG, "gtinm: " + c.getTimeInMillis() + ", gtgt: " + c.getTime().getTime());
//		//assertTrue("Is wrong time", c.getTimeInMillis() == c.getTime().getTime());
//	}
	
//	public void testUTCFUCKSHIT() throws IndexOutOfBoundsException, ParseException {
//		Log.d(TAG, "Start");
//		Log.d(TAG, RFC3339Date.localAsRFC3339(atime));
//		Log.d(TAG, RFC3339Date.UTCAsRFC3339(atime));
//		String a = RFC3339Date.localAsRFC3339(atime);
//		Long l = RFC3339Date.parseRFC3339Date(a).getTime();
//		Log.d(TAG, RFC3339Date.UTCAsRFC3339(l));
//		Log.d(TAG, RFC3339Date.localAsRFC3339(l));
//		Log.d(TAG, "End");
//		
//		// Should return UTC time!
//		final long utctime = RFC3339Date.localMilliToUTCMilli(atime);
//		
//		assertEquals("If UTC, difference should be two hours: " + atime + ", " + utctime, twohours, atime - utctime);
//	}
	
	public void testParseRFCDateBackAndForth() {
		// Make sure conversion is consistent
		// Calendar returns local time
		final long long1 = Calendar.getInstance().getTime().getTime();
		// String neutral
		final String string1 = RFC3339Date.asRFC3339(long1);
		
		Log.d(TAG, "" + long1 + " = " + string1);
	
		try {
			// utc
			final long long2 = RFC3339Date.parseRFC3339Date(string1).getTime();
			final String string2 = RFC3339Date.asRFC3339(long2);
			// utc again
			final long long3 = RFC3339Date.parseRFC3339Date(string2).getTime();
			
			Log.d(TAG, "" + long2 + " = " + string2);
			
			assertEquals("TimeInMilli did not match", long1/1000, long3/1000);
			//assertEquals("RFC String did not match", string1, string2);
		}
		catch (Exception e) {
			assertFalse(e.getLocalizedMessage(), true);
		}
	
	}
}
