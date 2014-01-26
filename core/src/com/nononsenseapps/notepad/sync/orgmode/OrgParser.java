package com.nononsenseapps.notepad.sync.orgmode;

import java.util.Calendar;
import java.util.List;

import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import android.text.format.Time;

public class OrgParser {
	final static String LIST_HEADING = "* ";
	final static String LIST_TYPE = "# type:";
	final static String LIST_SORTING = "# sorting:";

	final static String TASK_HEADING = "** ";
	final static String TASK_NONONSENSEID = "# nononsenseid:";
	final static String TASK_LOCKED = "# locked";
	final static String TODO = "TODO ";
	final static String DONE = "DONE ";
	final static String DEADLINE = "DEADLINE: ";
	final static String REP1D = " ++1d";
	final static String REP7D = " ++7d";

	// 2012-12-31
	final static String dateFormat = "%Y-%m-%d %a";
	final static String dateTimeFormat = "%Y-%m-%d %a %H:%M";
	private final Time time;

	public OrgParser() {
		time = new Time(Time.getCurrentTimezone());
	}

	/**
	 * Return a string representation of a list
	 */
	public String toOrgMode(final TaskList taskList) {
		if (taskList == null) throw new NullPointerException("tasklist was null");
		// Header is one star
		final StringBuilder sb = new StringBuilder(LIST_HEADING);
		// Title
		sb.append(taskList.title);
		// Type, if set, as comment
		if (taskList.listtype != null) {
			sb.append("\n").append(LIST_TYPE + taskList.listtype);
		}
		// Sorting, if set, as comment
		if (taskList.sorting != null) {
			sb.append("\n").append(LIST_SORTING + taskList.sorting);
		}

		return sb.toString();
	}

	/**
	 * Return a string representation of a task/note.
	 */
	public String toOrgMode(final OrgEntry orgEntry, final boolean asTodo,
			final List<Notification> notifications) {
		Log.d(OrgSyncer.TAG, "Building task as orgentry string");
		// Header is two stars
		final StringBuilder sb = new StringBuilder(TASK_HEADING);
		if (orgEntry == null) throw new NullPointerException("orgentry was null");
		final Task task = orgEntry.getTask();
		if (task == null) throw new NullPointerException("task was null");
		// DONE/TO-DO status
		if (task.completed == null && asTodo) {
			sb.append(TODO);
		} else if (task.completed != null) {
			sb.append(DONE);
		}
		// Title text
		sb.append(task.title);

		// Due date
		if (task.due != null) {
			time.set(task.due);
			sb.append("\n").append(DEADLINE).append("<")
					.append(time.format(dateFormat)).append(">");
		}

		// Notifications
		for (final Notification not : notifications) {
			if (not.radius == null && not.time != null) {
				sb.append("\n").append(toOrgMode(not));
			}
		}

		// Special id to keep track of changes
		sb.append("\n").append(TASK_NONONSENSEID).append(orgEntry.remoteId);

		// Locked comment
		if (task.locked) {
			sb.append("\n").append(TASK_LOCKED);
		}

		// Note
		if (task.note.length() > 0) {
			sb.append("\n\n").append(task.note);
		}

		return sb.toString();
	}

	/**
	 * Return a string representation of a reminder. For example <2014-01-21 Mon
	 * 23:59> or <2014-01-21 Mon 23:59 ++1d>
	 * 
	 * If the reminder repeats on more than one day, will return more than one
	 * entry, each on its own line.
	 */
	public String toOrgMode(final Notification not) {
		if (not == null) throw new NullPointerException("notification was null");
		if (not.radius != null || not.time == null) {
			return "";
		}

		final StringBuilder sb = new StringBuilder();
		if (not.repeats == 0) {
			// No repeat, just a single time stamp
			time.set(not.time);
			sb.append("<").append(time.format(dateTimeFormat)).append(">");
		} else if (not.repeats == (Notification.mon + Notification.tue
				+ Notification.wed + Notification.thu + Notification.fri
				+ Notification.sat + Notification.sun)) {
			// Repeats every day, single time stamp
			time.set(not.time);
			sb.append("<").append(time.format(dateTimeFormat)).append(REP1D)
					.append(">");
		} else {
			// Check each day individually
			// Each repeat gets its own stamp
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(not.time);
			int weekDay = cal.get(Calendar.DAY_OF_WEEK);

			// Check every day of the week
			for (int i = 0; i < 7; i++) {
				if (not.repeatsOn(weekDay)) {
					if (i > 0) {
						sb.append("\n");
					}
					time.set(cal.getTimeInMillis());
					sb.append("<").append(time.format(dateTimeFormat))
							.append(REP7D).append(">");
				}
				weekDay += 1;
				cal.add(Calendar.DAY_OF_WEEK, 1);
			}
		}

		return sb.toString();
	}

}
