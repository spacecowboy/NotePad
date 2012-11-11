package com.nononsenseapps.notepad.prefs;

import java.util.List;

import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.PasswordDialog.ActionResult;
import com.nononsenseapps.notepad_donate.R;
import com.nononsenseapps.notepad.interfaces.PasswordChecker;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

public class PrefsActivity extends PreferenceActivity implements
		PasswordChecker {

	private String pendingNewPassword = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up navigation (adds nice arrow to icon)
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			// actionBar.setDisplayShowTitleEnabled(false);
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
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Launches the main activity
	 */
	private void goUp() {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClass(this, MainActivity.class);

		startActivity(intent);
	}

	public void setPendingNewPassword(String newPassword) {
		this.pendingNewPassword = newPassword;
	}

	@Override
	public void PasswordVerified(ActionResult result) {
		if (result.result) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			// Set new password
			settings.edit()
					.putString(PasswordPrefs.KEY_PASSWORD, pendingNewPassword)
					.commit();
			Toast.makeText(
					this,
					("".equals(pendingNewPassword)) ? getText(R.string.password_cleared)
							: getText(R.string.password_set), Toast.LENGTH_SHORT)
					.show();
		}
	}
}
