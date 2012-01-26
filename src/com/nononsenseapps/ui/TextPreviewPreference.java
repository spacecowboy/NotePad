package com.nononsenseapps.ui;

import com.nononsenseapps.notepad.NotesPreferenceFragment;
import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TextPreviewPreference extends Preference {

	private final String TAG = getClass().getName();

	protected TextView mText = null;

	public TextPreviewPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG, "Constructor1");
	}

	public TextPreviewPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		Log.d(TAG, "Cosntructor2");
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		Log.d(TAG, "onCreateView");

		View layout = null;

		try {
			LayoutInflater mInflater = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			layout = mInflater.inflate(R.layout.preference_text_preview,
					parent, false);
		} catch (Exception e) {
			Log.e(TAG, "Error creating seek bar preference", e);
		}
		
		mText = (TextView) layout.findViewById(R.id.prefTextPreview);
		
		// Retrieve settings here and set them on the text
		mText.setTextSize(getSharedPreferences().getInt(NotesPreferenceFragment.KEY_FONT_SIZE_EDITOR, 22));
		setTextType(getSharedPreferences().getString(NotesPreferenceFragment.KEY_FONT_TYPE_EDITOR, NotesPreferenceFragment.SANS));

		return layout;

	}

	@Override
	public void onBindView(View view) {
		super.onBindView(view);
		Log.d(TAG, "onBindView");

		//mText = (TextView) view.findViewById(R.id.prefTextPreview);
	}

	public void setTextType(String type) {
		Log.d(TAG, "setTextType");
		if (mText != null) {
			Log.d(TAG, "mText type was not null: " + type);
			Typeface font;
			if (NotesPreferenceFragment.MONOSPACE.equals(type)) {
				font = Typeface.MONOSPACE;
			} else if (NotesPreferenceFragment.SERIF.equals(type)) {
				font = Typeface.SERIF;
			} else {
				font = Typeface.SANS_SERIF;
			}
			mText.setTypeface(font);
//			mText.post(new Runnable() {
//
//				@Override
//				public void run() {
//					Log.d(TAG, "Runnabletype: getText: " + mText.getText().toString());
//					mText.setTypeface(font);
//					mText.setText("Font changed in runnable");
//				}
//				
//			});
		}
	}

	public void setTextSize(float size) {
		Log.d(TAG, "setTextSize");
		if (mText != null) {
			Log.d(TAG, "mText size was not null: " + size);
			final float mySize = size;
			mText.post(new Runnable() {

				@Override
				public void run() {
					Log.d(TAG, "Runnablesize: getText: " + mText.getText().toString());
					mText.setTextSize(mySize);
					mText.setText("Size changed in runnable");
				}
				
			});
		}
	}

}
