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

package com.nononsenseapps.notepad.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.view.View;

import androidx.annotation.Nullable;

import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.ui.WeekDaysView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * A model for the SQLite table where reminders are saved
 */
public class Notification extends DAO {

	// TODO see if you can rename this to "Reminder" without ruining the database logic

	// These match WeekDaysView's values
	public static final int mon = 0x1;
	public static final int tue = 0x10;
	public static final int wed = 0x100;
	public static final int thu = 0x1000;
	public static final int fri = 0x10000;
	public static final int sat = 0x100000;
	public static final int sun = 0x1000000;
	// TODO maybe with more flags we can add special types of repeat,
	//  but we have to use long instead of int:
	//  public static final long nextMonth = 0x100000000;

	// TODO see if you can move these flags to an enum

	// Location repeat, one left of sun
	public static final int locationRepeat = 0x10000000;

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "notification";
	public static final String WITH_TASK_VIEW_NAME = "notification_with_tasks";
	public static final String WITH_TASK_PATH = TABLE_NAME + "/with_task_info";

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps."
			+ TABLE_NAME;

	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY), TABLE_NAME);
	public static final Uri URI_WITH_TASK_PATH = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY), WITH_TASK_PATH);

	public static final int BASEURICODE = 301;
	public static final int BASEITEMCODE = 302;
	public static final int WITHTASKQUERYCODE = 303;
	public static final int WITHTASKQUERYITEMCODE = 304;

	public static void addMatcherUris(UriMatcher sURIMatcher) {
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#", BASEITEMCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, WITH_TASK_PATH, WITHTASKQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, WITH_TASK_PATH + "/#",
				WITHTASKQUERYITEMCODE);
	}

	public static Uri getUri(final long id) {
		return Uri.withAppendedPath(URI, Long.toString(id));
	}

	public static class Columns implements BaseColumns {

		private Columns() {}

		public static final String TIME = "time";
		public static final String PERMANENT = "permanent";
		public static final String TASKID = "taskid";
		public static final String REPEATS = "repeats";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String RADIUS = "radius";
		public static final String LOCATIONNAME = "locationname";

		public static final String[] FIELDS = { _ID, TIME, PERMANENT, TASKID, REPEATS,
				LOCATIONNAME, LATITUDE, LONGITUDE, RADIUS };
	}

	public static class ColumnsWithTask extends Columns {

		private ColumnsWithTask() {}

		// public static final String notificationPrefix = "n.";
		public static final String taskPrefix = "t_";
		public static final String listPrefix = "l_";

		public static final String[] FIELDS = joinArrays(
				// prefixArray(notificationPrefix, Columns.FIELDS),
				Columns.FIELDS,
				prefixArray(taskPrefix, Task.Columns.SHALLOWFIELDS),
				prefixArray(listPrefix, TaskList.Columns.SHALLOWFIELDS));
	}

	/**
	 * Main table to store notification data
	 */
	public static final String CREATE_TABLE = "CREATE TABLE " +
			TABLE_NAME +
			"(" +
			Columns._ID +
			" INTEGER PRIMARY KEY," +
			Columns.TIME +
			" INTEGER," +
			Columns.PERMANENT +
			" INTEGER NOT NULL DEFAULT 0," +
			Columns.TASKID +
			" INTEGER," +
			// Interpreted binary
			Columns.REPEATS +
			" INTEGER NOT NULL DEFAULT 0," +
			// Location data
			Columns.LOCATIONNAME + " TEXT," +
			Columns.LATITUDE + " REAL, " +
			Columns.LONGITUDE +
			" REAL, " +
			Columns.RADIUS +
			" REAL, " +
			// Foreign key for task
			"FOREIGN KEY(" + Columns.TASKID +
			") REFERENCES " + Task.TABLE_NAME + "(" +
			Task.Columns._ID + ") ON DELETE CASCADE" +
			")";

	/**
	 * View that joins relevant data from tasks and lists tables
	 */
	public static final String CREATE_JOINED_VIEW = "CREATE TEMP VIEW IF NOT EXISTS " +
			WITH_TASK_VIEW_NAME + " AS " + " SELECT " +
			// Notifications as normal column names
			arrayToCommaString(TABLE_NAME + ".", Columns.FIELDS) + "," +
			// Rest gets prefixed
			arrayToCommaString("t.", Task.Columns.SHALLOWFIELDS, " AS "
					+ ColumnsWithTask.taskPrefix + "%1$s") + "," +
			arrayToCommaString("l.", TaskList.Columns.SHALLOWFIELDS, " AS "
					+ ColumnsWithTask.listPrefix + "%1$s") +
			" FROM " + TABLE_NAME + "," + Task.TABLE_NAME + " AS t," + TaskList.TABLE_NAME
			+ " AS l " + " WHERE " + TABLE_NAME + "." + Columns.TASKID + " = t."
			+ Task.Columns._ID + " AND t." + Task.Columns.DBLIST + " = l." +
			TaskList.Columns._ID + ";";

	/**
	 * A {@link Task} can have reminders, which are {@link Notification} objects.
	 * They are used to show (android) notifications at a user-provided day and time.
	 * Here it is expressed in milliseconds since 1970-01-01 UTC.
	 */
	public Long time = null;

	/**
	 * It's initialized, but never used. I think I'll use it for sticky reminder notifications!
	 */
	public boolean permanent = false;

	/**
	 * The {@link Task#_id} of the {@link Task} that this reminder is for
	 */
	public Long taskID = null;

	/**
	 * flags to indicate on which week days the note repeats. See {@link #sat} for example
	 */
	public long repeats = 0;
	// TODO make "repeats" private, and use .isRepeating() instead

	public String locationName = null;
	public Double latitude = null;
	public Double longitude = null;
	public Double radius = null;

	// Read only, fetched from VIEW
	public String listTitle = null;
	public Long listID = null;
	public String taskTitle = null;
	public String taskNote = null;

	// Convenience for the editor
	public View view = null;

	/**
	 * Must be associated with a task
	 */
	public Notification(final long taskID) {
		this.taskID = taskID;
	}

	public Notification(final Cursor c) {
		_id = c.getLong(0);
		time = c.isNull(1) ? null : c.getLong(1);
		permanent = 1 == c.getLong(2);
		taskID = c.isNull(3) ? null : c.getLong(3);
		repeats = c.getLong(4);
		locationName = c.isNull(5) ? null : c.getString(5);
		latitude = c.isNull(6) ? null : c.getDouble(6);
		longitude = c.isNull(7) ? null : c.getDouble(7);
		radius = c.isNull(8) ? null : c.getDouble(8);
		// if cursor has more fields, then assume it was constructed with
		// the WITH_TASKS view query
		if (c.getColumnCount() > 9) {
			int idx_list = c.getColumnIndex(ColumnsWithTask.listPrefix + TaskList.Columns.TITLE);
			int idx_id = c.getColumnIndex(ColumnsWithTask.listPrefix + TaskList.Columns._ID);
			int idx_title = c.getColumnIndex(ColumnsWithTask.taskPrefix + Task.Columns.TITLE);
			int idx_note = c.getColumnIndex(ColumnsWithTask.taskPrefix + Task.Columns.NOTE);
			listTitle = c.getString(idx_list);
			listID = c.getLong(idx_id);
			taskTitle = c.getString(idx_title);
			taskNote = c.getString(idx_note);
		}
	}

	public Notification(final Uri uri, final ContentValues values) {
		this(Long.parseLong(uri.getLastPathSegment()), values);
	}

	public Notification(final long id, final ContentValues values) {
		this(values);
		_id = id;
	}

	/**
	 * @param uri like content://com.nononsenseapps.NotePad/notification/1
	 * @return the {@link Notification} with the {@link #_id} in the given {@link Uri}, or NULL
	 * if it didn't find (exactly) one
	 */
	@Nullable
	public static Notification fromUri(final Uri uri, final Context context) {
		final Cursor c = context
				.getContentResolver()
				.query(uri, Columns.FIELDS, null, null, null);
		assert c != null;
		Notification n = null;
		if (c.getCount() == 1) {
			c.moveToFirst();
			n = new Notification(c);
		}
		c.close();
		return n;
	}

	public Notification(final JSONObject json) throws JSONException {
		if (json.has(Columns.TIME))
			time = json.getLong(Columns.TIME);
		if (json.has(Columns.PERMANENT))
			permanent = 1 == json.getLong(Columns.PERMANENT);
		if (json.has(Columns.TASKID))
			taskID = json.getLong(Columns.TASKID);
		if (json.has(Columns.REPEATS))
			repeats = json.getLong(Columns.REPEATS);
		if (json.has(Columns.LOCATIONNAME))
			locationName = json.getString(Columns.LOCATIONNAME);
		if (json.has(Columns.LATITUDE))
			latitude = json.getDouble(Columns.LATITUDE);
		if (json.has(Columns.LONGITUDE))
			longitude = json.getDouble(Columns.LONGITUDE);
		if (json.has(Columns.RADIUS))
			radius = json.getDouble(Columns.RADIUS);
	}

	public Notification(final ContentValues values) {
		time = values.getAsLong(Columns.TIME);
		permanent = 1 == values.getAsLong(Columns.PERMANENT);
		taskID = values.getAsLong(Columns.TASKID);
		repeats = values.getAsLong(Columns.REPEATS);
		locationName = values.getAsString(Columns.LOCATIONNAME);
		latitude = values.getAsDouble(Columns.LATITUDE);
		longitude = values.getAsDouble(Columns.LONGITUDE);
		radius = values.getAsDouble(Columns.RADIUS);
	}

	@Override
	public ContentValues getContent() {
		final ContentValues values = new ContentValues();

		values.put(Columns.TIME, time);
		values.put(Columns.TASKID, taskID);
		values.put(Columns.PERMANENT, permanent ? 1 : 0);
		values.put(Columns.REPEATS, repeats);
		values.put(Columns.LOCATIONNAME, locationName);
		values.put(Columns.LATITUDE, latitude);
		values.put(Columns.LONGITUDE, longitude);
		values.put(Columns.RADIUS, radius);

		return values;
	}

	@Override
	protected String getTableName() {
		return TABLE_NAME;
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	/**
	 * Returns date and time formatted in text in local time zone
	 */
	public CharSequence getLocalDateTimeText(final Context context) {
		return TimeFormatter.getLocalDateStringLong(context, time);
	}

	/**
	 * Returns time formatted in text in local time zone
	 */
	public CharSequence getLocalTimeText(final Context context) {
		return TimeFormatter.getLocalTimeOnlyString(context, time);
	}

	/**
	 * Returns date formatted in text in local time zone
	 */
	public CharSequence getLocalDateText(final Context context) {
		return TimeFormatter.getDateFormatter(context).format(new Date(time));
	}

	/**
	 * Calls {@link #insert} or performs an update, depending on the status of this
	 * {@link Notification} object
	 */
	@Override
	public int save(final Context context) {
		int result = 0;
		if (_id < 1) {
			result += insert(context);
		} else {
			result += context
					.getContentResolver()
					.update(getUri(), getContent(), null, null);
			if (result < 1) {
				// To allow editor to edit deleted notifications
				result += insert(context);
			}
		}
		return result;
	}

	/**
	 * Inserts this {@link Notification} as a new record into its SQLite table
	 * and sets its {@link #_id}
	 *
	 * @return 1 if it worked, 0 if it failed
	 */
	private int insert(final Context context) {
		int result = 0;
		final Uri uri = context.getContentResolver().insert(getBaseUri(), getContent());
		if (uri != null) {
			_id = Long.parseLong(uri.getLastPathSegment());
			result++;
		}
		return result;
	}

	/**
	 * If true, will also schedule/notify android notifications
	 */
	public void save(final Context context, final boolean schedule) {
		int result = save(context);
		if (schedule) {
			// First cancel any potentially old versions
			NotificationHelper.cancelNotification(context, this);
			// Then reschedule
			NotificationHelper.schedule(context);
		}
	}

	/**
	 * Delete the record of this {@link Notification} from the database and remove the
	 * associated {@link android.app.Notification} from the system's tray
	 *
	 * @return 1 if it was deleted, 0 otherwise
	 */
	@Override
	public int delete(final Context context) {
		// Make sure existing notifications are cancelled.
		NotificationHelper.cancelNotification(context, this);
		return super.delete(context);
	}

	public void saveInBackground(final Context context, final boolean schedule) {
		Executors.newSingleThreadExecutor().execute(() -> save(context, schedule));
	}

	/**
	 * Starts a background task that removes all notifications associated with
	 * the specified tasks.
	 */
	public static void removeWithTaskIds(final Context context, final Long... ids) {
		if (ids.length > 0) {
			// replacement for AsyncTask<,,>
			Executors.newSingleThreadExecutor().execute(() -> {
				// Background work here
				removeWithTaskIdsSynced(context, ids);
			});
		}
	}

	/**
	 * Removes all notifications associated with the specified tasks. Runs in
	 * the same thread as the caller.
	 */
	public static void removeWithTaskIdsSynced(final Context context, final Long... ids) {
		String idStrings = "(";
		ArrayList<String> idsToClear = new ArrayList<>();
		for (Long id : ids) {
			idStrings += id + ",";
			idsToClear.add(Long.toString(id));
		}
		idStrings = idStrings.substring(0, idStrings.length() - 1);
		idStrings += ")";

		final Cursor c = context
				.getContentResolver()
				.query(URI, Columns.FIELDS, Columns.TASKID + " IN " + idStrings,
						null, null);
		assert c != null;
		while (c.moveToNext()) {
			// Yes dont just call delete in database
			// We have to remove geofences (in delete)
			Notification n = new Notification(c);
			n.delete(context);
		}
		c.close();
	}

	/**
	 * Delete or reschedule a specific notification.
	 *
	 * @param uri like content://com.nononsenseapps.NotePad/notification/1
	 */
	public static void deleteOrReschedule(final Context context, final Uri uri) {
		final Cursor c = context
				.getContentResolver()
				.query(uri, Columns.FIELDS, null, null, null);
		assert c != null;
		while (c.moveToNext()) {
			Notification n = new Notification(c);
			n.deleteOrReschedule(context);
		}
		c.close();
	}

	/**
	 * Returns list of notifications coupled to specified task, sorted by time
	 */
	public static List<Notification> getNotificationsOfTask(final Context context, final long taskId) {
		return getNotificationsWithTasks(context, Columns.TASKID + " IS ?",
				new String[] { Long.toString(taskId) }, Columns.TIME);
	}

	/**
	 * @return a list of notifications occurring after/before specified time,
	 * and which do not have a location (radius == null). Sorted by time
	 * ascending
	 */
	public static List<Notification> getNotificationsWithTime(final Context context,
															  final long time,
															  final boolean before) {
		final String comparison = before ? " <= ?" : " > ?";
		return getNotificationsWithTasks(context,
				Columns.TIME + comparison + " AND " + Columns.RADIUS + " IS NULL",
				new String[] { Long.toString(time) }, Columns.TIME);
	}

	public static List<Notification> getNotificationsWithTasks(final Context context,
															   final String where,
															   final String[] whereArgs,
															   final String sortOrder) {
		ArrayList<Notification> list = new ArrayList<>();
		final Cursor c = context
				.getContentResolver()
				.query(URI_WITH_TASK_PATH, null, where, whereArgs, sortOrder);
		if (c != null) {
			while (c.moveToNext()) {
				list.add(new Notification(c));
			}
			c.close();
		}
		return list;
	}

	/**
	 * Used for snooze, only for non-repeating reminders
	 *
	 * @param newTime from {@link NotificationHelper#getSnoozedReminderNewTimeMillis}
	 */
	public static void setTime(final Context context, final Uri uri, final long newTime) {
		final ContentValues values = new ContentValues();
		values.put(Columns.TIME, newTime);
		// Use base ID to bypass type checks
		context.getContentResolver()
				.update(URI, values, Columns._ID + " IS ?",
						new String[] { uri.getLastPathSegment() });
	}

	/**
	 * Used for snooze
	 */
	public static void setTimeForListAndBefore(final Context context, final long listId,
											   final long maxTime, final long newTime) {
		// replacement for AsyncTask<,,>
		Executors.newSingleThreadExecutor().execute(() -> {
			// Background work here
			// First get the list of tasks in that list
			final Cursor c = context.getContentResolver()
					.query(Task.URI, Task.Columns.FIELDS, Task.Columns.DBLIST
									+ " IS ? AND "
									+ com.nononsenseapps.notepad.database.Notification.Columns.RADIUS
									+ " IS NULL", new String[] { Long.toString(listId) },
							null);
			assert c != null;
			String idStrings = "(";
			while (c.moveToNext()) {
				idStrings += c.getLong(0) + ",";
			}
			c.close();
			idStrings = idStrings.substring(0, idStrings.length() - 1);
			idStrings += ")";

			final ContentValues values = new ContentValues();
			values.put(Columns.TIME, newTime);

			context.getContentResolver().update(URI, values,
					Columns.TIME + " <= " + maxTime + " AND " + Columns.TASKID + " IN "
							+ idStrings, null);
		});
	}

	/**
	 * Returns true if the notification repeats on the given day. Day of the
	 * week as given by Calendar.getField(DayOfWeek)
	 */
	public boolean repeatsOn(final int calendarDay) {
		int day = switch (calendarDay) {
			case Calendar.MONDAY -> WeekDaysView.mon;
			case Calendar.TUESDAY -> WeekDaysView.tue;
			case Calendar.WEDNESDAY -> WeekDaysView.wed;
			case Calendar.THURSDAY -> WeekDaysView.thu;
			case Calendar.FRIDAY -> WeekDaysView.fri;
			case Calendar.SATURDAY -> WeekDaysView.sat;
			case Calendar.SUNDAY -> WeekDaysView.sun;
			default -> 0;
		};

		return (0 < (day & repeats));
	}

	/**
	 * If this {@link Notification} is a repeating reminder, set the {@link #time} to the next
	 * applicable day, A.K.A. reschedule it. If it is non-repeating, simply delete it.
	 */
	public void deleteOrReschedule(final Context context) {
		if (!this.isRepeating() || time == null) {
			// non-repeating reminder: just delete it
			delete(context);
			return;
		}
		// repeating reminder: re-schedule it

		// Need to set the correct time, but using today as the date
		// Because no sense in setting reminders in the past
		GregorianCalendar gcWasFired = new GregorianCalendar();
		gcWasFired.setTimeInMillis(time);
		// Use today's date
		GregorianCalendar gcToSchedule = new GregorianCalendar();
		final long now = gcToSchedule.getTimeInMillis();
		// With original time
		gcToSchedule.set(GregorianCalendar.HOUR_OF_DAY, gcWasFired.get(GregorianCalendar.HOUR_OF_DAY));
		gcToSchedule.set(GregorianCalendar.MINUTE, gcWasFired.get(GregorianCalendar.MINUTE));
		// this variable saves a moment in unix milliseconds:
		// * the day is today, when this function runs
		// * the hour & minute are those in which the Notification was scheduled to appear
		final long base = gcToSchedule.getTimeInMillis();

		// Check today if the time is actually in the future.
		// For example if this function runs at "now" = 19:30 to reschedule a reminder that was
		// planned for "base" = 19:15, then "now" is > "base", therefore start = 1
		final int start = now < base ? 0 : 1;
		boolean done = false;
		for (int i = start; i <= 7; i++) {
			if (i!=0) {
				// add a day to the hypotized new due date.
				// It automatically handles the transitions between ST and DST
				gcToSchedule.add(Calendar.DAY_OF_MONTH, 1);
			}

			// check if the reminder should repeat on this day
			if (repeatsOn(gcToSchedule.get(GregorianCalendar.DAY_OF_WEEK))) {
				// we found the 1° day in which the reminder needs to repeat: save that as the
				// new "due time" in the database, and we're done.
				done = true;
				time = gcToSchedule.getTimeInMillis();
				save(context);
				break;
			}

		}
		// Just in case of faulty repeat codes
		if (!done) {
			delete(context);
		}
	}

	public String getRepeatAsText(final Context context) {
		final StringBuilder sb = new StringBuilder();

		SimpleDateFormat weekDayFormatter = TimeFormatter.getLocalFormatterWeekdayShort(context);
		// 2013-05-13 was a monday
		GregorianCalendar gc = new GregorianCalendar(2013, GregorianCalendar.MAY, 13);
		final long base = gc.getTimeInMillis();
		final long day = 24 * 60 * 60 * 1000;
		for (int i = 0; i < 7; i++) {
			gc.setTimeInMillis(base + i * day);

			if (repeatsOn(gc.get(GregorianCalendar.DAY_OF_WEEK))) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(weekDayFormatter.format(gc.getTime()));
			}
		}

		return sb.toString();
	}

	/**
	 * Notifications (=Reminders) can be "repeating reminders": they are supposed to re-appear
	 * in one or more week days
	 *
	 * @return TRUE if this {@link Notification} is a repeating reminder
	 * @implNote See {@link #mon} and {@link #sun}
	 */
	public boolean isRepeating() {
		// "repeats == 0x1000001" means that the note repeats on monday and sunday, for example
		return this.repeats != 0;
	}

	/**
	 * a {@link Notification} belongs to a {@link Task} which belongs to a {@link TaskList}
	 * which can be of 2 types: "simple notes" or "checkable tasks"
	 *
	 * @return TRUE if this reminder is for a "checkable task", FALSE if it is for a "simple note",
	 * NULL if it could not be determined
	 */
	@Nullable
	public Boolean belongsToNoteInListOfTasks(final Context context) {
		Cursor cc = context
				.getContentResolver()
				.query(TaskList.URI, null, TaskList.Columns._ID + " IS ?",
						new String[] { Long.toString(this.listID) }, null);
		TaskList result = null;
		assert cc != null;
		if (cc.moveToFirst()) result = new TaskList(cc);
		cc.close();
		if (result == null) return null;

		return context.getString(R.string.const_listtype_tasks).equals(result.listtype);
	}
}