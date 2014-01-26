package com.nononsenseapps.notepad.sync.orgmode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.database.TaskList;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.preference.PreferenceManager;

public class OrgSyncer {
	public static final String TAG = "nononsensenotes-orgparser";

	// Where files are kept. User changeable in preferences.
	public static final String DEFAULT_ORG_DIR = Environment
			.getExternalStorageDirectory().toString() + "/NoNonsenseNotes";
	public static final String PREF_ORG_DIR = "pref_org_dir";

	private final String ORG_DIR;

	public OrgSyncer(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		ORG_DIR = prefs.getString(PREF_ORG_DIR, DEFAULT_ORG_DIR);
	}

	/**
	 * Write any changes made in the database to file. Does not touch files
	 * which have not been changed in database.
	 */
	public void writeChanges(final Context context) {
		// Get the changes from the database
		Log.d(OrgSyncer.TAG, "Getting db changes");
		final List<OrgFile> changedFiles = getDBChanges(context);

		Log.d(OrgSyncer.TAG, "Handling each orgfile");
		try {
			for (OrgFile orgFile : changedFiles) {
				if (orgFile.isDeleted()) {
					Log.d(OrgSyncer.TAG, "Handling deleted orgfile");
					// Delete the file
					deleteOrgFile(orgFile);
					// Remove from database
					orgFile.delete(context);
				} else {
					Log.d(OrgSyncer.TAG, "Handling orgfile");
					// Write the changes to file
					writeOrgFile(context, orgFile);
					// Save the latest modification time if something was
					// written
					Log.d(OrgSyncer.TAG, "Saving orgfile in db");
					orgFile.save(context);
				}
			}
			Log.d(OrgSyncer.TAG, "Removing deleted orgentries");
			// Delete orgEntries which have been deleted
			removeDeletedOrgEntries(context);
		} catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
	}

	private void deleteOrgFile(final OrgFile orgFile) {
		final File file = new File(ORG_DIR + File.separatorChar
				+ orgFile.remoteId);
		if (file.exists()) {
			file.delete();
		}
	}

	private void writeOrgFile(final Context context, final OrgFile orgFile)
			throws IOException {
		File file = new File(ORG_DIR + File.separatorChar
				+ orgFile.getFileName(context));
		file.getParentFile().mkdirs();
		// Not sure this is such a good idea...
		// if (orgFile.isNewFile()) {
		// while (file.exists()) {
		// // New files are not allowed to overwrite existing entries
		// file = new File(ORG_DIR + File.separatorChar
		// + orgFile.getUniqueFileName(context));
		// }
		// }

		if (!file.exists()) {
			file.createNewFile();
		}

		Log.d(OrgSyncer.TAG, "Getting orgfile as string");
		// Do this first as it accesses the database
		final byte[] buf = orgFile.getOrgString(context).getBytes();

		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file),
					buf.length);
			out.write(buf);
			out.flush();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private void removeDeletedOrgEntries(final Context context) {
		context.getContentResolver().delete(
				OrgEntry.URI,
				OrgEntry.Columns.SERVICE + " IS ? AND "
						+ OrgEntry.Columns.DELETED + " IS ?",
				new String[] { OrgEntry.ORGMODE, "deleted" });
	}

	/**
	 * Returns a list of OrgFiles from the database which have changed since
	 * last write. If a new tasklist has been created, this method will create a
	 * new OrgFile-object for it.
	 */
	private List<OrgFile> getDBChanges(final Context context) {
		final List<OrgFile> changedFiles = new ArrayList<OrgFile>();

		// Fetch each tasklist
		final Cursor c = context.getContentResolver().query(TaskList.URI,
				TaskList.Columns.FIELDS, null, null, TaskList.Columns.TITLE);

		try {
			while (c.moveToNext()) {
				final TaskList taskList = new TaskList(c);
				// Fetch entry for tasklist, create new if not exists
				changedFiles.add(fetchOrgFileFor(context, taskList));
				// Log.d(OrgSyncer.TAG, "Adding existing orgfile");
				// changedFiles.add(new OrgFile(c));
			}
		} finally {
			if (c != null)
				c.close();
		}

		// Log.d(OrgSyncer.TAG, "Getting existing orgfiles");
		// getExistingOrgFiles(context, changedFiles);
		// Log.d(OrgSyncer.TAG, "Getting new orgfiles");
		// getNewOrgFiles(context, changedFiles);

		return changedFiles;
	}

	/**
	 * Returns the orgfile associated with a tasklist. If none exists in the
	 * database, creates a new one.
	 */
	private OrgFile fetchOrgFileFor(final Context context,
			final TaskList taskList) {
		OrgFile orgFile = null;
		// Fetch for tasklist
		final Cursor c = context.getContentResolver().query(OrgFile.URI,
				OrgFile.Columns.FIELDS,
				OrgFile.fetchOrgFileForTaskListClause(),
				OrgFile.fetchOrgFileForTaskListArgs(taskList), null);

		if (c != null && c.moveToNext()) {
			orgFile = new OrgFile(c);
			orgFile.setTaskList(taskList);
			Log.d(OrgSyncer.TAG, "Found existing OrgFile: " + orgFile.remoteId);
		}

		if (c != null) {
			c.close();
		}

		if (orgFile == null) {
			// Create new entry
			orgFile = new OrgFile();
			orgFile.setTaskList(taskList);
			orgFile.dbid = taskList._id;
			Log.d(OrgSyncer.TAG, "Created new orgFile for: " + taskList.title);
		}

		return orgFile;
	}

	/**
	 * Get the orgFiles which have been updated.
	 */
	// private void getExistingOrgFiles(final Context context,
	// final List<OrgFile> changedFiles) {
	//
	// // TODO this clause does not look as task.updated which is what is
	// // relevant perhaps..
	// final Cursor c = context.getContentResolver().query(OrgFile.URI,
	// OrgFile.Columns.FIELDS, OrgFile.Columns.SERVICE + " IS ?",
	// new String[] { OrgFile.ORGMODE }, null);
	//
	// try {
	// while (c.moveToNext()) {
	// Log.d(OrgSyncer.TAG, "Adding existing orgfile");
	// changedFiles.add(new OrgFile(c));
	// }
	// } finally {
	// if (c != null)
	// c.close();
	// }
	// }

	/**
	 * Get the TaskLists which have been created and create orgFiles for them.
	 */
	// private void getNewOrgFiles(final Context context,
	// final List<OrgFile> changedFiles) {
	// final Cursor c = context.getContentResolver().query(TaskList.URI,
	// TaskList.Columns.FIELDS,
	// OrgFile.getTaskListWithoutRemoteClause(),
	// OrgFile.getTaskListWithoutOrgArgs(), null);
	// try {
	// while (c.moveToNext()) {
	// Log.d(OrgSyncer.TAG, "Creating new orgfile");
	// final TaskList taskList = new TaskList(c);
	// final OrgFile orgFile = new OrgFile();
	// orgFile.setTaskList(taskList);
	// orgFile.dbid = taskList._id;
	// changedFiles.add(orgFile);
	// }
	// } finally {
	// if (c != null)
	// c.close();
	// }
	// }

	/**
	 * If a file has changed since last read, parse and import it again.
	 */
	public void readChanges(final Context context) {
		// TODO
	}
}
