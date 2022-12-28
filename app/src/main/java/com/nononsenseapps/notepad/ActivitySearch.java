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
import androidx.fragment.app.Fragment;

import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.helpers.ThemeHelper;
import com.nononsenseapps.notepad.fragments.FragmentSearch;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.fullscreen_fragment)
public class ActivitySearch extends AppCompatActivity {

	protected String mQuery = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Must do this before super.onCreate
		ThemeHelper.setTheme(this);
		ActivityHelper.setSelectedLanguage(this);
		super.onCreate(savedInstanceState);

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

	@AfterViews
	void loadContent() {
		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragmentPlaceHolder, getFragment())
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
			mQuery = intent.getStringExtra(SearchManager.QUERY);
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			intent.setClass(getApplicationContext(), ActivityMain.class);
			startActivity(intent);
			finish();
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
