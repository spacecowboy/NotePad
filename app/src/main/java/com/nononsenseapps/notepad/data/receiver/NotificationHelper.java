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

package com.nononsenseapps.notepad.data.receiver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Task;
//import com.nononsenseapps.notepad.util.Log; AK comment out original code

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class NotificationHelper extends BroadcastReceiver {

	public static final int NOTIFICATION_ID = 4364;
    public static final String NOTIFICATION_CANCEL_ARG = "notification_cancel_arg";
    public static final String NOTIFICATION_DELETE_ARG = "notification_delete_arg";
    // static final String ARG_MAX_TIME = "maxtime";
	// static final String ARG_LISTID = "listid";
	static final String ARG_TASKID = "taskid";
	private static final String ACTION_COMPLETE = "com.nononsenseapps.notepad.ACTION.COMPLETE";
	private static final String ACTION_SNOOZE = "com.nononsenseapps.notepad.ACTION.SNOOZE";
	private static final String ACTION_RESCHEDULE = "com.nononsenseapps.notepad.ACTION.RESCHEDULE";
    private final static String TAG = "RICKSMESSAGE";
	//private static final String TAG = "nononsenseapps.NotificationHelper"; AK comment out

	private static ContextObserver observer = null;

	private static ContextObserver getObserver(final Context context) {
		if (observer == null) {
			observer = new ContextObserver(context, null);
		}
		return observer;
	}

	private static void monitorUri(final Context context) {
		context.getContentResolver().unregisterContentObserver(getObserver(context));
		context.getContentResolver().registerContentObserver(com.nononsenseapps.notepad.data.model.sql.Notification.URI, true, getObserver(context));
	}

    public static void clearNotification(@NonNull final Context context, @NonNull final Intent
            intent) {
        if (intent.getLongExtra(NOTIFICATION_DELETE_ARG, -1) > 0) {
            com.nononsenseapps.notepad.data.model.sql.Notification.deleteOrReschedule(context, com.nononsenseapps.notepad.data.model.sql.Notification.getUri(intent.getLongExtra
                            (NOTIFICATION_DELETE_ARG, -1)));
        }
        if (intent.getLongExtra(NOTIFICATION_CANCEL_ARG, -1) > 0) {
            NotificationHelper.cancelNotification(context, (int) intent.getLongExtra
                    (NOTIFICATION_CANCEL_ARG, -1));
        }
    }

	/**
	 * Displays notifications that have a time occurring in the past (and no
	 * location). If no notifications like that exist, will make sure to cancel
	 * any notifications showing.
	 */
	private static void notifyPast(Context context, boolean alertOnce) {
		// Get list of past notifications
		final Calendar now = Calendar.getInstance();

		final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications = com.nononsenseapps.notepad.data.model.sql.Notification
				.getNotificationsWithTime(context, now.getTimeInMillis(), true);

		// Remove duplicates
		makeUnique(context, notifications);

		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		//Log.d(TAG, "Number of notifications: " + notifications.size());

		// If empty, cancel
		if (notifications.isEmpty()) {
			// cancelAll permanent notifications here if/when that is
			// implemented. Don't touch others.
			// Dont do this, it clears location
			// notificationManager.cancelAll();
		}
		else {
			// else, notify
			// Fetch sound and vibrate settings
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);

			// Always use default lights
			int lightAndVibrate = Notification.DEFAULT_LIGHTS;
			// If vibrate on, use default vibration pattern also
            if (prefs.getBoolean(context.getString(R.string.const_preference_vibrate_key), false)
                    ) lightAndVibrate |= Notification.DEFAULT_VIBRATE;

			// Need to get a new one because the action buttons will duplicate
			// otherwise
			NotificationCompat.Builder builder;

			// if (false)
			// //
			// prefs.getBoolean(context.getString(R.string.key_pref_group_on_lists),
			// // false))
			// {
			// // Group together notes contained in the same list.
			// // Always use listid
			// for (long listId : getRelatedLists(notifications)) {
			// builder = getNotificationBuilder(context,
			// Integer.parseInt(prefs.getString(
			// context.getString(R.string.key_pref_prio),
			// "0")), lightAndVibrate,
			// Uri.parse(prefs.getString(context
			// .getString(R.string.key_pref_ringtone),
			// "DEFAULT_NOTIFICATION_URI")), alertOnce);
			//
			// List<com.nononsenseapps.notepad.data.model.sql.Notification> subList =
			// getSubList(
			// listId, notifications);
			// if (subList.size() == 1) {
			// // Notify as single
			// notifyBigText(context, notificationManager, builder,
			// listId, subList.get(0));
			// }
			// else {
			// notifyInboxStyle(context, notificationManager, builder,
			// listId, subList);
			// }
			// }
			// }
			// else {
			// Notify for each individually
			for (com.nononsenseapps.notepad.data.model.sql.Notification note : notifications) {
				builder = getNotificationBuilder(
						context,
						lightAndVibrate, Uri.parse(prefs.getString(context.getString(R.string.const_preference_ringtone_key),
								"DEFAULT_NOTIFICATION_URI")), alertOnce);

				notifyBigText(context, notificationManager, builder, note);
			}
			// }
		}
	}

	/**
	 * Returns a notification builder set with non-item specific properties.
	 */
	private static NotificationCompat.Builder getNotificationBuilder(final Context context,
			final int lightAndVibrate, final Uri ringtone,
			final boolean alertOnce) {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context)
				.setWhen(0)
				.setSmallIcon(R.drawable.ic_stat_notification_edit)
				.setLargeIcon(
						BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setDefaults(lightAndVibrate).setAutoCancel(true)
				.setOnlyAlertOnce(alertOnce).setSound(ringtone);
		return builder;
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
			final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications) {
		// get duplicates and iterate over them
		for (com.nononsenseapps.notepad.data.model.sql.Notification noti : getLatestOccurence(notifications)) {
			// remove all but the first one from database, and big list
			for (com.nononsenseapps.notepad.data.model.sql.Notification dupNoti : getDuplicates(
					noti, notifications)) {
				notifications.remove(dupNoti);
				cancelNotification(context, dupNoti);
				// Cancelled called in delete
				dupNoti.deleteOrReschedule(context);
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
	private static List<com.nononsenseapps.notepad.data.model.sql.Notification> getLatestOccurence(
			final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications) {
		final ArrayList<Long> seenIds = new ArrayList<Long>();
		final ArrayList<com.nononsenseapps.notepad.data.model.sql.Notification> firsts = new ArrayList<com.nononsenseapps.notepad.data.model.sql.Notification>();

		com.nononsenseapps.notepad.data.model.sql.Notification noti;
		for (int i = notifications.size() - 1; i >= 0; i--) {
			noti = notifications.get(i);
			if (!seenIds.contains(noti.taskID)) {
				seenIds.add(noti.taskID);
				firsts.add(noti);
			}
		}
		return firsts;
	}

	private static List<com.nononsenseapps.notepad.data.model.sql.Notification> getDuplicates(
			final com.nononsenseapps.notepad.data.model.sql.Notification first,
			final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications) {
		final ArrayList<com.nononsenseapps.notepad.data.model.sql.Notification> dups = new ArrayList<com.nononsenseapps.notepad.data.model.sql.Notification>();

		for (com.nononsenseapps.notepad.data.model.sql.Notification noti : notifications) {
			if (noti.taskID == first.taskID && noti._id != first._id) {
				dups.add(noti);
			}
		}
		return dups;
	}

	/**
	 * Needs the builder that contains non-note specific values.
	 *
	 */
	private static void notifyBigText(final Context context,
			final NotificationManager notificationManager,
			final NotificationCompat.Builder builder,
			final com.nononsenseapps.notepad.data.model.sql.Notification note) {
		final Intent delIntent = new Intent(Intent.ACTION_DELETE, note.getUri());
		if (note.repeats != 0) {
			delIntent.setAction(ACTION_RESCHEDULE);
		}
		// Add extra so we don't delete all
		// if (note.time != null) {
		// delIntent.putExtra(ARG_MAX_TIME, note.time);
		// }
		delIntent.putExtra(ARG_TASKID, note.taskID);
		// Delete it on clear
		PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0,
				delIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Open intent
		final Intent openIntent = new Intent(Intent.ACTION_VIEW, Task.getUri(note.taskID));
		// Should create a new instance to avoid fragment problems
		openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

		// Delete intent on non-location repeats
		// Opening the note should delete/reschedule the notification
		openIntent.putExtra(NOTIFICATION_DELETE_ARG, note._id);

		// Opening always cancels the notification though
		openIntent.putExtra(NOTIFICATION_CANCEL_ARG, note._id);

		// Open note on click
				PendingIntent clickIntent = PendingIntent.getActivity(context, 0,
						openIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);

		// Action to complete
		PendingIntent completeIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(ACTION_COMPLETE, note.getUri()).putExtra(ARG_TASKID,
						note.taskID), PendingIntent.FLAG_UPDATE_CURRENT);
        Log.i(TAG, "Action to Complete @ NotificationHelper Ln#294");

		// Action to snooze
		PendingIntent snoozeIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(ACTION_SNOOZE, note.getUri()).putExtra(ARG_TASKID,
						note.taskID), PendingIntent.FLAG_UPDATE_CURRENT);

		// Build notification
		builder.setContentTitle(note.taskTitle)
				.setContentText(note.taskNote)
				.setContentIntent(clickIntent)
				.setStyle(
						new NotificationCompat.BigTextStyle()
								.bigText(note.taskNote));
        Log.i(TAG, "Build notifications at NotificationHelper Ln# 307");

		// Delete intent on non-location repeats
		builder.setDeleteIntent(deleteIntent);

		// Snooze button only on time non-repeating
		if (note.time != null && note.repeats == 0) {
			builder.addAction(R.drawable.ic_alarm_24dp_white,
					context.getText(R.string.snooze), snoozeIntent);
		}
		// Complete button only on non-repeating, both time and location
		if (note.repeats == 0) {
			builder.addAction(R.drawable.ic_check_24dp_white,
					context.getText(R.string.completed), completeIntent);
		}

		final Notification noti = builder.build();

		notificationManager.notify((int) note._id, noti);
	}

    private static long getLatestTime(final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications) {
        long latest = 0;
        for (com.nononsenseapps.notepad.data.model.sql.Notification noti : notifications) {
            if (noti.time > latest)
                latest = noti.time;
        }
        return latest;
    }

    // private static void notifyInboxStyle(
	// final Context context,
	// final NotificationManager notificationManager,
	// final NotificationCompat.Builder builder,
	// final Long idToUse,
	// final List<com.nononsenseapps.notepad.data.model.sql.Notification>
	// notifications) {
	// // Delete intent must delete all notifications
	// Intent delint = new Intent(Intent.ACTION_DELETE,
	// com.nononsenseapps.notepad.data.model.sql.Notification.URI);
	// // Add extra so we don't delete all
	// final long maxTime = getLatestTime(notifications);
	// delint.putExtra(ARG_MAX_TIME, maxTime);
	// delint.putExtra(ARG_LISTID, idToUse);
	// PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0,
	// delint, PendingIntent.FLAG_UPDATE_CURRENT);
	//
	// // Open intent should open the list
	// PendingIntent clickIntent = PendingIntent.getActivity(context, 0,
	// new Intent(Intent.ACTION_VIEW, TaskList.getUri(idToUse),
	// context, ActivityMain_.class),
	// PendingIntent.FLAG_UPDATE_CURRENT);
	//
	// final String title = notifications.get(0).listTitle + " ("
	// + notifications.size() + ")";
	// // Build notification
	// builder.setContentTitle(title).setNumber(notifications.size())
	// .setContentText(notifications.get(0).taskTitle)
	// .setContentIntent(clickIntent).setDeleteIntent(deleteIntent);
	//
	// // Action to snooze
	// PendingIntent snoozeIntent = PendingIntent.getBroadcast(
	// context,
	// 0,
	// new Intent(Intent.ACTION_EDIT, TaskList.getUri(idToUse))
	// .putExtra(ARG_SNOOZE, true)
	// .putExtra(ARG_LISTID, idToUse)
	// .setClass(context, NotificationHelper.class),
	// PendingIntent.FLAG_UPDATE_CURRENT);
	// builder.addAction(R.drawable.ic_stat_snooze,
	// context.getText(R.string.snooze), snoozeIntent);
	//
	// // Action to complete
	// PendingIntent completeIntent = PendingIntent.getBroadcast(
	// context,
	// 0,
	// new Intent(Intent.ACTION_DELETE, TaskList.getUri(idToUse))
	// .putExtra(ARG_COMPLETE, true)
	// .putExtra(ARG_LISTID, idToUse)
	// .putExtra(ARG_MAX_TIME, maxTime)
	// .setClass(context, NotificationHelper.class),
	// PendingIntent.FLAG_UPDATE_CURRENT);
	// builder.addAction(R.drawable.navigation_accept_dark,
	// context.getText(R.string.completed), completeIntent);
	//
	// NotificationCompat.InboxStyle ib = new NotificationCompat.InboxStyle()
	// .setBigContentTitle(title);
	// if (notifications.size() > 6)
	// ib.setSummaryText("+" + (notifications.size() - 6) + " "
	// + context.getString(R.string.more));
	//
	// for (com.nononsenseapps.notepad.data.model.sql.Notification e : notifications)
	// {
	// ib.addLine(e.taskTitle);
	// }
	//
	// final Notification noti = builder.setStyle(ib).build();
	// notificationManager.notify(idToUse.intValue(), noti);
	// }

	/**
	 * Schedules to be woken up at the next notification time.
	 */
	private static void scheduleNext(Context context) {
		// Get first future notification
		final Calendar now = Calendar.getInstance();
		final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications = com.nononsenseapps.notepad.data.model.sql.Notification
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
	 * @param notification
	 */
	public static void updateNotification(final Context context,
			final com.nononsenseapps.notepad.data.model.sql.Notification notification) {
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
	 * @param not
	 */
	public static void cancelNotification(final Context context,
			final com.nononsenseapps.notepad.data.model.sql.Notification not) {
		if (not != null) {
			cancelNotification(context, not.getUri());
		}
	}

	/**
	 * Does not touch db
	 */
	public static void cancelNotification(final Context context, final Uri uri) {
		if (uri != null)
			cancelNotification(context, Integer.parseInt(uri.getLastPathSegment()));
	}

	/**
	 * Does not touch db
	 */
	public static void cancelNotification(final Context context, final int notId) {
		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notId);
	}

	/**
	 * Given a list of notifications, returns a list of the lists the notes
	 * belong to.
	 */
	private static Collection<Long> getRelatedLists(
			final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications) {
		final HashSet<Long> lists = new HashSet<Long>();
		for (com.nononsenseapps.notepad.data.model.sql.Notification not : notifications) {
			lists.add(not.listID);
		}

		return lists;
	}

	/**
     * Modifies DB
     */
    // public static void deleteNotification(final Context context, long listId,
    // long maxTime) {
    // com.nononsenseapps.notepad.data.model.sql.Notification.removeWithListId(
    // context, listId, maxTime);
    //
    // final NotificationManager notificationManager = (NotificationManager)
    // context
    // .getSystemService(Context.NOTIFICATION_SERVICE);
    // notificationManager.cancel((int) listId);
    // }

    /**
     * Returns a list of those notifications that are associated to notes in the
	 * specified list.
	 */
	private static List<com.nononsenseapps.notepad.data.model.sql.Notification> getSubList(
			final long listId,
			final List<com.nononsenseapps.notepad.data.model.sql.Notification> notifications) {
		final ArrayList<com.nononsenseapps.notepad.data.model.sql.Notification> subList = new ArrayList<com.nononsenseapps.notepad.data.model.sql.Notification>();
		for (com.nononsenseapps.notepad.data.model.sql.Notification not : notifications) {
			if (not.listID == listId) {
				subList.add(not);
			}
		}

		return subList;
    }

    /**
     * Fires notifications that have elapsed and sets an alarm to be woken at
     * the next notification.
     * <p/>
     * If the intent action is ACTION_DELETE, will delete the notification with
     * the indicated ID, and cancel it from any active notifications.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_RUN.equals
                (intent.getAction())) {
            // Can't cancel anything. Just schedule and notify at end
        } else {
            // Always cancel
            cancelNotification(context, intent.getData());

            if (Intent.ACTION_DELETE.equals(intent.getAction()) || ACTION_RESCHEDULE.equals
                    (intent.getAction())) {
                // Just a notification
                com.nononsenseapps.notepad.data.model.sql.Notification.deleteOrReschedule(context,
                        intent.getData());
            } else if (ACTION_SNOOZE.equals(intent.getAction())) {
                // msec/sec * sec/min * 30
                long delay30min = 1000 * 60 * 30;
                final Calendar now = Calendar.getInstance();

                com.nononsenseapps.notepad.data.model.sql.Notification.setTime(context, intent.getData
                        (), delay30min + now.getTimeInMillis());
            } else if (ACTION_COMPLETE.equals(intent.getAction())) {
                // Complete note
                Task.setCompletedSynced(context, true, intent.getLongExtra(ARG_TASKID, -1));
                // Delete notifications with the same task id
                com.nononsenseapps.notepad.data.model.sql.Notification.removeWithTaskIdsSynced(context, intent.getLongExtra(ARG_TASKID, -1));
            }
        }

        notifyPast(context, true);
        scheduleNext(context);
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
