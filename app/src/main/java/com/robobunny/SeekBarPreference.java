/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.robobunny;


import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.R;

/**
 * Found this on http://robobunny.com/wp/2011/08/13/android-seekbar-preference/
 *
 * A great thank you to Kirk Baucom for posting it online for others to use!
 *
 * It has been modified to handle attributes (strings etc) dynamically.
 */
public class SeekBarPreference extends Preference implements
		OnSeekBarChangeListener {

	// TODO useless, see app_pref_list.xml for instructions on how to remove this

	private static final int DEFAULT_VALUE = 50;

	private int mMaxValue = 100;
	private int mMinValue = 0;
	private int mInterval = 1;
	private int mCurrentValue;
	private String mUnitsLeft = "";
	private String mUnitsRight = "";
	private SeekBar mSeekBar;

	private TextView mStatusText;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPreference(context, attrs);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPreference(context, attrs);
	}

	private void initPreference(Context context, AttributeSet attrs) {
		setValuesFromXml(context, attrs);

	}

	private void setValuesFromXml(final Context context, final AttributeSet attrs) {

		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.SeekBarPreference, 0, 0);

		try {
			mMaxValue = a.getInt(R.styleable.SeekBarPreference_max, 100);
			mMinValue = a.getInt(R.styleable.SeekBarPreference_min, 0);
			mUnitsLeft = a.getString(R.styleable.SeekBarPreference_unitsLeft);
			if (mUnitsLeft == null)
				mUnitsLeft = "";
			String units = a.getString(R.styleable.SeekBarPreference_units);
			if (units == null)
				units = "";
			mUnitsRight = a.getString(R.styleable.SeekBarPreference_unitsRight);
			if (mUnitsRight == null)
				mUnitsRight = "";
			if (mUnitsRight.isEmpty()) {
				mUnitsRight = units;
			}
			mCurrentValue = a.getInt(R.styleable.SeekBarPreference_summary, 100);
			mInterval = a.getInt(R.styleable.SeekBarPreference_interval, 1);
		} finally {
			a.recycle();
		}


	}


	@Override
	protected View onCreateView(ViewGroup parent) {

		super.onCreateView(parent);
		View layout = null;

		try {
			LayoutInflater mInflater = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			layout = mInflater.inflate(
					R.layout.preference_widget_seekbar, parent, false);
		} catch (Exception e) {
			NnnLogger.error(SeekBarPreference.class, "Error creating seek bar preference:");
			NnnLogger.exception(e);
		}

		return layout;

	}


	@Override
	public void onBindView(View view) {
		super.onBindView(view);

		// Set seekbar
		mSeekBar = view.findViewById(R.id.prefSeekBar);
		mSeekBar.setMax(mMaxValue - mMinValue);
		mSeekBar.setOnSeekBarChangeListener(this);

		mStatusText = view.findViewById(R.id.seekbarPrefValue);

		// Sets the text etc
		updateView(view);
	}

	/**
	 * Update a SeekBarPreference view with our current state
	 */
	protected void updateView(View layout) {

		try {

			mStatusText.setText(String.valueOf(mCurrentValue));

			mSeekBar.setProgress(mCurrentValue - mMinValue);

			TextView unitsRight = layout
					.findViewById(R.id.seekBarPrefUnitsRight);
			unitsRight.setText(mUnitsRight);

			TextView unitsLeft = layout
					.findViewById(R.id.seekBarPrefUnitsLeft);
			unitsLeft.setText(mUnitsLeft);

		} catch (Exception e) {
			NnnLogger.error(SeekBarPreference.class, "Error updating seek bar preference");
			NnnLogger.exception(e);
		}

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
								  boolean fromUser) {
		int newValue = progress + mMinValue;

		if (newValue > mMaxValue)
			newValue = mMaxValue;
		else if (newValue < mMinValue)
			newValue = mMinValue;
		else if (mInterval != 1 && newValue % mInterval != 0)
			newValue = Math.round(((float) newValue) / mInterval) * mInterval;

		// change rejected, revert to the previous value
//		if (!callChangeListener(newValue)) {
//			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Rejected change. resetting.");
//			seekBar.setProgress(mCurrentValue - mMinValue);
//			return;
//		}

		// change accepted, store it
		mCurrentValue = newValue;
		mStatusText.setText(String.valueOf(newValue));
		persistInt(newValue);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		notifyChanged();
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {

		return ta.getInt(index, DEFAULT_VALUE);

	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		if (restoreValue) {
			mCurrentValue = getPersistedInt(mCurrentValue);
		} else {
			int temp = 0;
			try {
				temp = (Integer) defaultValue;
			} catch (Exception ex) {
				NnnLogger.error(SeekBarPreference.class,
						"Invalid default value: " + defaultValue.toString());
				NnnLogger.exception(ex);
			}

			persistInt(temp);
			mCurrentValue = temp;
		}

	}
}
