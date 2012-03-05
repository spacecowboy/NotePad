package com.nononsenseapps.notepad.prefs;

import java.util.List;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.sync.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

public class PrefsActivity extends PreferenceActivity implements AccountManagerCallback<Bundle> {
	public static final int DIALOG_ACCOUNTS = 23;
	private Account account;

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
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_ACCOUNTS:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// TODO resource
			builder.setTitle("Select a Google account");
			final Account[] accounts = AccountManager.get(this)
					.getAccountsByType("com.google");
			final int size = accounts.length;
			String[] names = new String[size];
			for (int i = 0; i < size; i++) {
				names[i] = accounts[i].name;
			}
			// TODO
			// Could add a clear alternative here
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Stuff to do when the account is selected by the user
					accountSelected(accounts[which]);
				}
			});
			return builder.create();
		}
		return null;
	}
	
	/**
	 * Called from the activity, since that one builds the dialog
	 * 
	 * @param account
	 */
	public void accountSelected(Account account) {
		if (account != null) {
			Log.d("prefsActivity", "step one");
			this.account = account;
			// Request user's permission
			AccountManager.get(this).getAuthToken(account,
					SyncAdapter.AUTH_TOKEN_TYPE, null, this, this, null);
			// work continues in callback, method run()
		}
	}
	
	/**
	 * User wants to select an account to sync with. If we get an approval, activate sync and set
	 * periodicity also.
	 */
	@Override
	public void run(AccountManagerFuture<Bundle> future) {
		try {
			Log.d("prefsActivity", "step two");
			// If the user has authorized
			// your application to use the
			// tasks API
			// a token is available.
			String token = future.getResult().getString(
					AccountManager.KEY_AUTHTOKEN);
			// Now we are authorized by the user.

			if (token != null && !token.equals("") && account != null) {
				Log.d("prefsActivity", "step three: " + account.name);
				SharedPreferences customSharedPreference = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = customSharedPreference.edit();
				editor.putString(SyncPrefs.KEY_ACCOUNT, account.name);
				editor.commit();

				// Set it syncable
				ContentResolver.setIsSyncable(account, NotePad.AUTHORITY, 1);
				// Set sync frequency
				SyncPrefs.setSyncInterval(this, getPreferenceScreen().getSharedPreferences());
			}
		} catch (OperationCanceledException e) {
			// TODO: The user has denied you
			// access to the API, you should
			// handle that
		} catch (Exception e) {
		}
	}
}
