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

import android.annotation.SuppressLint;

import com.nononsenseapps.notepad.data.model.orgmode.RemoteTaskListFile;
import com.nononsenseapps.notepad.data.model.orgmode.RemoteTaskNode;
import com.nononsenseapps.notepad.data.model.sql.Notification;
import com.nononsenseapps.notepad.data.model.sql.RemoteTask;
import com.nononsenseapps.notepad.data.model.sql.RemoteTaskList;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import org.cowboyprogrammer.org.OrgFile;
import org.cowboyprogrammer.org.OrgNode;
import org.cowboyprogrammer.org.OrgTimestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles conversion from the internal database format to org-mode
 * fileformat
 */
public class OrgConverter {

	private static final String LISTSTYLECOMMENT = "# NONSENSESTYLE: ";
	private static final String LISTSORTCOMMENT = "# NONSENSESORTING: ";
	private static final String TASKNODEID = "# NONSENSEID: ";
	private static final Pattern PatternStyle = Pattern.compile(
			"#\\s*NONSENSESTYLE:\\s*(.+)\\s*?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PatternSorting = Pattern
			.compile("#\\s*NONSENSESORTING:\\s*(.+)\\s*?",
                    Pattern.CASE_INSENSITIVE);
	// Ending white space used when removed
	private static final String NonsenseIdPattern = "#\\s*NONSENSEID:\\s*(\\w+)\\s*";
	private static final Pattern PatternId = Pattern.compile(NonsenseIdPattern,
			Pattern.CASE_INSENSITIVE);
    private static final String TAG = "OrgConverter";
    private static Random rand;

	/**
	 * Generates an id for RemoteTask(List) objects.
	 */
	public static String generateId() {
		final int len = 8;
		if (rand == null) {
			rand = new Random();
		}
		String hex = Integer.toHexString(rand.nextInt());
		// Pad with zeros if too short
		while (hex.length() < len) {
			hex = "0".concat(hex);
		}
		return hex.substring(0, len);
	}

	/**
	 * Fill in all the properties of the file that should go in the TaskList
	 * object.
	 */
	public static void toListFromFile(final TaskList list, final OrgFile file) {
		// Minus .org extension
		list.title = file.getFilename().substring(0,
				file.getFilename().length() - 4);
		list.sorting = getListSortingFromMeta(file);
		list.listtype = getListTypeFromMeta(file);
	}

	/**
	 * Reads comment section of file. Returns null if not found.
	 */
	public static String getListTypeFromMeta(final OrgFile file) {
		final Matcher m = PatternStyle.matcher(file.getComments());
		if (m.find()) {
			return m.group(1);
		} else {
			return null;
		}
	}

	/**
	 * Reads comment section of file. Returns null if not found.
	 */
	public static String getListSortingFromMeta(final OrgFile file) {
		final Matcher m = PatternSorting.matcher(file.getComments());
		if (m.find()) {
			return m.group(1);
		} else {
			return null;
		}
	}

	/**
	 * Fill in all the properties of the file that should go in the
	 * RemoteTaskList object.
	 */
	public static void toRemoteFromFile(final RemoteTaskList entry,
			final OrgFile file) {
		entry.remoteId = file.getFilename();
		RemoteTaskListFile.setSorting(entry, getListSortingFromMeta(file));
		RemoteTaskListFile.setListType(entry, getListTypeFromMeta(file));
		entry.updated = Calendar.getInstance().getTimeInMillis();
	}

	/**
	 * Fill in all the properties of the node that should go in the Task object.
	 */
	public static void toTaskFromNode(final Task task, final OrgNode node) {
		task.title = node.getTitle();
		task.due = getDeadline(node);
		task.completed = getCompleted(node);
        task.note = node.getBody();

        /*
        * It's not possible to differentiate if the user added a trailing
        * newline or the sync logic did. I will assume that the sync logic did.
        */
        if (task.note != null && !task.note.isEmpty() && task.note.endsWith("\n")) {
            task.note = task.note.substring(0, task.note.length() - 1);
        }
    }

	/**
	 * Fill in all the properties of the nodes from the task object.
	 */
	public static void toNodeFromTask(final Task task, final OrgNode node) {
		node.setLevel(1);
		node.setTitle(task.title);
		node.setBody(task.note);
		setTodo(node, task.completed);
		removeTimestamps(node);
		setDeadline(node, task.due);
	}

	private static void setNotifications(final OrgNode node,
			final List<Notification> reminders) {
		if (reminders == null)
			return;

		for (Notification reminder : reminders) {
			if (reminder.radius == null && reminder.time != null) {
				OrgTimestamp ts = new OrgTimestamp(reminder.time, true);
				ts.setInactive(false);
				node.getTimestamps().add(ts);
			}
		}
	}

	/**
	 * Returns NOW as completed if node is DONE. Else null.
	 */
	private static Long getCompleted(final OrgNode node) {
		if ("DONE".equals(node.getTodo())) {
			return new Date().getTime();
		} else {
			return null;
		}
	}

	private static void setTodo(final OrgNode node, final Long completed) {
		if (completed == null) {
			node.setTodo("TODO");
		} else {
			node.setTodo("DONE");
		}
	}

	/**
	 * Return the (first) deadline of the object, or null
	 */
	public static Long getDeadline(final OrgNode node) {
		for (OrgTimestamp ts : node.getTimestamps()) {
			if (OrgTimestamp.Type.DEADLINE == ts.getType()) {
				return ts.getDate().toDate().getTime();
			}
		}
		return null;
	}

	private static void removeTimestamps(final OrgNode node) {
		node.getTimestamps().clear();
	}

	public static void setDeadline(final OrgNode node, final Long due) {
		node.getTimestamps().clear();
		// Add deadline if not null
		if (due != null) {
			OrgTimestamp ts = new OrgTimestamp(due, false);
			ts.setType(OrgTimestamp.Type.DEADLINE);
			node.getTimestamps().add(ts);
		}
	}

	/**
	 * Remove a possible comment containing a nonsenseid
	 */
	private static String getOrgBodySansId(final OrgNode node) {
		return node.getOrgBody().replaceFirst(NonsenseIdPattern, "");
	}

	/**
	 * Fill in all the properties of the node that should go in the RemoteTask
	 * object. If no ID is set, then an ID is added to both the entry and the
	 * node. This method returns true if an id was added to the node, and it
	 * should be updated in file.
	 */
	public static boolean toRemoteFromNode(final RemoteTask dbEntry,
			final OrgNode node) {
		boolean addedToNode = false;
		if (dbEntry.remoteId == null) {
			String id = getNodeId(node);
			if (id == null) {
				id = generateId();
				addIdToNode(id, node);
				addedToNode = true;
			}
			dbEntry.remoteId = id;
		}
		dbEntry.updated = Calendar.getInstance().getTimeInMillis();

		RemoteTaskNode.setTitle(dbEntry, node.getTitle());
		RemoteTaskNode.setBody(dbEntry, node.getBody());
		final Long t = getDeadline(node);
		String s = null;
		if (t != null)
			s = Long.toString(t);
		RemoteTaskNode.setDueTime(dbEntry, s);
		RemoteTaskNode.setTodo(dbEntry, node.getTodo());

		return addedToNode;
	}

	/**
	 * Add an id to the meta-section of a node.
	 */
	@SuppressLint("DefaultLocale")
	private static void addIdToNode(final String id, final OrgNode node) {
		node.setComments(new StringBuilder(TASKNODEID).append(id.toUpperCase())
				.append("\n").toString());
	}

	/**
	 * Set the meta section to be only the id. This does not overwrite rest of
	 * comments since they are stored in the task and sent to the body.
	 */
	@SuppressLint("DefaultLocale")
	public static void toNodeFromRemote(final OrgNode node,
			final RemoteTask dbEntry) {
		node.setComments(TASKNODEID + dbEntry.remoteId.toUpperCase() + "\n");
	}

	/**
	 * Returns the id from the meta-section, if present. Null otherwise.
	 */
	@SuppressLint("DefaultLocale")
	public static String getNodeId(final OrgNode node) {
		final Matcher m = PatternId.matcher(node.getComments());
		if (m.find()) {
			return m.group(1).toUpperCase();
		} else {
			return null;
		}
	}

	/**
	 * Fills in the information in the file from the list
	 */
	public static void toFileFromList(final TaskList list, final OrgFile file) {
		setSortingOnFile(list, file);
		setListTypeOnFile(list, file);
	}

    public static String getTitleAsFilename(TaskList list) {
        return list.title + ".org";
    }

	public static void setListTypeOnFile(TaskList list, OrgFile file) {
		final StringBuilder comments = new StringBuilder();
		if (list.listtype != null) {
			comments.append(LISTSTYLECOMMENT).append(list.listtype)
					.append("\n");
		}
		comments.append(PatternStyle.matcher(file.getComments()).replaceAll
                ("").trim());
		file.setComments(comments.toString());
	}

	public static void setSortingOnFile(final TaskList list, final OrgFile file) {
		final StringBuilder comments = new StringBuilder();
		if (list.sorting != null) {
			comments.append(LISTSORTCOMMENT).append(list.sorting).append("\n");
		}
		comments.append(PatternSorting.matcher(file.getComments()).replaceAll
                ("").trim());
		file.setComments(comments.toString());
	}
}
