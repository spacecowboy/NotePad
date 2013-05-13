package com.nononsenseapps.notepad;

import java.util.Locale;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.SystemService;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.utils.views.TitleNoteTextView;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView.OnQueryTextListener;

@EActivity(R.layout.activity_search)
public class ActivitySearch extends FragmentActivity {

	@SystemService
	SearchManager searchManager;

	@ViewById
	ListView list;

	private SimpleCursorAdapter mAdapter;

	private LoaderCallbacks<Cursor> mCallback;

	private String mQuery = "";

	private SearchView mSearchView;

	protected void readAndSetSettings() {
		// Read settings and set
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		final String theme = prefs.getString(MainPrefs.KEY_THEME,
				MainPrefs.THEME_LIGHT_ICS_AB);
		if (MainPrefs.THEME_LIGHT_ICS_AB.equals(theme)) {
			setTheme(R.style.ThemeHoloLightDarkActonBar);
		}
		else if (MainPrefs.THEME_BLACK.equals(theme)) {
			setTheme(R.style.ThemeHoloBlack);
		}
		else // if (theme.equals(getResources().getString(
				// R.string.const_theme_googlenow_dark)))
		{
			setTheme(R.style.ThemeGoogleNowDark);
		}

		// Set language
		Configuration config = getResources().getConfiguration();

		String lang = prefs.getString(getString(R.string.pref_locale), "");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if ("".equals(lang))
				locale = Locale.getDefault();
			else if (lang.length() == 5) {
				locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			}
			else {
				locale = new Locale(lang.substring(0, 2));
			}
			// Locale.setDefault(locale);
			config.locale = locale;
			getResources().updateConfiguration(config,
					getResources().getDisplayMetrics());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_search, menu);

		// Get the SearchView and set the searchable configuration
		mSearchView = (SearchView) menu.findItem(R.id.menu_search)
				.getActionView();
		// Assumes current activity is the searchable activity
		mSearchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		mSearchView.setIconifiedByDefault(false); // Do not iconify the widget;
													// expand it by default
		mSearchView.setQueryRefinementEnabled(true);
		mSearchView.setSubmitButtonEnabled(false);

		mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(final String query) {
				doSearch(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(final String query) {
				doSearch(query);
				return true;
			}
		});

		mSearchView.setQuery(mQuery, false);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return false;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Must do this before super.onCreate
		readAndSetSettings();
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true);

		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	void handleIntent(Intent intent) {
		if (intent == null) return;

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String query = intent.getStringExtra(SearchManager.QUERY);

			doSearch(query);
		}
		else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			intent.setClass(getApplicationContext(), ActivityMain_.class);
			startActivity(intent);
			finish();
		}
	}

	@AfterViews
	void setupAdapter() {
		mAdapter = getAdapter();

		mAdapter.setViewBinder(getViewBinder());

		// Set adapter
		list.setAdapter(mAdapter);
		list.setOnItemClickListener(getOnItemClickListener());

		// Start loading data
		mCallback = new LoaderCallbacks<Cursor>() {
			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
				return new CursorLoader(ActivitySearch.this, getSearchUri(),
						getFields(), null, new String[] { mQuery },
						getSortOrder());
			}

			@Override
			public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
				mAdapter.swapCursor(c);
			}

			@Override
			public void onLoaderReset(Loader<Cursor> loader) {
				mAdapter.swapCursor(null);
			}
		};

		doSearch(mQuery);
	}

	void doSearch(final String query) {
		mQuery = query == null ? "" : query;

		// If not loaded yet, let it load
		if (mCallback != null)
			getSupportLoaderManager().restartLoader(0, null, mCallback);
	}

	/**
	 * Override to give different search behaviour
	 */
	Uri getSearchUri() {
		return Task.URI_SEARCH;
	}

	/**
	 * Override to give different search behaviour
	 */
	String[] getFields() {
		return Task.Columns.FIELDS;
	}

	/**
	 * Override to give different search behaviour
	 */
	String getSortOrder() {
		return Task.Columns.TITLE;
	}
	
	/**
	 * Override to get different search behaviour
	 */
	SimpleCursorAdapter getAdapter() {
		return new SimpleCursorAdapter(this, R.layout.tasklist_item_rich,
				null, new String[] { Task.Columns.TITLE, Task.Columns.NOTE,
				Task.Columns.DUE, Task.Columns.COMPLETED,
				Task.Columns.LEFT, Task.Columns.RIGHT }, new int[] { android.R.id.text1,
				android.R.id.text1, R.id.date, R.id.checkbox,
				R.id.drag_handle, R.id.dragpadding }, 0);
	}
	
	/**
	 * Override to give different search behaviour
	 */
	OnItemClickListener getOnItemClickListener() {
		return new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View origin, int pos,
					long id) {
				startActivity(new Intent(Intent.ACTION_EDIT, Task.getUri(id)));
			}
		};
	}

	/**
	 * Override to give different search behaviour
	 */
	ViewBinder getViewBinder() {
		// Get the global list settings
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Load pref for item height
		final int rowCount = prefs.getInt(
				getString(R.string.key_pref_item_max_height), 3);

		return new ViewBinder() {
			String sTemp = "";

			@Override
			public boolean setViewValue(View view, Cursor c, int colIndex) {
				switch (colIndex) {
				// Matches order in Task.Columns.Fields
				case 1:
					// Title
					sTemp = c.getString(colIndex);

					// Set height of text for non-headers
					if (rowCount == 1) {
						((TitleNoteTextView) view).setSingleLine(true);
					}
					else {
						((TitleNoteTextView) view).setSingleLine(false);
						((TitleNoteTextView) view).setMaxLines(rowCount);
					}

					// Change color based on complete status
					((TitleNoteTextView) view).useSecondaryColor(!c.isNull(3));

					((TitleNoteTextView) view).setTextTitle(sTemp);
					return true;
				case 2:
					// Note
					// Only if task it not locked
					if (c.getInt(9) != 1) {
						((TitleNoteTextView) view).setTextRest(c
								.getString(colIndex));
					}
					else {
						((TitleNoteTextView) view).setTextRest("");
					}
					return true;
				default:
					// Checkbox
					view.setVisibility(View.GONE);
					return true;
				}
			}
		};
	}
}
