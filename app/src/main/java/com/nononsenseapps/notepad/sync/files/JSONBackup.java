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

package com.nononsenseapps.notepad.sync.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.RemoteTask;
import com.nononsenseapps.notepad.database.RemoteTaskList;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

public class JSONBackup {

	private static final String KEY_REMINDERS = "reminders";
	private static final String KEY_TASKS = "tasks";
	private static final String KEY_REMOTES = "remotes";
	private static final String KEY_LISTS = "lists";

	private final Context context;

	public JSONBackup(final Context context) {
		this.context = context;
	}

	/**
	 * @return the absolute path of the JSON file for backups, located in:
	 * /storage/emulated/0/Android/data/com.nononsenseapps.notepad/files/Backups/backup.json
	 */
	@NonNull
	public static String getBackupFilePath(@NonNull Context ctx) {
		var backupDir = ctx.getExternalFilesDir("Backups");
		var backupFile = new File(backupDir, "backup.json");

		// TODO the old code saved the backup JSON file in the external storage directory. It is
		//  more convenient, but to do it we need the user's permission. Eventually we should
		//  show a file picker to let the user choose where to save the file, and keep this path
		//  as a default, fallback value. See:
		//  https://developer.android.com/training/data-storage/app-specific
		//  https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
		//  https://stackoverflow.com/questions/58662166
		//  https://stackoverflow.com/questions/71725859/
		return backupFile.getAbsolutePath();
	}

	private List<TaskList> getTaskLists() {
		final ArrayList<TaskList> taskLists = new ArrayList<>();

		final Cursor c = context.getContentResolver().query(TaskList.URI,
				TaskList.Columns.FIELDS, null, null, TaskList.Columns.TITLE);

		while (c != null && c.moveToNext()) {
			taskLists.add(new TaskList(c));
		}

		if (c != null)
			c.close();

		return taskLists;
	}

	private List<RemoteTaskList> getRemotesOf(final TaskList list) {
		final ArrayList<RemoteTaskList> remotes = new ArrayList<>();

		final Cursor c = context.getContentResolver().query(RemoteTaskList.URI,
				RemoteTaskList.Columns.FIELDS,
				RemoteTaskList.Columns.DBID + " IS ?",
				new String[] { Long.toString(list._id) },
				RemoteTaskList.Columns.SERVICE);

		while (c != null && c.moveToNext()) {
			remotes.add(new RemoteTaskList(c));
		}

		if (c != null)
			c.close();

		return remotes;
	}

	private List<Task> getTasksIn(final TaskList list) {
		final ArrayList<Task> tasks = new ArrayList<>();

		// Reverse order because adding stuff is always done at the top
		final Cursor c = context.getContentResolver().query(Task.URI,
				Task.Columns.FIELDS, Task.Columns.DBLIST + " IS ?",
				new String[] { Long.toString(list._id) },
				Task.Columns.LEFT + " DESC");

		while (c != null && c.moveToNext()) {
			tasks.add(new Task(c));
		}

		if (c != null)
			c.close();

		return tasks;
	}

	private List<RemoteTask> getRemotesOf(final Task task) {
		final ArrayList<RemoteTask> remotes = new ArrayList<>();

		final Cursor c = context.getContentResolver().query(RemoteTask.URI,
				RemoteTask.Columns.FIELDS, RemoteTask.Columns.DBID + " IS ?",
				new String[] { Long.toString(task._id) },
				RemoteTask.Columns.SERVICE);

		while (c != null && c.moveToNext()) {
			remotes.add(new RemoteTask(c));
		}

		if (c != null)
			c.close();

		return remotes;
	}

	private List<Notification> getRemindersFor(final Task task) {
		final ArrayList<Notification> reminders = new ArrayList<>();

		final Cursor c = context.getContentResolver().query(Notification.URI,
				Notification.Columns.FIELDS,
				Notification.Columns.TASKID + " IS ?",
				new String[] { Long.toString(task._id) },
				Notification.Columns.TIME);

		while (c != null && c.moveToNext()) {
			reminders.add(new Notification(c));
		}

		if (c != null)
			c.close();

		return reminders;
	}

	private JSONObject getJSONBackup() throws JSONException {
		final JSONArray listarray = new JSONArray();
		for (final TaskList list : getTaskLists()) {
			final JSONObject jsonlist = new JSONObject();
			jsonlist.put(TaskList.Columns._ID, list._id);
			addAllContentToJSON(list.getContent(), jsonlist);

			jsonlist.put(KEY_REMOTES, getJSONRemotesFor(list));
			jsonlist.put(KEY_TASKS, getJSONTasksFor(list));

			// Add tasklist to array
			listarray.put(jsonlist);
		}
		final JSONObject backup = new JSONObject();
		backup.put(KEY_LISTS, listarray);
		return backup;
	}

	private void addAllContentToJSON(final ContentValues content,
									 final JSONObject json) throws JSONException {
		for (String key : content.keySet()) {
			json.put(key, content.get(key));
		}
	}

	private JSONArray getJSONRemotesFor(final TaskList list)
			throws JSONException {
		final JSONArray remotelistarray = new JSONArray();
		for (final RemoteTaskList remote : getRemotesOf(list)) {
			final JSONObject jsonremote = new JSONObject();
			jsonremote.put(RemoteTaskList.Columns._ID, remote._id);
			addAllContentToJSON(remote.getContent(), jsonremote);

			remotelistarray.put(jsonremote);
		}
		return remotelistarray;
	}

