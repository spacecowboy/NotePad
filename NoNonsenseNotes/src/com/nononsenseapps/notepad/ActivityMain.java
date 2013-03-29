package com.nononsenseapps.notepad;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.helpers.dualpane.DualLayoutActivity.CONTENTVIEW;
import com.nononsenseapps.notepad.database.LegacyDBHelper;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment_;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;
import com.nononsenseapps.notepad.interfaces.OnFragmentInteractionListener;
import com.nononsenseapps.notepad.prefs.PrefsActivity;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

@EActivity(R.layout.activity_main)
public class ActivityMain extends FragmentActivity implements
		OnFragmentInteractionListener {

	@ViewById
	View fragment1;

	// Only present on tablets
	@ViewById
	View fragment2;

	// Shown on tablets on start up. Hide on selection
	@ViewById
	View taskHint;

	/**
	 * Loads the appropriate fragments depending on state and intent.
	 */
	@AfterViews
	protected void loadContent() {
		final Intent intent = getIntent();
		final FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction().setCustomAnimations(R.anim.slide_in_top,
						R.anim.slide_out_bottom);
		/*
		 * If it contains a noteId, load an editor. If also tablet, load the
		 * lists.
		 */
		if (fragment2 != null) {
			if (getNoteId(intent) > 0) {
				transaction.replace(R.id.fragment2,
						TaskDetailFragment_.getInstance(getNoteId(intent)));
				taskHint.setVisibility(View.GONE);
			}
		}
		else if (isNoteIntent(intent)) {
			transaction.replace(R.id.fragment1,
					TaskDetailFragment_.getInstance(getNoteId(intent)));

			// also set up-navigation
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		/*
		 * Other case, is a list id or a tablet
		 */
		if (!isNoteIntent(intent) || fragment2 != null) {
			transaction.replace(R.id.fragment1, TaskListViewPagerFragment
					.getInstance(getListIdToShow(intent)));
		}

		// Commit transaction
		transaction.commit();
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
	 * Returns true the intent URI targets a note. Either an edit/view or
	 * insert.
	 */
	boolean isNoteIntent(final Intent intent) {
		if (intent != null
				&& intent.getData() != null
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
		else {
			return false;
		}
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
				&& (Intent.ACTION_EDIT.equals(intent.getAction()) || Intent.ACTION_VIEW
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
			else if (null != intent
					.getStringExtra(Task.Columns.DBLIST)) {
				// TODO why string?
				retval = Long
						.parseLong(intent
								.getStringExtra(Task.Columns.DBLIST));
			}
			else if (null != intent
					.getStringExtra(TaskDetailFragment.ARG_ITEM_LIST_ID)) {
				retval = Long.parseLong(intent
						.getStringExtra(TaskDetailFragment.ARG_ITEM_LIST_ID));
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
			//final Intent intent = new Intent().setAction(Intent.ACTION_EDIT)
			//		.setClass(this, ActivityMain_.class).setData(taskUri);
			//startActivity(intent);
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
					.putExtra(Task.Columns.DBLIST, listId);
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
