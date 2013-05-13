package com.nononsenseapps.notepad;

import java.util.Locale;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.SeekBarProgressChange;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.utils.views.TitleNoteTextView;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

@EActivity(R.layout.activity_task_history)
public class ActivityTaskHistory extends FragmentActivity {
	public static final String RESULT_TEXT_KEY = "task_text_key";
	private long mTaskID;
	private boolean loaded = false;
	private Cursor mCursor;

	@ViewById
	SeekBar seekBar;

	@ViewById
	TitleNoteTextView taskText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Must do this before super.onCreate
		ActivityHelper.readAndSetSettings(this);
		super.onCreate(savedInstanceState);

		// Intent must contain a task id
		if (getIntent() == null
				|| getIntent().getLongExtra(Task.Columns._ID, -1) < 1) {
			setResult(RESULT_CANCELED, new Intent());
			finish();
		}
		else {
			mTaskID = getIntent().getLongExtra(Task.Columns._ID, -1);
		}
	}

	@AfterViews
	void loadLayout() {
		// Prepare for failure
		setResult(RESULT_CANCELED, new Intent());
		// Inflate a "Done/Discard" custom action bar view.
		LayoutInflater inflater = (LayoutInflater) getActionBar()
				.getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customActionBarView = inflater.inflate(
				R.layout.actionbar_custom_view_done_discard, null);
		customActionBarView.findViewById(R.id.actionbar_done)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Done"
						// TODO set result text in intent
						final Intent returnIntent = new Intent();
						returnIntent.putExtra(RESULT_TEXT_KEY, taskText
								.getText().toString());
						setResult(RESULT_OK, returnIntent);
						finish(); // TODO: don't just finish()!
					}
				});
		customActionBarView.findViewById(R.id.actionbar_discard)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Cancel result already set"
						finish();
					}
				});

		// Show the custom action bar view and hide the normal Home icon and
		// title.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView,
				new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));
		/*
		 * seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
		 * 
		 * @Override public void onStopTrackingTouch(SeekBar seekBar) { // TODO
		 * Auto-generated method stub
		 * 
		 * }
		 * 
		 * @Override public void onStartTrackingTouch(SeekBar seekBar) { // TODO
		 * Auto-generated method stub
		 * 
		 * }
		 * 
		 * @Override public void onProgressChanged(SeekBar seekBar, int
		 * progress, boolean fromUser) { // TODO Auto-generated method stub if
		 * (mCursor != null) { if (progress < mCursor.getCount()) {
		 * mCursor.moveToPosition(progress);
		 * taskText.setTextTitle(mCursor.getString(1));
		 * taskText.setTextRest(mCursor.getString(2)); } } } });
		 */
	}

	@Override
	public void onStart() {
		super.onStart();
		getSupportLoaderManager().restartLoader(0, null,
				new LoaderCallbacks<Cursor>() {

					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
						return new CursorLoader(ActivityTaskHistory.this,
								Task.URI_TASK_HISTORY,
								Task.Columns.HISTORY_COLUMNS,
								Task.Columns.HIST_TASK_ID + " IS ?",
								new String[] { Long.toString(mTaskID) }, null);
					}

					@Override
					public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
						mCursor = c;
						setSeekBarProperties();
						if (!loaded) {
							seekBar.setProgress(c.getCount() - 1);
							loaded = true;
						}
					}

					@Override
					public void onLoaderReset(Loader<Cursor> arg0) {
						mCursor = null;
						setSeekBarProperties();
					}
				});
	}

	@SeekBarProgressChange(R.id.seekBar)
	void onSeekBarChanged(int progress) {
		if (mCursor != null) {
			if (progress < mCursor.getCount()) {
				mCursor.moveToPosition(progress);
				taskText.setTextTitle(mCursor.getString(1));
				taskText.setTextRest(mCursor.getString(2));
			}
		}
	}

	void setSeekBarProperties() {
		if (mCursor == null) {
			seekBar.setEnabled(false);
			seekBar.setMax(0);
		}
		else {
			seekBar.setEnabled(true);
			seekBar.setMax(mCursor.getCount() - 1);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}
}
