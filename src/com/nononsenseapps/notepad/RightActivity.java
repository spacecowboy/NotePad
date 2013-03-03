package com.nononsenseapps.notepad;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nononsenseapps.helpers.SyncHelper;
import com.nononsenseapps.helpers.dualpane.DualLayoutActivity;
import com.nononsenseapps.helpers.dualpane.DualLayoutActivity.CONTENTVIEW;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.prefs.SyncPrefs;

/**
 * Convenience class where external edits are opened.
 * 
 */
public class RightActivity extends MainActivity {

	/**
	 * Launches the main activity
	 */
	@Override
	protected void goUp() {
		finish();
		super.goUp();
	}

	/**
	 * Dont sync in editor
	 */
	protected void syncOnStart() {
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.right_activity_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
}
