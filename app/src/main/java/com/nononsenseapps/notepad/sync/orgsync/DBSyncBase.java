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

package com.nononsenseapps.notepad.sync.orgsync;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.Pair;

import com.nononsenseapps.notepad.database.RemoteTask;
import com.nononsenseapps.notepad.database.RemoteTaskList;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import org.cowboyprogrammer.org.OrgFile;
import org.cowboyprogrammer.org.OrgNode;
import org.cowboyprogrammer.org.OrgTimestamp;
import org.cowboyprogrammer.org.parser.RegexParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * This class is suitable for synchronizers to inherit from. It contains the
 * necessary logic to handle the database communication and conversions.
 */
public abstract class DBSyncBase implements SynchronizerInterface {

	protected final Context context;
	private final ContentResolver resolver;

	public DBSyncBase(final Context context) {
		this.context = context;
		this.resolver = context.getContentResolver();

	}

	/**
	 * Reads the database and the OrgFile. Returns the matching Tasks and Nodes.
	 * <p/>
	 * TODO
	 *  For gods' sake, test me!
	 *
	 * @param file The OrgFile containing all the tasks
	 * @param list The TaskList corresponding to the OrgFile.
	 * @return A list of all task-related objects necessary for synchronization.
	 */
	protected List<Pair<OrgNode, Pair<RemoteTask, Task>>> getNodesAndDBEntries(
			OrgFile file, TaskList list) {
		final List<Pair<OrgNode, Pair<RemoteTask, Task>>> result = new ArrayList<>();

		final HashMap<Long, Task> tasks = getTasks(list);

		final HashMap<Long, RemoteTask> remotes = getValidRemoteTasks(list);

		final List<RemoteTask> remotesDeleted = getInvalidRemoteTasks(list);

		final HashMap<String, OrgNode> nodes = getNodes(file);

		// Start with tasks
		for (long dbid : tasks.keySet()) {
			Task task = tasks.get(dbid);
			RemoteTask remote = remotes.remove(dbid);
			OrgNode node = null;
			// Can be null
			if (remote != null) {
				node = nodes.remove(remote.remoteId.toUpperCase());
			}
			result.add(new Pair<>(node,
					new Pair<>(remote, task)));
		}
		// Follow with remaining remotes where task is null
		for (RemoteTask remote : remotes.values()) {
			OrgNode node = nodes.remove(remote.remoteId.toUpperCase());
			result.add(new Pair<>(node, new Pair<>(remote, null)));
		}
		for (RemoteTask remote : remotesDeleted) {
			OrgNode node = nodes.remove(remote.remoteId.toUpperCase());
			result.add(new Pair<>(node, new Pair<>(remote, null)));
		}
		// Last, nodes with no database connections
		for (OrgNode node : nodes.values()) {
			result.add(new Pair<>(node, new Pair<>(/*task=*/ null, /*remote=*/ null)));
		}

		return result;
	}

	private HashMap<String, OrgNode> getNodes(final OrgFile file) {
		final HashMap<String, OrgNode> map = new HashMap<>();

		for (OrgNode node : file.getSubNodes()) {
			addNodeToMap(node, map);
		}

		return map;
	}

	/**
	 * By convention, all generated ids are stored in uppercase.
	 */
	private void addNodeToMap(final OrgNode node,
							  final HashMap<String, OrgNode> map) {
		String key = OrgConverter.getNodeId(node);
		Log.d(Synchronizer.TAG, "Key: " + key + ", node: " + node.getComments());
		if (key == null) {
			// This key won't necessarily be used later.
			key = OrgConverter.generateId();
		}
		map.put(key.toUpperCase(), node);

		for (OrgNode subnode : node.getSubNodes()) {
			addNodeToMap(subnode, map);
		}
	}

	private HashMap<Long, RemoteTask> getValidRemoteTasks(final TaskList list) {
		final HashMap<Long, RemoteTask> map = new HashMap<>();
		try (Cursor c = resolver.query(
				RemoteTask.URI,
				RemoteTask.Columns.FIELDS,
				RemoteTask.Columns.SERVICE + " IS ? AND "
						+ RemoteTask.Columns.ACCOUNT + " IS ? AND "
						+ RemoteTask.Columns.LISTDBID + " IS ? AND "
						+ RemoteTask.Columns.DBID + " > 0",
				new String[] { getServiceName(), getAccountName(),
						Long.toString(list._id) }, null)) {
			while (c.moveToNext()) {
				RemoteTask remote = new RemoteTask(c);
				map.put(remote.dbid, remote);
			}
		}

		return map;
	}

