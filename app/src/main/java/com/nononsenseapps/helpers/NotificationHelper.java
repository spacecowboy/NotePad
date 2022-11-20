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

package com.nononsenseapps.helpers;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.util.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class NotificationHelper extends BroadcastReceiver {

	// Intent notification argument
	public static final String NOTIFICATION_CANCEL_ARG = "notification_cancel_arg";
	public static final String NOTIFICATION_DELETE_ARG = "notification_delete_arg";

	static final String ARG_TASKID = "taskid";
	private static final String ACTION_COMPLETE = "com.nononsenseapps.notepad.ACTION.COMPLETE";
	private static final String ACTION_SNOOZE = "com.nononsenseapps.notepad.ACTION.SNOOZE";
	private static final String ACTION_RESCHEDULE = "com.nononsenseapps.notepad.ACTION.RESCHEDULE";
	public static final String CHANNEL_ID = "remindersNotificationChannelId";

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
	public void onReceive(Context context, @NonNull Intent intent) {
		String action = intent.getAction();
		if (action != null) {
			// should we cancel something? we decide it here:
			if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_RUN.equals(action)) {
				// => can't cancel anything. Just schedule and notify at end of the function.
				// (Intent.ACTION_BOOT_COMPLETED is for when the phone is rebooted,
				// Intent.ACTION_RUN is for notifications scheduled through the Alarm Manager)
			} else {
				// => always cancel
				cancelNotification(context, intent.getData());

				switch (action) {
					case Intent.ACTION_DELETE:
					case ACTION_RESCHEDULE:
						// Just a notification
						com.nononsenseapps.notepad.database.Notification
								.deleteOrReschedule(context, intent.getData());
						break;
					case ACTION_SNOOZE:
						// TODO snooze logic is hardcoded to 30' here.
						//  Set a custom timer in the preferences and load the number here
						final long minutes = 30;
						// msec/sec * sec/min * (snooze minutes)
						final long snoozeDelayInMillis = 1000 * 60 * minutes;
						final Calendar now = Calendar.getInstance();

						com.nononsenseapps.notepad.database.Notification
								.setTime(context, intent.getData(),
										snoozeDelayInMillis + now.getTimeInMillis());
						break;
					case ACTION_COMPLETE:
						final long taskId = intent.getLongExtra(ARG_TASKID, -1);
						// Complete note
						Task.setCompletedSynced(context, true, taskId);
						// Delete notifications with the same task id
						com.nononsenseapps.notepad.database.Notification
								.removeWithTaskIdsSynced(context, taskId);
						break;
				}
			}
		}
		// run these in ANY case
		notifyPast(context);
		scheduleNext(context);
	}

	/**
	 * creates the notification channel needed on API 26 and higher to show notifications.
	 * This is safe to call multiple times
	 */
	@TargetApi(Build.VERSION_CODES.O)
	@RequiresApi(Build.VERSION_CODES.O)
	public static void createNotificationChannel(final Context context, NotificationManager nm) {
		String name = context.getString(R.string.notification_channel_name);
		String description = context.getString(R.string.notification_channel_description);

		// not higher, not lower. This is equivalent to notifications before API 24
		int importance = NotificationManager.IMPORTANCE_DEFAULT;

		NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
		channel.setDescription(description);
		nm.createNotificationChannel(channel);
	}

	private static void monitorUri(final Context context) {
		context.getContentResolver().unregisterContentObserver(getObserver(context));
		context.getContentResolver().registerContentObserver(
				com.nononsenseapps.notepad.database.Notification.URI,
				true,
				getObserver(context));
	}


	public static void clearNotification(@NonNull final Context context,
										 @NonNull final Intent intent) {
		if (intent.getLongExtra(NOTIFICATION_DELETE_ARG, -1) > 0) {
			com.nononsenseapps.notepad.database.Notification.deleteOrReschedule(context,
					com.nononsenseapps.notepad.database.Notification.getUri(
							intent.getLongExtra(NOTIFICATION_DELETE_ARG, -1)));
		}
		if (intent.getLongExtra(NOTIFICATION_CANCEL_ARG, -1) > 0) {
			NotificationHelper.cancelNotification(context,
					(int) intent.getLongExtra(NOTIFICATION_CANCEL_ARG, -1));
		}
	}

	/**
	 * Displays notifications that have a time occurring in the past. If no notifications
	 * like that exist, it will make sure to cancel any notifications showing.
	 */
	private static void notifyPast(Context context) {
		// Get list of past notifications
		final Calendar now = Calendar.getInstance();

		final List<com.nononsenseapps.notepad.database.Notification> notifications
				= com.nononsenseapps.notepad.database.Notification
				.getNotificationsWithTime(context, now.getTimeInMillis(), true);

		// Remove duplicates
		makeUnique(context, notifications);

		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel(context, notificationManager);
		}

		NnnLogger.debug(NotificationHelper.class,
				"Number of notifications: " + notifications.size());

		// If empty, cancel
		if (notifications.isEmpty()) {
			// cancelAll permanent notifications here if/when that is implemented.
			// Don't touch others. Dont do this, it clears location
			// notificationManager.cancelAll();
		} else {
			// else, notify

			// Fetch sound and vibrate settings
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

			// Always use default lights
			int lightAndVibrate = Notification.DEFAULT_LIGHTS;
			// If vibrate on, use default vibration pattern also
			if (prefs.getBoolean(context.getString(R.string.key_pref_vibrate), false))
				lightAndVibrate |= Notification.DEFAULT_VIBRATE;

			// Need to get a new one because the action buttons will duplicate otherwise
			NotificationCompat.Builder builder;

			// (Here there was code to group notifications together by list.
			// I removed it because it wasn't being used.
			// Check git history if you're interested)

			// get priority and ringtone. See NotificationPrefs.java
			final int priority = Integer.parseInt(
					prefs.getString(context.getString(R.string.key_pref_prio), "0"));
			final Uri ringtone = Uri.parse(prefs.getString(
					context.getString(R.string.key_pref_ringtone),
					"DEFAULT_NOTIFICATION_URI"));

			// Notify for each individually
			for (com.nononsenseapps.notepad.database.Notification note : notifications) {
				// notifications.length is ~3 => optimization is not needed
				builder = getNotificationBuilder(context, priority, lightAndVibrate, ringtone);
				notifyBigText(context, notificationManager, builder, note);
			}
		}
	}

	/**
	 * Returns a notification builder set with non-item specific properties.
	 */
	private static NotificationCompat.Builder getNotificationBuilder(final Context context,
																	 final int priority,
																	 final int lightAndVibrate,
																	 final Uri ringtone) {
		// useless ? the small icon should be enough
		final Bitmap largeIcon = BitmapFactory
				.decodeResource(context.getResources(), R.drawable.app_icon);

		final NotificationCompat.Builder builder = new NotificationCompat
				.Builder(context, CHANNEL_ID) // let's use only 1 channel in this app
				.setWhen(0)
				.setSmallIcon(R.drawable.ic_stat_notification_edit)
				.setLargeIcon(largeIcon)
				.setPriority(priority) // TODO always use NotificationCompat.PRIORITY_DEFAULT instead ?
				.setDefaults(lightAndVibrate)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setSound(ringtone);
		return builder;
	}

	/**
	 * Remove from the database, and the specified list, duplicate
	 * notifications. The result is that each note is only associated with ONE
	 * EXPIRED notification.
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
				// Cancelled called in delete
				dupNoti.deleteOrReschedule(context);
			}
		}

	}

	/**
	 * Returns the first occurrence of each note's notification. Effectively the
	 * returned list has unique elements with regard to the note id.
	 */
	private static List<com.nononsenseapps.notepad.database.Notification> getLatestOccurence(
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		final ArrayList<Long> seenIds = new ArrayList<>();
		final ArrayList<com.nononsenseapps.notepad.database.Notification> firsts = new ArrayList<>();

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
		final ArrayList<com.nononsenseapps.notepad.database.Notification> dups = new ArrayList<>();

		for (com.nononsenseapps.notepad.database.Notification noti : notifications) {
			if (noti.taskID.equals(first.taskID) && noti._id != first._id) {
				dups.add(noti);
			}
		}
		return dups;
	}

	/**
	 * Needs the builder that contains non-note specific values.
	 */
	private static void notifyBigText(final Context context,
									  final NotificationManager notificationManager,
									  final NotificationCompat.Builder builder,
									  final com.nononsenseapps.notepad.database.Notification note) {
		// create the intent that reacts to deleting the notification
		final Intent iDelete = new Intent(context, NotificationHelper.class)
				.setAction(Intent.ACTION_DELETE)
				.setData(note.getUri());
		if (note.repeats != 0) {
			iDelete.setAction(ACTION_RESCHEDULE);
		}

		iDelete.putExtra(ARG_TASKID, note.taskID);
		// Delete it on clear
		PendingIntent piDelete = PendingIntent.getBroadcast(context, 0,
				iDelete, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Open intent
		final Intent openIntent = new Intent(Intent.ACTION_VIEW, Task.getUri(note.taskID));
		// Should create a new instance to avoid fragment problems
		openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

		// Repeating reminders should have a delete intent:
		// opening the note should delete/reschedule the notification
		openIntent.putExtra(NOTIFICATION_DELETE_ARG, note._id);

		// Opening always cancels the notification though
		openIntent.putExtra(NOTIFICATION_CANCEL_ARG, note._id);

		// Open note on click
		PendingIntent clickIntent = PendingIntent.getActivity(context, 0,
				openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Action to complete
		Intent iComplete = new Intent(context, NotificationHelper.class)
				.setAction(ACTION_COMPLETE)
				.setData(note.getUri())
				.putExtra(ARG_TASKID, note.taskID);
		PendingIntent piComplete = PendingIntent.getBroadcast(context, 0,
				iComplete, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Action to snooze
		Intent iSnooze = new Intent(context, NotificationHelper.class)
				.setAction(ACTION_SNOOZE)
				.setData(note.getUri())
				.putExtra(ARG_TASKID, note.taskID);
		PendingIntent piSnooze = PendingIntent.getBroadcast(context, 0,
				iSnooze, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Build notification
		builder.setContentTitle(note.taskTitle)
				.setContentText(note.taskNote)
				.setChannelId(CHANNEL_ID)
				.setContentIntent(clickIntent)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(note.taskNote));

		// the Delete intent for non-location repeats
		builder.setDeleteIntent(piDelete);

		// Snooze button only on time non-repeating
		if (note.time != null && note.repeats == 0) {
			builder.addAction(R.drawable.ic_alarm_24dp_white, context.getText(R.string.snooze), piSnooze);
		}
		// Complete button only on non-repeating, both time and location
		if (note.repeats == 0) {
			builder.addAction(R.drawable.ic_check_24dp_white, context.getText(R.string.completed), piComplete);
		}

		final Notification noti = builder.build();
		notificationManager.notify((int) note._id, noti);
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
	 * Schedules this {@link BroadcastReceiver} to be woken up at the next notification time.
	 * Uses {@link AlarmManager}, which can set alarms with different priorities.
	 * See https://developer.android.com/training/scheduling/alarms#exact
	 * You can't expect android to be precise or reliable: reminders will appear within
	 * a few minutes from the specified time, or may not appear at all until the app is restarted
	 */
	private static void scheduleNext(Context context) {
		// Get first future notification
		final Calendar now = Calendar.getInstance();
		final List<com.nononsenseapps.notepad.database.Notification> notifications
				= com.nononsenseapps.notepad.database.Notification
				.getNotificationsWithTime(context, now.getTimeInMillis(), false);

		// TODO learn about the disasters Google made with alarms:
		//  https://developer.android.com/reference/android/Manifest.permission#SCHEDULE_EXACT_ALARM
		//  https://developer.android.com/reference/android/Manifest.permission#USE_EXACT_ALARM

		// TODO if this function still does not work, try these
		//  https://stackoverflow.com/a/60323379/6307322
		//  https://stackoverflow.com/a/60477054/6307322
		//  https://github.com/yuriykulikov/AlarmClock/blob/78fe0d2077260e2c68a5f0731c563bf57f8c0fa2/app/src/main/java/com/better/alarm/presenter/ScheduledReceiver.java
		//  https://stackoverflow.com/a/71464360/6307322
		//  which should let notifications appear even if the app is removed from the "recents" list

		// if not empty, schedule alarm wake up
		if (!notifications.isEmpty()) {
			// at first's time
			var thingToNotify = notifications.get(0);

			// must be an explicit intent
			Intent intent = new Intent(context, NotificationHelper.class)
					.addFlags(Intent.FLAG_RECEIVER_FOREGROUND) // useless flag => remove freely
					.setAction(Intent.ACTION_RUN);
			// Create a new PendingIntent and add it to the AlarmManager
			var pendingIntent = PendingIntent.getBroadcast(context, 1, intent,
					PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			AlarmManager aMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			aMgr.cancel(pendingIntent);

			if (useExactReminders(context, aMgr)) {
				// an "exact" alarm is more reliable, but requires user permission in API 31
				// TODO there is also .setAlarmClock(), but it didn't work when I tried.
				//  It's supposed to have the highest priority, so we SHOULD use it.
				//  var aci = new AlarmManager.AlarmClockInfo(...);
				//  am.setAlarmClock(aci,pendingIntent);
				aMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
						getTimeForAlarm(thingToNotify), pendingIntent);
			} else {
				// these kinds of alarms don't require permission, but they are more vague
				aMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
						getTimeForAlarm(thingToNotify), pendingIntent);
			}
		}
		monitorUri(context);
	}

	/**
	 * @return TRUE if the Alarms (reminders) should be sent with the Exact method,
	 * which is more reliable and precise but heavier on the battery. The user can choose this
	 * in the preferences, and the OS may deny us the permission to do it
	 */
	private static boolean useExactReminders(Context context, AlarmManager aMgr) {
		// The user can (must) request the use of Exact alarms
		boolean shouldUseExact = SharedPreferencesHelper.shouldUseExactAlarms(context);
		if (!shouldUseExact) return false;

		// the user may revoke the permission in android 12
		boolean canUseExact;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
			canUseExact = aMgr.canScheduleExactAlarms();
		} else {
			// in older androids, we can always use Exact reminders
			canUseExact = true;
		}
		// it SHOULD and CAN do it
		return canUseExact;
	}

	/**
	 * @return the time to start the alarm, of the System.currentTimeMillis() type,
	 * so a {@link long} representing a "wall clock time" in UTC
	 */
	private static long getTimeForAlarm(com.nononsenseapps.notepad.database.Notification input) {
		// TODO since android takes some time to understand that it has to send the reminder,
		//  here we could subtract 60~100 seconds so that, by the time it understands what to do,
		//  we're not late with the reminder
		return input.time;
	}

	/**
	 * Schedules coming notifications, and displays expired ones. Only
	 * notififies once for existing notifications.
	 */
	public static void schedule(final Context context) {
		notifyPast(context);
		scheduleNext(context);
	}

	/**
	 * Updates/Inserts notifications in the database. Immediately notifies and
	 * schedules next wake up on finish.
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

		notifyPast(context);
		scheduleNext(context);
	}

	/**
	 * Deletes the indicated notification from the notification tray (does not
	 * touch database)
	 *
	 * Called by notification.delete()
	 */
	public static void cancelNotification(final Context context,
										  final com.nononsenseapps.notepad.database.Notification not) {
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
			final List<com.nononsenseapps.notepad.database.Notification> notifications) {
		final HashSet<Long> lists = new HashSet<>();
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
		final ArrayList<com.nononsenseapps.notepad.database.Notification> subList = new ArrayList<>();
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
			notifyPast(context);
		}
	}
}
