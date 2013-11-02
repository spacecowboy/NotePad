package com.nononsenseapps.notepad.fragments;

import java.util.Calendar;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.notepad.R;
import com.squareup.timessquare.CalendarPickerView;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Button;

@EFragment(R.layout.fragment_dialog_calendar)
public class DialogCalendar extends DialogFragment {

	public interface DateSetListener {
		void onDateSet(long time);
	}

	static final String SELECTED_DATE = "selected_date";

	@ViewById
	CalendarPickerView calendarView;

	@ViewById(R.id.dialog_yes)
	Button okButton;

	@ViewById(R.id.dialog_no)
	Button cancelButton;

	private DateSetListener listener;

	/**
	 * Use to create new list
	 */
	public static DialogCalendar_ getInstance() {
		DialogCalendar_ dialog = new DialogCalendar_();
		dialog.setArguments(new Bundle());
		return dialog;
	}

	public static DialogCalendar_ getInstance(final long selectedTime) {
		DialogCalendar_ dialog = new DialogCalendar_();
		Bundle args = new Bundle();
		args.putLong(SELECTED_DATE, selectedTime);
		dialog.setArguments(args);
		return dialog;
	}

	public DialogCalendar() {

	}

	public void setListener(final DateSetListener listener) {
		this.listener = listener;
	}

	@AfterViews
	void setup() {
		final Calendar future = Calendar.getInstance();
		future.add(Calendar.YEAR, 1);
		final Calendar selected = Calendar.getInstance();
		// Must be greater/equal to today
		if (getArguments().getLong(SELECTED_DATE, -1) >= selected
				.getTimeInMillis()) {
			// set date
			selected.setTimeInMillis(getArguments().getLong(SELECTED_DATE, -1));
		}
		else {
			// Default to tomorrow
			// selected.add(Calendar.DAY_OF_YEAR, 1);
		}

		calendarView.init(Calendar.getInstance().getTime(), future.getTime(),
				ActivityHelper.getUserLocale(getActivity())).withSelectedDate(
				selected.getTime());

		getDialog().setTitle(R.string.select_date);
	}

	@Click(R.id.dialog_no)
	void cancelClicked() {
		dismiss();
	}

	@Click(R.id.dialog_yes)
	void okClicked() {
		if (listener != null) {
			listener.onDateSet(calendarView.getSelectedDate().getTime());
		}

		this.dismiss();
	}
}
