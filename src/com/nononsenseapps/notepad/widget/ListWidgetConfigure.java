package com.nononsenseapps.notepad.widget;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotesPreferenceFragment;
import com.nononsenseapps.notepad.R;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Spinner;

public class ListWidgetConfigure extends Activity {

	private int appWidgetId;
	private Spinner listSpinner;
	private Spinner sortTypeSpinner;
	private Spinner sortOrderSpinner;

	public static final String SHARED_PREFS_BASE = "prefs_widget_";
	public static final String LIST_WHERE = "listWhere";
	public static final String LIST_ID = "listId";

	public static String getSharedPrefsFile(int widgetId) {
		return SHARED_PREFS_BASE + widgetId;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		} else {
			appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		}
		// Do this in case users backs out
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_CANCELED, resultValue);

		// Inflate UI
		setContentView(R.layout.list_widget_configure);

		// Get spinners
		listSpinner = (Spinner) findViewById(R.id.list_widget_config_list);
		sortTypeSpinner = (Spinner) findViewById(R.id.list_widget_config_sort_type);
		sortOrderSpinner = (Spinner) findViewById(R.id.list_widget_config_sort_order);

		// Populate list spinner with the list titles
		populateListSpinner();

		// Set button listeners

		findViewById(R.id.list_widget_config_cancel).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						configCancel();
					}
				});

		findViewById(R.id.list_widget_config_ok).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						configDone();
					}
				});
	}

	private void populateListSpinner() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private void configCancel() {
		// Not much else to do
		finish();
	}

	private void configDone() {
		// Save values to preferences
		getSharedPreferences(getSharedPrefsFile(appWidgetId), MODE_PRIVATE)
				.edit()
				.putString(LIST_WHERE, "")
				.putString(NotesPreferenceFragment.KEY_SORT_TYPE,
						NotesPreferenceFragment.DUEDATESORT)
				.putString(NotesPreferenceFragment.KEY_SORT_ORDER, NotePad.Notes.DEFAULT_SORT_ORDERING)
				.commit();

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		appWidgetManager.updateAppWidget(appWidgetId,
				ListWidgetProvider.buildRemoteViews(this, appWidgetId));
		// AppWidgetManager.getInstance(mContext)
		// .notifyAppWidgetViewDataChanged(mAppWidgetId,
		// R.id.notes_list);
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}
}
