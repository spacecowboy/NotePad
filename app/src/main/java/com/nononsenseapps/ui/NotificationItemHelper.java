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

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener;
import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.ActivityLocation;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.ui.WeekDaysView.onCheckedDaysChangeListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

	public static void setLocationName(final Notification not) {
		if (not.view != null) {
			// Fill in location name
			((TextView) not.view.findViewById(R.id.notificationLocation))
					.setText(not.locationName);
		}
	}

	public static void switchToLocation(final View nv) {
		hideViews(nv.findViewById(R.id.notificationTime),
				nv.findViewById(R.id.notificationDate),
				nv.findViewById(R.id.notificationTypeTime),
				nv.findViewById(R.id.weekdays));
		showViews(nv.findViewById(R.id.notificationTypeLocation),
				nv.findViewById(R.id.notificationLocation),
				nv.findViewById(R.id.repeatSwitch));
	}

	private static void switchToTime(final View nv) {
		showViews(nv.findViewById(R.id.notificationTime),
				nv.findViewById(R.id.notificationDate),
				nv.findViewById(R.id.notificationTypeTime),
				nv.findViewById(R.id.weekdays));
		hideViews(nv.findViewById(R.id.notificationTypeLocation),
				nv.findViewById(R.id.notificationLocation),
				nv.findViewById(R.id.repeatSwitch));
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
		if (mTask.due != null) {
			cal.setTimeInMillis(mTask.due);
		} else {
			// Default to today, set time one hour from now
			cal.add(Calendar.HOUR_OF_DAY, 1);
		}

		// And set time on notification
		not.time = cal.getTimeInMillis();
	}

	private static void startLocationActivity(
			final TaskDetailFragment fragment, final Notification not) {
        if (BuildConfig.NONFREE) {
            fragment.setPendingLocationNotification(not);

            Intent i = new Intent(fragment.getActivity(), ActivityLocation.getAnnotatedActivityClass());
            i.putExtra(ActivityLocation.EXTRA_ID, not._id);
            if (not.latitude != null && not.longitude != null && not.radius != null) {
                i.putExtra(ActivityLocation.EXTRA_LATITUDE, (double) not.latitude)
                        .putExtra(ActivityLocation.EXTRA_LONGITUDE,
                                (double) not.longitude)
                        .putExtra(ActivityLocation.EXTRA_RADIUS,
                                (double) not.radius);
            }
            fragment.startActivityForResult(i, 2);
        }
	}

	public static void setup(final TaskDetailFragment fragment,
			final LinearLayout notificationList, final View nv,
			final Notification not, final Task mTask) {

		if (not.radius == null || !BuildConfig.NONFREE) {
			switchToTime(nv);
		} else {
			switchToLocation(nv);
		}

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

		final OnMenuItemClickListener onTypeListener = new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				int itemId = item.getItemId();
				if (itemId == R.id.not_type_time) {
					switchToTime(nv);
					return true;
				} else if (itemId == R.id.not_type_location) {
                    if (BuildConfig.NONFREE) {
                        switchToLocation(nv);
                        startLocationActivity(fragment, not);
                        return true;
                    } else {
                        return false;
                    }
				} else {
					return false;
				}
			}

		};
		// Time menu, if locations enabled
        if (BuildConfig.NONFREE) {
            final ImageButton notTimeMenu = (ImageButton) nv
                    .findViewById(R.id.notificationTypeTime);
            notTimeMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu popup = new PopupMenu(fragment.getActivity(), v);
                    try {
                        Field[] fields = popup.getClass().getDeclaredFields();
                        for (Field field : fields) {
                            if ("mPopup".equals(field.getName())) {
                                field.setAccessible(true);
                                Object menuPopupHelper = field.get(popup);
                                Class<?> classPopupHelper = Class
                                        .forName(menuPopupHelper.getClass()
                                                .getName());
                                Method setForceIcons = classPopupHelper.getMethod(
                                        "setForceShowIcon", boolean.class);
                                setForceIcons.invoke(menuPopupHelper, true);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    popup.setOnMenuItemClickListener(onTypeListener);
                    popup.inflate(R.menu.notification_types);
                    popup.show();
                }
            });
        }

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

		// Location menu
        if (BuildConfig.NONFREE) {
            final ImageButton notLocationMenu = (ImageButton) nv
                    .findViewById(R.id.notificationTypeLocation);
            notLocationMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu popup = new PopupMenu(fragment.getActivity(), v);
                    try {
                        Field[] fields = popup.getClass().getDeclaredFields();
                        for (Field field : fields) {
                            if ("mPopup".equals(field.getName())) {
                                field.setAccessible(true);
                                Object menuPopupHelper = field.get(popup);
                                Class<?> classPopupHelper = Class
                                        .forName(menuPopupHelper.getClass()
                                                .getName());
                                Method setForceIcons = classPopupHelper.getMethod(
                                        "setForceShowIcon", boolean.class);
                                setForceIcons.invoke(menuPopupHelper, true);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    popup.setOnMenuItemClickListener(onTypeListener);
                    popup.inflate(R.menu.notification_types);
                    popup.show();
                }
            });

            // Location button
            final TextView location = (TextView) nv
                    .findViewById(R.id.notificationLocation);
            if (not.locationName != null)
                location.setText(not.locationName);

            location.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startLocationActivity(fragment, not);
                }
            });


            // Location repeat
            final CheckBox repeatSwitch = (CheckBox) nv
                    .findViewById(R.id.repeatSwitch);
            repeatSwitch.setChecked(not.isLocationRepeat());
            repeatSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    not.setLocationRepeat(isChecked);
                    not.saveInBackground(fragment.getActivity(), true);
                }
            });

        }

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
