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

package com.nononsenseapps.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Handle setting up all the listeners for a Notification list item
 */
public final class NotificationItemHelper {

	private static String getDateString(final Context context, final long time) {
		return TimeFormatter.getDateFormatter(context).format(new Date(time));
	}

	private static void switchToTime(final View nv) {
		showViews(nv.findViewById(R.id.notificationTime),
				nv.findViewById(R.id.notificationDate),
				nv.findViewById(R.id.notificationTypeTime),
				nv.findViewById(R.id.weekdays));

		// hide this view
		nv.findViewById(R.id.repeatSwitch).setVisibility(View.GONE);
	}

	private static void showViews(final View... views) {
		for (View v : views) {
			v.setVisibility(View.VISIBLE);
		}
	}

	private static void setTime(final Context context, final Notification not, final Task mTask) {
		final GregorianCalendar cal = TimeFormatter.getLocalCalendar(context);
		// Start with date, either due date or today (default)
		// If due date is in the past, default to today + 1hour
		if (mTask.due != null && mTask.due > cal.getTimeInMillis()) {
			cal.setTimeInMillis(mTask.due);
		} else {
			// Default to today, set time one hour from now
			cal.add(Calendar.HOUR_OF_DAY, 1);
		}

		// And set time on notification
		not.time = cal.getTimeInMillis();
	}

	/**
	 * add a new reminder and show it in the note detail page
	 *
	 * @param fragment         the {@link TaskDetailFragment} that will host this reminder widget
	 * @param notificationList the list in {@link TaskDetailFragment} that contains this reminder
	 *                         widget
	 */
	public static void setup(final TaskDetailFragment fragment,
							 final LinearLayout notificationList, final View nv,
							 final Notification not, final Task mTask) {
		switchToTime(nv);

		// Set time on notification if not set already
		if (not.time == null) {
			setTime(fragment.getActivity(), not, mTask);
		}

		// Set time text
		final TextView notTimeButton = nv.findViewById(R.id.notificationTime);
		notTimeButton.setText(not.getLocalTimeText(fragment.getActivity()));

		// Set date text
		final TextView notDateButton = nv.findViewById(R.id.notificationDate);
		notDateButton.setText(getDateString(fragment.getActivity(), not.time));

		final View notRemoveButton = nv.findViewById(R.id.notificationRemove);

		// Remove button
		notRemoveButton.setOnClickListener(v -> {
			if (fragment.isLocked()) {
				Toast.makeText(fragment.getContext(), R.string.canceled_note_locked,
						Toast.LENGTH_SHORT).show();
				return;
			}
			// Remove row from UI
			notificationList.removeView((View) v.getParent());
			// Remove from database and renotify
			not.delete(fragment.getActivity());
		});

		// Date button
		notDateButton.setOnClickListener(v -> {
			if (fragment.isLocked()) {
				Toast.makeText(fragment.getContext(), R.string.canceled_note_locked,
						Toast.LENGTH_SHORT).show();
				return;
			}

			final Calendar localTime = Calendar.getInstance();
			if (not.time != null) {
				localTime.setTimeInMillis(not.time);
			}

			var onDateSetListnr = new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker dialog, int year, int monthOfYear, int dayOfMonth) {
					localTime.set(Calendar.YEAR, year);
					localTime.set(Calendar.MONTH, monthOfYear);
					localTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

					not.time = localTime.getTimeInMillis();
					notDateButton.setText(not.getLocalDateText(fragment.getActivity()));
					not.save(fragment.getActivity(), true);
				}
			};

			// configure and show a popup with a date-picker calendar view
			final DatePickerDialog datedialog = new DatePickerDialog(
					fragment.requireContext(),
					onDateSetListnr,
					localTime.get(Calendar.YEAR),
					localTime.get(Calendar.MONTH),
					localTime.get(Calendar.DAY_OF_MONTH));
			datedialog.setTitle(R.string.select_date);

			datedialog.show();
		});

		// Time button
		notTimeButton.setOnClickListener(v -> {
			if (fragment.isLocked()) {
				Toast.makeText(fragment.getContext(), R.string.canceled_note_locked,
						Toast.LENGTH_SHORT).show();
				return;
			}

			// Display time picker
			final Calendar localTime = Calendar.getInstance();
			if (not.time != null) {
				localTime.setTimeInMillis(not.time);
			}

			TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute) -> {
				localTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				localTime.set(Calendar.MINUTE, minute);
				not.time = localTime.getTimeInMillis();
				notTimeButton.setText(not.getLocalTimeText(fragment.getActivity()));
				not.save(fragment.getActivity(), true);
			};

			final TimePickerDialog timedialog = fragment
					.getTimePickerDialog(localTime, onTimeSetListener);
			timedialog.setTitle(R.string.time);
			timedialog.show();
		});

		// week days button strip
		WeekDaysView days = nv.findViewById(R.id.weekdays);
		days.setCheckedDays(not.repeats);
		days.setOnCheckedDaysChangedListener(checkedDays -> {
			if (fragment.isLocked()) {
				Toast.makeText(fragment.getContext(), R.string.canceled_note_locked,
						Toast.LENGTH_SHORT).show();
				return false; // return the button to the previous state
			}
			not.repeats = checkedDays;
			not.saveInBackground(fragment.getActivity(), true);
			return true; // all ok, it can proceed
		});
	}
}