	/**
	 * These remote tasks are no longer connected to a task.
	 * This typically happens when a task is
	 * deleted or moved to another list.
	 */
	private List<RemoteTask> getInvalidRemoteTasks(final TaskList list) {
		final ArrayList<RemoteTask> remoteList = new ArrayList<>();
		try (Cursor c = resolver.query(
				RemoteTask.URI,
				RemoteTask.Columns.FIELDS,
				RemoteTask.Columns.SERVICE + " IS ? AND "
						+ RemoteTask.Columns.ACCOUNT + " IS ? AND "
						+ RemoteTask.Columns.LISTDBID + " IS ? AND "
						+ RemoteTask.Columns.DBID + " < 1",
				new String[] { getServiceName(), getAccountName(),
						Long.toString(list._id) }, null)) {
			while (c.moveToNext()) {
				RemoteTask remote = new RemoteTask(c);
				remoteList.add(remote);
			}
		}

		return remoteList;
	}

	private HashMap<Long, Task> getTasks(final TaskList list) {
		final HashMap<Long, Task> map = new HashMap<>();
		try (Cursor c = resolver.query(Task.URI, Task.Columns.FIELDS,
				Task.Columns.DBLIST + " IS ?",
				new String[] { Long.toString(list._id) }, null)) {
			while (c.moveToNext()) {
				Task task = new Task(c);
				map.put(task._id, task);
			}
		}

		return map;
	}

	/**
	 * Reads the database and the remote source.
	 *
	 * @return The matching TaskList and OrgFiles.
	 */
	protected List<Pair<OrgFile, Pair<RemoteTaskList, TaskList>>> getFilesAndDBEntries()
			throws IOException, ParseException {
		final List<Pair<OrgFile, Pair<RemoteTaskList, TaskList>>> result = new ArrayList<>();

		// get all lists
		final HashMap<Long, TaskList> lists = getLists();

		// get all db entries
		final HashMap<Long, RemoteTaskList> remotes = getRemoteTaskLists();

		// get all files
		final HashSet<String> filenames = getRemoteFilenames();
		for (String filename : filenames) {
			Log.d(Synchronizer.TAG, "Get Filename: " + filename);
		}

		// Construct pairs from lists first. This removes entries as it goes.
		for (Long dbid : lists.keySet()) {
			TaskList list = lists.get(dbid);
			RemoteTaskList remote = remotes.remove(dbid);
			OrgFile file = null;
			// Can be null
			if (remote != null && filenames.remove(remote.remoteId)) {
				final BufferedReader br = getRemoteFile(remote.remoteId);
				if (br != null) {
					file = OrgFile.createFromBufferedReader(
							new RegexParser(), remote.remoteId, br);
				}
			}
			// list title, if available
			String l = list == null ? null : list.title;

			String r = null;
			if (remote != null) r = remote.remoteId;
			String f = null;
			if (file != null) f = file.getFilename();
			Log.d(Synchronizer.TAG, "Pair:" + l + ", " + r + ", " + f);
			result.add(new Pair<>(file, new Pair<>(remote, list)));
		}

		// Add remotes that no longer have a list
		for (RemoteTaskList remote : remotes.values()) {
			OrgFile file = null;
			// Can be null
			if (remote != null && filenames.remove(remote.remoteId)) {
				final BufferedReader br = getRemoteFile(remote.remoteId);
				if (br != null) {
					file = OrgFile.createFromBufferedReader(
							new RegexParser(), remote.remoteId, br);
				}
			}
			String r = null;
			if (remote != null)
				r = remote.remoteId;
			String f = null;
			if (file != null)
				f = file.getFilename();
			Log.d(Synchronizer.TAG, "Pair:" + "(null)" + ", " + r + ", " + f);
			result.add(new Pair<>(file,
					new Pair<>(remote, null)));
		}

		// Add files that do not exist in database
		for (String filename : filenames) {
			OrgFile file = null;
			final BufferedReader br = getRemoteFile(filename);
			if (br != null) {
				file = OrgFile.createFromBufferedReader(new RegexParser(), filename, br);
			}
			String f;
			// An obvious precaution. If everything is null, there's nothing to add.
			if (file != null) {
				f = file.getFilename();
				Log.d(Synchronizer.TAG, "Pair:" + "(null)" + ", " + "(null)" + ", " + f);
				result.add(new Pair<>(file, new Pair<>(/*remote=*/null, /*list=*/null)));
			}
		}

		return result;
	}

