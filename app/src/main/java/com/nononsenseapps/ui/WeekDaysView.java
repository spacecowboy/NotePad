package com.nononsenseapps.ui;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.utils.views.GreyableToggleButton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

public class WeekDaysView extends LinearLayout implements
		OnCheckedChangeListener {

	public static interface onCheckedDaysChangeListener {
		public void onChange(long checkedDays);
	}

	public static final int mon = 0x1;
	public static final int tue = 0x10;
	public static final int wed = 0x100;
	public static final int thu = 0x1000;
	public static final int fri = 0x10000;
	public static final int sat = 0x100000;
	public static final int sun = 0x1000000;

	GreyableToggleButton monday;
	GreyableToggleButton tuesday;
	GreyableToggleButton wednesday;
	GreyableToggleButton thursday;
	GreyableToggleButton friday;
	GreyableToggleButton saturday;
	GreyableToggleButton sunday;

	private onCheckedDaysChangeListener listener = null;
	private Locale mLocale;

	public WeekDaysView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mInflater.inflate(R.layout.weekdays_layout, this, true);

		// TODO respect locale settings regarding first day of week

		try {
			mLocale = ActivityHelper.getUserLocale(context);

			SimpleDateFormat dayFormat = TimeFormatter
					.getLocalFormatterWeekdayShort(context);
			// 2013-05-13 was a monday
			GregorianCalendar gc = new GregorianCalendar(2013,
					GregorianCalendar.MAY, 13);
			final long base = gc.getTimeInMillis();
			final long day = 24 * 60 * 60 * 1000;

			monday = ((GreyableToggleButton) findViewById(R.id.day1));
			initializeToggleButton(dayFormat.format(gc.getTime()), monday);

			tuesday = ((GreyableToggleButton) findViewById(R.id.day2));
			gc.setTimeInMillis(base + 1 * day);
			initializeToggleButton(dayFormat.format(gc.getTime()), tuesday);

			wednesday = ((GreyableToggleButton) findViewById(R.id.day3));
			gc.setTimeInMillis(base + 2 * day);
			initializeToggleButton(dayFormat.format(gc.getTime()), wednesday);

			thursday = ((GreyableToggleButton) findViewById(R.id.day4));
			gc.setTimeInMillis(base + 3 * day);
			initializeToggleButton(dayFormat.format(gc.getTime()), thursday);

			friday = ((GreyableToggleButton) findViewById(R.id.day5));
			gc.setTimeInMillis(base + 4 * day);
			initializeToggleButton(dayFormat.format(gc.getTime()), friday);

			saturday = ((GreyableToggleButton) findViewById(R.id.day6));
			gc.setTimeInMillis(base + 5 * day);
			initializeToggleButton(dayFormat.format(gc.getTime()), saturday);

			sunday = ((GreyableToggleButton) findViewById(R.id.day7));
			gc.setTimeInMillis(base + 6 * day);
			initializeToggleButton(dayFormat.format(gc.getTime()), sunday);
		} catch (Exception e) {
			// For UI editor's sake
			mLocale = Locale.getDefault();
		}

	}

	void initializeToggleButton(final String text,
								final GreyableToggleButton button) {
		button.setText(text.toUpperCase(mLocale));
		button.setTextOn(text.toUpperCase(mLocale));
		button.setTextOff(text.toUpperCase(mLocale));
		button.setOnCheckedChangeListener(this);
	}

	public long getCheckedDays() {
		long checkedDays = 0;

		if (monday.isChecked()) checkedDays |= mon;
		if (tuesday.isChecked()) checkedDays |= tue;
		if (wednesday.isChecked()) checkedDays |= wed;
		if (thursday.isChecked()) checkedDays |= thu;
		if (friday.isChecked()) checkedDays |= fri;
		if (saturday.isChecked()) checkedDays |= sat;
		if (sunday.isChecked()) checkedDays |= sun;

		return checkedDays;
	}

	public void setCheckedDays(long checkedDays) {
		monday.setChecked(0 < (checkedDays & mon));
		tuesday.setChecked(0 < (checkedDays & tue));
		wednesday.setChecked(0 < (checkedDays & wed));
		thursday.setChecked(0 < (checkedDays & thu));
		friday.setChecked(0 < (checkedDays & fri));
		saturday.setChecked(0 < (checkedDays & sat));
		sunday.setChecked(0 < (checkedDays & sun));
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (listener != null) {
			listener.onChange(getCheckedDays());
		}
	}

	public void setOnCheckedDaysChangedListener(
			onCheckedDaysChangeListener listener) {
		this.listener = listener;
	}

}
