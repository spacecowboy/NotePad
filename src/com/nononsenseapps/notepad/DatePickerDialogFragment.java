/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad.prefs.MainPrefs;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.format.Time;
import android.widget.CalendarView;

public class DatePickerDialogFragment extends DialogFragment {
	private NotesEditorFragment mFragment = null;

	public void setCallback(NotesEditorFragment fragment) {
		mFragment = fragment;
	}

	@Override
	public void onCreate(Bundle saves) {
		super.onCreate(saves);
		if (mFragment == null) {
			// Device was rotated perhaps
			// This activity was destroyed and restarted. Can't save the
			// callback then.
			dismiss();
		}
	}
	
    
    /**
     * Get first day of week as android.text.format.Time constant.
     *
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String pref = prefs.getString(MainPrefs.KEY_WEEK_START_DAY, MainPrefs.WEEK_START_DEFAULT);

        int startDay;
        if (MainPrefs.WEEK_START_DEFAULT.equals(pref)) {
            startDay = Calendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = Integer.parseInt(pref);
        }

        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (mFragment == null) {
			dismiss();
			return super.onCreateDialog(savedInstanceState);
		}
		else{
			Activity mActivity = getActivity();
			DatePickerDialog dpd = new DatePickerDialog(mActivity, mFragment,
					mFragment.year, mFragment.month, mFragment.day);
			CalendarView cv = dpd.getDatePicker().getCalendarView();
	        cv.setShowWeekNumber(true);
	        int startOfWeek = getFirstDayOfWeek(mActivity);
	        // Utils returns Time days while CalendarView wants Calendar days
	        if (startOfWeek == Time.SATURDAY) {
	            startOfWeek = Calendar.SATURDAY;
	        } else if (startOfWeek == Time.SUNDAY) {
	            startOfWeek = Calendar.SUNDAY;
	        } else {
	            startOfWeek = Calendar.MONDAY;
	        }
	        cv.setFirstDayOfWeek(startOfWeek);
	        dpd.setCanceledOnTouchOutside(true);
        	return dpd;
		}
	}
}
