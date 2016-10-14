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

package com.nononsenseapps.notepad.ui.common;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener;
import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener;
import com.nononsenseapps.notepad.util.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Notification;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.ui.editor.TaskDetailFragment;
import com.nononsenseapps.notepad.ui.common.WeekDaysView.onCheckedDaysChangeListener;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Handle setting up all the listeners for a Notification list item
 */
public class NotificationItemHelper {

	private static String getDateString(final Context context, final long time) {
		return TimeFormatter.getDateFormatter(context).format(new Date(time));
	}

	private static void hideViews(final View... views) {
		for (View v : views) {
			v.setVisibility(View.GONE);
		}
	}

	private static void showViews(final View... views) {
		for (View v : views) {
			v.setVisibility(View.VISIBLE);
		}
	}

	private static void setTime(final Context context, final Notification not,
			final Task mTask) {
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

	public static void setup(final TaskDetailFragment fragment,
			final LinearLayout notificationList, final View nv,
			final Notification not, final Task mTask) {

		// Set time on notification if not set already
		if (not.time == null) {
			setTime(fragment.getActivity(), not, mTask);
		}

		// Set time text
		final TextView notTimeButton = (TextView) nv
				.findViewById(R.id.notificationTime);
		notTimeButton.setText(not.getLocalTimeText(fragment.getActivity()));

		// Set date text
		final TextView notDateButton = (TextView) nv
				.findViewById(R.id.notificationDate);
		notDateButton.setText(getDateString(fragment.getActivity(), not.time));

		final View notRemoveButton = nv.findViewById(R.id.notificationRemove);

		// Remove button
		notRemoveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!fragment.isLocked()) {
					// Remove row from UI
					notificationList.removeView((View) v.getParent());
					// Remove from database and renotify
					not.delete(fragment.getActivity());
				}
			}
		});

		// Date button
		notDateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!fragment.isLocked()) {

					final Calendar localTime = Calendar.getInstance();
					if (not.time != null) {
						localTime.setTimeInMillis(not.time);
					}

					final DatePickerDialog datedialog = DatePickerDialog
							.newInstance(new OnDateSetListener() {
								@Override
								public void onDateSet(DatePickerDialog dialog,
										int year, int monthOfYear,
										int dayOfMonth) {
									localTime.set(Calendar.YEAR, year);
									localTime.set(Calendar.MONTH, monthOfYear);
									localTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
									
									not.time = localTime.getTimeInMillis();
									notDateButton.setText(not.getLocalDateText(fragment
									.getActivity()));
									not.save(fragment.getActivity(), true);
								}
							}, localTime.get(Calendar.YEAR), localTime
									.get(Calendar.MONTH), localTime
									.get(Calendar.DAY_OF_MONTH));
					
					datedialog.show(fragment.getFragmentManager(), "date");

					// final DialogCalendar datePicker;
					//
					// if (mTask != null && mTask.due != null) {
					// datePicker = DialogCalendar.getInstance(mTask.due);
					// }
					// else {
					// datePicker = DialogCalendar.getInstance();
					// }
					// datePicker.setListener(new DateSetListener() {
					//
					// @Override
					// public void onDateSet(long time) {
					// final Calendar localTime = Calendar.getInstance();
					// localTime.setTimeInMillis(time);
					// if (not.time != null) {
					// final Calendar notTime = Calendar.getInstance();
					// notTime.setTimeInMillis(not.time);
					// localTime.set(Calendar.HOUR_OF_DAY,
					// notTime.get(Calendar.HOUR_OF_DAY));
					// localTime.set(Calendar.MINUTE,
					// notTime.get(Calendar.MINUTE));
					// }
					//
					// not.time = localTime.getTimeInMillis();
					// notDateButton.setText(not.getLocalDateText(fragment
					// .getActivity()));
					// not.save(fragment.getActivity(), true);
					// }
					// });
					//
					// datePicker.show(fragment.getFragmentManager(), "date");

				}
			}
		});
		// Time button
		notTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!fragment.isLocked()) {
					// Display time picker
					final Calendar localTime = Calendar.getInstance();
					if (not.time != null) {
						localTime.setTimeInMillis(not.time);
					}

					final TimePickerDialog timedialog = fragment
							.getTimePickerDialog();
					timedialog.setStartTime(
							localTime.get(Calendar.HOUR_OF_DAY),
							localTime.get(Calendar.MINUTE));
					timedialog.setOnTimeSetListener(new OnTimeSetListener() {
						@Override
						public void onTimeSet(RadialPickerLayout view,
								int hourOfDay, int minute) {
							localTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
							localTime.set(Calendar.MINUTE, minute);

							not.time = localTime.getTimeInMillis();

							notTimeButton.setText(not.getLocalTimeText(fragment
									.getActivity()));

							not.save(fragment.getActivity(), true);
						}
					});

					timedialog.show(fragment.getFragmentManager(), "time");

					// // Now display time picker
					// final TimePickerDialogFragment timePicker = fragment
					// .getTimePickerFragment();
					// timePicker.setListener(new TimePickerDialogHandler() {
					// @Override
					// public void onDialogTimeSet(int hourOfDay, int minute) {
					// final Calendar localTime = Calendar.getInstance();
					// if (not.time != null) {
					// localTime.setTimeInMillis(not.time);
					// }
					// localTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
					// localTime.set(Calendar.MINUTE, minute);
					//
					// not.time = localTime.getTimeInMillis();
					//
					// notTimeButton.setText(not.getLocalTimeText(fragment
					// .getActivity()));
					//
					// not.save(fragment.getActivity(), true);
					// }
					//
					// @Override
					// public void onDialogTimeCancel() {
					// }
					//
					// });
					//
					// timePicker.show(fragment.getFragmentManager(), "time");
				}
			}
		});

		WeekDaysView days = ((WeekDaysView) nv.findViewById(R.id.weekdays));
		days.setCheckedDays(not.repeats);
		days.setOnCheckedDaysChangedListener(new onCheckedDaysChangeListener() {

			@Override
			public void onChange(final long checkedDays) {
				not.repeats = checkedDays;
				not.saveInBackground(fragment.getActivity(), true);
			}
		});
	}
}
