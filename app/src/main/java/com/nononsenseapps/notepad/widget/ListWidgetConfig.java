package com.nononsenseapps.notepad.widget;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.ui.ExtrasCursorAdapter;
import com.nononsenseapps.utils.views.TitleNoteTextView;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.ActionBar;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

@EActivity(resName = "activity_widget_config")
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
	// All lists id
	public final static int ALL_LISTS_ID = -2;

	@ViewById(resName = "widgetPreviewWrapper")
	View widgetPreviewWrapper;

	@ViewById(resName = "listSpinner")
	Spinner listSpinner;

	@ViewById(resName = "sortingSpinner")
	Spinner sortingSpinner;

	@ViewById(resName = "itemRowsSeekBar")
	SeekBar itemRowsSeekBar;

	@ViewById(resName = "transparencySeekBar")
	SeekBar transparencySeekBar;

	@ViewById(resName = "themeSpinner")
	Spinner themeSpinner;

	@ViewById(resName = "shade")
	ImageView shade;

	@ViewById(resName = "notesList")
	ListView notesList;

	@ViewById(resName = "titleButton")
	TextView titleButton;

	@ViewById(resName = "widgetHeader")
	View widgetHeader;

	@ViewById(resName = "transparentHeaderCheckBox")
	CheckBox transparentHeaderCheckBox;

	@ViewById(resName = "hideCheckBox")
	CheckBox hideCheckBox;

	@ViewById(resName = "hideDateCheckBox")
	CheckBox hideDateCheckBox;

	private int appWidgetId;

	private SimpleWidgetPreviewAdapter mNotesAdapter;

	private LoaderCallbacks<Cursor> mCallback;

	private ExtrasCursorAdapter mListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		} else {
			appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
			Log.d(TAG, "Invalid ID given in the intent");
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetId);
			setResult(RESULT_CANCELED, resultValue);
			finish();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@AfterViews
	void setupPreview() {
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
			final SimpleDateFormat weekdayFormatter = TimeFormatter
					.getLocalFormatterWeekday(ListWidgetConfig.this);
			final SimpleDateFormat dateFormatter = TimeFormatter
					.getLocalFormatterMicro(ListWidgetConfig.this);

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
							} else if (Task.HEADER_KEY_TODAY.equals(sTemp)) {
								sTemp = getString(R.string.date_header_today);
							} else if (Task.HEADER_KEY_PLUS1.equals(sTemp)) {
								sTemp = getString(R.string.date_header_tomorrow);
							} else if (Task.HEADER_KEY_PLUS2.equals(sTemp)
									|| Task.HEADER_KEY_PLUS3.equals(sTemp)
									|| Task.HEADER_KEY_PLUS4.equals(sTemp)) {
								sTemp = weekdayFormatter.format(new Date(c
										.getLong(4)));
							} else if (Task.HEADER_KEY_LATER.equals(sTemp)) {
								sTemp = getString(R.string.date_header_future);
							} else if (Task.HEADER_KEY_NODATE.equals(sTemp)) {
								sTemp = getString(R.string.date_header_none);
							} else if (Task.HEADER_KEY_COMPLETE.equals(sTemp)) {
								sTemp = getString(R.string.date_header_completed);
							}

							((TextView) view).setText(sTemp);
							// ((TextView) view).setText(TitleNoteTextView
							// .getStyledText(sTemp, 1.3f, 1, 1));
						} else {
							((TextView) view).setText(TitleNoteTextView
									.getStyledText(c.getString(1), c.getString(2),
											1.0f, 1, 1));
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
						} else if (view.getId() == R.id.completedCheckBoxDark) {
							visible = THEME_DARK == widgetPrefs.getInt(KEY_THEME,
									DEFAULT_THEME);
						} else {
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
						if (c.isNull(colIndex)) {
							((TextView) view).setText("");
						} else {
							((TextView) view).setText(dateFormatter
									.format(new Date(c.getLong(colIndex))));
						}
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
							getString(R.string.const_as_alphabetic, TaskList.Columns.TITLE));
				} else {
					final Uri targetUri;

					final long listId = widgetPrefs.getLong(KEY_LIST,
							ALL_LISTS_ID);
					final String sortSpec;
					final String sortType = widgetPrefs
							.getString(KEY_SORT_TYPE,
									getString(R.string.default_sorttype));

					if (sortType.equals(getString(R.string.const_possubsort))
							&& listId > 0) {
						targetUri = Task.URI;
						sortSpec = Task.Columns.LEFT;
					} else if (sortType
							.equals(getString(R.string.const_modified))) {
						targetUri = Task.URI;
						sortSpec = Task.Columns.UPDATED + " DESC";
					}
					// due date sorting
					else if (sortType.equals(getString(R.string.const_duedate))) {
						targetUri = Task.URI_SECTIONED_BY_DATE;
						sortSpec = null;
					}
					// Alphabetic
					else {
						targetUri = Task.URI;
						sortSpec = getString(R.string.const_as_alphabetic, Task.Columns.TITLE);
					}

					String listWhere = null;
					String[] listArg = null;
					if (listId > 0) {
						listWhere = Task.Columns.DBLIST + " IS ? AND "
								+ Task.Columns.COMPLETED + " IS NULL";
						listArg = new String[] { Long.toString(listId) };
					} else {
						listWhere = Task.Columns.COMPLETED + " IS NULL";
						listArg = null;
					}

					return new CursorLoader(ListWidgetConfig.this, targetUri,
							Task.Columns.FIELDS,
							listWhere,
							listArg, sortSpec);
				}
			}

			@Override
			public void onLoadFinished(Loader<Cursor> l, Cursor c) {
				if (l.getId() == 1) {
					mListAdapter.swapCursor(c);
					final int pos = getListPositionOf(mListAdapter,
							widgetPrefs.getLong(KEY_LIST, ALL_LISTS_ID));
					//if (c.getCount() > 0) {
					// Set current item
					listSpinner.setSelection(pos);
					//}
				} else {
					mNotesAdapter.swapCursor(c);
				}
			}

			@Override
			public void onLoaderReset(Loader<Cursor> l) {
				if (l.getId() == 1) {
					mListAdapter.swapCursor(null);
				} else {
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
		final String[] themeValues = getResources().getStringArray(
				R.array.widget_themevalues_preference);

		if (themeValues == null) {
			Log.d(TAG, "themevalues null");
		} else {
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
		sortingSpinner.setSelection(getArrayPositionOf(sortTypeValues,
				widgetPrefs.getString(KEY_SORT_TYPE,
						getString(R.string.default_sorttype))));

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
				} else {
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
		final String currentThemeString;
		if (widgetPrefs.getInt(KEY_THEME, DEFAULT_THEME) == THEME_LIGHT) {
			currentThemeString = getString(R.string.settings_summary_theme_light);
		} else {
			currentThemeString = getString(R.string.settings_summary_theme_dark);
		}
		themeSpinner.setSelection(getSpinnerPositionOf(
				themeSpinner.getAdapter(), currentThemeString));

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
						if (mNotesAdapter != null) {
							mNotesAdapter.notifyDataSetChanged();
						}
					}
				});
		itemRowsSeekBar.setProgress(widgetPrefs.getInt(KEY_TITLEROWS,
				DEFAULT_ROWS) - 1);

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
		// Set current item
		int opacity = widgetPrefs.getInt(KEY_SHADE_COLOR, DEFAULT_SHADE);
		// Isolate the alpha
		opacity = opacity >> 24;
		opacity &= 0xff;
		// Get percentage
		opacity = (100 * opacity) / 0xff;
		transparencySeekBar.setProgress(opacity);

		listSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapter, View arg1,
									   int pos, long id) {
				widgetPrefs.putLong(KEY_LIST, id);
				try {
					widgetPrefs.putString(KEY_LIST_TITLE, ((Cursor) adapter
							.getItemAtPosition(pos)).getString(1));
				} catch (ClassCastException e) {
					// Its the all lists item
					widgetPrefs.putString(KEY_LIST_TITLE,
							((String) adapter.getItemAtPosition(pos)));
				}

				// Need to reload tasks
				reloadTasks();
				// And set title
				titleButton.setText(widgetPrefs.getString(KEY_LIST_TITLE, ""));
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		mListAdapter = new ExtrasCursorAdapter(this,
				android.R.layout.simple_spinner_dropdown_item, null,
				new String[] { TaskList.Columns.TITLE },
				new int[] { android.R.id.text1 }, new int[] { ALL_LISTS_ID },
				new int[] { R.string.show_from_all_lists },
				android.R.layout.simple_spinner_dropdown_item);

		// new SimpleCursorAdapter(this,
		// android.R.layout.simple_spinner_dropdown_item, null,
		// new String[] { TaskList.Columns.TITLE },
		// new int[] { android.R.id.text1 }, 0);
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
		transparentHeaderCheckBox.setChecked(widgetPrefs.getBoolean(
				KEY_HIDDENHEADER, false));

		hideCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
										 boolean isChecked) {
				widgetPrefs.putBoolean(KEY_HIDDENCHECKBOX, isChecked);
				if (mNotesAdapter != null)
					mNotesAdapter.notifyDataSetChanged();
			}
		});
		hideCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENCHECKBOX,
				false));

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
		hideDateCheckBox.setChecked(widgetPrefs.getBoolean(KEY_HIDDENDATE,
				false));
	}

	private int getListPositionOf(final Adapter adapter, final long id) {
		if (adapter == null || adapter.getCount() == 0) return 0;
		int pos = 0;
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItemId(i) == id) {
				pos = i;
				break;
			}
		}
		return pos;
	}

	private int getSpinnerPositionOf(final Adapter adapter, final String entry) {
		if (adapter == null || adapter.getCount() == 0) return 0;
		int pos = 0;
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItem(i).toString().equals(entry)) {
				pos = i;
				break;
			}
		}
		return pos;
	}

	private int getArrayPositionOf(final String[] array, final String entry) {
		if (array == null || array.length == 0) return 0;
		int pos = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(entry)) {
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
		} else if (opacity <= 0) {
			return 0;
		} else {
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
			} else {
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
			} else {
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
