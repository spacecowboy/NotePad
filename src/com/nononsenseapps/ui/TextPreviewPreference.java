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
	}

	public TextPreviewPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {

		View layout = null;

		try {
			LayoutInflater mInflater = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			layout = mInflater.inflate(R.layout.preference_text_preview,
					parent, false);
		} catch (Exception e) {
			Log.e(TAG, "Error creating seek bar preference", e);
		}

		return layout;

	}

	@Override
	public void onBindView(View view) {
		super.onBindView(view);

		mText = (TextView) view.findViewById(R.id.prefTextPreview);
	}

	public void setTextType(String type) {
		if (mText != null) {
			Log.d(TAG, "mText type was not null");
			Typeface font;
			if (NotesPreferenceFragment.MONOSPACE.equals(type)) {
				font = Typeface.MONOSPACE;
			} else if (NotesPreferenceFragment.SERIF.equals(type)) {
				font = Typeface.SERIF;
			} else {
				font = Typeface.SANS_SERIF;
			}
			mText.setTypeface(font);
			mText.postInvalidate();
		}
	}

	public void setTextSize(float size) {
		if (mText != null) {
			Log.d(TAG, "mText size was not null");
			mText.setTextSize(size);
			mText.postInvalidate();
		}
	}

}
