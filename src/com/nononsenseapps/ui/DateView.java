package com.nononsenseapps.ui;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A simple textview that displays time. It expects input to be a string represtentation of time in milliseconds since the epoch.
 * @author Jonas
 *
 */
public class DateView extends TextView {
	private static final int SECONDS_PER_DAY = 3600;
	//private String day = "E, d MMM";
	private String day = "d MMM";
	private String time = "kk:mm";

	public DateView(Context context) {
		super(context);
	}

	public DateView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DateView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		if (text == null || text.toString().length() == 0)
			super.setText("", type);
		else {
			super.setText(toDate(text.toString()), type);
		}
	}
	
	public CharSequence toDate(String time3339) {
		Time time = new Time(Time.getCurrentTimezone());
		time.parse3339(time3339);
		
		return toDate(time.toMillis(false));
	}

	public CharSequence toDate(long msecs) {
		String format = day;

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(msecs);
		
		return DateFormat.format(format, c);
	}
	
	/**
     * Returns the number of days between two dates. The time part of the
     * days is ignored in this calculation, so 2007-01-01 13:00 and 2007-01-02 05:00
     * have one day inbetween.
     */
    public static long daysBetween(Date firstDate, Date secondDate) {
        // We only use the date part of the given dates
        long firstSeconds = truncateToDate(firstDate).getTime()/1000;
        long secondSeconds = truncateToDate(secondDate).getTime()/1000;
        // Just taking the difference of the millis.
        // These will not be exactly multiples of 24*60*60, since there
        // might be daylight saving time somewhere inbetween. However, we can
        // say that by adding a half day and rounding down afterwards, we always
        // get the full days.
        long difference = secondSeconds-firstSeconds;
        // Adding half a day
        if( difference >= 0 ) {
            difference += SECONDS_PER_DAY/2; // plus half a day in seconds
        } else {
            difference -= SECONDS_PER_DAY/2; // minus half a day in seconds
        }
        // Rounding down to days
        difference /= SECONDS_PER_DAY;

        return difference;
    }

    /**
     * Truncates a date to the date part alone.
     */
    public static Date truncateToDate(Date d) {
        if( d instanceof java.sql.Date ) {
            return d; // java.sql.Date is already truncated to date. And raises an
                      // Exception if we try to set hours, minutes or seconds.
        }
        d = (Date)d.clone();
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        d.setTime(((d.getTime()/1000)*1000));
        return d;
    }

}
