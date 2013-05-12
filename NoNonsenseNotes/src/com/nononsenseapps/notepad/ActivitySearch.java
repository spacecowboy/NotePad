package com.nononsenseapps.notepad;

import java.util.Locale;

import com.googlecode.androidannotations.annotations.EActivity;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.prefs.MainPrefs;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

@EActivity
public class ActivitySearch extends FragmentActivity {

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
		else //if (theme.equals(getResources().getString(
			//	R.string.const_theme_googlenow_dark))) 
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
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
	    // Assumes current activity is the searchable activity
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
		searchView.setQueryRefinementEnabled(true);
		searchView.setSubmitButtonEnabled(true);
	    
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// TODO more than finish
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
		if (intent == null)
			return;
		
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      final String query = intent.getStringExtra(SearchManager.QUERY);
	      doSearch(query);
	    }
	}

	void doSearch(final String query) {
		// TODO 
	}
}
