package com.nononsenseapps.notepad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OnActivityResult;
import com.googlecode.androidannotations.annotations.SystemService;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.billing.IabHelper;
import com.nononsenseapps.billing.IabResult;
import com.nononsenseapps.billing.Inventory;
import com.nononsenseapps.billing.Purchase;
import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.helpers.SyncHelper;
import com.nononsenseapps.helpers.dualpane.DualLayoutActivity.CONTENTVIEW;
import com.nononsenseapps.notepad.database.LegacyDBHelper;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase;
import com.nononsenseapps.notepad.fragments.DialogEditList_;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment_;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.notepad.legacy.DonateMigrator;
import com.nononsenseapps.notepad.legacy.DonateMigrator_;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.prefs.PrefsActivity;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

@EActivity(R.layout.activity_main)
public class ActivityMain extends FragmentActivity implements
		OnFragmentInteractionListener {

	// In-app donate identifier
	static final String SKU_DONATE = "donate_inapp";
	// For static testing
	// static final String SKU_DONATE = "android.test.purchased";
	// static final String SKU_DONATE = "android.test.cancelled";
	static final int SKU_DONATE_REQUEST_CODE = 7331;

	// Set to true in bundle if exits should be animated
	public static final String ANIMATEEXIT = "animateexit";
	// Using tags for test
	public static final String DETAILTAG = "detailfragment";
	public static final String LISTPAGERTAG = "listpagerfragment";

	@ViewById
	View fragment1;

	// Only present on tablets
	@ViewById
	View fragment2;

	// Shown on tablets on start up. Hide on selection
	@ViewById
	View taskHint;

	boolean mAnimateExit = false;
	IabHelper mBillingHelper;
	// True if user has donated
	boolean mIsDonate = false;

	@Override
	public void onCreate(Bundle b) {
		// Must do this before super.onCreate
		readAndSetSettings();
		super.onCreate(b);
		// Schedule notifications
		NotificationHelper.schedule(this);

		// To know if we should animate exits
		if (getIntent() != null
				&& getIntent().getBooleanExtra(ANIMATEEXIT, false)) {
			mAnimateExit = true;
		}

		// If user has donated some other time
		mIsDonate = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(SKU_DONATE, false);

		// For in-app billing
		final String base64EncodedPublicKey = new StringBuilder(
				"MIIBIjANBgkqhki")
				.append("G9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkNMrvFQmGKm5YoSD7UMCvKMvlEguAHVNCzEb")
				.append("Bww7T8iQHPr5H7Ltag03HLT4oToG1hDKsbEV7tks2tjwAm1ftzlud+gFMEG/GCL6G")
				.append("F+aisWKLJJZtpODRzidAAJVjlDaIROJBsnDnBQ2f8uoukSrXNaT42k/plIjhCiCdZ")
				.append("AmMb7Yb48v6aMvnB7FHXBffrkI2mpn4c8kSe721ROovZXNDw7/U94ZODKkOrnZGON")
				.append("rJ1isUwibFA3MhOfRdemE1aZF6KwCMD2EvgV8y9KVOPwF3lE0ucsJ4I56eEQpmzzR")
				.append("jItb/Gn8iHp0qa7IhSR/vXBL4p+byCcoQDlfvjeAmjY6dQIDAQAB")
				.toString();
		try {
			mBillingHelper = new IabHelper(this, base64EncodedPublicKey);
			mBillingHelper.enableDebugLogging(true, "nononsenseapps billing");
			mBillingHelper
					.startSetup(new IabHelper.OnIabSetupFinishedListener() {
						public void onIabSetupFinished(IabResult result) {
							if (!result.isSuccess()) {
								// Oh noes, there was a problem.
								Log.d("nononsenseapps billing",
										"Problem setting up In-app Billing: "
												+ result);
								return;
							}
							// Hooray, IAB is fully set up!
							mBillingHelper
									.queryInventoryAsync(mBillingInventoryListener);
						}
					});
		}
		catch (Exception e) {
			Log.d("nononsenseapps billing",
					"InApp billing cant be allowed to crash app, EVER");
		}

		// See if the donate version is installed and offer to import if so
		isOldDonateVersionInstalled();
	}

	@Background
	void isOldDonateVersionInstalled() {
		List<ApplicationInfo> packages;
		PackageManager pm;
		pm = getPackageManager();
		packages = pm.getInstalledApplications(0);
		for (ApplicationInfo packageInfo : packages) {
			if (packageInfo.packageName
					.equals("com.nononsenseapps.notepad_donate")) {
				migrateDonateUser();
				// Stop loop
				break;
			}
		}
	}

	@UiThread
	void migrateDonateUser() {
		// migrate user
		if (!DonateMigrator.hasImported(this)) {
			final DialogConfirmBase dialog = new DialogConfirmBase() {

				@Override
				public void onOKClick() {
					startService(new Intent(ActivityMain.this,
							DonateMigrator_.class));
				}

				@Override
				public int getTitle() {
					return R.string.import_data_question;
				}

				@Override
				public int getMessage() {
					return R.string.import_data_msg;
				}
			};
			dialog.show(getSupportFragmentManager(), "migrate_question");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mBillingHelper != null) mBillingHelper.dispose();
		mBillingHelper = null;
	}

	// Listener that's called when we finish querying the items and
	// subscriptions we own
	IabHelper.QueryInventoryFinishedListener mBillingInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			if (result.isFailure()) {
				Log.d("nononsenseapps billing", "Failed to query inventory: "
						+ result);
				return;
			}

			Log.d("nononsenseapps billing", "Query inventory was successful.");

			/*
			 * Check for items we own. Notice that for each purchase, we check
			 * the developer payload to see if it's correct! See
			 * verifyDeveloperPayload().
			 */

			// Do we have the premium upgrade?
			// Purchase premiumPurchase = inventory.getPurchase(SKU_DONATE);
			// mIsDonate = (premiumPurchase != null);// &&
			// verifyDeveloperPayload(premiumPurchase));
			mIsDonate = inventory.hasPurchase(SKU_DONATE);
			Log.d("nononsenseapps billing", "User is "
					+ (mIsDonate ? "PREMIUM" : "NOT PREMIUM"));

			if (mIsDonate) {
				// Save in prefs
				PreferenceManager
						.getDefaultSharedPreferences(ActivityMain.this).edit()
						.putBoolean(SKU_DONATE, mIsDonate).commit();
				// Update relevant parts of UI
				updateUiDonate();
			}
		}
	};

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
		else if (theme.equals(getResources().getString(
				R.string.const_theme_googlenow_dark))) {
			setTheme(R.style.ThemeGoogleNowDark);
		}
		else {
			setTheme(R.style.ThemeHolo);
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

		// String sortType = prefs.getString(MainPrefs.KEY_SORT_TYPE,
		// NotePad.Notes.DEFAULT_SORT_TYPE);
		// String sortOrder = prefs.getString(MainPrefs.KEY_SORT_ORDER,
		// NotePad.Notes.DEFAULT_SORT_ORDERING);

		// NotePad.Notes.SORT_ORDER = sortType; // + " " + sortOrder;

		// We want to be notified of future changes
		// TODO monitor changes so we can restart
		// prefs.registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Loads the appropriate fragments depending on state and intent.
	 */
	@AfterViews
	protected void loadContent() {
		final Intent intent = getIntent();
		final FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction().setCustomAnimations(R.anim.slide_in_top,
						R.anim.slide_out_bottom);

		Log.d("JONAS", "loading content");
		/*
		 * If it contains a noteId, load an editor. If also tablet, load the
		 * lists.
		 */
		if (fragment2 != null) {
			Log.d("JONAS", "detail in 2");
			if (getNoteId(intent) > 0) {
				transaction.replace(R.id.fragment2,
						TaskDetailFragment_.getInstance(getNoteId(intent)),
						DETAILTAG);
				taskHint.setVisibility(View.GONE);
			}
			else if (getListId(intent) > 0) {
				transaction.replace(R.id.fragment2, TaskDetailFragment_
						.getInstance(getNoteShareText(intent),
								TaskListViewPagerFragment.getAList(this,
										getListId(intent),
										getString(R.string.pref_defaultlist))),
						DETAILTAG);
			}
		}
		else if (isNoteIntent(intent)) {
			Log.d("JONAS", "detail in 1");
			if (getNoteId(intent) > 0) {
				transaction.replace(R.id.fragment1,
						TaskDetailFragment_.getInstance(getNoteId(intent)),
						DETAILTAG);
			}
			else {
				// Get a share text (null safe)
				// In a list (if specified, or default otherwise)
				transaction.replace(R.id.fragment1, TaskDetailFragment_
						.getInstance(getNoteShareText(intent),
								TaskListViewPagerFragment.getAList(this,
										getListId(intent),
										getString(R.string.pref_defaultlist))),
						DETAILTAG);
			}

			// Courtesy of Mr Roman Nurik
			// Inflate a "Done" custom action bar view to serve as the "Up"
			// affordance.
			LayoutInflater inflater = (LayoutInflater) getActionBar()
					.getThemedContext().getSystemService(
							LAYOUT_INFLATER_SERVICE);
			final View customActionBarView = inflater.inflate(
					R.layout.actionbar_custom_view_done, null);
			customActionBarView.findViewById(R.id.actionbar_done)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// "Done"
							// finish(); // TODO: don't just finish()!
							final Intent intent = new Intent()
									.setAction(Intent.ACTION_VIEW)
									.setClass(ActivityMain.this,
											ActivityMain_.class)
									.setFlags(
											Intent.FLAG_ACTIVITY_CLEAR_TASK
													| Intent.FLAG_ACTIVITY_NEW_TASK);
							// Should load the same list again
							// Try getting the list from the original intent
							final long listId = getListId(getIntent());
							if (listId > 0) {
								intent.setData(TaskList.getUri(listId));
							}
							startActivity(intent);
							finishSlideTop();
						}
					});

			// Show the custom action bar view and hide the normal Home icon and
			// title.
			final ActionBar actionBar = getActionBar();
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
					ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
							| ActionBar.DISPLAY_SHOW_TITLE);
			actionBar.setCustomView(customActionBarView);
		}
		/*
		 * Other case, is a list id or a tablet
		 */
		if (!isNoteIntent(intent) || fragment2 != null) {
			Log.d("JONAS", "lists in 1");
			transaction.replace(R.id.fragment1, TaskListViewPagerFragment
					.getInstance(getListIdToShow(intent)), LISTPAGERTAG);
		}

		Log.d("JONAS", "commit content");
		// Commit transaction
		transaction.commit();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Do absolutely NOT call super class here. Will bug out the viewpager!
		// super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_preferences:
			Intent intent = new Intent();
			intent.setClass(this, PrefsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_donate:
			try {
				mBillingHelper.launchPurchaseFlow(this, SKU_DONATE,
						SKU_DONATE_REQUEST_CODE,
						new IabHelper.OnIabPurchaseFinishedListener() {
							public void onIabPurchaseFinished(IabResult result,
									Purchase purchase) {
								if (result.isFailure()) {
									Log.d("nononsenseapps billing",
											"Error purchasing: " + result);
									return;
								}
								else if (purchase.getSku().equals(SKU_DONATE)) {
									mIsDonate = true;
									// Save in prefs
									PreferenceManager
											.getDefaultSharedPreferences(
													ActivityMain.this).edit()
											.putBoolean(SKU_DONATE, true)
											.commit();
									// Update relevant parts of UI
									updateUiDonate();
								}
							}
						});
			}
			catch (Exception e) {
				Log.d("nononsenseapps billing",
						"Shouldnt start two purchases! "
								+ e.getLocalizedMessage());
			}
			return true;
		case R.id.menu_sync:
			SyncHelper.requestSyncIf(this, SyncHelper.MANUAL);
			return true;
		case R.id.menu_delete:
		default:
			return false;
		}
	}

	@OnActivityResult(SKU_DONATE_REQUEST_CODE)
	void onDonatePurchased(int resultCode, Intent data) {
		if (mBillingHelper != null)
			mBillingHelper.handleActivityResult(SKU_DONATE_REQUEST_CODE,
					resultCode, data);
	}

	void updateUiDonate() {
		if (mIsDonate) {
			Toast.makeText(this, "WOW, Thanks a bunch dude!",
					Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(this, "No premium for you!", Toast.LENGTH_SHORT)
					.show();
		}
	}

	/**
	 * Returns a note id from an intent if it contains one, either as part of
	 * its URI or as an extra
	 * 
	 * Returns -1 if no id was contained, this includes insert actions
	 */
	long getNoteId(final Intent intent) {
		long retval = -1;
		if (intent != null
				&& intent.getData() != null
				&& (Intent.ACTION_EDIT.equals(intent.getAction()) || Intent.ACTION_VIEW
						.equals(intent.getAction()))) {
			if ((intent
					.getData()
					.getPath()
					.startsWith(LegacyDBHelper.NotePad.Notes.PATH_VISIBLE_NOTES)
					|| intent
							.getData()
							.getPath()
							.startsWith(LegacyDBHelper.NotePad.Notes.PATH_NOTES) || intent
					.getData().getPath().startsWith(Task.URI.getPath()))) {
				retval = Long.parseLong(intent.getData().getLastPathSegment());
			}
			else if (null != intent
					.getStringExtra(TaskDetailFragment.ARG_ITEM_ID)) {
				retval = Long.parseLong(intent
						.getStringExtra(TaskDetailFragment.ARG_ITEM_ID));
			}
		}
		return retval;
	}

	/**
	 * Returns the text that has been shared with the app. Does not check
	 * anything other than EXTRA_SUBJECT AND EXTRA_TEXT
	 */
	String getNoteShareText(final Intent intent) {
		StringBuilder retval = new StringBuilder();
		// possible title
		if (intent.getExtras().containsKey(Intent.EXTRA_SUBJECT)) {
			retval.append(intent.getExtras().get(Intent.EXTRA_SUBJECT));
		}
		// possible note
		if (intent.getExtras().containsKey(Intent.EXTRA_TEXT)) {
			if (retval.length() > 0) {
				retval.append("\n");
			}
			retval.append(intent.getExtras().get(Intent.EXTRA_TEXT));
		}
		return retval.toString();
	}

	/**
	 * Returns true the intent URI targets a note. Either an edit/view or
	 * insert.
	 */
	boolean isNoteIntent(final Intent intent) {
		if (intent == null) {
			return false;
		}
		if (Intent.ACTION_SEND.equals(intent.getAction())
				|| "com.google.android.gm.action.AUTO_SEND".equals(intent
						.getAction())) {
			return true;
		}

		if (intent.getData() != null
				&& (Intent.ACTION_EDIT.equals(intent.getAction())
						|| Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_INSERT
							.equals(intent.getAction()))
				&& (intent
						.getData()
						.getPath()
						.startsWith(
								LegacyDBHelper.NotePad.Notes.PATH_VISIBLE_NOTES)
						|| intent
								.getData()
								.getPath()
								.startsWith(
										LegacyDBHelper.NotePad.Notes.PATH_NOTES) || intent
						.getData().getPath().startsWith(Task.URI.getPath()))
				&& !intent.getData().getPath()
						.startsWith(TaskList.URI.getPath())) {
			return true;
		}

		return false;
	}

	/**
	 * Returns a list id from an intent if it contains one, either as part of
	 * its URI or as an extra
	 * 
	 * Returns -1 if no id was contained, this includes insert actions
	 */
	long getListId(final Intent intent) {
		long retval = -1;
		if (intent != null
				&& intent.getData() != null
				&& (Intent.ACTION_EDIT.equals(intent.getAction())
						|| Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_INSERT
							.equals(intent.getAction()))) {
			if ((intent.getData().getPath()
					.startsWith(NotePad.Lists.PATH_VISIBLE_LISTS)
					|| intent.getData().getPath()
							.startsWith(NotePad.Lists.PATH_LISTS) || intent
					.getData().getPath().startsWith(TaskList.URI.getPath()))) {
				retval = Long.parseLong(intent.getData().getLastPathSegment());
			}
			else if (null != intent
					.getStringExtra(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_LIST)) {
				// TODO why string?
				retval = Long
						.parseLong(intent
								.getStringExtra(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_LIST));
			}
			else if (0 < intent.getLongExtra(
					TaskDetailFragment.ARG_ITEM_LIST_ID, -1)) {
				retval = intent.getLongExtra(
						TaskDetailFragment.ARG_ITEM_LIST_ID, -1);
			}
			else if (0 < intent.getLongExtra(Task.Columns.DBLIST, -1)) {
				retval = intent.getLongExtra(Task.Columns.DBLIST, -1);
			}
		}
		return retval;
	}

	/**
	 * If intent contains a list_id, returns that. Else, checks preferences for
	 * default list setting. Else, -1.
	 */
	long getListIdToShow(final Intent intent) {
		long result = getListId(intent);
		return TaskListViewPagerFragment.getAList(this, result,
				getString(R.string.pref_defaultlist));
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onFragmentInteraction(final Uri taskUri, final View origin) {
		// User clicked a task in the list
		// tablet
		if (fragment2 != null) {
			getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.anim.slide_in_top,
							R.anim.slide_out_bottom)
					.replace(R.id.fragment2,
							TaskDetailFragment_.getInstance(taskUri))
					.commitAllowingStateLoss();
			taskHint.setVisibility(View.GONE);
		}
		// phone
		else {
			final Intent intent = new Intent().setAction(Intent.ACTION_EDIT)
					.setClass(this, ActivityMain_.class).setData(taskUri);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
					&& origin != null) {
				Log.d("nononsenseapps animation", "Animating");
				intent.putExtra(ANIMATEEXIT, true);
				startActivity(
						intent,
						ActivityOptions.makeCustomAnimation(this,
								R.anim.activity_slide_in_left,
								R.anim.activity_slide_out_left).toBundle());

				// startActivity(
				// intent,
				// ActivityOptions.makeScaleUpAnimation(origin, 0, 0,
				// origin.getWidth(), origin.getHeight())
				// .toBundle());

				// Bitmap b = Bitmap.createBitmap(origin.getWidth(),
				// origin.getHeight(), Bitmap.Config.ARGB_8888);
				//
				// startActivity(intent, ActivityOptions
				// .makeThumbnailScaleUpAnimation(origin, b, 0, 0)
				// .toBundle());
			}
			else {
				Log.d("nononsenseapps animation", "Not animating");
				startActivity(intent);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void addTaskInList(final String text, final long listId) {
		if (fragment2 != null) {
			// Replace editor fragment
			getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.anim.slide_in_top,
							R.anim.slide_out_bottom)
					.replace(R.id.fragment2,
							TaskDetailFragment_.getInstance(text, listId))
					.commitAllowingStateLoss();
			taskHint.setVisibility(View.GONE);
		}
		else {
			// Open an activity
			final Intent intent = new Intent().setAction(Intent.ACTION_INSERT)
					.setClass(this, ActivityMain_.class).setData(Task.URI)
					.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				Log.d("nononsenseapps animation", "Animating");
				intent.putExtra(ANIMATEEXIT, true);
				startActivity(
						intent,
						ActivityOptions.makeCustomAnimation(this,
								R.anim.activity_slide_in_left,
								R.anim.activity_slide_out_left).toBundle());
			}
			else {
				startActivity(intent);
			}
		}
	}

	@Override
	public void closeFragment(final Fragment fragment) {
		if (fragment2 != null) {
			getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.anim.slide_in_top,
							R.anim.slide_out_bottom).remove(fragment)
					.commitAllowingStateLoss();
			taskHint.setAlpha(0f);
			taskHint.setVisibility(View.VISIBLE);
			taskHint.animate().alpha(1f).setStartDelay(500);
		}
		else {
			// Phone case, just finish the activity
			finish();
		}
	}

	/**
	 * Only call this when pressing the up-navigation. Makes sure the new
	 * activity comes in on top of this one.
	 */
	void finishSlideTop() {
		super.finish();
		overridePendingTransition(R.anim.activity_slide_in_right_full,
				R.anim.activity_slide_out_right);
	}

	@Override
	public void finish() {
		super.finish();
		// Only animate when specified. Should be when it was animated "in"
		if (mAnimateExit) {
			overridePendingTransition(R.anim.activity_slide_in_right,
					R.anim.activity_slide_out_right_full);
		}
	}
}
