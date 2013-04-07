package com.nononsenseapps.notepad;

import java.util.Locale;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.helpers.dualpane.DualLayoutActivity.CONTENTVIEW;
import com.nononsenseapps.notepad.database.LegacyDBHelper;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.DialogEditList_;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment_;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.prefs.PrefsActivity;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

@EActivity(R.layout.activity_main)
public class ActivityMain extends FragmentActivity implements
		OnFragmentInteractionListener {

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

	@Override
	public void onCreate(Bundle b) {
		// Must do this before super.onCreate
		readAndSetSettings();
		super.onCreate(b);
	}

	private void readAndSetSettings() {
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
				transaction.replace(R.id.fragment2,
						TaskDetailFragment_.getInstance(getNoteShareText(intent),
								TaskListViewPagerFragment.getAList(this,
										getListId(intent))), DETAILTAG);
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
										getListId(intent))), DETAILTAG);
			}

			// also set up-navigation
			getActionBar().setDisplayHomeAsUpEnabled(true);
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
		case R.id.menu_sync:
		case R.id.menu_delete:
		default:
			return false;
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
	 * Returns the text that has been shared with the app. Returns null if not a
	 * share intent.
	 */
	String getNoteShareText(final Intent intent) {
		// TODO

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
		if (Intent.ACTION_SEND.equals(intent.getAction()) ||
				"com.google.android.gm.action.AUTO_SEND".equals(intent.getAction())) {
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
		}
		return retval;
	}

	/**
	 * If intent contains a list_id, returns that. Else, checks preferences for
	 * default list setting. Else, -1.
	 */
	long getListIdToShow(final Intent intent) {
		long result = getListId(intent);
		return TaskListViewPagerFragment.getAList(this, result);
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@Override
	public void onFragmentInteraction(final Uri taskUri) {
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
			// TODO
			final Intent intent = new Intent().setAction(Intent.ACTION_EDIT)
					.setClass(this, ActivityMain_.class).setData(taskUri);
			startActivity(intent);
		}
	}

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
			// TODO
			final Intent intent = new Intent().setAction(Intent.ACTION_INSERT)
					.setClass(this, ActivityMain_.class).setData(Task.URI)
					.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);
			startActivity(intent);
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
}
