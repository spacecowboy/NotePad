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

package com.nononsenseapps.notepad;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.helpers.ThemeHelper;
import com.nononsenseapps.notepad.databinding.FullscreenFragmentBinding;
import com.nononsenseapps.notepad.fragments.FragmentSearch;


public class ActivitySearch extends AppCompatActivity {

	protected String mQuery = "";

	/**
	 * for {@link R.layout#fullscreen_fragment}
	 */
	private FullscreenFragmentBinding mBinding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Must do this before super.onCreate
		ThemeHelper.setTheme(this);
		ActivityHelper.setSelectedLanguage(this);
		super.onCreate(savedInstanceState);
		mBinding = FullscreenFragmentBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		loadContent();

		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		handleIntent(getIntent());
	}

	/**
	 * To allow child classes to override content
	 */
	protected Fragment getFragment() {
		return FragmentSearch.getInstance(mQuery);
	}

	/**
	 * Shows the {@link FragmentSearch} with the results
	 */
	void loadContent() {
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragmentPlaceHolder, getFragment())
				.commit();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent(intent);
	}

	void handleIntent(Intent intent) {
		if (intent == null) return;

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// as a result of voice search in the main activity
			mQuery = intent.getStringExtra(SearchManager.QUERY);

			var searchViewMenuItem = (SearchView) this.findViewById(R.id.menu_search);
			if (searchViewMenuItem == null) {
				// the search activity did not load yet (for example, a voice search is opening
				// ActivitySearch from ActivityMain). In this case, you need to load the content
				loadContent();
			} else {
				// there is a searchview in this activity. You MUST NOT re-create the fragment.
				// Instead, update the query text, and the fragment will show the results
				searchViewMenuItem.setQuery(mQuery, false);
			}
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// when you click a note from the search suggestion in the main activity
			intent.setClass(getApplicationContext(), ActivityMain_.class);
			startActivity(intent);
			finish();
		} else if (intent.getAction() == null) {
			// the archive view was launched from ActivityMain
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return false;
	}
}
