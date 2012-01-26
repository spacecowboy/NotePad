package com.robobunny;

/**
 * Found this on http://robobunny.com/wp/2011/08/13/android-seekbar-preference/
 * 
 * A great thank you to Kirk Baucom for posting it online for others to use!
 */

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements
		OnSeekBarChangeListener {

	private final String TAG = getClass().getName();

	private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
	private static final String ROBOBUNNYNS = "http://robobunny.com";
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
		setValuesFromXml(attrs);
		//mSeekBar = new SeekBar(context, attrs);
		//mSeekBar.setMax(mMaxValue - mMinValue);
		//mSeekBar.setOnSeekBarChangeListener(this);	
		}

	private void setValuesFromXml(AttributeSet attrs) {
		mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 100);
		mMinValue = attrs.getAttributeIntValue(ROBOBUNNYNS, "min", 0);

		mUnitsLeft = getAttributeStringValue(attrs, ROBOBUNNYNS, "unitsLeft",
				"");
		String units = getAttributeStringValue(attrs, ROBOBUNNYNS, "units", "");
		mUnitsRight = getAttributeStringValue(attrs, ROBOBUNNYNS, "unitsRight",
				units);
		
		mCurrentValue = attrs.getAttributeIntValue(ANDROIDNS, "summary", 100);

		try {
			String newInterval = attrs.getAttributeValue(ROBOBUNNYNS,
					"interval");
			if (newInterval != null)
				mInterval = Integer.parseInt(newInterval);
		} catch (Exception e) {
			Log.e(TAG, "Invalid interval value", e);
		}

	}

	private String getAttributeStringValue(AttributeSet attrs,
			String namespace, String name, String defaultValue) {
		String value = attrs.getAttributeValue(namespace, name);
		if (value == null)
			value = defaultValue;

		return value;
	}

	@Override
	protected View onCreateView(ViewGroup parent) {

		View layout = null;

		try {
			LayoutInflater mInflater = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			layout =  mInflater.inflate(
					R.layout.preference_widget_seekbar, parent, false);
		} catch (Exception e) {
			Log.e(TAG, "Error creating seek bar preference", e);
		}

		return layout;

	}

	
	@Override
	public void onBindView(View view) {
		super.onBindView(view);
		
		// Set seekbar
		mSeekBar = (SeekBar) view.findViewById(R.id.prefSeekBar);
		mSeekBar.setMax(mMaxValue - mMinValue);
		mSeekBar.setOnSeekBarChangeListener(this);
		
		mStatusText = (TextView) view.findViewById(R.id.seekbarPrefValue);

		// Sets the text etc
		updateView(view);
	}

	/**
	 * Update a SeekBarPreference view with our current state
	 * 
	 * @param view
	 */
	protected void updateView(View layout) {

		try {
			//RelativeLayout layout = (RelativeLayout) view;
			

			//mStatusText = (TextView) layout.findViewById(android.R.id.summary);
			mStatusText.setText(String.valueOf(mCurrentValue));

			mSeekBar.setProgress(mCurrentValue - mMinValue);

			TextView unitsRight = (TextView) layout
					.findViewById(R.id.seekBarPrefUnitsRight);
			unitsRight.setText(mUnitsRight);

			TextView unitsLeft = (TextView) layout
					.findViewById(R.id.seekBarPrefUnitsLeft);
			unitsLeft.setText(mUnitsLeft);

		} catch (Exception e) {
			Log.e(TAG, "Error updating seek bar preference", e);
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
		
		Log.d(TAG, "onProgressChanged new value: " + newValue);

		// change rejected, revert to the previous value
		if (!callChangeListener(newValue)) {
			Log.d(TAG, "Rejected change. resetting.");
			seekBar.setProgress(mCurrentValue - mMinValue);
			return;
		}

		// change accepted, store it
		mCurrentValue = newValue;
		Log.d(TAG, "onProgressChanged mCurrentvalue: " + mCurrentValue);
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

		int defaultValue = ta.getInt(index, DEFAULT_VALUE);
		return defaultValue;

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
				Log.e(TAG, "Invalid default value: " + defaultValue.toString());
			}

			persistInt(temp);
			mCurrentValue = temp;
		}

	}

}
