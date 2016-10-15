/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.ui.common;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.databinding.FragmentSearchBinding;

public class FragmentSearch extends Fragment {
	
	public final static String QUERY  = "query";

	protected SimpleCursorAdapter mAdapter;

	protected LoaderCallbacks<Cursor> mCallback;

	protected String mQuery = "";

	protected SearchView mSearchView;
	private FragmentSearchBinding binding;

	public static FragmentSearch getInstance(final String initialQuery) {
		FragmentSearch f = new FragmentSearch();
		Bundle args = new Bundle();
		args.putString(QUERY, initialQuery);
		f.setArguments(args);
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getArguments() != null) {
			if (getArguments().containsKey(QUERY))
				mQuery = getArguments().getString(QUERY);
		}
		
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_search, menu);

		// Get the SearchView and set the searchable configuration
		mSearchView = (SearchView) menu.findItem(R.id.menu_search)
				.getActionView();
		// Assumes current activity is the searchable activity
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager
                .getSearchableInfo(getActivity().getComponentName()));
        mSearchView.setIconifiedByDefault(false); // Do not iconify the widget;
													// expand it by default
		mSearchView.setQueryRefinementEnabled(true);
		mSearchView.setSubmitButtonEnabled(false);
		
		// Disable suggestions in search activity
		mSearchView.setSuggestionsAdapter(null);

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
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, true);
		return binding.getRoot();
	}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupAdapter();
    }

	void setupAdapter() {
		mAdapter = getAdapter();

		mAdapter.setViewBinder(getViewBinder());

		// Set adapter
		binding.list.setAdapter(mAdapter);
		binding.list.setOnItemClickListener(getOnItemClickListener());

		// Start loading data
		mCallback = new LoaderCallbacks<Cursor>() {
			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
				return new CursorLoader(getActivity(), getSearchUri(),
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

	protected void doSearch(final String query) {
		mQuery = query == null ? "" : query;

		// If not loaded yet, let it load
		if (mCallback != null)
			getLoaderManager().restartLoader(0, null, mCallback);
	}

	/**
	 * Override to give different search behaviour
	 */
	protected Uri getSearchUri() {
		return Task.URI_SEARCH;
	}

	/**
	 * Override to give different search behaviour
	 */
	protected String[] getFields() {
		return Task.Columns.FIELDS;
	}

	/**
	 * Override to give different search behaviour
	 */
	protected String getSortOrder() {
		return Task.Columns.TITLE;
	}
	
	/**
	 * Override to get different search behaviour
	 */
	protected SimpleCursorAdapter getAdapter() {
		return new SimpleCursorAdapter(getActivity(), R.layout.tasklist_item_rich,
				null, new String[] { Task.Columns.TITLE, Task.Columns.NOTE,
				Task.Columns.DUE, Task.Columns.COMPLETED,
				Task.Columns.LEFT, Task.Columns.RIGHT }, new int[] { android.R.id.text1,
				android.R.id.text1, R.id.date, R.id.checkbox,
				R.id.drag_handle, R.id.drag_padding }, 0);
	}
	
	/**
	 * Override to give different search behaviour
	 */
	protected OnItemClickListener getOnItemClickListener() {
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
	protected ViewBinder getViewBinder() {
		// Get the global list settings
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

		// Load pref for item height
		final int rowCount = 3;

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
