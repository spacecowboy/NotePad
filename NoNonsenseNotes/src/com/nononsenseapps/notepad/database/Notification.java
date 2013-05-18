package com.nononsenseapps.notepad.database;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.ui.WeekDaysView;
import com.nononsenseapps.util.GeofenceRemover;
import com.nononsenseapps.utils.views.GreyableToggleButton;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class Notification extends DAO {

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "notification";
	public static final String WITH_TASK_VIEW_NAME = "notification_with_tasks";
	public static final String WITH_TASK_PATH = TABLE_NAME + "/with_task_info";

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps."
			+ TABLE_NAME;

	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			TABLE_NAME);
	public static final Uri URI_WITH_TASK_PATH = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			WITH_TASK_PATH);

	public static final int BASEURICODE = 301;
	public static final int BASEITEMCODE = 302;
	public static final int WITHTASKQUERYCODE = 303;
	public static final int WITHTASKQUERYITEMCODE = 304;

	public static void addMatcherUris(UriMatcher sURIMatcher) {
		sURIMatcher
				.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#",
				BASEITEMCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, WITH_TASK_PATH,
				WITHTASKQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, WITH_TASK_PATH + "/#",
				WITHTASKQUERYITEMCODE);
	}

	public static Uri getUri(final long id) {
		return Uri.withAppendedPath(URI, Long.toString(id));
	}

	public static class Columns implements BaseColumns {

		private Columns() {
		}

		public static final String TIME = "time";
		public static final String PERMANENT = "permanent";
		public static final String TASKID = "taskid";
		public static final String REPEATS = "repeats";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String RADIUS = "radius";
		public static final String LOCATIONNAME = "locationname";

		public static final String[] FIELDS = { _ID, TIME, PERMANENT, TASKID,
				REPEATS, LOCATIONNAME, LATITUDE, LONGITUDE, RADIUS };
	}

	public static class ColumnsWithTask extends Columns {

		private ColumnsWithTask() {
		}

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
	public static final String CREATE_TABLE = new StringBuilder("CREATE TABLE ")
			.append(TABLE_NAME)
			.append("(")
			.append(Columns._ID)
			.append(" INTEGER PRIMARY KEY,")
			.append(Columns.TIME)
			.append(" INTEGER,")
			.append(Columns.PERMANENT)
			.append(" INTEGER NOT NULL DEFAULT 0,")
			.append(Columns.TASKID)
			.append(" INTEGER,")
			// Interpreted binary
			.append(Columns.REPEATS)
			.append(" INTEGER NOT NULL DEFAULT 0,")
			// Location data
			.append(Columns.LOCATIONNAME).append(" TEXT,")
			.append(Columns.LATITUDE).append(" REAL, ")
			.append(Columns.LONGITUDE)
			.append(" REAL, ")
			.append(Columns.RADIUS)
			.append(" REAL, ")
			// Foreign key for task
			.append("FOREIGN KEY(").append(Columns.TASKID)
			.append(") REFERENCES ").append(Task.TABLE_NAME).append("(")
			.append(Task.Columns._ID).append(") ON DELETE CASCADE").append(")")
			.toString();

	/**
	 * View that joins relevant data from tasks and lists tables
	 */
	public static final String CREATE_JOINED_VIEW = new StringBuilder()
			.append("CREATE VIEW ")
			.append(WITH_TASK_VIEW_NAME)
			.append(" AS ")
			.append(" SELECT ")
			// Notifications as normal column names
			.append(arrayToCommaString(TABLE_NAME + ".", Columns.FIELDS))
			.append(",")
			// Rest gets prefixed
			.append(arrayToCommaString("t.", Task.Columns.SHALLOWFIELDS, " AS "
					+ ColumnsWithTask.taskPrefix + "%1$s"))
			.append(",")
			.append(arrayToCommaString("l.", TaskList.Columns.SHALLOWFIELDS,
					" AS " + ColumnsWithTask.listPrefix + "%1$s"))
			.append(" FROM ").append(TABLE_NAME).append(",")
			.append(Task.TABLE_NAME).append(" AS t,")
			.append(TaskList.TABLE_NAME).append(" AS l ").append(" WHERE ")
			.append(TABLE_NAME).append(".").append(Columns.TASKID)
			.append(" = t.").append(Task.Columns._ID).append(" AND t.")
			.append(Task.Columns.DBLIST).append(" = l.")
			.append(TaskList.Columns._ID).append(";").toString();

	// milliseconds since 1970-01-01 UTC
	public Long time = null;
	public boolean permanent = false;

	public Long taskID = null;
	public long repeats = 0;

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
			listTitle = c.getString(c.getColumnIndex(ColumnsWithTask.listPrefix
					+ TaskList.Columns.TITLE));
			listID = c.getLong(c.getColumnIndex(ColumnsWithTask.listPrefix
					+ TaskList.Columns._ID));
			taskTitle = c.getString(c.getColumnIndex(ColumnsWithTask.taskPrefix
					+ Task.Columns.TITLE));
			taskNote = c.getString(c.getColumnIndex(ColumnsWithTask.taskPrefix
					+ Task.Columns.NOTE));
		}
	}

	public Notification(final Uri uri, final ContentValues values) {
		this(Long.parseLong(uri.getLastPathSegment()), values);
	}

	public Notification(final long id, final ContentValues values) {
		this(values);
		_id = id;
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
	 * 
	 */
	public CharSequence getLocalDateTimeText(final Context context) {
		// TODO respect global settings for 24 hour clock?
		return TimeFormatter.getLocalDateStringLong(context, time);
	}

	@Override
	public int save(final Context context) {
		int result = 0;
		if (_id < 1) {
			final Uri uri = context.getContentResolver().insert(getBaseUri(),
					getContent());
			if (uri != null) {
				_id = Long.parseLong(uri.getLastPathSegment());
				result++;
			}
		}
		else {
			result += context.getContentResolver().update(getUri(),
					getContent(), null, null);
		}
		return result;
	}

	/**
	 * If true, will also schedule/notify android notifications
	 */
	public int save(final Context context, final boolean schedule) {
		int result = save(context);
		if (schedule) {
			NotificationHelper.schedule(context);
		}
		return result;
	}

	@Override
	public int delete(final Context context) {
		// Make sure existing notifications are cancelled.
		NotificationHelper.cancelNotification(context, this);
		// Also remove any associated geofences
		GeofenceRemover.removeFences(context, Long.toString(_id));
		return super.delete(context);
	}

	public void saveInBackground(final Context context, final boolean schedule) {
		final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				save(context, schedule);
				return null;
			}
		};
		task.execute();
	}

	/**
	 * Starts a background task that removes all notifications associated with
	 * the specified tasks.
	 */
	public static void removeWithTaskIds(final Context context,
			final Long... ids) {
		if (ids.length > 0) {
			final AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {
				@Override
				protected Void doInBackground(final Long... ids) {
					String idStrings = "(";
					ArrayList<String> idsToClear = new ArrayList<String>();
					for (Long id : ids) {
						idStrings += id + ",";
						idsToClear.add(Long.toString(id));
					}
					idStrings = idStrings.substring(0, idStrings.length() - 1);
					idStrings += ")";
					
					final Cursor c = context.getContentResolver().query(
							URI,
							Columns.FIELDS,
							Columns.TASKID + " IN " + idStrings, null,
							null);

					while (c.moveToNext()) {
						// Yes dont just call delete in database
						// We have to remove geofences (in delete)
						Notification n = new Notification(c);
						n.delete(context);
					}
					c.close();
					
					return null;
				}
			};
			task.execute(ids);
		}
	}

	/**
	 * Delete or reschedule a specific notification.
	 */
	public static void deleteOrReschedule(final Context context, final Uri uri) {
		final Cursor c = context.getContentResolver().query(uri,
				Columns.FIELDS, null, null, null);

		while (c.moveToNext()) {
			Notification n = new Notification(c);
			n.deleteOrReschedule(context);
		}
		c.close();
	}

	/**
	 * Starts a background task that removes all notifications associated with
	 * the specified tasks up to the specified time.
	 */
	public static void removeWithMaxTimeAndTaskIds(final Context context,
			final long maxTime, final Long... ids) {
		if (ids.length > 0) {
			final AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {
				@Override
				protected Void doInBackground(final Long... ids) {
					String idStrings = "(";
					for (Long id : ids) {
						idStrings += id + ",";
					}
					idStrings = idStrings.substring(0, idStrings.length() - 1);
					idStrings += ")";

					final Cursor c = context.getContentResolver().query(
							URI,
							Columns.FIELDS,
							Columns.TASKID + " IN " + idStrings + " AND "
									+ Columns.TIME + " <= " + maxTime, null,
							null);

					ArrayList<String> idsToClear = new ArrayList<String>();
					while (c.moveToNext()) {
						Notification n = new Notification(c);
						idsToClear.add(Long.toString(n._id));
						n.deleteOrReschedule(context);
					}
					c.close();

					if (idsToClear.size() > 0) {
						// Remove geofences as well
						GeofenceRemover.removeFences(context, idsToClear);
					}

					// context.getContentResolver().delete(URI,
					// Columns.TASKID + " IN " + idStrings +
					// " AND " + Columns.TIME + " <= " + maxTime, null);
					return null;
				}
			};
			task.execute(ids);
		}
	}

	/**
	 * Starts a background task that removes all notifications associated with
	 * the specified list, occurring before the specified time
	 */
