package com.nononsenseapps.notepad.database;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.nononsenseapps.helpers.NotificationHelper;
import com.nononsenseapps.notepad.R;

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

		public static final String[] FIELDS = { _ID, TIME, PERMANENT, TASKID };
	}

	public static class ColumnsWithTask extends Columns {

		private ColumnsWithTask() {
		}

		//public static final String notificationPrefix = "n.";
		public static final String taskPrefix = "t_";
		public static final String listPrefix = "l_";

		public static final String[] FIELDS = joinArrays(
				//prefixArray(notificationPrefix, Columns.FIELDS),
				Columns.FIELDS,
				prefixArray(taskPrefix, Task.Columns.SHALLOWFIELDS),
				prefixArray(listPrefix, TaskList.Columns.SHALLOWFIELDS));
	}

	/**
	 * Main table to store notification data
	 */
	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ "(" + Columns._ID + " INTEGER PRIMARY KEY," + Columns.TIME
			+ " INTEGER," + Columns.PERMANENT + " INTEGER NOT NULL DEFAULT 0,"
			+ Columns.TASKID + " INTEGER,"

			// Foreign key for task
			+ "FOREIGN KEY(" + Columns.TASKID + ") REFERENCES "
			+ Task.TABLE_NAME + "(" + Task.Columns._ID + ") ON DELETE CASCADE"

			+ ")";

	/**
	 * View that joins relevant data from tasks and lists tables
	 */
	public static final String CREATE_JOINED_VIEW = new StringBuilder()
			.append("CREATE VIEW ").append(WITH_TASK_VIEW_NAME).append(" AS ")
			.append(" SELECT ")
			// Notifications as normal column names
			.append(arrayToCommaString(TABLE_NAME +".", Columns.FIELDS))
			.append(",")
			// Rest gets prefixed
			.append(arrayToCommaString("t.", Task.Columns.SHALLOWFIELDS, " AS " + ColumnsWithTask.taskPrefix + "%1$s"))
			.append(",")
			.append(arrayToCommaString("l.", TaskList.Columns.SHALLOWFIELDS, " AS " + ColumnsWithTask.listPrefix + "%1$s"))
			.append(" FROM ").append(TABLE_NAME).append(",")
			.append(Task.TABLE_NAME).append(" AS t,")
			.append(TaskList.TABLE_NAME).append(" AS l ").append(" WHERE ").append(TABLE_NAME).append(".")
			.append(Columns.TASKID).append(" = t.").append(Task.Columns._ID)
			.append(" AND t.").append(Task.Columns.DBLIST).append(" = l.")
			.append(TaskList.Columns._ID).append(";").toString();

	// milliseconds since 1970-01-01 UTC
	public Long time = null;
	public boolean permanent = false;
	public Long taskID = null;

	// Read only, fetched from VIEW
	public String listTitle = null;
	public Long listID = null;
	public String taskTitle = null;
	public String taskNote = null;

	/**
	 * Must be associated with a task
	 */
	public Notification(final long taskID) {
		this.taskID = taskID;
	}

	public Notification(final Cursor c) {
		_id = c.getLong(0);
		time = c.getLong(1);
		permanent = 1 == c.getLong(2);
		taskID = c.getLong(3);
		// if cursor has more fields, then assume it was constructed with
		// the WITH_TASKS view query
		if (c.getColumnCount() > 4) {
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
	}

	@Override
	public ContentValues getContent() {
		final ContentValues values = new ContentValues();

		values.put(Columns.TIME, time);
		values.put(Columns.TASKID, taskID);
		values.put(Columns.PERMANENT, permanent ? 1 : 0);

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
	 * @return
	 */
	public CharSequence getLocalDateTimeText(final Context context) {
		// TODO respect global settings for 24 hour clock?
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return DateFormat.format(prefs.getString(
				context.getString(R.string.key_pref_dateformat_long),
				context.getString(R.string.dateformat_long_1)), cal);
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
		return super.delete(context);
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
					for (Long id : ids) {
						idStrings += id + ",";
					}
					idStrings = idStrings.substring(0, idStrings.length() - 1);
					idStrings += ")";
					Log.d("JONAS", "where: " + Columns.TASKID + " IN "
							+ idStrings);
					context.getContentResolver().delete(URI,
							Columns.TASKID + " IN " + idStrings, null);
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
	public static void removeWithListId(final Context context,
			final long listId, final long maxTime) {
		final AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {
			@Override
			protected Void doInBackground(final Long... ids) {
				// First get the list of tasks in that list
				final Cursor c = context.getContentResolver().query(Task.URI,
						Task.Columns.FIELDS, Task.Columns.DBLIST + " IS ?",
						new String[] { Long.toString(listId) }, null);

				String idStrings = "(";
				while (c.moveToNext()) {
					idStrings += c.getLong(0) + ",";
				}
				c.close();
				idStrings = idStrings.substring(0, idStrings.length() - 1);
				idStrings += ")";
				Log.d("JONAS", "where: " + Columns.TASKID + " IN " + idStrings);
				context.getContentResolver().delete(
						URI,
						Columns.TIME + " <= " + maxTime + " AND "
								+ Columns.TASKID + " IN " + idStrings, null);
				return null;
			}
		};
		task.execute(listId);
	}

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
						new String[] { Long.toString(taskId) }, new StringBuilder()
						.append(com.nononsenseapps.notepad.database.Notification.Columns.TIME)
						.toString());
	}

	/**
	 * Returns a list of notifications occurring after/before specified time.
	 * Sorted by time ascending
	 */
	public static List<Notification> getNotificationsWithTime(
			final Context context, final long time, final boolean before) {
		final String comparison = before ? " <= ?" : " > ?";
		return getNotificationsWithTasks(
				context,
				new StringBuilder()
						.append(com.nononsenseapps.notepad.database.Notification.Columns.TIME)
						.append(comparison).toString(),
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
}
