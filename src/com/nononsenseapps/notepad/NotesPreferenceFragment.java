package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad.sync.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class NotesPreferenceFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener, AccountManagerCallback<Bundle> {
	public static final String KEY_THEME = "key_current_theme";
	public static final String KEY_SORT_ORDER = "key_sort_order";
	public static final String KEY_SORT_TYPE = "key_sort_type";
	public static final String KEY_FONT_TYPE_EDITOR = "key_font_type_editor";
	public static final String KEY_FONT_SIZE_EDITOR = "key_font_size_editor";
	public static final String KEY_TEXT_PREVIEW = "key_text_preview";

	public static final String KEY_SYNC_ENABLE = "syncEnablePref";
	public static final String KEY_ACCOUNT = "accountPref";
	public static final String KEY_SYNC_FREQ = "syncFreq";

	public static final String SANS = "Sans";
	public static final String SERIF = "Serif";
	public static final String MONOSPACE = "Monospace";

	public static final String THEME_DARK = "dark";
	public static final String THEME_LIGHT = "light";
	public static final String THEME_LIGHT_ICS_AB = "light_ab";
	private static final String TAG = "NotesPreferenceFragment";

	private Preference prefSortOrder;
	private Preference prefSortType;
	private Preference prefTheme;
	private Preference prefFontType;

	public String SUMMARY_SORT_TYPE;
	public String SUMMARY_SORT_ORDER;
	public String SUMMARY_THEME;
	private Activity activity;

	private Account account;
	private Preference prefAccount;
	private Preference prefSyncFreq;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.main_preferences);

		SUMMARY_SORT_TYPE = getText(
				R.string.settings_summary_sort_type_serverposition).toString();

		SUMMARY_SORT_ORDER = getText(R.string.settings_summary_sort_order_asc)
				.toString();

		SUMMARY_THEME = getText(R.string.settings_summary_theme_dark)
				.toString();

		prefSortOrder = getPreferenceScreen().findPreference(KEY_SORT_ORDER);
		prefSortType = getPreferenceScreen().findPreference(KEY_SORT_TYPE);
		prefTheme = getPreferenceScreen().findPreference(KEY_THEME);
		prefFontType = getPreferenceScreen().findPreference(
				KEY_FONT_TYPE_EDITOR);

		prefAccount = getPreferenceScreen().findPreference(KEY_ACCOUNT);
		prefSyncFreq = getPreferenceScreen().findPreference(KEY_SYNC_FREQ);

		SharedPreferences sharedPrefs = getPreferenceScreen()
				.getSharedPreferences();
		// Set up a listener whenever a key changes
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Set summaries
		setTypeSummary(sharedPrefs);
		setOrderSummary(sharedPrefs);
		setThemeSummary(sharedPrefs);
		setEditorFontTypeSummary(sharedPrefs);

		setAccountTitle(sharedPrefs);
		setFreqSummary(sharedPrefs);

		Preference accountPref = (Preference) findPreference("accountPref");
		accountPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						// Show dialog
						activity.showDialog(FragmentLayout.NotesPreferencesDialog.DIALOG_ACCOUNTS);
						return true;
					}
				});
	}

	/**
	 * Called from the activity, since that one builds the dialog
	 * 
	 * @param account
	 */
	public void accountSelected(Account account) {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "accountSelected: " + account);
		
		if (account != null) {
			this.account = account;
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Trying to get permission");
			// Request user's permission
			AccountManager.get(activity).getAuthToken(account,
					SyncAdapter.AUTH_TOKEN_TYPE, null, activity, this, null);
			// work continues in callback, method run()
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		try {
			if (activity.isFinishing()) {
				if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "isFinishing, should not update summaries");
				// Setting the summary now would crash it with
				// IllegalStateException since we are not attached to a view
			} else {
				if (KEY_THEME.equals(key)) {
					setThemeSummary(sharedPreferences);
				} else if (KEY_SORT_TYPE.equals(key)) {
					setTypeSummary(sharedPreferences);
				} else if (KEY_SORT_ORDER.equals(key)) {
					setOrderSummary(sharedPreferences);
				} else if (KEY_FONT_TYPE_EDITOR.equals(key)) {
					setEditorFontTypeSummary(sharedPreferences);
					// updatePreviewFontType(sharedPreferences);
				} else if (KEY_FONT_SIZE_EDITOR.equals(key)) {
					// updatePreviewFontSize(sharedPreferences);
				} else if (KEY_SYNC_ENABLE.equals(key)) {
					if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "Toggled sync");
					toggleSync(sharedPreferences);
				} else if (KEY_SYNC_FREQ.equals(key)) {
					setSyncInterval(sharedPreferences);
					setFreqSummary(sharedPreferences);
				} else
					if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "Somethign changed!");
			}
		} catch (IllegalStateException e) {
			// This is just in case the "isFinishing" wouldn't be enough
			// The isFinishing will try to prevent us from doing something
			// stupid
			// This catch prevents the app from crashing if we do something
			// stupid
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "Exception was caught: " + e.getMessage());
		}
	}

	/**
	 * Finds and returns the account of the name given
	 * 
	 * @param accountName
	 * @return
	 */
	public static Account getAccount(AccountManager manager, String accountName) {
		Account[] accounts = manager.getAccountsByType("com.google");
		for (Account account : accounts) {
			if (account.name.equals(accountName)) {
				return account;
			}
		}
		return null;
	}

	private void setSyncInterval(SharedPreferences sharedPreferences) {
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");
		String sFreqMins = sharedPreferences.getString(KEY_SYNC_FREQ, "0");
		int freqMins = 0;
		try {
			freqMins = Integer.parseInt(sFreqMins);
		} catch (NumberFormatException e) {
			// Debugging error because of a mistake...
		}
		if (accountName == "") {
			// Something is very wrong if this happens
		} else if (freqMins == 0) {
			// Disable periodic syncing
			ContentResolver.removePeriodicSync(
					getAccount(AccountManager.get(activity), accountName),
					NotePad.AUTHORITY, new Bundle());
		} else {
			// Convert from minutes to seconds
			long pollFrequency = freqMins * 60;
			// Set periodic syncing
			ContentResolver.addPeriodicSync(
					getAccount(AccountManager.get(activity), accountName),
					NotePad.AUTHORITY, new Bundle(), pollFrequency);
		}
	}

	private void toggleSync(SharedPreferences sharedPreferences) {
		boolean enabled = sharedPreferences.getBoolean(KEY_SYNC_ENABLE, false);
		String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");
		if (accountName.equals("")) {
			// do nothing yet
		} else if (enabled) {
			// set syncable
			ContentResolver.setIsSyncable(
					getAccount(AccountManager.get(activity), accountName),
					NotePad.AUTHORITY, 1);
			// Also set sync frequency
			setSyncInterval(sharedPreferences);
		} else {
			// set unsyncable
			ContentResolver.setIsSyncable(
					getAccount(AccountManager.get(activity), accountName),
					NotePad.AUTHORITY, 0);
		}
	}

	/*
	private void updatePreviewFontSize(SharedPreferences sharedPreferences) {
		int size = sharedPreferences.getInt(KEY_FONT_SIZE_EDITOR,
				R.integer.default_editor_font_size);
		if (textPreview != null) {
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "updatePreviewFontSize textPreview");
			textPreview.setTextSize(size);
		} else {
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "updatePreviewFontSize textPreview was null!");
		}
	}

	private void updatePreviewFontType(SharedPreferences sharedPreferences) {
		String type = sharedPreferences.getString(KEY_FONT_TYPE_EDITOR, SANS);
		if (textPreview != null) {
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "updatePreviewFontType textPreview!");
			textPreview.setTextType(type);
		} else {
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("settings", "updatePreviewFontType textPreview was null!");
		}
	}*/

	private void setOrderSummary(SharedPreferences sharedPreferences) {
		String value = sharedPreferences.getString(KEY_SORT_ORDER,
				NotePad.Notes.DEFAULT_SORT_ORDERING);
		String summary = "";
		if (NotePad.Notes.ASCENDING_SORT_ORDERING.equals(value))
			summary = getText(R.string.settings_summary_sort_order_asc)
					.toString();
		else if (NotePad.Notes.DESCENDING_SORT_ORDERING.equals(value))
			summary = getText(R.string.settings_summary_sort_order_desc)
					.toString();
		SUMMARY_SORT_ORDER = summary;
		prefSortOrder.setSummary(summary);
	}

	private void setTypeSummary(SharedPreferences sharedPreferences) {
		String value = sharedPreferences.getString(KEY_SORT_TYPE,
				NotePad.Notes.DEFAULT_SORT_TYPE);
		String summary = "";
		if (NotePad.Notes.ALPHABETIC_SORT_TYPE.equals(value))
			summary = getText(R.string.settings_summary_sort_type_alphabetic)
					.toString();
		else if (NotePad.Notes.DUEDATE_SORT_TYPE.equals(value))
			summary = getText(R.string.settings_summary_sort_type_duedate)
					.toString();
		else if (NotePad.Notes.POSITION_SORT_TYPE.equals(value))
			summary = getText(R.string.settings_summary_sort_type_serverposition).toString();
		SUMMARY_SORT_TYPE = summary;
		prefSortType.setSummary(summary);
	}

	private void setThemeSummary(SharedPreferences sharedPreferences) {
		// Light theme is default
		String value = sharedPreferences.getString(KEY_THEME, THEME_LIGHT);
		String summary;
		if (THEME_DARK.equals(value))
			summary = getText(R.string.settings_summary_theme_dark).toString();
		else if (THEME_LIGHT.equals(value))
			summary = getText(R.string.settings_summary_theme_light).toString();
		else
			summary = getText(R.string.settings_summary_theme_light_dark_ab)
					.toString();
		SUMMARY_THEME = summary;
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("setThemeSummary", "Setting summary now");
		prefTheme.setSummary(summary);
	}

	private void setEditorFontTypeSummary(SharedPreferences sharedPreferences) {
		// Dark theme is default
		String value = sharedPreferences.getString(KEY_FONT_TYPE_EDITOR, SANS);

		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d("setFontSummary", value);
		prefFontType.setSummary(value);
	}

	private void setAccountTitle(SharedPreferences sharedPreferences) {
		//boolean enabled = sharedPreferences.getBoolean(KEY_SYNC_ENABLE, false);
		String account = sharedPreferences.getString(KEY_ACCOUNT, "");
		
		if (!account.equals("")) {
			prefAccount.setTitle(account);
			prefAccount.setSummary(R.string.settings_account_summary);
		}
		
	}
	
	private void setFreqSummary(SharedPreferences sharedPreferences) {
		String sFreqMins = sharedPreferences.getString(KEY_SYNC_FREQ, "0");
		int freq = 0;
		try {
			freq = Integer.parseInt(sFreqMins);
		} catch (NumberFormatException e) {
			// Debugging error because of a mistake...
		}
		if (freq == 0)
			prefSyncFreq.setSummary(R.string.manual);
		else if (freq == 60)
			prefSyncFreq.setSummary(R.string.onehour);
		else if (freq == 1440)
			prefSyncFreq.setSummary(R.string.oneday);
		else if (freq > 60)
			prefSyncFreq.setSummary("" + freq/60 + " " + getText(R.string.hours).toString());
		else
			prefSyncFreq.setSummary("" + freq + " " + getText(R.string.minutes).toString());
	}

	/**
	 * User wants to select an account to sync with. If we get an approval, activate sync and set
	 * periodicity also.
	 */
	@Override
	public void run(AccountManagerFuture<Bundle> future) {
		try {
			// If the user has authorized
			// your application to use the
			// tasks API
			// a token is available.
			String token = future.getResult().getString(
					AccountManager.KEY_AUTHTOKEN);
			// Now we are authorized by the user.
			if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "So token is: " + token);

			if (token != null && !token.equals("") && account != null) {
				SharedPreferences customSharedPreference = getPreferenceScreen()
						.getSharedPreferences();
				SharedPreferences.Editor editor = customSharedPreference.edit();
				editor.putString(KEY_ACCOUNT, account.name);
				editor.commit();

				prefAccount.setTitle(account.name);
				prefAccount.setSummary(R.string.settings_account_summary);

				if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(TAG, "Setting syncable and setting frequency");
				// Set it syncable
				ContentResolver.setIsSyncable(account, NotePad.AUTHORITY, 1);
				// Set sync frequency
				setSyncInterval(getPreferenceScreen().getSharedPreferences());
			}
		} catch (OperationCanceledException e) {
			// TODO: The user has denied you
			// access to the API, you should
			// handle that
		} catch (Exception e) {
		}
	}
}