package com.nononsenseapps.notepad;

import java.util.Calendar;
import java.util.Date;

import com.nononsenseapps.helpers.NotificationHelper;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

/**
 * Used for notifications
 * 
 */
public class TimePickerDialogFragment extends DialogFragment implements
		TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

	public static final String KEY_DATEPICKER = "datepicker";

	private Button timeButton = null;
	private NotificationHelper.NoteNotification notification = null;

	public void setCallbacks(Button button,
			NotificationHelper.NoteNotification not) {
		timeButton = button;
		notification = not;
	}

	@Override
	public void onCreate(Bundle saves) {
		super.onCreate(saves);
		if (timeButton == null
				|| notification == null) {
			// Device was rotated perhaps
			// This activity was destroyed and restarted. Can't save the
			// callback then.
			dismiss();
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(notification.time));

		if (getArguments().getBoolean(KEY_DATEPICKER, false)) {
			int year = c.get(Calendar.YEAR);
			int monthOfYear = c.get(Calendar.MONTH);
			int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);

			return new DatePickerDialog(getActivity(), this, year, monthOfYear,
					dayOfMonth);
		} else {
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);

			// Create a new instance of TimePickerDialog and return it
			return new TimePickerDialog(getActivity(), this, hour, minute,
					DateFormat.is24HourFormat(getActivity()));
		}
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		// Set time on notification
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(notification.time));
		c.set(Calendar.HOUR_OF_DAY, hourOfDay);
		c.set(Calendar.MINUTE, minute);

		notification.time = c.getTime().getTime();

		// Save in DB
		NotificationHelper.updateNotification(getActivity(), notification);

		// Set time in UI
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				timeButton.setText(notification.getLocalTime());
			}

		});
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		// Set time on notification
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(notification.time));
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		c.set(Calendar.MONTH, monthOfYear);
		c.set(Calendar.YEAR, year);

		notification.time = c.getTime().getTime();

		// Save in DB
		NotificationHelper.updateNotification(getActivity(), notification);

		// Set time in UI
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				timeButton.setText(notification.getLocalDate());
			}

		});
	}
}
