package com.nononsenseapps.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.ActivityLocation;
import com.nononsenseapps.notepad.ActivityLocation_;
import com.nononsenseapps.notepad.core.R;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.fragments.DialogCalendar;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment;
import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment.TimePickerDialogHandler;
import com.nononsenseapps.notepad.fragments.DialogCalendar.DateSetListener;
import com.nononsenseapps.ui.WeekDaysView.onCheckedDaysChangeListener;

import android.content.Context;
import android.content.Intent;
import android.sax.StartElementListener;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.PopupMenu.OnMenuItemClickListener;

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
		// Start with date
		if (mTask.due != null) {
			cal.setTimeInMillis(mTask.due);
		}
		else {
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		// Now set a reasonable time of day like 9 AM
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 0);

		// And set time on notification
		not.time = cal.getTimeInMillis();
	}

	private static void startLocationActivity(
			final TaskDetailFragment fragment, final Notification not) {
		fragment.setPendingLocationNotification(not);

		Intent i = new Intent(fragment.getActivity(), ActivityLocation_.class);
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

	public static void setup(final TaskDetailFragment fragment,
			final LinearLayout notificationList, final View nv,
			final Notification not, final Task mTask) {

		if (not.radius == null) {
			switchToTime(nv);
		}
		else {
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
					switchToLocation(nv);
					startLocationActivity(fragment, not);
					return true;
				} else {
					return false;
				}
			}

		};
		// Time menu
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
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				popup.setOnMenuItemClickListener(onTypeListener);
				popup.inflate(R.menu.notification_types);
				popup.show();
			}
		});

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
				}
				catch (Exception e) {
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
		if (not.locationName != null) location.setText(not.locationName);

		location.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationActivity(fragment, not);
			}
		});

		final View weekDays = nv.findViewById(R.id.weekdays);

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

		// Date button
		notDateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!fragment.isLocked()) {
					final DialogCalendar datePicker;

					if (mTask != null && mTask.due != null) {
						datePicker = DialogCalendar.getInstance(mTask.due);
					}
					else {
						datePicker = DialogCalendar.getInstance();
					}
					datePicker.setListener(new DateSetListener() {

						@Override
						public void onDateSet(long time) {
							final Calendar localTime = Calendar.getInstance();
							localTime.setTimeInMillis(time);
							if (not.time != null) {
								final Calendar notTime = Calendar.getInstance();
								notTime.setTimeInMillis(not.time);
								localTime.set(Calendar.HOUR_OF_DAY,
										notTime.get(Calendar.HOUR_OF_DAY));
								localTime.set(Calendar.MINUTE,
										notTime.get(Calendar.MINUTE));
							}

							not.time = localTime.getTimeInMillis();
							notDateButton.setText(not.getLocalDateText(fragment
									.getActivity()));
							not.save(fragment.getActivity(), true);
						}
					});

					datePicker.show(fragment.getFragmentManager(), "date");

				}
			}
		});
		// Time button
		notTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!fragment.isLocked()) {
					// Now display time picker
					final TimePickerDialogFragment timePicker = fragment
							.getTimePickerFragment();
					timePicker.setListener(new TimePickerDialogHandler() {
						@Override
						public void onDialogTimeSet(int hourOfDay, int minute) {
							final Calendar localTime = Calendar.getInstance();
							if (not.time != null) {
								localTime.setTimeInMillis(not.time);
							}
							localTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
							localTime.set(Calendar.MINUTE, minute);

							not.time = localTime.getTimeInMillis();

							notTimeButton.setText(not.getLocalTimeText(fragment
									.getActivity()));

							not.save(fragment.getActivity(), true);
						}

						@Override
						public void onDialogTimeCancel() {
						}

					});

					timePicker.show(fragment.getFragmentManager(), "time");
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
