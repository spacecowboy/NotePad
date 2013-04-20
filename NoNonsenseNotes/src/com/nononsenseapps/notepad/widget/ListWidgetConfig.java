package com.nononsenseapps.notepad.widget;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;
import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.widget.ListWidgetConfig.SimpleWidgetPreviewAdapter;
import com.nononsenseapps.ui.ExtrasCursorAdapter;
import com.nononsenseapps.utils.views.TitleNoteTextView;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.method.HideReturnsTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

@EActivity(R.layout.activity_widget_config)
public class ListWidgetConfig extends FragmentActivity {
	private static final String TAG = "ListWidgetConfig";
	public static final String KEY_LIST = "widget1_key_list";
	public static final String KEY_LIST_TITLE = "widget1_key_list_title";
	public static final String KEY_SORT_TYPE = "widget1_key_sort_type";
	// public static final String KEY_SORT_ORDER = "widget1_key_sort_order";
	public static final String KEY_THEME = "widget1_key_current_theme";
	public static final String KEY_TEXTPRIMARY = "widget1_key_primary_text";
	public static final String KEY_TEXTSECONDARY = "widget1_key_secondary_text";
	// public static final String KEY_HIDDENAPPICON =
	// "widget1_key_hiddenappicon";
	// public static final String KEY_HIDDENNEW = "widget1_key_hiddennew";
	public static final String KEY_HIDDENHEADER = "widget1_key_hiddenheader";
	public static final String KEY_SHADE_COLOR = "widget1_key_shadecolor";
	// public static final String KEY_HIDDENNOTE = "widget1_key_hiddennote";
	public static final String KEY_HIDDENDATE = "widget1_key_hiddendate";
	public static final String KEY_HIDDENCHECKBOX = "widget1_key_hiddencheckbox";
	public static final String KEY_TITLEROWS = "widget1_key_titlerows";
	// Used in widget service/provider
	public static final String KEY_LOCKSCREEN = "widget1_key_lockscreen";

	public final static int THEME_DARK = 0;
	public final static int THEME_LIGHT = 1;

	// These are the default widget values
	public final static int DEFAULT_THEME = THEME_DARK;
	// 75% translucent black
	public final static int DEFAULT_SHADE = 0xC0000000;
	// White (android primary dark)
	public final static int DEFAULT_TEXTPRIMARY = 0xff000000;
	// Greyish (android secondary dark)
	public final static int DEFAULT_TEXTSECONDARY = 0xffbebebe;
	// Number of rows
	public final static int DEFAULT_ROWS = 3;

	@ViewById
	View widgetPreviewWrapper;

	@ViewById
	Spinner listSpinner;

	@ViewById
	Spinner sortingSpinner;

	@ViewById
	SeekBar itemRowsSeekBar;

	@ViewById
	SeekBar transparencySeekBar;

	@ViewById
	Spinner themeSpinner;

	@ViewById
	ImageView shade;

	@ViewById
	ListView notesList;

	@ViewById
	TextView titleButton;

	@ViewById
	View widgetHeader;

	@ViewById
	CheckBox transparentHeaderCheckBox;

	@ViewById
	CheckBox hideCheckBox;

	@ViewById
	CheckBox hideDateCheckBox;

	private int appWidgetId;

	private SimpleWidgetPreviewAdapter mNotesAdapter;

	private LoaderCallbacks<Cursor> mCallback;

