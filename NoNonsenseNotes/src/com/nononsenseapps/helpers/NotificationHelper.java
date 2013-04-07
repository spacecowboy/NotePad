package com.nononsenseapps.helpers;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class NotificationHelper extends BroadcastReceiver {

	public static final int NOTIFICATION_ID = 4364;

	static final String ARG_MAX_TIME = "maxtime";
	static final String ARG_LISTID = "listid";
	static final String ARG_TASKID = "taskid";
	private static final String ARG_COMPLETE = "complete";
	private static final String ARG_SNOOZE = "snooze";

	private static final String TAG = "nononsenseapps.NotificationHelper";

	private static ContextObserver observer = null;

	private static ContextObserver getObserver(final Context context) {
		if (observer == null) {
			observer = new ContextObserver(context, null);
		}
		return observer;
	}

	/**
	 * Fires notifications that have elapsed and sets an alarm to be woken at
	 * the next notification.
	 * 
	 * If the intent action is ACTION_DELETE, will delete the notification with
	 * the indicated ID, and cancel it from any active notifications.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_DELETE.equals(intent.getAction())) {
			if (intent.hasExtra(ARG_MAX_TIME)) {
				deleteNotification(context,
						intent.getLongExtra(ARG_LISTID, -1),
						intent.getLongExtra(ARG_MAX_TIME, 0));

				// TODO handle complete here
			}
			else {
				// Just a notification
				context.getContentResolver().delete(intent.getData(), null,
						null);
				cancelNotification(context, intent.getData());
				if (intent.getBooleanExtra(ARG_COMPLETE, false)) {
					// Also complete note
					Task.setCompleted(context, true,
							intent.getLongExtra(ARG_TASKID, -1));
				}
			}

			// User in editor, don't spam
			notifyPast(context, true);
		}
		else if (Intent.ACTION_EDIT.equals(intent.getAction())) {
			if (intent.getBooleanExtra(ARG_SNOOZE, false)) {
				// Cancel current notification
				cancelNotification(context, intent.getData());
				// msec/sec * sec/min * 15
				long delay15min = 1000 * 60 * 15;
				final Calendar now = Calendar.getInstance();
				com.nononsenseapps.notepad.database.Notification.setTime(
						context, intent.getData(),
						delay15min + now.getTimeInMillis());
			}
			notifyPast(context, true);
		}
		else {
			notifyPast(context, false);
		}

		scheduleNext(context);
	}

	private static void monitorUri(final Context context) {
		context.getContentResolver().unregisterContentObserver(
				getObserver(context));
		context.getContentResolver().registerContentObserver(
				com.nononsenseapps.notepad.database.Notification.URI, true,
				getObserver(context));
	}

	/**
	 * Displays notifications that have a time occurring in the past. If no
	 * notifications like that exist, will make sure to cancel any notifications
	 * showing.
	 */
	private static void notifyPast(Context context, boolean alertOnce) {
		// Get list of past notifications
		final Calendar now = Calendar.getInstance();

		final List<com.nononsenseapps.notepad.database.Notification> notifications = com.nononsenseapps.notepad.database.Notification
				.getNotificationsWithTime(context, now.getTimeInMillis(), true);

		// Remove duplicates
		makeUnique(context, notifications);

		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Log.d(TAG, "Number of notifications: " + notifications.size());

		// If empty, cancel
		if (notifications.isEmpty()) {
			// cancelAll permanent notifications here if/when that is
			// implemented. Don't touch others.
			notificationManager.cancelAll();
		}
		else {
			// else, notify
			// Fetch sound and vibrate settings
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);

			// Always use default lights
			int lightAndVibrate = Notification.DEFAULT_LIGHTS;
			// If vibrate on, use default vibration pattern also
			if (prefs.getBoolean(context.getString(R.string.key_pref_vibrate),
					false)) lightAndVibrate |= Notification.DEFAULT_VIBRATE;

			final NotificationCompat.Builder builder = new NotificationCompat.Builder(
					context)
					.setWhen(0)
					.setSmallIcon(R.drawable.ic_stat_notification_edit)
					.setLargeIcon(
							BitmapFactory.decodeResource(
									context.getResources(), R.drawable.app_icon))
					.setPriority(
							Integer.parseInt(prefs.getString(
									context.getString(R.string.key_pref_prio),
									"0")))
					.setDefaults(lightAndVibrate)
					.setAutoCancel(true)
					.setOnlyAlertOnce(alertOnce)
					.setSound(
							Uri.parse(prefs.getString(context
									.getString(R.string.key_pref_ringtone),
									"DEFAULT_NOTIFICATION_URI")));

			if (prefs.getBoolean(
					context.getString(R.string.key_pref_group_on_lists), true)) {
				// Group together notes contained in the same list.
				// Always use listid
				for (long listId : getRelatedLists(notifications)) {
					List<com.nononsenseapps.notepad.database.Notification> subList = getSubList(
							listId, notifications);
					if (subList.size() == 1) {
						// Notify as single
						notifyBigText(context, notificationManager, builder,
								listId, subList.get(0));
					}
					else {
						notifyInboxStyle(context, notificationManager, builder,
								listId, subList);
					}
				}
			}
			else {
				// Notify for each individually
				for (com.nononsenseapps.notepad.database.Notification note : notifications) {
					notifyBigText(context, notificationManager, builder,
							note.taskID, note);
				}
			}
		}
	}

	/**
	 * Remove from the database, and the specified list, duplicate
	 * notifications. The result is that each note is only associated with ONE
	 * EXPIRED notification.
	 * 
	 * @param context
	 * @param notifications
	 */
	private static void makeUnique(
			final Context context,
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		// get duplicates and iterate over them
		for (com.nononsenseapps.notepad.database.Notification noti : getLatestOccurence(notifications)) {
			// remove all but the first one from database, and big list
			for (com.nononsenseapps.notepad.database.Notification dupNoti : getDuplicates(
					noti, notifications)) {
				notifications.remove(dupNoti);
				cancelNotification(context, dupNoti);
			}
		}

	}

	/**
	 * Returns the first occurrence of each note's notification. Effectively the
	 * returned list has unique elements with regard to the note id.
	 * 
	 * @param notifications
	 * @return
	 */
	private static List<com.nononsenseapps.notepad.database.Notification> getLatestOccurence(
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		final ArrayList<Long> seenIds = new ArrayList<Long>();
		final ArrayList<com.nononsenseapps.notepad.database.Notification> firsts = new ArrayList<com.nononsenseapps.notepad.database.Notification>();

		com.nononsenseapps.notepad.database.Notification noti;
		for (int i = notifications.size() - 1; i >= 0; i--) {
			noti = notifications.get(i);
			if (!seenIds.contains(noti.taskID)) {
				seenIds.add(noti.taskID);
				firsts.add(noti);
			}
		}
		return firsts;
	}

	private static List<com.nononsenseapps.notepad.database.Notification> getDuplicates(
			final com.nononsenseapps.notepad.database.Notification first,
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		final ArrayList<com.nononsenseapps.notepad.database.Notification> dups = new ArrayList<com.nononsenseapps.notepad.database.Notification>();

		for (com.nononsenseapps.notepad.database.Notification noti : notifications) {
			if (noti.taskID == first.taskID && noti._id != first._id) {
				dups.add(noti);
			}
		}
		return dups;
	}

	/**
	 * Needs the builder that contains non-note specific values. idToUse is
	 * usually the NoteNotification.id, but it could also be listid for example.
	 * 
	 */
	private static void notifyBigText(final Context context,
			final NotificationManager notificationManager,
			final NotificationCompat.Builder builder, final Long idToUse,
			final com.nononsenseapps.notepad.database.Notification note) {
		// Delete it on clear
		PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(Intent.ACTION_DELETE, note.getUri()),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Open note on click
		PendingIntent clickIntent = PendingIntent.getActivity(context, 0,
				new Intent(Intent.ACTION_VIEW, Task.getUri(note.taskID)),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Action to complete
		PendingIntent completeIntent = PendingIntent.getBroadcast(
				context,
				0,
				new Intent(Intent.ACTION_DELETE, note.getUri()).putExtra(
						ARG_COMPLETE, true).putExtra(ARG_TASKID, note.taskID),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Action to snooze
		PendingIntent snoozeIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(Intent.ACTION_EDIT, note.getUri()).putExtra(
						ARG_SNOOZE, true), PendingIntent.FLAG_UPDATE_CURRENT);

		// Build notification
		final Notification noti = builder
				.setContentTitle(note.taskTitle)
				.setContentText(note.taskNote)
				.setContentIntent(clickIntent)
				.setDeleteIntent(deleteIntent)
				.setStyle(
						new NotificationCompat.BigTextStyle()
								.bigText(note.taskNote))
				.addAction(R.drawable.navigation_accept_dark,
						context.getText(R.string.completed), completeIntent)
				.addAction(R.drawable.device_access_alarms_dark,
						context.getText(R.string.snooze), snoozeIntent).build();

		notificationManager.notify(idToUse.intValue(), noti);
	}

	private static void notifyInboxStyle(
			final Context context,
			final NotificationManager notificationManager,
			final NotificationCompat.Builder builder,
			final Long idToUse,
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		// Delete intent must delete all notifications
		Intent delint = new Intent(Intent.ACTION_DELETE,
				com.nononsenseapps.notepad.database.Notification.URI);
		// Add extra so we don't delete all
		delint.putExtra(ARG_MAX_TIME, getLatestTime(notifications));
		delint.putExtra(ARG_LISTID, idToUse);
		PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0,
				delint, PendingIntent.FLAG_UPDATE_CURRENT);

		// Open intent should open the list
		PendingIntent clickIntent = PendingIntent.getActivity(context, 0,
				new Intent(Intent.ACTION_VIEW, TaskList.getUri(idToUse),
						context, ActivityMain_.class),
				PendingIntent.FLAG_UPDATE_CURRENT);

		final String title = notifications.get(0).listTitle + " ("
				+ notifications.size() + ")";
		// Build notification
		builder.setContentTitle(title)
				.setContentText(notifications.get(0).taskTitle)
				.setContentIntent(clickIntent).setDeleteIntent(deleteIntent);

		// Action to snooze
		// TODO

		// TODO Action to complete

		NotificationCompat.InboxStyle ib = new NotificationCompat.InboxStyle()
				.setBigContentTitle(title);
		if (notifications.size() > 6)
			ib.setSummaryText("+" + (notifications.size() - 6) + " "
					+ context.getString(R.string.more));

		for (com.nononsenseapps.notepad.database.Notification e : notifications) {
			ib.addLine(e.taskTitle);
		}

		final Notification noti = builder.setStyle(ib).build();
		notificationManager.notify(idToUse.intValue(), noti);
	}

	private static long getLatestTime(
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		long latest = 0;
		for (com.nononsenseapps.notepad.database.Notification noti : notifications) {
			if (noti.time > latest) latest = noti.time;
		}
		return latest;
	}

	/**
	 * Schedules to be woken up at the next notification time.
	 */
	private static void scheduleNext(Context context) {
		// Get first future notification
		final Calendar now = Calendar.getInstance();
		final List<com.nononsenseapps.notepad.database.Notification> notifications = com.nononsenseapps.notepad.database.Notification
				.getNotificationsWithTime(context, now.getTimeInMillis(), false);

		// if not empty, schedule alarm wake up
		if (!notifications.isEmpty()) {
			// at first's time
			// Create a new PendingIntent and add it to the AlarmManager
			Intent intent = new Intent(Intent.ACTION_RUN);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
			AlarmManager am = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			am.cancel(pendingIntent);
			am.set(AlarmManager.RTC_WAKEUP, notifications.get(0).time,
					pendingIntent);
		}

		monitorUri(context);
	}

	/**
	 * Schedules coming notifications, and displays expired ones. Only
	 * notififies once for existing notifications.
	 */
	public static void schedule(final Context context) {
		notifyPast(context, true);
		scheduleNext(context);
	}

	/**
	 * Updates/Inserts notifications in the database. Immediately notifies and
	 * schedules next wake up on finish.
	 * 
	 * @param context
	 * @param notifications
	 */
	public static void updateNotification(final Context context,
			final com.nononsenseapps.notepad.database.Notification notification) {
		/*
		 * Only don't insert if update is success This way the editor can update
		 * a deleted notification and still have it persisted in the database
		 */
		boolean shouldInsert = true;
		// If id is -1, then this should be inserted
		if (notification._id > 0) {
			// Else it should be updated
			int result = notification.save(context);
			if (result > 0) shouldInsert = false;
		}

		if (shouldInsert) {
			notification._id = -1;
			notification.save(context);
		}

		notifyPast(context, true);
		scheduleNext(context);
	}

	/**
	 * Deletes the indicated notification from the notification tray (does not
	 * touch database)
	 * 
	 * Called by notification.delete()
	 * 
	 * @param context
	 * @param id
	 */
	public static void cancelNotification(final Context context,
			final com.nononsenseapps.notepad.database.Notification not) {
		if (not != null) {
			cancelNotification(context, not.getUri());
		}
	}

	static void cancelNotification(final Context context, final Uri uri) {
		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(Integer.parseInt(uri.getLastPathSegment()));
	}

	public static void deleteNotification(final Context context, long listId,
			long maxTime) {
		com.nononsenseapps.notepad.database.Notification.removeWithListId(
				context, listId, maxTime);

		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel((int) listId);
	}

	/**
	 * Given a list of notifications, returns a list of the lists the notes
	 * belong to.
	 */
	private static Collection<Long> getRelatedLists(
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		final HashSet<Long> lists = new HashSet<Long>();
		for (com.nononsenseapps.notepad.database.Notification not : notifications) {
			lists.add(not.listID);
		}

		return lists;
	}

	/**
	 * Returns a list of those notifications that are associated to notes in the
	 * specified list.
	 */
	private static List<com.nononsenseapps.notepad.database.Notification> getSubList(
			final long listId,
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		final ArrayList<com.nononsenseapps.notepad.database.Notification> subList = new ArrayList<com.nononsenseapps.notepad.database.Notification>();
		for (com.nononsenseapps.notepad.database.Notification not : notifications) {
			if (not.listID == listId) {
				subList.add(not);
			}
		}

		return subList;
	}

	private static class ContextObserver extends ContentObserver {
		private final Context context;

		public ContextObserver(final Context context, Handler h) {
			super(h);
			this.context = context.getApplicationContext();
		}

		// Implement the onChange(boolean) method to delegate the
		// change notification to the onChange(boolean, Uri) method
		// to ensure correct operation on older versions
		// of the framework that did not have the onChange(boolean,
		// Uri) method.
		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}

		// Implement the onChange(boolean, Uri) method to take
		// advantage of the new Uri argument.
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			// Handle change but don't spam
			notifyPast(context, true);
		}
	}
}
