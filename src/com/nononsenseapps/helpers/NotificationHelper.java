package com.nononsenseapps.helpers;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.NotesEditorFragment;
import com.nononsenseapps.notepad_donate.R;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class NotificationHelper extends BroadcastReceiver {

	// The order is damn important here, see constructor for NoteNotification
	public static final String[] PROJECTION = new String[] {
			NotePad.Notifications._ID, NotePad.Notifications.COLUMN_NAME_TIME,
			NotePad.Notifications.COLUMN_NAME_PERMANENT,
			NotePad.Notifications.COLUMN_NAME_NOTEID,
			NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE,
			NotePad.Notes.COLUMN_NAME_LIST,
			NotePad.Notifications.JOINED_COLUMN_LIST_TITLE };

	public static final int NOTIFICATION_ID = 4364;

	private static final String TAG = "NoNonsenseNotes.NotificationHelper";

	/**
	 * Fires notifications that have elapsed and sets an alarm to be woken at
	 * the next notification.
	 * 
	 * If the intent action is ACTION_DELETE, will delete the notification with
	 * the indicated ID, and cancel it from any active notifications.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, intent.getAction());
		if (Intent.ACTION_DELETE.equals(intent.getAction())) {
			Log.d(TAG, "Got delete broadcast");
			if (intent.hasExtra(NotePad.Notifications.COLUMN_NAME_TIME)) {
				Log.d(TAG, "Got delete list broadcast");
				deleteNotification(context,
						NotesEditorFragment.getIdFromUri(intent.getData()),
						intent.getLongExtra(
								NotePad.Notifications.COLUMN_NAME_TIME, 0));
			} else
				// Just a notification
				deleteNotification(context, intent.getData());

			// User in editor, don't spam
			notifyPast(context, true);
		} else {
			notifyPast(context, false);
		}

		scheduleNext(context);
	}

	/**
	 * Displays notifications that have a time occurring in the past. If no
	 * notifications like that exist, will make sure to cancel any notifications
	 * showing.
	 */
	private static void notifyPast(Context context, boolean alertOnce) {
		// Get list of past notifications
		Date now = new Date();

		List<NoteNotification> notifications = getNotifications(context,
				NotePad.Notifications.COLUMN_NAME_TIME + " <= ?",
				new String[] { Long.toString(now.getTime()) });

		// Remove duplicates
		makeUnique(context, notifications,
				NotePad.Notifications.COLUMN_NAME_TIME + " <= ?",
				new String[] { Long.toString(now.getTime()) });

		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// If empty, cancel
		if (notifications.isEmpty()) {
			// cancelAll permanent notifications here if/when that is
			// implemented. Don't touch others.
			// notificationManager.cancelAll();
		} else {
			// else, notify
			// Fetch sound and vibrate settings
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);

			// Always use default lights
			int lightAndVibrate = Notification.DEFAULT_LIGHTS;
			// If vibrate on, use default vibration pattern also
			if (prefs.getBoolean(context.getString(R.string.key_pref_vibrate),
					false))
				lightAndVibrate |= Notification.DEFAULT_VIBRATE;

			NotificationCompat.Builder builder = new NotificationCompat.Builder(
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
					.setAutoCancel(false)
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
					List<NoteNotification> subList = getSubList(listId,
							notifications);
					if (subList.size() == 1) {
						// Notify as single
						notifyBigText(context, notificationManager, builder,
								(int) listId, subList.get(0));
					} else {
						notifyInboxStyle(context, notificationManager, builder,
								(int) listId, subList);
					}
				}
			} else {
				// Notify for each individually
				for (NoteNotification note : notifications) {
					notifyBigText(context, notificationManager, builder,
							(int) note.noteId, note);
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
	private static void makeUnique(final Context context,
			final List<NoteNotification> notifications, String where,
			String[] whereArgs) {
		// get duplicates and iterate over them
		for (NoteNotification noti : getLatestOccurence(notifications)) {
			// remove all but the first one from database, and big list
			for (NoteNotification dupNoti : getDuplicates(noti, notifications)) {
				notifications.remove(dupNoti);
				deleteNotification(context, dupNoti);
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
	private static List<NoteNotification> getLatestOccurence(
			final List<NoteNotification> notifications) {
		ArrayList<Long> seenIds = new ArrayList<Long>();
		ArrayList<NoteNotification> firsts = new ArrayList<NoteNotification>();
		
		NoteNotification noti;
		for (int i = notifications.size() - 1; i >= 0; i--) {
			noti = notifications.get(i);
			if (!seenIds.contains(noti.noteId)) {
				seenIds.add(noti.noteId);
				firsts.add(noti);
			}
		}
		return firsts;
	}

	private static List<NoteNotification> getDuplicates(
			final NoteNotification first,
			final List<NoteNotification> notifications) {
		ArrayList<NoteNotification> dups = new ArrayList<NoteNotification>();
		
		for (NoteNotification noti: notifications) {
			if (noti.noteId == first.noteId && noti.id != first.id) {
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
			final NotificationCompat.Builder builder, final int idToUse,
			final NoteNotification note) {
		// Delete it on clear
		PendingIntent deleteIntent = PendingIntent.getBroadcast(
				context,
				0,
				new Intent(Intent.ACTION_DELETE, Uri.withAppendedPath(
						NotePad.Notifications.CONTENT_ID_URI_BASE,
						Integer.toString(note.id))),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Open note on click
		PendingIntent clickIntent = PendingIntent.getActivity(
				context,
				0,
				new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(
						NotePad.Notes.CONTENT_ID_URI_BASE,
						Long.toString(note.noteId))),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Build notification
		final Notification noti = builder
				.setContentTitle(note.title)
				.setContentText(note.summary)
				.setContentIntent(clickIntent)
				.setDeleteIntent(deleteIntent)
				.setStyle(
						new NotificationCompat.BigTextStyle()
								.bigText(note.summary)).build();

		notificationManager.notify(idToUse, noti);
	}

	private static void notifyInboxStyle(final Context context,
			final NotificationManager notificationManager,
			final NotificationCompat.Builder builder, final int idToUse,
			final List<NoteNotification> notifications) {
		// Delete intent must delete all notifications
		Intent delint = new Intent(Intent.ACTION_DELETE, Uri.withAppendedPath(
				NotePad.Notifications.CONTENT_ID_URI_BASE,
				Integer.toString(idToUse)));
		// Add extra so we don't delete all
		delint.putExtra(NotePad.Notifications.COLUMN_NAME_TIME,
				getLatestTime(notifications));
		PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0,
				delint, PendingIntent.FLAG_UPDATE_CURRENT);

		// Open intent should open the list
		PendingIntent clickIntent = PendingIntent
				.getActivity(
						context,
						0,
						new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(
								NotePad.Lists.CONTENT_VISIBLE_ID_URI_BASE,
								Integer.toString(idToUse)), context,
								MainActivity.class),
						PendingIntent.FLAG_UPDATE_CURRENT);

		final String title = notifications.get(0).listTitle + " ("
				+ notifications.size() + ")";
		// Build notification
		builder.setContentTitle(title)
				.setContentText(notifications.get(0).title)
				.setContentIntent(clickIntent).setDeleteIntent(deleteIntent);

		NotificationCompat.InboxStyle ib = new NotificationCompat.InboxStyle()
				.setBigContentTitle(title);
		if (notifications.size() > 6)
			ib.setSummaryText("+" + (notifications.size() - 6) + " "
					+ context.getString(R.string.more));

		for (NoteNotification e : notifications) {
			ib.addLine(e.title);
		}

		final Notification noti = builder.setStyle(ib).build();
		notificationManager.notify(idToUse, noti);
	}

	private static long getLatestTime(final List<NoteNotification> notifications) {
		long latest = 0;
		for (NoteNotification noti : notifications) {
			if (noti.time > latest)
				latest = noti.time;
		}
		return latest;
	}

	/**
	 * Schedules to be woken up at the next notification time.
	 */
	private static void scheduleNext(Context context) {
		// Get first future notification
		Date now = new Date();
		List<NoteNotification> notifications = getNotifications(context,
				NotePad.Notifications.COLUMN_NAME_TIME + " > ?",
				new String[] { Long.toString(now.getTime()) });

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
			NoteNotification notification) {
		ContentValues values = new ContentValues();

		values.put(NotePad.Notifications.COLUMN_NAME_NOTEID,
				notification.noteId);
		values.put(NotePad.Notifications.COLUMN_NAME_PERMANENT,
				notification.permanent);
		values.put(NotePad.Notifications.COLUMN_NAME_TIME, notification.time);

		Log.d(TAG, notification.getLocalTime());
		/*
		 * Only don't insert if update is success This way the editor can update
		 * a deleted notification and still have it persisted in the database
		 */
		boolean shouldInsert = true;
		// If id is -1, then this should be inserted
		if (notification.id != -1) {
			// Else it should be updated
			int result = context.getContentResolver().update(
					Uri.withAppendedPath(
							NotePad.Notifications.CONTENT_ID_URI_BASE,
							Integer.toString(notification.id)), values, null,
					null);
			if (result > 0)
				shouldInsert = false;
		}

		if (shouldInsert) {
			Uri uri = context.getContentResolver().insert(
					NotePad.Notifications.CONTENT_URI, values);
			notification.id = (int) NotesEditorFragment.getIdFromUri(uri);
		}

		notifyPast(context, true);
		scheduleNext(context);
	}

	/**
	 * Deletes the indicated notification from the database.
	 * 
	 * @param context
	 * @param id
	 */
	public static void deleteNotification(Context context, NoteNotification not) {
		if (not != null) {
			deleteNotification(context, not.id);
		}
	}

	/**
	 * Deletes the indicated notification from the database.
	 * 
	 * @param context
	 * @param id
	 */
	public static void deleteNotification(Context context, long id) {
		Log.d(TAG, "delete notification ID: " + id);
		if (id > -1) {
			context.getContentResolver().delete(
					Uri.withAppendedPath(
							NotePad.Notifications.CONTENT_ID_URI_BASE,
							Long.toString(id)), null, null);

			/*
			 * final NotificationManager notificationManager =
			 * (NotificationManager) context
			 * .getSystemService(Context.NOTIFICATION_SERVICE);
			 * notificationManager.cancel((int) id);
			 */
		}
	}

	public static void deleteNotification(Context context, Uri uri) {
		Log.d(TAG, "Trying to delete " + uri.toString());
		context.getContentResolver().delete(uri, null, null);

		/*
		 * final NotificationManager notificationManager = (NotificationManager)
		 * context .getSystemService(Context.NOTIFICATION_SERVICE);
		 * notificationManager.cancel((int)
		 * NotesEditorFragment.getIdFromUri(uri));
		 */
	}

	public static void deleteNotification(Context context, long listid,
			long modlimit) {
		Log.d(TAG, "Trying to delete with modlimit: " + listid);
		context.getContentResolver().delete(
				Uri.withAppendedPath(
						NotePad.Notifications.CONTENT_LISTID_URI_BASE,
						Long.toString(listid)),
				NotePad.Notifications.COLUMN_NAME_TIME + " <= ?",
				new String[] { Long.toString(modlimit) });

		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel((int) listid);
	}

	/**
	 * Returns the notifications associated with this note
	 * 
	 * @param context
	 * @param noteId
	 * @return
	 */
	public static List<NoteNotification> getNotifications(Context context,
			long noteId) {
		return getNotifications(context,
				NotePad.Notifications.COLUMN_NAME_NOTEID + " IS ?",
				new String[] { Long.toString(noteId) });
	}

	private static List<NoteNotification> getNotifications(Context context,
			String where, String[] whereArgs) {
		List<NoteNotification> notifications = new ArrayList<NotificationHelper.NoteNotification>();

		Cursor cursor = context.getContentResolver().query(
				NotePad.Notifications.CONTENT_JOINED_URI, PROJECTION, where,
				whereArgs, NotePad.Notifications.COLUMN_NAME_TIME);

		if (cursor != null) {
			if (!cursor.isClosed() && !cursor.isAfterLast()) {
				while (cursor.moveToNext()) {
					notifications.add(new NoteNotification(context, cursor));
				}
			}
			cursor.close();
		}

		return notifications;
	}

	/**
	 * Given a list of notifications, returns a list of the lists the notes
	 * belong to.
	 * 
	 * @param notifications
	 * @return
	 */
	private static Collection<Long> getRelatedLists(
			List<NoteNotification> notifications) {
		HashSet<Long> lists = new HashSet<Long>();
		for (NoteNotification not : notifications) {
			lists.add(not.listId);
		}

		return lists;
	}

	/**
	 * Returns a list of those notifications that are associated to notes in the
	 * specified list.
	 * 
	 * @param listId
	 * @param notifications
	 * @return
	 */
	private static List<NoteNotification> getSubList(long listId,
			List<NoteNotification> notifications) {
		ArrayList<NoteNotification> subList = new ArrayList<NoteNotification>();
		for (NoteNotification not : notifications) {
			if (not.listId == listId) {
				subList.add(not);
			}
		}

		return subList;
	}

	public static class NoteNotification {
		public int id;
		public long time;
		public boolean permanent;
		public long noteId;
		public long listId;

		// Actually note title
		public String title;
		// Actually note content
		public String summary;

		// Actually list title
		public String listTitle;

		/**
		 * Returns the date according to the locale
		 * 
		 * @return
		 */
		public String getLocalDate() {
			DateFormat df = DateFormat.getDateInstance();
			// df.setTimeZone(TimeZone.getTimeZone("gmt"));
			return df.format(new Date(time));
		}

		/**
		 * Returns the time according to the locale
		 * 
		 * @return
		 */
		public String getLocalTime() {
			DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
			// df.setTimeZone(TimeZone.getTimeZone("gmt"));
			return df.format(new Date(time));
		}

		/**
		 * Defaults to current date and time. Used to insert notifications in DB
		 */
		public NoteNotification(long noteId, String title, String note) {
			id = -1;
			this.listId = -1; // Not needed to insert notifications
			this.listTitle = ""; // Not needed either
			this.noteId = noteId;
			this.title = title;
			summary = note;
			permanent = false;

			// default to now + 2 hours
			Date now = new Date();
			this.time = now.getTime() + 2 * 60 * 60 * 1000;
		}

		public NoteNotification(Context context, Cursor cursor) {
			id = cursor
					.getInt(cursor.getColumnIndex(NotePad.Notifications._ID));
			time = cursor.getLong(cursor
					.getColumnIndex(NotePad.Notifications.COLUMN_NAME_TIME));
			noteId = cursor.getLong(cursor
					.getColumnIndex(NotePad.Notifications.COLUMN_NAME_NOTEID));
			listId = cursor.getLong(cursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_LIST));

			// See projection array for this. Notes.COLUMN_NAME_TITLE
			// Issue with join projection
			title = cursor.getString(4);
			summary = cursor.getString(cursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));

			// See projection array for this. Notes.COLUMN_NAME_TITLE
			// Issue with join projection
			listTitle = cursor.getString(7);

			if (0 == cursor
					.getLong(cursor
							.getColumnIndex(NotePad.Notifications.COLUMN_NAME_PERMANENT))) {
				permanent = false;
			} else {
				permanent = true;
			}

			Log.d(TAG, "constructor notification id = " + id);
		}
	}
}