	private SimpleCursorAdapter mListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		else {
			appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
			Log.d(TAG, "Invalid ID given in the intent");
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetId);
			setResult(RESULT_CANCELED, resultValue);
			finish();
		}
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@AfterViews
	void setupPreview() {
		// Setup wallpaper preview
		WallpaperManager wpm = WallpaperManager.getInstance(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			// TODO fix aspect ratio
			//widgetPreviewWrapper.setBackground(wpm.getDrawable());
		}
		else {
			//widgetPreviewWrapper.setBackgroundDrawable(wpm.getDrawable());
		}

		final WidgetPrefs widgetPrefs = new WidgetPrefs(this, appWidgetId);

		mNotesAdapter = new SimpleWidgetPreviewAdapter(this,
				R.layout.widgetlist_item, R.layout.widgetlist_header, null,
				new String[] { Task.Columns.TITLE, Task.Columns.DUE,
						Task.Columns.COMPLETED, Task.Columns.COMPLETED,
						Task.Columns.COMPLETED }, new int[] {
						android.R.id.text1, R.id.dueDate,
						R.id.completedCheckBoxDark, R.id.itemSpacer,
						R.id.completedCheckBoxLight }, 0);
		mNotesAdapter.setViewBinder(new ViewBinder() {
			final WidgetPrefs widgetPrefs = new WidgetPrefs(
					ListWidgetConfig.this, appWidgetId);
			boolean isHeader = false;
			String sTemp = "";
			SimpleDateFormat weekdayFormatter = TimeFormatter
					.getLocalFormatterWeekday(ListWidgetConfig.this);

			@Override
			public boolean setViewValue(View view, Cursor c, int colIndex) {
				// Check for headers, they have invalid ids
				isHeader = c.getLong(0) == -1;
				switch (colIndex) {
				case 1:
					if (isHeader) {
						sTemp = c.getString(1);

						if (Task.HEADER_KEY_OVERDUE.equals(sTemp)) {
							sTemp = getString(R.string.date_header_overdue);
						}
						else if (Task.HEADER_KEY_TODAY.equals(sTemp)) {
							sTemp = getString(R.string.date_header_today);
						}
						else if (Task.HEADER_KEY_PLUS1.equals(sTemp)) {
							sTemp = getString(R.string.date_header_tomorrow);
						}
						else if (Task.HEADER_KEY_PLUS2.equals(sTemp)
								|| Task.HEADER_KEY_PLUS3.equals(sTemp)
								|| Task.HEADER_KEY_PLUS4.equals(sTemp)) {
							sTemp = weekdayFormatter.format(new Date(c
									.getLong(4)));
						}
						else if (Task.HEADER_KEY_LATER.equals(sTemp)) {
							sTemp = getString(R.string.date_header_future);
						}
						else if (Task.HEADER_KEY_NODATE.equals(sTemp)) {
							sTemp = getString(R.string.date_header_none);
						}
						else if (Task.HEADER_KEY_COMPLETE.equals(sTemp)) {
							sTemp = getString(R.string.date_header_completed);
						}

						((TextView) view).setText(TitleNoteTextView
								.getStyledText(sTemp, 1.3f, 1, 1));
					}
					else {
						((TextView) view).setText(TitleNoteTextView
								.getStyledText(c.getString(1), c.getString(2),
										1.3f, 1, 1));
						final int rows = widgetPrefs.getInt(KEY_TITLEROWS,
								DEFAULT_ROWS);
						((TextView) view).setMaxLines(rows < 1 ? 1 : rows);
					}
					// Set color
					((TextView) view).setTextColor(widgetPrefs.getInt(
							KEY_TEXTPRIMARY, DEFAULT_TEXTPRIMARY));
					return true;
				case 2:
					// already done.
					return true;
				case 3:
					// Complete checkbox
					boolean visible;
					if (view.getId() == R.id.completedCheckBoxLight) {
						visible = THEME_LIGHT == widgetPrefs.getInt(KEY_THEME,
								DEFAULT_THEME);
					}
					else if (view.getId() == R.id.completedCheckBoxDark) {
						visible = THEME_DARK == widgetPrefs.getInt(KEY_THEME,
								DEFAULT_THEME);
					}
					else {
						// Spacer
						visible = true;
					}
					visible &= !widgetPrefs.getBoolean(KEY_HIDDENCHECKBOX,
							false);
					view.setVisibility(visible ? View.VISIBLE : View.GONE);
					return true;
				case 4:
					// Date
					view.setVisibility(widgetPrefs.getBoolean(KEY_HIDDENDATE,
							false) ? View.GONE : View.VISIBLE);
					((TextView) view).setTextColor(widgetPrefs.getInt(
							KEY_TEXTPRIMARY, DEFAULT_TEXTPRIMARY));
					return true;
				default:
					return false;
				}
			}
		});

		notesList.setAdapter(mNotesAdapter);

		mCallback = new LoaderCallbacks<Cursor>() {

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {

				if (id == 1) {
					return new CursorLoader(ListWidgetConfig.this,
							TaskList.URI, TaskList.Columns.FIELDS, null, null,
							TaskList.Columns.TITLE);
				}
				else {
					final Uri targetUri;

					final long listId = widgetPrefs.getLong(KEY_LIST, -1);
					final String sortSpec;
					final String sortType = widgetPrefs
							.getString(KEY_SORT_TYPE,
									getString(R.string.default_sorttype));

					if (sortType.equals(getString(R.string.const_possubsort))
							&& listId > 0) {
						targetUri = Task.URI_INDENTED_QUERY;
						sortSpec = null;
					}
					else if (sortType
							.equals(getString(R.string.const_modified))) {
						targetUri = Task.URI;
						sortSpec = Task.Columns.UPDATED + " DESC";
					}
					// due date sorting
					else if (sortType.equals(getString(R.string.const_duedate))
							&& listId > 0) {
						targetUri = Task.URI_SECTIONED_BY_DATE;
						sortSpec = null;
					}
					// Alphabetic
					else {
						targetUri = Task.URI;
						sortSpec = Task.Columns.TITLE;
					}

					return new CursorLoader(ListWidgetConfig.this, targetUri,
							Task.Columns.FIELDS, Task.Columns.DBLIST + " IS ?",
							new String[] { Long.toString(listId) }, sortSpec);
				}
			}

			@Override
			public void onLoadFinished(Loader<Cursor> l, Cursor c) {
				if (l.getId() == 1) {
					mListAdapter.swapCursor(c);
					// Set default item
					widgetPrefs.putLong(KEY_LIST, mListAdapter.getItemId(0));
					widgetPrefs.putString(KEY_LIST_TITLE,
							((Cursor) mListAdapter.getItem(0)).getString(1));
					titleButton.setText(widgetPrefs.getString(KEY_LIST_TITLE,
							""));
					reloadTasks();
				}
				else {
					mNotesAdapter.swapCursor(c);
				}
			}

			@Override
			public void onLoaderReset(Loader<Cursor> l) {
				if (l.getId() == 1) {
					mListAdapter.swapCursor(null);
				}
				else {
					mNotesAdapter.swapCursor(null);
				}
			}

		};
		getSupportLoaderManager().restartLoader(1, null, mCallback);
	}

	void reloadTasks() {
		getSupportLoaderManager().restartLoader(0, null, mCallback);
	}

	@AfterViews
	void setupActionBar() {
		final WidgetPrefs widgetPrefs = new WidgetPrefs(this, appWidgetId);

		LayoutInflater inflater = (LayoutInflater) getActionBar()
				.getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customActionBarView = inflater.inflate(
				R.layout.actionbar_custom_view_done, null);
		customActionBarView.findViewById(R.id.actionbar_done)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Done"
						// // Set success
						widgetPrefs.setPresent();
						Intent resultValue = new Intent();
						resultValue.putExtra(
								AppWidgetManager.EXTRA_APPWIDGET_ID,
								appWidgetId);
						setResult(RESULT_OK, resultValue);
						// Build/Update widget
						AppWidgetManager appWidgetManager = AppWidgetManager
								.getInstance(getApplicationContext());
						// Log.d(TAG, "finishing WidgetId " + appWidgetId);
						appWidgetManager.updateAppWidget(appWidgetId,
								ListWidgetProvider.buildRemoteViews(
										getApplicationContext(),
										appWidgetManager, appWidgetId,
										widgetPrefs));
						// Update list items
						appWidgetManager.notifyAppWidgetViewDataChanged(
								appWidgetId, R.id.notesList);
						// Destroy activity
						finish();
					}
				});
		// Show the custom action bar view and hide the normal Home icon and
		// title.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView);
	}

	@AfterViews
	void setupConfig() {

		final WidgetPrefs widgetPrefs = new WidgetPrefs(this, appWidgetId);

		// final String[] sortOrderValues = getResources().getStringArray(
		// R.array.sorting_ordervalues_preference);
		final String[] sortTypeValues = getResources().getStringArray(
				R.array.sortingvalues_preference);
		final String[] titleRowsValues = getResources().getStringArray(
				R.array.title_rows_values);
		final String[] themeValues = getResources().getStringArray(
				R.array.widget_themevalues_preference);

		if (themeValues == null) {
			Log.d(TAG, "themevalues null");
		}
		else {
			for (String s : themeValues) {
				Log.d(TAG, "themevalue: " + s);
			}
		}

		sortingSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				widgetPrefs.putString(KEY_SORT_TYPE, sortTypeValues[pos]);
				// Need to recreate loader for this
				reloadTasks();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		themeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				final String theme = parent.getItemAtPosition(pos).toString();
				final int mTheme;
				final int primaryTextColor;
				final int secondaryTextColor;
				if (theme
						.equals(getString(R.string.settings_summary_theme_light))) {
					mTheme = THEME_LIGHT;
					primaryTextColor = getResources().getColor(
							android.R.color.primary_text_light);
					secondaryTextColor = getResources().getColor(
							android.R.color.secondary_text_light);

				}
				else {
					mTheme = THEME_DARK;
					primaryTextColor = getResources().getColor(
							android.R.color.primary_text_dark);
					secondaryTextColor = getResources().getColor(
							android.R.color.secondary_text_dark);
				}
				widgetPrefs.putInt(KEY_THEME, mTheme);
				widgetPrefs.putInt(KEY_TEXTPRIMARY, primaryTextColor);
				widgetPrefs.putInt(KEY_TEXTSECONDARY, secondaryTextColor);
				updateTheme(mTheme, widgetPrefs);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		itemRowsSeekBar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// Plus one since seekbars start at zero
						widgetPrefs.putInt(KEY_TITLEROWS, progress + 1);
						// Only need to reload existing loader
						mNotesAdapter.notifyDataSetChanged();
					}
				});

		transparencySeekBar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// final int color =
						// getHomescreenBackgroundColor(progress, 0xffffff);
						final int color = getHomescreenBackgroundColor(
								progress, widgetPrefs.getInt(KEY_SHADE_COLOR,
										DEFAULT_SHADE));

						widgetPrefs.putInt(KEY_SHADE_COLOR, color);
						updateBG(color);
					}
				});
		// Set default value
		final int progress = transparencySeekBar.getProgress();
		final int color = getHomescreenBackgroundColor(progress, DEFAULT_SHADE);
		widgetPrefs.putInt(KEY_SHADE_COLOR, color);
		updateBG(color);

		listSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapter, View arg1,
					int pos, long id) {
				widgetPrefs.putLong(KEY_LIST, id);
				widgetPrefs.putString(KEY_LIST_TITLE,
						((Cursor) adapter.getItemAtPosition(pos)).getString(1));

				// Need to reload tasks
				reloadTasks();
				// And set title
				titleButton.setText(widgetPrefs.getString(KEY_LIST_TITLE, ""));
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		mListAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_dropdown_item_1line, null,
				new String[] { TaskList.Columns.TITLE },
				new int[] { android.R.id.text1 }, 0);
		listSpinner.setAdapter(mListAdapter);

		transparentHeaderCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						widgetHeader.setVisibility(isChecked ? View.GONE
								: View.VISIBLE);
						widgetPrefs.putBoolean(KEY_HIDDENHEADER, isChecked);
					}
				});

		hideCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				widgetPrefs.putBoolean(KEY_HIDDENCHECKBOX, isChecked);
				if (mNotesAdapter != null)
					mNotesAdapter.notifyDataSetChanged();
			}
		});

		hideDateCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						widgetPrefs.putBoolean(KEY_HIDDENDATE, isChecked);
						if (mNotesAdapter != null)
							mNotesAdapter.notifyDataSetChanged();
					}
				});

		// Spinner titleRowsSpinner = (Spinner)
		// findViewById(R.id.list_widget_config_titlerows);
		// titleRowsSpinner
		// .setOnItemSelectedListener(new OnItemSelectedListener() {
		// @Override
		// public void onItemSelected(AdapterView<?> parent,
		// View view, int pos, long id) {
		// // An item was selected. You can retrieve the selected
		// // item using
		// // parent.getItemAtPosition(pos)
		// Log.d(TAG, "titlerows: " + titleRowsValues[pos]);
		// widgetPrefs.putString(KEY_TITLEROWS,
		// titleRowsValues[pos]);
		// }
		//
		// @Override
		// public void onNothingSelected(AdapterView<?> parent) {
		// // Another interface callback
		// }
		// });

		// Spinner themeSpinner = (Spinner)
		// findViewById(R.id.list_widget_config_theme);
		// themeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
		// @Override
		// public void onItemSelected(AdapterView<?> parent, View view,
		// int pos, long id) {
		// // An item was selected. You can retrieve the selected item
		// // using
		// // parent.getItemAtPosition(pos)
		// Log.d(TAG, "" + themeValues);
		// Log.d(TAG, "theme: " + pos);
		// widgetPrefs.putString(KEY_THEME, themeValues[pos]);
		// }
		//
		// @Override
		// public void onNothingSelected(AdapterView<?> parent) {
		// // Another interface callback
		// }
		// });
		//
		// CheckBox doneCheckBox = (CheckBox)
		// findViewById(R.id.list_widget_config_hide_checkbox);
		// doneCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		// {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// Log.d(TAG, "checkbox: " + isChecked);
		// widgetPrefs.putBoolean(KEY_HIDDENCHECKBOX, isChecked);
		// }
		// });
		//
		// CheckBox dateCheckBox = (CheckBox)
		// findViewById(R.id.list_widget_config_hide_date);
		// dateCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		// {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// Log.d(TAG, "date: " + isChecked);
		// widgetPrefs.putBoolean(KEY_HIDDENDATE, isChecked);
		// }
		// });
		//
		// CheckBox noteCheckBox = (CheckBox)
		// findViewById(R.id.list_widget_config_hide_note);
		// noteCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		// {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// Log.d(TAG, "note: " + isChecked);
		// widgetPrefs.putBoolean(KEY_HIDDENNOTE, isChecked);
		// }
		// });
		//
		// CheckBox appIconCheckBox = (CheckBox)
		// findViewById(R.id.list_widget_config_hide_appicon);
		// appIconCheckBox
		// .setOnCheckedChangeListener(new OnCheckedChangeListener() {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// Log.d(TAG, "note: " + isChecked);
		// widgetPrefs.putBoolean(KEY_HIDDENAPPICON, isChecked);
		// }
		// });
		//
		// CheckBox newCheckBox = (CheckBox)
		// findViewById(R.id.list_widget_config_hide_new);
		// newCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		// {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// Log.d(TAG, "note: " + isChecked);
		// widgetPrefs.putBoolean(KEY_HIDDENNEW, isChecked);
		// }
		// });
		//
		// CheckBox headerCheckBox = (CheckBox)
		// findViewById(R.id.list_widget_config_hide_header);
		// headerCheckBox
		// .setOnCheckedChangeListener(new OnCheckedChangeListener() {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// Log.d(TAG, "note: " + isChecked);
		// widgetPrefs.putBoolean(KEY_HIDDENHEADER, isChecked);
		// }
		// });
		//
		// CheckBox transparentCheckBox = (CheckBox)
		// findViewById(R.id.list_widget_config_transparent);
		// transparentCheckBox
		// .setOnCheckedChangeListener(new OnCheckedChangeListener() {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// Log.d(TAG, "note: " + isChecked);
		// widgetPrefs.putBoolean(KEY_TRANSPARENT, isChecked);
		// }
		// });
		//
		// Button cancelButton = (Button)
		// findViewById(R.id.list_widget_config_cancel);
		// cancelButton.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// setResult(RESULT_CANCELED);
		// finish();
		// }
		// });
		//
		// Button okButton = (Button) findViewById(R.id.list_widget_config_ok);
		// okButton.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// // Set success
		// widgetPrefs.setPresent();
		// Intent resultValue = new Intent();
		// resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
		// appWidgetId);
		// setResult(RESULT_OK, resultValue);
		//
		// // Build/Update widget
		// AppWidgetManager appWidgetManager = AppWidgetManager
		// .getInstance(getApplicationContext());
		// Log.d(TAG, "finishing WidgetId " + appWidgetId);
		// appWidgetManager.updateAppWidget(appWidgetId,
		// ListWidgetProvider.buildRemoteViews(
		// getApplicationContext(), appWidgetManager,
		// appWidgetId, widgetPrefs));
		//
		// // Update list items
		// appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,
		// R.id.notes_list);
		//
		// // Destroy activity
		// finish();
		// }
		// });
		//
		// Spinner listSpinner = (Spinner)
		// findViewById(R.id.list_widget_config_list);
		// setListEntries(listSpinner);
		// listSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
		// @Override
		// public void onItemSelected(AdapterView<?> parent, View view,
		// int pos, long id) {
		// // An item was selected. You can retrieve the selected item
		// // using
		// // parent.getItemAtPosition(pos)
		// Log.d(TAG, "list spinner pos: " + pos);
		// Log.d(TAG, "list spinner id: " + id);
		// Log.d(TAG,
		// "list spinner getItemIdAt: "
		// + parent.getItemIdAtPosition(pos));
		//
		// String title = "DUMMY";
		//
		// Object item = parent.getItemAtPosition(pos);
		// if (item != null) {
		// Log.d(TAG, "item not null: " + item);
		// if (id > -1) {
		// int col = ((Cursor) item)
		// .getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE);
		// title = ((Cursor) item).getString(col);
		// }
		// else {
		// title = item.toString();
		// }
		// }
		//
		// Log.d(TAG, "list spinner title: " + title);
		//
		// widgetPrefs.putString(KEY_LIST, Long.toString(id));
		// widgetPrefs.putString(KEY_LIST_TITLE, title);
		// }
		//
		// @Override
		// public void onNothingSelected(AdapterView<?> parent) {
		// // Another interface callback
		// }
		// });
		//
		// // Set previous values
		// // List
		// int pos = getListPositionOf(
		// listSpinner,
		// Long.parseLong(widgetPrefs.getString(KEY_LIST,
		// Integer.toString(MainActivity.ALL_NOTES_ID))));
		// if (pos > -1) listSpinner.setSelection(pos);
		//
		// // Sort type
		// pos = getPositionOf(sortTypeValues, widgetPrefs.getString(
		// KEY_SORT_TYPE, getString(R.string.default_sorttype)));
		// // if (pos > -1) sortTypeSpinner.setSelection(pos);
		//
		// // Sort order
		// /*
		// * pos = getPositionOf(sortOrderValues, widgetPrefs.getString(
		// * KEY_SORT_ORDER, NotePad.Notes.DEFAULT_SORT_ORDERING)); if (pos >
		// -1)
		// * sortOrderSpinner.setSelection(pos);
		// */
		// // Theme
		// pos = getPositionOf(themeValues, widgetPrefs.getString(KEY_THEME,
		// getString(R.string.const_theme_light)));
		// if (pos > -1) themeSpinner.setSelection(pos);
		//
		// // title rows
		// pos = getPositionOf(titleRowsValues,
		// widgetPrefs.getString(KEY_TITLEROWS, "2"));
		// if (pos > -1) titleRowsSpinner.setSelection(pos);
		//
		// // hide checkbox
		// doneCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENCHECKBOX,
		// false));
		//
		// // hide note
		// noteCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENNOTE,
		// false));
		//
		// // hide due date
		// dateCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENDATE,
		// false));
		//
		// // hide app icon
		// appIconCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENAPPICON,
		// false));
		//
		// // hide new button
		// newCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENNEW, false));
		//
		// // hide header
		// headerCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENHEADER,
		// false));
		//
		// // transparent
		// transparentCheckBox.setChecked(widgetPrefs.getBoolean(KEY_TRANSPARENT,
		// false));
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
		if (adapter == null || adapter.getCount() == 0) return -1;
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
		if (values == null || values.length == 0) return -1;
		int pos = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(selectedVal)) {
				pos = i;
				break;
			}
		}
		return pos;
	}

	void updateBG(final int color) {
		if (shade != null) {
			shade.setBackgroundColor(color);
			shade.setVisibility((color & 0xff000000) == 0 ? View.GONE
					: View.VISIBLE);
		}
	}

	void updateTheme(final int theme, final WidgetPrefs widgetPrefs) {
		int color;
		int alpha = widgetPrefs.getInt(KEY_SHADE_COLOR, DEFAULT_SHADE);
		// Isolate alpha channel
		alpha = 0xff000000 & alpha;
		switch (theme) {
		case THEME_LIGHT:
			// WHITE
			color = 0xffffff;
			break;
		case THEME_DARK:
		default:
			color = 0;
			break;
		}
		// Add alpha
		color = alpha | color;
		widgetPrefs.putInt(KEY_SHADE_COLOR, color);
		updateBG(color);
		mNotesAdapter.notifyDataSetChanged();
	}

	/**
	 * Returns black, with the opacity specified
	 */
	public static int getHomescreenBackgroundColor(final int opacity) {
		// int opacity = 50;
		// try {
		// opacity =
		// Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
		// .getString(PREF_HOMESCREEN_BACKGROUND_OPACITY, "50"));
		// } catch (NumberFormatException ignored) {
		// }

		if (opacity >= 100) {
			return 0xff000000;
		}
		else if (opacity <= 0) {
			return 0;
		}
		else {
			return (opacity * 256 / 100) << 24;
		}
	}

	/**
	 * Returns the specified color, with the opacity specified. The color will
	 * have its alpha overwritten.
	 */
	public static int getHomescreenBackgroundColor(final int opacity,
			final int color) {
		// Get rid of possible alpha
		int retColor = color & 0x00ffffff;
		// retColor = retColor >> 8;

		return getHomescreenBackgroundColor(opacity) | retColor;
	}

	static class SimpleWidgetPreviewAdapter extends SimpleCursorAdapter {
		final int mItemLayout;
		final int mHeaderLayout;
		final static int itemType = 0;
		final static int headerType = 1;

		public SimpleWidgetPreviewAdapter(Context context, int layout,
				int headerLayout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			mItemLayout = layout;
			mHeaderLayout = headerLayout;
		}

		int getViewLayout(final int position) {
			if (itemType == getItemViewType(position)) {
				return mItemLayout;
			}
			else {
				return mHeaderLayout;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			final Cursor c = (Cursor) getItem(position);
			// If the id is invalid, it's a header
			if (c.getLong(0) < 1) {
				return headerType;
			}
			else {
				return itemType;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				final LayoutInflater inflater = LayoutInflater.from(mContext);
				convertView = inflater.inflate(getViewLayout(position), parent,
						false);
			}
			return super.getView(position, convertView, parent);
		}
	}
}
