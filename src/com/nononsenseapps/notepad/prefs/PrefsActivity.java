package com.nononsenseapps.notepad.prefs;

import java.util.List;

import com.nononsenseapps.notepad.FragmentLayout;
import com.nononsenseapps.notepad.R;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class PrefsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up navigation (adds nice arrow to icon)
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			//actionBar.setDisplayShowTitleEnabled(false);
		}
	}

	/**
	 * Populate the activity with the top-level headers.
	 */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.app_pref_headers, target);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			goUp();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Launches the main activity with Flag CLEAR TOP
	 */
	private void goUp() {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setClass(this, FragmentLayout.class);

		startActivity(intent);
	}
}