	/**
	 * @return a map from list-dbid to RemoteTaskList
	 */
	private HashMap<Long, RemoteTaskList> getRemoteTaskLists() {
		final HashMap<Long, RemoteTaskList> map = new HashMap<>();
		try (Cursor c = resolver.query(RemoteTaskList.URI,
				RemoteTaskList.Columns.FIELDS, RemoteTaskList.Columns.SERVICE
						+ " IS ? AND " + RemoteTask.Columns.ACCOUNT + " IS ?",
				new String[] { getServiceName(), getAccountName() }, null)) {
			while (c.moveToNext()) {
				RemoteTaskList remote = new RemoteTaskList(c);
				Log.d(Synchronizer.TAG, "Get remote: " + remote.remoteId);
				map.put(remote.dbid, remote);
			}
		}

		return map;
	}

	/**
	 * @return a map from list-dbid to TaskList
	 */
	private HashMap<Long, TaskList> getLists() {
		final HashMap<Long, TaskList> map = new HashMap<>();
		try (Cursor c = resolver.query(TaskList.URI, TaskList.Columns.FIELDS,
				null, null, null)) {
			while (c.moveToNext()) {
				TaskList list = new TaskList(c);
				Log.d(Synchronizer.TAG, "Get list: " + list.title);
				map.put(list._id, list);
			}
		}

		return map;
	}

	/**
	 * Make sure notifications are synchronized from node to database.
	 */
	protected void replaceNotifications(final Task task, final OrgNode node) {
		// TODO Auto-generated method stub
		// Remove existing notifications

		// Add new notifications
		for (OrgTimestamp ts : node.getTimestamps()) {
			if (!ts.isInactive()) {

			}
		}
	}

	protected boolean wasRenamed(final TaskList list, final OrgFile file) {
		return !(OrgConverter.getTitleAsFilename(list)).equals(file.getFilename());
	}

	/**
	 * (re)Names a file to match the DB version's current name.
	 *
	 * @param list    Current version in the database
	 * @param dbEntry Current remote version in the database which will also be
	 *                renamed.
	 * @param file    File to rename.
	 */
	protected void renameFile(final TaskList list,
							  final RemoteTaskList dbEntry, final OrgFile file) {
		if (list.title != null && list.title.length() > 0) {
			file.setFilename(OrgConverter.getTitleAsFilename(list));
		}
		dbEntry.remoteId = file.getFilename();
		dbEntry.save(context);
	}

	/**
	 * Delete remote versions of tasks to current service.
	 *
	 * @param listdbid List they belong to.
	 */
	private void deleteRemoteTasksIn(final long listdbid) {
		context.getContentResolver().delete(
				RemoteTask.URI,
				RemoteTask.Columns.SERVICE + " IS ? AND " + RemoteTask.Columns.ACCOUNT +
						" IS ? AND " + RemoteTask.Columns.LISTDBID + " IS ?",
				new String[] { getServiceName(), getAccountName(), Long.toString(listdbid) });
	}

	/**
	 * Deletes a list and all tasks and related entries (to current service).
	 * Call this when remote file has been deleted.
	 *
	 * @param list    List to delete. Can be null.
	 * @param dbEntry RemoteEntry in DB to delete. Can be null.
	 */
	protected void deleteLocal(final TaskList list, final RemoteTaskList dbEntry) {
		long listdbid = -1;
		if (list != null) {
			list.delete(context);
			listdbid = list._id;
		}
		if (dbEntry != null) {
			dbEntry.delete(context);
			listdbid = dbEntry.dbid;
		}
		// Tasks are deleted automatically, but not the
		// remote-versions
		deleteRemoteTasksIn(listdbid);
	}

	/**
	 * Deletes a task and dbEntry from database.
	 *
	 * @param task    Task to delete, can be null.
	 * @param dbEntry dbEntry to delete, can be null.
	 */
	protected void deleteLocal(final Task task, final RemoteTask dbEntry) {
		if (task != null) {
			task.delete(context);
		}
		if (dbEntry != null) {
			dbEntry.delete(context);
		}
	}
}
