package com.nononsenseapps.notepad;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment_;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.View;

@EActivity(R.layout.activity_main)
public class ActivityMain extends FragmentActivity {

	@ViewById
	View fragment1;

	// Only present on tablets
	@ViewById
	View fragment2;

	/**
	 * Loads the appropriate fragments depending on state and intent.
	 */
	@AfterViews
	protected void loadFragments() {
		final Intent intent = getIntent();
		final FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();
		/*
		 * If it contains a noteId, load an editor. If also tablet, load the
		 * lists.
		 */
		if (isNoteIntent(intent) || fragment2 != null) {
			Bundle arguments = new Bundle();
			arguments
					.putLong(TaskDetailFragment.ARG_ITEM_ID, getNoteId(intent));
			TaskDetailFragment fragment = TaskDetailFragment_.builder().build();
			fragment.setArguments(arguments);
			if (fragment2 != null) {
				transaction.add(R.id.fragment2, fragment);
			} else {
				transaction.add(R.id.fragment1, fragment);
			}
		}
		/*
		 * Other case, is a list id or a tablet
		 */
		if (!isNoteIntent(intent) || fragment2 != null) {
			//Bundle arguments = new Bundle();
			//arguments.putLong(TaskListFragment.ARG_ITEM_ID, getListId(intent));
			//TaskListFragment fragment = TaskListFragment_.builder().build();
			//fragment.setArguments(arguments);
			//transaction.add(R.id.fragment1, fragment);
		}

		// Commit transaction
		transaction.commit();
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
			if ((intent.getData().getPath()
					.startsWith(NotePad.Notes.PATH_VISIBLE_NOTES) || intent
					.getData().getPath().startsWith(NotePad.Notes.PATH_NOTES))) {
				retval = Long.parseLong(intent.getData().getLastPathSegment());
			} else if (null != intent
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
				&& (intent.getData().getPath()
						.startsWith(NotePad.Notes.PATH_VISIBLE_NOTES) || intent
						.getData().getPath()
						.startsWith(NotePad.Notes.PATH_NOTES))) {
			return true;
		} else {
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
					.startsWith(NotePad.Lists.PATH_VISIBLE_LISTS) || intent
					.getData().getPath().startsWith(NotePad.Lists.PATH_LISTS))) {
				retval = Long.parseLong(intent.getData().getLastPathSegment());
			} else if (null != intent
					.getStringExtra(NotePad.Notes.COLUMN_NAME_LIST)) {
				retval = Long.parseLong(intent
						.getStringExtra(NotePad.Notes.COLUMN_NAME_LIST));
			}
		}
		return retval;
	}

	@Override
	public void onNewIntent(Intent intent) {

	}
}
