package com.nononsenseapps.notepad.activities.main;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.nononsenseapps.helpers.ListHelper;
import com.nononsenseapps.notepad.database.LegacyDBHelper;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
/**
 * static methods that take some code away from {@link ActivityMain}. It's used only in that
 * class, that's why it's package-private
 */
class ActivityMainHelper {

	/**
	 * @param intent from code in {@link ActivityMain}
	 * @return a list id from an intent if it contains one, either as part of
	 * its URI or as an extra. Returns -1 if no id was contained, this includes insert actions
	 */
	static long getListId(final Intent intent) {
		long retval = -1;
		if (intent != null && intent.getData() != null &&
				(Intent.ACTION_EDIT.equals(intent.getAction()) ||
						Intent.ACTION_VIEW.equals(intent.getAction()) ||
						Intent.ACTION_INSERT.equals(intent.getAction()))) {
			if ((intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Lists.PATH_VISIBLE_LISTS) ||
					intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Lists.PATH_LISTS) ||
					intent.getData().getPath().startsWith(TaskList.URI.getPath()))) {
				try {
					retval = Long.parseLong(intent.getData().getLastPathSegment());
				} catch (NumberFormatException ignored) {
					// retval remains = -1
				}
			} else if (-1 != intent.getLongExtra(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_LIST, -1)) {
				retval = intent.getLongExtra(LegacyDBHelper.NotePad.Notes.COLUMN_NAME_LIST, -1);
			} else if (-1 != intent.getLongExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, -1)) {
				retval = intent.getLongExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, -1);
			} else if (-1 != intent.getLongExtra(Task.Columns.DBLIST, -1)) {
				retval = intent.getLongExtra(Task.Columns.DBLIST, -1);
			}
		}
		return retval;
	}

	/**
	 * Returns the text that has been shared with the app. Does not check
	 * anything other than EXTRA_SUBJECT AND EXTRA_TEXT
	 * <p/>
	 * If it is a Google Now intent, will ignore the subject which is
	 * "Note to self"
	 */
	static String getNoteShareText(final Intent intent) {
		if (intent == null || intent.getExtras() == null) {
			return "";
		}

		StringBuilder retval = new StringBuilder();
		// possible title
		if (intent.getExtras().containsKey(Intent.EXTRA_SUBJECT) &&
				!"com.google.android.gm.action.AUTO_SEND".equals(intent.getAction())) {
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
	 * Returns a note id from an intent if it contains one, either as part of
	 * its URI or as an extra
	 * <p/>
	 * Returns -1 if no id was contained, this includes insert actions
	 */
	static long getNoteId(@NonNull final Intent intent) {
		long retval = -1;
		if (intent.getData() != null &&
				(Intent.ACTION_EDIT.equals(intent.getAction()) ||
						Intent.ACTION_VIEW.equals(intent.getAction()))) {
			if (intent.getData().getPath().startsWith(TaskList.URI.getPath())) {
				// Find it in the extras. See DashClock extension for an example
				retval = intent.getLongExtra(Task.TABLE_NAME, -1);
			} else if ((intent.getData().getPath().startsWith(
					LegacyDBHelper.NotePad.Notes.PATH_VISIBLE_NOTES) ||
					intent.getData().getPath().startsWith(
							LegacyDBHelper.NotePad.Notes.PATH_NOTES) ||
					intent.getData().getPath()
							.startsWith(Task.URI.getPath()))) {
				retval = Long.parseLong(intent.getData().getLastPathSegment());
			}
		}
		return retval;
	}

	/**
	 * Returns true the intent URI targets a note. Either an edit/view or
	 * insert.
	 */
	static boolean isNoteIntent(final Intent intent) {
		if (intent == null) {
			return false;
		}
		if (Intent.ACTION_SEND.equals(intent.getAction()) ||
				"com.google.android.gm.action.AUTO_SEND"
						.equals(intent.getAction())) {
			return true;
		}

		return intent.getData() != null &&
				(Intent.ACTION_EDIT.equals(intent.getAction()) ||
						Intent.ACTION_VIEW.equals(intent.getAction()) ||
						Intent.ACTION_INSERT.equals(intent.getAction())) &&
				(intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Notes.PATH_VISIBLE_NOTES) ||
						intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Notes.PATH_NOTES) ||
						intent.getData().getPath().startsWith(Task.URI.getPath())) &&
				!intent.getData().getPath().startsWith(TaskList.URI.getPath());
	}

	/**
	 * If intent contains a list_id, returns that.
	 * Else, checks preferences for default list setting.
	 * Else, -1.
	 */
	static long getListIdToShow(final Intent intent, Context context) {
		long result = ActivityMainHelper.getListId(intent);
		return ListHelper.getAShowList(context, result);
	}
}