//	public static void removeWithListId(final Context context,
//			final long listId, final long maxTime) {
//		final AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {
//			@Override
//			protected Void doInBackground(final Long... ids) {
//				// First get the list of tasks in that list
//				final Cursor c = context
//						.getContentResolver()
//						.query(Task.URI,
//								Task.Columns.FIELDS,
//								Task.Columns.DBLIST
//										+ " IS ? AND "
//										+ com.nononsenseapps.notepad.database.Notification.Columns.RADIUS
//										+ " IS NULL",
//								new String[] { Long.toString(listId) }, null);
//
//				String idStrings = "(";
//				while (c.moveToNext()) {
//					idStrings += c.getLong(0) + ",";
//				}
//				c.close();
//				idStrings = idStrings.substring(0, idStrings.length() - 1);
//				idStrings += ")";
//
//				context.getContentResolver().delete(
//						URI,
//						Columns.TIME + " <= " + maxTime + " AND "
//								+ Columns.TASKID + " IN " + idStrings, null);
//				return null;
//			}
//		};
//		task.execute(listId);
//	}

	/**
	 * Returns list of notifications coupled to specified task, sorted by time
	 */
	public static List<Notification> getNotificationsOfTask(
			final Context context, final long taskId) {
		return getNotificationsWithTasks(
				context,
				new StringBuilder()
						.append(com.nononsenseapps.notepad.database.Notification.Columns.TASKID)
						+ " IS ?",
				new String[] { Long.toString(taskId) },
				new StringBuilder()
						.append(com.nononsenseapps.notepad.database.Notification.Columns.TIME)
						.toString());
	}

	/**
	 * Returns a list of notifications occurring after/before specified time,
	 * and which do not have a location (radius == null). Sorted by time
	 * ascending
	 */
	public static List<Notification> getNotificationsWithTime(
			final Context context, final long time, final boolean before) {
		final String comparison = before ? " <= ?" : " > ?";
		return getNotificationsWithTasks(
				context,
				new StringBuilder()
						.append(com.nononsenseapps.notepad.database.Notification.Columns.TIME)
						.append(comparison)
						.append(" AND ")
						.append(com.nononsenseapps.notepad.database.Notification.Columns.RADIUS)
						.append(" IS NULL").toString(),
				new String[] { Long.toString(time) },
				new StringBuilder()
						.append(com.nononsenseapps.notepad.database.Notification.Columns.TIME)
						.toString());
	}

	public static List<Notification> getNotificationsWithTasks(
			final Context context, final String where,
			final String[] whereArgs, final String sortOrder) {
		ArrayList<Notification> list = new ArrayList<Notification>();

		final Cursor c = context.getContentResolver().query(URI_WITH_TASK_PATH,
				null, where, whereArgs, sortOrder);

		while (c.moveToNext()) {
			list.add(new Notification(c));
		}

		c.close();
		return list;
	}

	/**
	 * Used for snooze
	 */
	public static int setTime(final Context context, final Uri uri,
			final long newTime) {
		final ContentValues values = new ContentValues();
		values.put(Columns.TIME, newTime);
		// Use base ID to bypass type checks
		return context.getContentResolver().update(URI, values,
				Columns._ID + " IS ?",
				new String[] { uri.getLastPathSegment() });
	}

	/**
	 * Used for snooze
	 */
	public static void setTimeForListAndBefore(final Context context,
			final long listId, final long maxTime, final long newTime) {
		final AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {
			@Override
			protected Void doInBackground(final Long... ids) {
				// First get the list of tasks in that list
				final Cursor c = context
						.getContentResolver()
						.query(Task.URI,
								Task.Columns.FIELDS,
								Task.Columns.DBLIST
										+ " IS ? AND "
										+ com.nononsenseapps.notepad.database.Notification.Columns.RADIUS
										+ " IS NULL",
								new String[] { Long.toString(listId) }, null);

				String idStrings = "(";
				while (c.moveToNext()) {
					idStrings += c.getLong(0) + ",";
				}
				c.close();
				idStrings = idStrings.substring(0, idStrings.length() - 1);
				idStrings += ")";

				final ContentValues values = new ContentValues();
				values.put(Columns.TIME, newTime);

				context.getContentResolver().update(
						URI,
						values,
						Columns.TIME + " <= " + maxTime + " AND "
								+ Columns.TASKID + " IN " + idStrings, null);
				return null;
			}
		};
		task.execute(listId);
	}

	public static void completeTasksInList(final Context context,
			final long listId, final long maxTime) {

	}

	/**
	 * Returns true if the notification repeats on the given day. Day of the
	 * week as given by Calendar.getField(DayOfWeek)
	 */
	public boolean repeatsOn(final int calendarDay) {
		int day;

		switch (calendarDay) {
		case Calendar.MONDAY:
			day = WeekDaysView.mon;
			break;
		case Calendar.TUESDAY:
			day = WeekDaysView.tue;
			break;
		case Calendar.WEDNESDAY:
			day = WeekDaysView.wed;
			break;
		case Calendar.THURSDAY:
			day = WeekDaysView.thu;
			break;
		case Calendar.FRIDAY:
			day = WeekDaysView.fri;
			break;
		case Calendar.SATURDAY:
			day = WeekDaysView.sat;
			break;
		case Calendar.SUNDAY:
			day = WeekDaysView.sun;
			break;
		default:
			day = 0;
		}

		return (0 < (day & repeats));
	}

	public void deleteOrReschedule(final Context context) {
		if (repeats == 0) {
			delete(context);
		}
		else {
			// Need to set the correct time, but using today as the date
			// Because no sense in setting reminders in the past
			GregorianCalendar gcOrgTime = new GregorianCalendar();
			gcOrgTime.setTimeInMillis(time);
			// Use today's date
			GregorianCalendar gc = new GregorianCalendar();
			final long now = gc.getTimeInMillis();
			// With original time
			gc.set(GregorianCalendar.HOUR_OF_DAY,
					gcOrgTime.get(GregorianCalendar.HOUR_OF_DAY));
			gc.set(GregorianCalendar.MINUTE,
					gcOrgTime.get(GregorianCalendar.MINUTE));
			// Save as base
			final long base = gc.getTimeInMillis();

			// Check today if the time is actually in the future
			final int start = now < base ? 0 : 1;
			final long oneDay = 24 * 60 * 60 * 1000;
			boolean done = false;
			for (int i = start; i <= 7; i++) {
				gc.setTimeInMillis(base + i * oneDay);

				if (repeatsOn(gc.get(GregorianCalendar.DAY_OF_WEEK))) {
					done = true;
					time = gc.getTimeInMillis();
					save(context);
					break;
				}

			}
			// Just in case of faulty repeat codes
			if (!done) {
				delete(context);
			}
		}
	}

	public String getRepeatAsText(final Context context) {
		final StringBuilder sb = new StringBuilder();

		SimpleDateFormat weekDayFormatter = TimeFormatter
				.getLocalFormatterWeekdayShort(context);
		// 2013-05-13 was a monday
		GregorianCalendar gc = new GregorianCalendar(2013,
				GregorianCalendar.MAY, 13);
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
}