	private JSONArray getJSONTasksFor(final TaskList list) throws JSONException {
		final JSONArray taskarray = new JSONArray();
		for (final Task task : getTasksIn(list)) {
			final JSONObject jsontask = new JSONObject();
			jsontask.put(Task.Columns._ID, task._id);
			addAllContentToJSON(task.getContent(), jsontask);
			jsontask.put(Task.Columns.LEFT, task.left);
			jsontask.put(Task.Columns.RIGHT, task.right);

			jsontask.put(KEY_REMOTES, getJSONRemotesFor(task));
			jsontask.put(KEY_REMINDERS, getJSONRemindersFor(task));

			taskarray.put(jsontask);
		}
		return taskarray;
	}

	private JSONArray getJSONRemotesFor(final Task task) throws JSONException {
		final JSONArray remotetaskarray = new JSONArray();
		for (final RemoteTask remote : getRemotesOf(task)) {
			final JSONObject jsonremote = new JSONObject();
			jsonremote.put(RemoteTask.Columns._ID, remote._id);
			addAllContentToJSON(remote.getContent(), jsonremote);

			remotetaskarray.put(jsonremote);
		}
		return remotetaskarray;
	}

	private JSONArray getJSONRemindersFor(final Task task) throws JSONException {
		final JSONArray reminderarray = new JSONArray();
		for (final Notification reminder : getRemindersFor(task)) {
			final JSONObject jsonreminder = new JSONObject();
			jsonreminder.put(Notification.Columns._ID, reminder._id);
			addAllContentToJSON(reminder.getContent(), jsonreminder);

			reminderarray.put(jsonreminder);
		}
		return reminderarray;
	}

	/**
	 * Backs up the entire database to a JSON file. The location and name of the
	 * file are hardcoded.
	 *
	 * @throws JSONException
	 * @throws IOException
	 */
	public void writeBackup() throws JSONException, IOException {
		// Create JSON object
		final JSONObject backup = getJSONBackup();

		// Serialise the JSON object to a file
		final File backupFile = new File(getBackupFilePath(this.context));
		if (backupFile.exists()) {
			backupFile.delete();
		}
		backupFile.getParentFile().mkdirs();
		backupFile.createNewFile();
		final FileWriter writer = new FileWriter(backupFile);
		writer.write(backup.toString(2));
		writer.flush();
		writer.close();
	}

	/**
	 * Clears the database and restores the backup. Throws exceptions on
	 * failure.
	 *
	 * @throws JSONException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void restoreBackup() throws FileNotFoundException, JSONException,
			IOException {
		final JSONObject backup = readBackup();
		// Only if backup exists will we clear the database
		clearDatabase();

		final JSONArray listsarray = backup.getJSONArray(KEY_LISTS);
		for (int i = 0; i < listsarray.length(); i++) {
			final JSONObject jsonlist = listsarray.getJSONObject(i);
			final TaskList tasklist = new TaskList(jsonlist);
			if (tasklist.updated != null)
				tasklist.save(context, tasklist.updated);
			else
				tasklist.save(context);

			if (!jsonlist.isNull(KEY_REMOTES)) {
				restoreRemotes(tasklist, jsonlist.getJSONArray(KEY_REMOTES));
			} else {
				Log.d("JONAS", "Remotes was null");
			}
			if (!jsonlist.isNull(KEY_TASKS)) {
				restoreTasks(tasklist, jsonlist.getJSONArray(KEY_TASKS));
			}
		}

		// Schedule notifications
		NotificationHelper.schedule(context);

		// TODO Add geofences
	}

	private void clearDatabase() {
		// TODO Remove geofences

		context.getContentResolver().delete(RemoteTask.URI, null, null);
		context.getContentResolver().delete(RemoteTaskList.URI, null, null);

		context.getContentResolver().delete(TaskList.URI, null, null);

		context.getContentResolver().delete(Task.URI, null, null);
		context.getContentResolver().delete(Notification.URI, null, null);

	}

	private JSONObject readBackup() throws JSONException, IOException,
			FileNotFoundException {
		// Try to read the backup file
		final File backupFile = new File(getBackupFilePath(this.context));
		final StringBuilder sb = new StringBuilder();
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(backupFile));
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		return new JSONObject(sb.toString());
	}

	private void restoreRemotes(final TaskList tasklist,
								final JSONArray jsonArray) throws JSONException {
		Log.d("JONAS", "Remote length: " + jsonArray.length());
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject json = jsonArray.getJSONObject(i);
			final RemoteTaskList remote = new RemoteTaskList(json);
			remote.dbid = tasklist._id;
			remote.save(context);
			Log.d("JONAS", "RemoteL restored: " + remote._id);
		}
	}

	private void restoreTasks(final TaskList list, final JSONArray tasksarray)
			throws JSONException {
		for (int i = 0; i < tasksarray.length(); i++) {
			final JSONObject jsontask = tasksarray.getJSONObject(i);
			final Task task = new Task(jsontask);
			task.dblist = list._id;
			if (task.updated != null)
				task.save(context, task.updated);
			else
				task.save(context);

			if (!jsontask.isNull(KEY_REMOTES)) {
				restoreRemotes(task, jsontask.getJSONArray(KEY_REMOTES));
			}
			if (!jsontask.isNull(KEY_REMINDERS)) {
				restoreReminders(task, jsontask.getJSONArray(KEY_REMINDERS));
			}
		}
	}

	private void restoreRemotes(final Task task, final JSONArray jsonArray)
			throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject json = jsonArray.getJSONObject(i);
			final RemoteTask remote = new RemoteTask(json);
			remote.dbid = task._id;
			remote.listdbid = task.dblist;
			remote.save(context);
			Log.d("JONAS", "RemoteT restored: " + remote._id);
		}
	}

	private void restoreReminders(final Task task, final JSONArray jsonArray)
			throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject json = jsonArray.getJSONObject(i);
			final Notification not = new Notification(json);
			not.taskID = task._id;
			not.save(context);
		}
	}
}
