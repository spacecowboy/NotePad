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

package com.nononsenseapps.notepad.data.local.orgmode;

import android.content.Context;
import android.util.Pair;

import com.nononsenseapps.notepad.data.model.orgmode.RemoteTaskListFile;
import com.nononsenseapps.notepad.data.model.orgmode.RemoteTaskNode;
import com.nononsenseapps.notepad.data.model.sql.RemoteTask;
import com.nononsenseapps.notepad.data.model.sql.RemoteTaskList;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import org.cowboyprogrammer.org.OrgFile;
import org.cowboyprogrammer.org.OrgNode;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

public abstract class Synchronizer extends DBSyncBase implements
		SynchronizerInterface {

	public static final int SAVENONE = 0x0;
	public static final int SAVEDB = 0x01;
	public static final int SAVEORG = 0x10;
	public static final String TAG = "OrgSynchronizer";

	public Synchronizer(Context context) {
		super(context);
	}

	/**
	 * Performs a full 2-way sync between the DB and the remote source.
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public void fullSync() throws IOException, ParseException {
		// For all pairs of files and db entries
		final List<Pair<OrgFile, Pair<RemoteTaskList, TaskList>>> pairs = getFilesAndDBEntries();

		for (Pair<OrgFile, Pair<RemoteTaskList, TaskList>> pair : pairs) {
			OrgFile file = pair.first;
			RemoteTaskList dbEntry = pair.second.first;
			TaskList list = pair.second.second;
			if (dbEntry == null) {
				if (file == null) {
					// NEW CREATE FILE
					// Create file
                    file = getNewFile(list.title);
                    OrgConverter.toFileFromList(list, file);

					// Add tasks to File
					syncTasks(context, list, file);

					// Save file
					putRemoteFile(file);

                    // If name was not available, rename list as well
                    if (!file.getFilename().equals(OrgConverter
                            .getTitleAsFilename(list))) {
                        list.title = file.getFilename().substring(0,
                                file.getFilename().length() - 4);
                        list.save(context);
                    }

					// Create DbEntry
					dbEntry = new RemoteTaskList();
					dbEntry.dbid = list._id;
					dbEntry.account = getAccountName();
					dbEntry.service = getServiceName();
					OrgConverter.toRemoteFromFile(dbEntry, file);
					dbEntry.save(context);

				} else {
					// NEW CREATE DB LIST
					// Create TaskList
					list = new TaskList();
					OrgConverter.toListFromFile(list, file);
					list.save(context, file.lastModified());

					// Create DbEntry
					dbEntry = new RemoteTaskList();
					dbEntry.dbid = list._id;
					dbEntry.account = getAccountName();
					dbEntry.service = getServiceName();
					OrgConverter.toRemoteFromFile(dbEntry, file);
					dbEntry.save(context);

					// Now do the tasks
					if (syncTasks(context, list, file)) {
						// Something changed in the file.
						putRemoteFile(file);
					}
				}
			} else {
				if (list == null) {
					// DELETE FILE DB
					deleteRemoteFile(file);
					deleteLocal(list, dbEntry);
				} else {
					if (file == null) {
						// DELETE DB LIST
						// List and entry
						deleteLocal(list, dbEntry);
					} else {
                        // UPDATE EXISTING LIST, IF CHANGED
                        boolean shouldSaveFile = false;

						if (wasRenamed(list, dbEntry, file)) {
							final String oldName = file.getFilename();
							renameFile(list, dbEntry, file);
							renameRemoteFile(oldName, file);
						}

                        // Merge information in database and file
                        final int shouldSave = merge(list, dbEntry, file);

						if (0 < (shouldSave & SAVEORG)) {
							// UPDATE FILE DB
							shouldSaveFile = true;
						}
						if (0 < (shouldSave & SAVEDB)) {
                            // UPDATE LIST DB
                            list.save(context);
						}
                        if (shouldSave != SAVENONE) {
                            OrgConverter.toRemoteFromFile(dbEntry, file);
                            dbEntry.updated = Calendar.getInstance()
                                    .getTimeInMillis();
                            dbEntry.save(context);
                        }

						// In both cases, sync tasks
						if (syncTasks(context, list, file) || shouldSaveFile) {
							// Something changed in the file.
							putRemoteFile(file);
						}
					}
				}
			}
		}
	}

	/**
	 * Merge the list and file. Fields considered are the listtype and
	 * listsorting which are stored as comments in the file.
	 * 
	 * @param list
	 * @param dbEntry
	 * @param file
	 * @return an integer denoting which should be saved. 0 for none, 0x01 for
	 *         task, 0x10 for node. 0x11 for both.
	 */
	private int merge(final TaskList list, final RemoteTaskList dbEntry,
			final OrgFile file) {
		int shouldSave = SAVENONE;

		shouldSave |= mergeSorting(list, dbEntry, file);
		shouldSave |= mergeListType(list, dbEntry, file);

		return shouldSave;
	}

	private int mergeSorting(final TaskList list, final RemoteTaskList dbEntry,
			final OrgFile file) {
		final int shouldSave;
		final String filesorting = OrgConverter.getListSortingFromMeta(file);
		if (list.sorting == null
				&& RemoteTaskListFile.getSorting(dbEntry) != null
				|| list.sorting != null
				&& !list.sorting.equals(RemoteTaskListFile.getSorting(dbEntry))) {
			shouldSave = SAVEORG;
			OrgConverter.setSortingOnFile(list, file);
		} else if (filesorting == null
				&& RemoteTaskListFile.getSorting(dbEntry) != null
				|| filesorting != null
				&& !filesorting.equals(RemoteTaskListFile.getSorting(dbEntry))) {
			shouldSave = SAVEORG;
			list.sorting = filesorting;
		} else {
			shouldSave = SAVENONE;
		}
		return shouldSave;
	}

	private int mergeListType(final TaskList list,
			final RemoteTaskList dbEntry, final OrgFile file) {
		final int shouldSave;
		final String filelisttype = OrgConverter.getListTypeFromMeta(file);
		if (list.listtype == null
				&& RemoteTaskListFile.getListType(dbEntry) != null
				|| list.listtype != null
				&& !list.listtype.equals(RemoteTaskListFile
						.getListType(dbEntry))) {
			shouldSave = SAVEORG;
			OrgConverter.setListTypeOnFile(list, file);
		} else if (filelisttype == null
				&& RemoteTaskListFile.getListType(dbEntry) != null
				|| filelisttype != null
				&& !filelisttype
						.equals(RemoteTaskListFile.getListType(dbEntry))) {
			shouldSave = SAVEORG;
			list.listtype = filelisttype;
		} else {
			shouldSave = SAVENONE;
		}
		return shouldSave;
	}

	private boolean syncTasks(final Context context, final TaskList list,
			final OrgFile file) {
		final List<Pair<OrgNode, Pair<RemoteTask, Task>>> pairs = getNodesAndDBEntries(
				file, list);
		boolean shouldUpdateFile = false;

		OrgNode prevNode = null;

		for (Pair<OrgNode, Pair<RemoteTask, Task>> pair : pairs) {
			OrgNode node = pair.first;
			RemoteTask dbEntry = pair.second.first;
			Task task = pair.second.second;

			if (dbEntry == null) {
				if (node == null) {
					// CREATE NODE DB
					//Log.d(TAG, "CREATE NODE DB");
					node = new OrgNode(file.getParser());
					node.setLevel(1);
					node.setParent(file);
					int idx = -1;
					if (prevNode != null) {
						idx = file.getSubNodes().indexOf(prevNode);
					}
					file.getSubNodes().add(idx + 1, node);
					OrgConverter.toNodeFromTask(task, node);

					dbEntry = new RemoteTask();
					dbEntry.dbid = task._id;
					dbEntry.listdbid = list._id;
					dbEntry.account = getAccountName();
					dbEntry.service = getServiceName();
					OrgConverter.toRemoteFromNode(dbEntry, node);
					dbEntry.save(context);

					shouldUpdateFile = true;
				} else {
					// CREATE TASK DB
					//Log.d(TAG, "CREATE TASK DB");
					task = new Task();
					task.dblist = list._id;
					OrgConverter.toTaskFromNode(task, node);
					task.save(context);

					dbEntry = new RemoteTask();
					dbEntry.dbid = task._id;
					dbEntry.listdbid = list._id;
					dbEntry.account = getAccountName();
					dbEntry.service = getServiceName();
					shouldUpdateFile = OrgConverter.toRemoteFromNode(dbEntry,
							node);
					dbEntry.save(context);

					replaceNotifications(task, node);
				}
			} else {
				if (task == null) {
					// DELETE NODE DB
					//Log.d(TAG, "DELETE NODE DB");
					deleteLocal(task, dbEntry);
                    if (node != null) {
                        deleteNode(node);
                        shouldUpdateFile = true;
                    }
				} else {
					if (node == null) {
						// DELETE DB TASK
						//Log.d(TAG, "DELETE TASK DB");
						deleteLocal(task, dbEntry);
					} else {
						// TODO need to check notifications also
						//Log.d(TAG, "MERGE TASKS");
						final int shouldSave = merge(task, dbEntry, node);

						if (0 < (shouldSave & SAVEORG)) {
							// UPDATE NODE DB
							OrgConverter.toNodeFromRemote(node, dbEntry);
							shouldUpdateFile = true;
						}
						if (0 < (shouldSave & SAVEDB)) {
							task.save(context);
						}
                        if (0 < shouldSave) {
                            // Remember this version for later
                            OrgConverter.toRemoteFromNode(dbEntry, node);
                            dbEntry.save(context);
                        }
                    }
				}
			}

			// Remember the previous next time for positioning
			if (node != null) {
				prevNode = node;
			}
		}

		return shouldUpdateFile;
	}

	/**
	 * 
	 * @param node
	 *            to delete from the tree structure. Preserves sub nodes.
	 */
	private void deleteNode(final OrgNode node) {
		final OrgNode parent = node.getParent();
		// If no parent, nothing to do
		if (parent == null)
			return;

		// If sub nodes, transfer to root
		if (!node.getSubNodes().isEmpty()) {
			final int i = parent.getSubNodes().indexOf(node);
			parent.getSubNodes().addAll(i, node.getSubNodes());
		}
		// Remove the node
		parent.getSubNodes().remove(node);
	}

	/**
	 * Merges the task and node. The fields considered are title, body,
	 * completed and deadline.
	 * 
	 * @param task
	 * @param remote
	 * @param node
	 * @return an integer denoting which should be saved. 0 for none, 0x01 for
	 *         task, 0x10 for node. 0x11 for both.
	 */
	protected int merge(final Task task, final RemoteTask remote,
			final OrgNode node) {
		if (task == null || remote == null || node == null) {
			throw new NullPointerException(
					"A merge operation can't have null parties!");
		}
		// 0x01 if task should be saved
		// 0x10 if node should be saved
		// 0x11 if both should be saved
		// 0x00 if nothing needs to be saved
		int shouldSave = SAVENONE;

		shouldSave |= mergeTitles(task, remote, node);
		shouldSave |= mergeBodies(task, remote, node);
		shouldSave |= mergeTodo(task, remote, node);
		shouldSave |= mergeTimestamps(task, remote, node);

		return shouldSave;
	}

	private int mergeTodo(final Task task, final RemoteTask remote,
			final OrgNode node) {
		final int shouldSave;
		final String taskTodo;
		if (task.completed != null)
			taskTodo = "DONE";
		else
			taskTodo = "TODO";

		if (!taskTodo.equals(RemoteTaskNode.getTodo(remote))) {
			shouldSave = SAVEORG;
			node.setTodo(taskTodo);
		} else if (RemoteTaskNode.getTodo(remote) != null
				&& !RemoteTaskNode.getTodo(remote).equals(node.getTodo())) {
			shouldSave = SAVEDB;
			if ("DONE".equals(node.getTodo())) {
				task.completed = Calendar.getInstance().getTimeInMillis();
			} else {
				task.completed = null;
			}
		} else {
			shouldSave = SAVENONE;
		}

		return shouldSave;
	}

	private int mergeTimestamps(final Task task, final RemoteTask remote,
			final OrgNode node) {
		final int shouldSave;
		Long basedue = null;
		if (RemoteTaskNode.getDueTime(remote) != null
				&& !RemoteTaskNode.getDueTime(remote).isEmpty()) {
			basedue = Long.parseLong(RemoteTaskNode.getDueTime(remote));
		}

		final Long nodedue = OrgConverter.getDeadline(node);

		if (task.due != basedue) {
			shouldSave = SAVEORG;
			OrgConverter.setDeadline(node, task.due);
		} else if (nodedue != basedue) {
			shouldSave = SAVEDB;
			task.due = nodedue;
		} else {
			shouldSave = SAVENONE;
		}

		return shouldSave;
	}

	private int mergeBodies(final Task task, final RemoteTask remote,
			final OrgNode node) {
		final int shouldSave;
        boolean taskChanged = !task.note.equals(RemoteTaskNode.getBody(remote));
        // Check with trailing newline also
        if (taskChanged) {
            taskChanged = !(task.note + "\n").equals(RemoteTaskNode.getBody(remote));
        }

		if (taskChanged) {
			shouldSave = SAVEORG;
			node.setBody(task.note);
		} else if (!node.getBody().equals(RemoteTaskNode.getBody(remote))) {
			shouldSave = SAVEDB;
			task.note = node.getBody();
             /*
              * It's not possible to differentiate if the user added a trailing
              * newline or the sync logic did. I will assume that the sync logic did.
              */
            if (task.note != null && !task.note.isEmpty() && task.note.endsWith("\n")) {
                task.note = task.note.substring(0, task.note.length() - 1);
            }
		} else {
			shouldSave = SAVENONE;
		}

		return shouldSave;
	}

	private int mergeTitles(final Task task, final RemoteTask remote,
			final OrgNode node) {
		final int shouldSave;
		if (!task.title.equals(RemoteTaskNode.getTitle(remote))) {
			shouldSave = SAVEORG;
			node.setTitle(task.title);
		} else if (!node.getTitle().equals(RemoteTaskNode.getTitle(remote))) {
			shouldSave = SAVEDB;
			task.title = node.getTitle();
		} else {
			shouldSave = SAVENONE;
		}
		return shouldSave;
	}

}
