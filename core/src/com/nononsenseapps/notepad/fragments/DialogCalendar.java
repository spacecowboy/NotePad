package com.nononsenseapps.notepad.fragments;

import java.util.Calendar;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.rest.Post;
import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.core.R;
import com.squareup.timessquare.CalendarPickerView;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Button;

@EFragment(resName="fragment_dialog_calendar")
public class DialogCalendar extends DialogFragment {

	public interface DateSetListener {
		void onDateSet(long time);
	}

	static final String SELECTED_DATE = "selected_date";

	@ViewById(resName="calendarView")
	CalendarPickerView calendarView;

	@ViewById(resName="dialog_yes")
	Button okButton;

	@ViewById(resName="dialog_no")
	Button cancelButton;

	private DateSetListener listener = null;

	private long savedSelection = -1;

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
	
	@Override
	public void onStart() {
		super.onStart();
		waitToFixDialog();
	}
	
	/**
	 * This can only be called when the view has been shown.
	 * Apparently it has not been shown when onStart is called, hence
	 * the need for the delay.
	 */
	@Background
	void waitToFixDialog() {
		try {
		Thread.sleep(100);
		fixDialog();
		} catch (Exception e) {
		}
	}
	@UiThread
	void fixDialog() {
		if (calendarView != null) {
			calendarView.fixDialogDimens();
		}
	}
	
	@Override
	public void onCreate(Bundle state) {
		if (state != null) {
			savedSelection  = state.getLong(SELECTED_DATE, -1);
		}
		super.onCreate(state);
	}

	@AfterViews
	void setup() {
		final Calendar future = Calendar.getInstance();
		future.add(Calendar.YEAR, 1);
		final Calendar selected = Calendar.getInstance();
		// Must be greater/equal to today
		if (savedSelection >= selected.getTimeInMillis()) {
			// set date
			selected.setTimeInMillis(savedSelection);
		}
		else if (getArguments().getLong(SELECTED_DATE, -1) >= selected
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
		getDialog().requestWindowFeature(STYLE_NO_TITLE);
	}

	@Click(resName="dialog_no")
	void cancelClicked() {
		dismiss();
	}

	@Click(resName="dialog_yes")
	void okClicked() {
		if (listener != null) {
			listener.onDateSet(calendarView.getSelectedDate().getTime());
		}
		else {
			Log.d("JONAS", "No listener available to handle click");
		}

		this.dismiss();
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (calendarView != null) {
        	outState.putLong(SELECTED_DATE, calendarView.getSelectedDate().getTime());
        }
    }
}
