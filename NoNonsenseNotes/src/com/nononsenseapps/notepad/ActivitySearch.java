package com.nononsenseapps.notepad;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.notepad.fragments.FragmentSearch;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

@EActivity(R.layout.fullscreen_fragment)
public class ActivitySearch extends FragmentActivity {

	protected String mQuery = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Must do this before super.onCreate
		ActivityHelper.readAndSetSettings(this);
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		handleIntent(getIntent());
	}

	@AfterViews
	void loadContent() {
		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragmentPlaceHolder,
						FragmentSearch.getInstance(mQuery)).commit();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	void handleIntent(Intent intent) {
		if (intent == null) return;

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			mQuery = intent.getStringExtra(SearchManager.QUERY);
		}
		else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			intent.setClass(getApplicationContext(), ActivityMain_.class);
			startActivity(intent);
			finish();
		}
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
}
