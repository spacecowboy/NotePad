package com.nononsenseapps.notepad.widget;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.ui.ExtrasCursorAdapter;

import android.os.Bundle;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class ListWidgetConfig extends Activity {

	private static final String TAG = "ListWidgetConfig";

	public static final String KEY_LIST = "widget1_key_list";
	public static final String KEY_LIST_TITLE = "widget1_key_list_title";
	public static final String KEY_SORT_TYPE = "widget1_key_sort_type";
	public static final String KEY_SORT_ORDER = "widget1_key_sort_order";
	public static final String KEY_THEME = "widget1_key_current_theme";

	public static final String KEY_HIDDENAPPICON = "widget1_key_hiddenappicon";
	public static final String KEY_HIDDENNEW = "widget1_key_hiddennew";
	public static final String KEY_HIDDENHEADER = "widget1_key_hiddenheader";
	public static final String KEY_TRANSPARENT = "widget1_key_transparent";

	public static final String KEY_HIDDENNOTE = "widget1_key_hiddennote";
	public static final String KEY_HIDDENDATE = "widget1_key_hiddendate";
	public static final String KEY_HIDDENCHECKBOX = "widget1_key_hiddencheckbox";
	public static final String KEY_TITLEROWS = "widget1_key_titlerows";

	// Used in widget service/provider
	public static final String KEY_LOCKSCREEN = "widget1_key_lockscreen";

	private int appWidgetId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_widget_config);

		Log.d(TAG, "Setting initial result");
		setResult(RESULT_CANCELED);

		Log.d(TAG, "Getting id");
		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			Log.d(TAG, "Valid id: " + appWidgetId);
		} else {
			appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
			Log.d(TAG, "Invalid ID given in the intent");
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetId);
			setResult(RESULT_CANCELED, resultValue);
			finish();
		}

		final WidgetPrefs widgetPrefs = new WidgetPrefs(this, appWidgetId);

		if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
			Log.d(TAG, "Configstart appwidgetid: " + appWidgetId);

		//final String[] sortOrderValues = getResources().getStringArray(
		//		R.array.sorting_ordervalues_preference);
		final String[] sortTypeValues = getResources().getStringArray(
				R.array.sortingvalues_preference);
		final String[] titleRowsValues = getResources().getStringArray(
				R.array.title_rows_values);
		final String[] themeValues = getResources().getStringArray(
				R.array.widget_themevalues_preference);

		if (themeValues == null) {
			Log.d(TAG, "themevalues null");
		} else {
			for (String s : themeValues) {
				Log.d(TAG, "themevalue: " + s);
			}
		}

		/*
		Spinner sortOrderSpinner = (Spinner) findViewById(R.id.list_widget_config_sort_order);
		sortOrderSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						// An item was selected. You can retrieve the selected
						// item using
						// parent.getItemAtPosition(pos)
						Log.d(TAG, "order: " + sortOrderValues[pos]);
						widgetPrefs.putString(KEY_SORT_ORDER,
								sortOrderValues[pos]);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						// Another interface callback
					}
				});
*/
		Spinner sortTypeSpinner = (Spinner) findViewById(R.id.list_widget_config_sort_type);
		sortTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				// An item was selected. You can retrieve the selected item
				// using
				// parent.getItemAtPosition(pos)
				Log.d(TAG, "type: " + sortTypeValues[pos]);
				widgetPrefs.putString(KEY_SORT_TYPE, sortTypeValues[pos]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Another interface callback
			}
		});

		Spinner titleRowsSpinner = (Spinner) findViewById(R.id.list_widget_config_titlerows);
		titleRowsSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						// An item was selected. You can retrieve the selected
						// item using
						// parent.getItemAtPosition(pos)
						Log.d(TAG, "titlerows: " + titleRowsValues[pos]);
						widgetPrefs.putString(KEY_TITLEROWS,
								titleRowsValues[pos]);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						// Another interface callback
					}
				});

		Spinner themeSpinner = (Spinner) findViewById(R.id.list_widget_config_theme);
		themeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				// An item was selected. You can retrieve the selected item
				// using
				// parent.getItemAtPosition(pos)
				Log.d(TAG, "" + themeValues);
				Log.d(TAG, "theme: " + pos);
				widgetPrefs.putString(KEY_THEME, themeValues[pos]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Another interface callback
			}
		});

		CheckBox doneCheckBox = (CheckBox) findViewById(R.id.list_widget_config_hide_checkbox);
		doneCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Log.d(TAG, "checkbox: " + isChecked);
				widgetPrefs.putBoolean(KEY_HIDDENCHECKBOX, isChecked);
			}
		});

		CheckBox dateCheckBox = (CheckBox) findViewById(R.id.list_widget_config_hide_date);
		dateCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Log.d(TAG, "date: " + isChecked);
				widgetPrefs.putBoolean(KEY_HIDDENDATE, isChecked);
			}
		});

		CheckBox noteCheckBox = (CheckBox) findViewById(R.id.list_widget_config_hide_note);
		noteCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Log.d(TAG, "note: " + isChecked);
				widgetPrefs.putBoolean(KEY_HIDDENNOTE, isChecked);
			}
		});

		CheckBox appIconCheckBox = (CheckBox) findViewById(R.id.list_widget_config_hide_appicon);
		appIconCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						Log.d(TAG, "note: " + isChecked);
						widgetPrefs.putBoolean(KEY_HIDDENAPPICON, isChecked);
					}
				});

		CheckBox newCheckBox = (CheckBox) findViewById(R.id.list_widget_config_hide_new);
		newCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Log.d(TAG, "note: " + isChecked);
				widgetPrefs.putBoolean(KEY_HIDDENNEW, isChecked);
			}
		});

		CheckBox headerCheckBox = (CheckBox) findViewById(R.id.list_widget_config_hide_header);
		headerCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						Log.d(TAG, "note: " + isChecked);
						widgetPrefs.putBoolean(KEY_HIDDENHEADER, isChecked);
					}
				});

		CheckBox transparentCheckBox = (CheckBox) findViewById(R.id.list_widget_config_transparent);
		transparentCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						Log.d(TAG, "note: " + isChecked);
						widgetPrefs.putBoolean(KEY_TRANSPARENT, isChecked);
					}
				});

		Button cancelButton = (Button) findViewById(R.id.list_widget_config_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		Button okButton = (Button) findViewById(R.id.list_widget_config_ok);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Set success
				widgetPrefs.setPresent();
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);
				setResult(RESULT_OK, resultValue);

				// Build/Update widget
				AppWidgetManager appWidgetManager = AppWidgetManager
						.getInstance(getApplicationContext());
				Log.d(TAG, "finishing WidgetId " + appWidgetId);
				appWidgetManager.updateAppWidget(appWidgetId,
						ListWidgetProvider.buildRemoteViews(
								getApplicationContext(), appWidgetManager,
								appWidgetId, widgetPrefs));

				// Update list items
				appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,
						R.id.notes_list);

				// Destroy activity
				finish();
			}
		});

		Spinner listSpinner = (Spinner) findViewById(R.id.list_widget_config_list);
		setListEntries(listSpinner);
		listSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				// An item was selected. You can retrieve the selected item
				// using
				// parent.getItemAtPosition(pos)
				Log.d(TAG, "list spinner pos: " + pos);
				Log.d(TAG, "list spinner id: " + id);
				Log.d(TAG,
						"list spinner getItemIdAt: "
								+ parent.getItemIdAtPosition(pos));

				String title = "DUMMY";

				Object item = parent.getItemAtPosition(pos);
				if (item != null) {
					Log.d(TAG, "item not null: " + item);
					if (id > -1) {
						int col = ((Cursor) item)
								.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE);
						title = ((Cursor) item).getString(col);
					} else {
						title = item.toString();
					}
				}

				Log.d(TAG, "list spinner title: " + title);

				widgetPrefs.putString(KEY_LIST, Long.toString(id));
				widgetPrefs.putString(KEY_LIST_TITLE, title);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Another interface callback
			}
		});

		// Set previous values
		// List
		int pos = getListPositionOf(
				listSpinner,
				Long.parseLong(widgetPrefs.getString(KEY_LIST,
						Integer.toString(MainActivity.ALL_NOTES_ID))));
		if (pos > -1)
			listSpinner.setSelection(pos);

		// Sort type
		pos = getPositionOf(sortTypeValues,
				widgetPrefs.getString(KEY_SORT_TYPE, MainPrefs.DUEDATESORT));
		if (pos > -1)
			sortTypeSpinner.setSelection(pos);

		// Sort order
		/*
		pos = getPositionOf(sortOrderValues, widgetPrefs.getString(
				KEY_SORT_ORDER, NotePad.Notes.DEFAULT_SORT_ORDERING));
		if (pos > -1)
			sortOrderSpinner.setSelection(pos);
*/
		// Theme
		pos = getPositionOf(themeValues, widgetPrefs.getString(KEY_THEME,
				getString(R.string.const_theme_light)));
		if (pos > -1)
			themeSpinner.setSelection(pos);

		// title rows
		pos = getPositionOf(titleRowsValues,
				widgetPrefs.getString(KEY_TITLEROWS, "2"));
		if (pos > -1)
			titleRowsSpinner.setSelection(pos);

		// hide checkbox
		doneCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENCHECKBOX,
				false));

		// hide note
		noteCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENNOTE, false));

		// hide due date
		dateCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENDATE, false));

		// hide app icon
		appIconCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENAPPICON,
				false));

		// hide new button
		newCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENNEW, false));

		// hide header
		headerCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENHEADER,
				false));

		// transparent
		transparentCheckBox.setChecked(widgetPrefs.getBoolean(KEY_TRANSPARENT,
				false));
	}

	private void setListEntries(Spinner listSpinner) {
		Cursor cursor = getContentResolver().query(
				NotePad.Lists.CONTENT_VISIBLE_URI,
				new String[] { NotePad.Lists._ID,
						NotePad.Lists.COLUMN_NAME_TITLE }, null, null,
				NotePad.Lists.SORT_ORDER);
		if (cursor == null) {
			return;
		}

		ExtrasCursorAdapter mSpinnerAdapter = new ExtrasCursorAdapter(this,
				android.R.layout.simple_spinner_item, cursor,
				new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 },
				new int[] { MainActivity.ALL_NOTES_ID },
				new int[] { R.string.show_from_all_lists },
				android.R.layout.simple_dropdown_item_1line);

		mSpinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
		listSpinner.setAdapter(mSpinnerAdapter);
	}

	private int getListPositionOf(Spinner listSpinner, long listId) {
		SpinnerAdapter adapter = listSpinner.getAdapter();
		if (adapter == null || adapter.getCount() == 0)
			return -1;
		int pos = 0;
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItemId(i) == listId) {
				pos = i;
				break;
			}
		}
		return pos;
	}

	private int getPositionOf(String[] values, String selectedVal) {
		if (values == null || values.length == 0)
			return -1;
		int pos = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(selectedVal)) {
				pos = i;
				break;
			}
		}
		return pos;
	}
}
