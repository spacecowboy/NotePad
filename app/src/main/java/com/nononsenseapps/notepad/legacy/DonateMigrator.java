/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.legacy;

import java.text.ParseException;
import java.util.HashMap;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.UiThread;
import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.database.LegacyDBHelper.NotePad;
import com.nononsenseapps.notepad.fragments.DialogConfirmBase;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTask;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;
import com.nononsenseapps.utils.time.RFC3339Date;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

@EService
public class DonateMigrator extends IntentService {

	public static final String PREFS_ALREADY_IMPORTED = "already_imported_donate_data";
	static final String DONATE_AUTHORITY = "com.nononsenseapps.donate.NotePad";
	static final Uri BASEURI = Uri.parse(MyContentProvider.SCHEME
			+ DONATE_AUTHORITY);
	static final String PATH_NOTES = "notes";
	static final String PATH_LISTS = "lists";
	static final String PATH_GTASKS = "gtasks";
	static final String PATH_GTASKLISTS = "gtasklists";

	static final String[] NOTEPROJECTION = new String[] { NotePad.Notes._ID,
			NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
			NotePad.Notes.COLUMN_NAME_LIST,
			NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
	static final String[] GTASKPROJECTION = new String[] { NotePad.GTasks._ID,
			NotePad.GTasks.COLUMN_NAME_GTASKS_ID,
			NotePad.GTasks.COLUMN_NAME_GOOGLE_ACCOUNT,
			NotePad.GTasks.COLUMN_NAME_DB_ID };

	static final String[] LISTPROJECTION = new String[] { NotePad.Lists._ID,
			NotePad.Lists.COLUMN_NAME_TITLE };
	static final String[] GTASKLISTPROJECTION = new String[] {
			NotePad.GTaskLists._ID, NotePad.GTaskLists.COLUMN_NAME_GTASKS_ID,
			NotePad.GTaskLists.COLUMN_NAME_GOOGLE_ACCOUNT,
			NotePad.GTaskLists.COLUMN_NAME_DB_ID };

	int mNotesImportedCount = 0;
	int mListsImportedCount = 0;
	String mError = null;

	public DonateMigrator() {
		super("DonateMigrateService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// This app does only one thing
		if (hasImported(this)) {
			return;
		}

		reportStarting();
		importNotes();
		if (mError != null) {
			reportFailure(mError);
		}
		else {
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putBoolean(PREFS_ALREADY_IMPORTED, true).commit();
			reportCompleteStatus(mNotesImportedCount, mListsImportedCount);
			askIfUninstall();
		}
	}

	public static boolean hasImported(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(PREFS_ALREADY_IMPORTED, false);
	}

	/**
	 * Copies data from the donate app's database to this app's database. Result
	 * is updated in fields.
	 */
	void importNotes() {
		final HashMap<Long, Long> listIDMap = new HashMap<Long, Long>();
		final HashMap<Long, Long> taskIDMap = new HashMap<Long, Long>();

		// Work through, list by list
		final Cursor listCursor = getContentResolver().query(
				Uri.withAppendedPath(BASEURI, PATH_LISTS), LISTPROJECTION,
				NotePad.Lists.COLUMN_NAME_DELETED + " IS NOT 1", null,
				NotePad.Lists.COLUMN_NAME_TITLE);
		try {
			while (listCursor.moveToNext()) {
				TaskList tl = new TaskList();
				tl.title = listCursor.getString(1);
				if (tl.save(this) > 0) {
					listIDMap.put(listCursor.getLong(0), tl._id);
					mListsImportedCount += 1;
				}
				// Gtasklist
				final Cursor gtasklistCursor = getContentResolver().query(
						Uri.withAppendedPath(BASEURI, PATH_GTASKLISTS),
						GTASKLISTPROJECTION,
						NotePad.GTaskLists.COLUMN_NAME_DB_ID + " IS ?",
						new String[] { Long.toString(listCursor.getLong(0)) },
						null);
				try {
					if (gtasklistCursor.moveToFirst()) {
						GoogleTaskList gl = new GoogleTaskList(tl,
								gtasklistCursor.getString(2));
						gl.remoteId = gtasklistCursor.getString(1);
						gl.updated = tl.updated;
						gl.save(this);
					}
				}
				catch (Exception e) {
					mError = e.getLocalizedMessage();
					return;
				}
				finally {
					if (gtasklistCursor != null) gtasklistCursor.close();
				}
			}
		}
		catch (Exception e) {
			mError = e.getLocalizedMessage();
			return;
		}
		finally {
			if (listCursor != null) listCursor.close();
		}

		final Cursor noteCursor = getContentResolver().query(
				Uri.withAppendedPath(BASEURI, PATH_NOTES),
				NOTEPROJECTION,
				NotePad.Notes.COLUMN_NAME_DELETED + " IS NOT 1 AND "
						+ NotePad.Notes.COLUMN_NAME_HIDDEN + " IS NOT 1", null,
				NotePad.Notes.COLUMN_NAME_POSSUBSORT);
		try {
			while (noteCursor.moveToNext()) {
				Task t = new Task();
				t.title = noteCursor.getString(1);
				t.note = noteCursor.getString(2);

				if (t.note.contains("[locked]")) {
					t.locked = true;
					t.note = t.note.replace("[locked]", "");
				}

				try {
					t.due = RFC3339Date.parseRFC3339Date(
							noteCursor.getString(3)).getTime();
				}
				catch (Exception e) {
				}

				// completed must be converted
				if (noteCursor.getString(4) != null
						&& "completed".equals(noteCursor.getString(4))) {
					t.setAsCompleted();
				}
				t.dblist = listIDMap.get(noteCursor.getLong(5));

				t.updated = noteCursor.getLong(6);

				if (t.dblist != null) {
					t.save(this, t.updated);
					taskIDMap.put(noteCursor.getLong(0), t._id);
					mNotesImportedCount += 1;
				}
				// Gtask
				final Cursor gtaskCursor = getContentResolver().query(
						Uri.withAppendedPath(BASEURI, PATH_GTASKS),
						GTASKPROJECTION,
						NotePad.GTasks.COLUMN_NAME_DB_ID + " IS ?",
						new String[] { Long.toString(noteCursor.getLong(0)) },
						null);
				try {
					if (gtaskCursor.moveToFirst()) {
						GoogleTask gt = new GoogleTask(t,
								gtaskCursor.getString(2));
						gt.remoteId = gtaskCursor.getString(1);
						gt.updated = t.updated;
						gt.save(this);
					}
				}
				catch (Exception e) {
					mError = e.getLocalizedMessage();
					return;
				}
				finally {
					if (gtaskCursor != null) gtaskCursor.close();
				}
			}
		}
		catch (Exception e) {
			mError = e.getLocalizedMessage();
			return;
		}
		finally {
			if (noteCursor != null) noteCursor.close();
		}
	}

	/**
	 * Tell the user how much was imported and ask if he wants to uninstall the
	 * app now.
	 */
	@UiThread
	void reportCompleteStatus(final int noteCount, final int listCount) {
		try {
			Toast.makeText(this,
					getString(R.string.imported_result, noteCount, listCount),
					Toast.LENGTH_LONG).show();
		}
		catch (Exception e) {
			// In case of bad translations
		}
	}

	@UiThread
	void reportFailure(final String errorMessage) {
		try {
			Log.d("nononsenseapps migrate", errorMessage);
			Toast.makeText(this,
					getString(R.string.import_error, errorMessage),
					Toast.LENGTH_LONG).show();
		}
		catch (Exception e) {
			// In case of bad translations
		}
	}

	@UiThread
	void reportStarting() {
		try {
			Toast.makeText(this, getString(R.string.import_started),
					Toast.LENGTH_SHORT).show();
		}
		catch (Exception e) {
			// In case of bad translations
		}
	}

	void askIfUninstall() {
		Uri packageURI = Uri.parse("package:com.nononsenseapps.notepad_donate");
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		// Need this flag since this is a service
		uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(uninstallIntent);
	}
}
