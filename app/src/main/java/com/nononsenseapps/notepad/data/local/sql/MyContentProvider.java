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

package com.nononsenseapps.notepad.data.local.sql;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;

import com.nononsenseapps.notepad.data.local.UpdateNotifier;
import com.nononsenseapps.notepad.data.model.sql.RemoteTask;
import com.nononsenseapps.notepad.data.model.sql.RemoteTaskList;
import com.nononsenseapps.notepad.data.model.sql.DAO;
import com.nononsenseapps.notepad.data.model.sql.Notification;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

import java.util.ArrayList;

public class MyContentProvider extends ContentProvider {
	public static final String AUTHORITY = "com.nononsenseapps.NotePad";
	public static final String SCHEME = "content://";
	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

    static {
        TaskList.addMatcherUris(sURIMatcher);
        Task.addMatcherUris(sURIMatcher);
        Notification.addMatcherUris(sURIMatcher);
        RemoteTaskList.addMatcherUris(sURIMatcher);
        RemoteTask.addMatcherUris(sURIMatcher);
    }

    public MyContentProvider() {
    }

	@Override
	public String getType(Uri uri) {
		switch (sURIMatcher.match(uri)) {
		case Notification.BASEITEMCODE:
		case Notification.BASEURICODE:
		case Notification.WITHTASKQUERYCODE:
		case Notification.WITHTASKQUERYITEMCODE:
			return Notification.CONTENT_TYPE;
		case TaskList.BASEITEMCODE:
		case TaskList.BASEURICODE:
		case TaskList.LEGACYBASEITEMCODE:
		case TaskList.LEGACYBASEURICODE:
		case TaskList.LEGACYVISIBLEITEMCODE:
		case TaskList.LEGACYVISIBLEURICODE:
			return TaskList.CONTENT_TYPE;
		case Task.BASEITEMCODE:
		case Task.BASEURICODE:
		case Task.SECTIONEDDATEITEMCODE:
		case Task.SECTIONEDDATEQUERYCODE:
		case Task.LEGACYBASEITEMCODE:
		case Task.LEGACYBASEURICODE:
		case Task.LEGACYVISIBLEITEMCODE:
		case Task.LEGACYVISIBLEURICODE:
		case Task.SEARCHCODE:
		case Task.SEARCHSUGGESTIONSCODE:
			return Task.CONTENT_TYPE;
		default:
			// throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Legacy URIs, above didn't work for some reason
		if (uri.toString().startsWith(
				LegacyDBHelper.NotePad.Lists.CONTENT_URI.toString())
				|| uri.toString().startsWith(
						LegacyDBHelper.NotePad.Lists.CONTENT_VISIBLE_URI
								.toString())) {
			return TaskList.CONTENT_TYPE;
		}
		else if (uri.toString().startsWith(
				LegacyDBHelper.NotePad.Notes.CONTENT_URI.toString())
				|| uri.toString().startsWith(
						LegacyDBHelper.NotePad.Notes.CONTENT_VISIBLE_URI
								.toString())) {
			return Task.CONTENT_TYPE;
		}

		throw new IllegalArgumentException("Unknown URI " + uri);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	synchronized public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = DatabaseHandler.getInstance(getContext())
				.getWritableDatabase();

		Uri result = null;

		db.beginTransaction();
		// Do not add legacy URIs
		try {
			final DAO item;
			switch (sURIMatcher.match(uri)) {
			case TaskList.BASEURICODE:
				item = new TaskList(values);
				break;
			case Task.BASEURICODE:
				item = new Task(values);
				break;
			case Notification.BASEURICODE:
			case Notification.WITHTASKQUERYITEMCODE:
				item = new Notification(values);
				break;
			case RemoteTaskList.BASEURICODE:
				item = new RemoteTaskList(values);
				break;
			case RemoteTask.BASEURICODE:
				item = new RemoteTask(values);
				break;
			default:
				throw new IllegalArgumentException(
						"Faulty insertURI provided: " + uri.toString());
			}

			result = item.insert(getContext(), db);
			db.setTransactionSuccessful();
		}
		catch (SQLException e) {
			// Crap...
		}
		finally {
			db.endTransaction();
		}

		if (result != null) {
			DAO.notifyProviderOnChange(getContext(), uri);
			DAO.notifyProviderOnChange(getContext(), TaskList.URI_WITH_COUNT);
			UpdateNotifier.updateWidgets(getContext());
            UpdateNotifier.notifyChangeList(getContext());
		}

		return result;
	}

	@Override
	synchronized public int update(Uri uri, ContentValues values,
			String selection, String[] selectionArgs) {
		final SQLiteDatabase db = DatabaseHandler.getInstance(getContext())
				.getWritableDatabase();
		int result = 0;
		final Task t;
		final SQLiteStatement stmt;
		final String sql;
        final ArrayList<Uri> updateUris = new ArrayList<Uri>();
		db.beginTransaction();

		try {
			// Do not add legacy URIs
			switch (sURIMatcher.match(uri)) {
			case TaskList.BASEITEMCODE:
                updateUris.add(TaskList.URI);
                updateUris.add(TaskList.URI_WITH_COUNT);
				final TaskList list = new TaskList(uri, values);
				result += db.update(TaskList.TABLE_NAME, list.getContent(),
						TaskList.whereIdIs(selection),
						TaskList.whereIdArg(list._id, selectionArgs));
				break;
			case Task.MOVEITEMLEFTCODE:
                updateUris.add(Task.URI);
				t = new Task(values);
				sql = t.getSQLMoveItemLeft(values);
				if (sql != null) {
					stmt = db.compileStatement(sql);
					result += stmt.executeUpdateDelete();
				}
				break;
			case Task.MOVEITEMRIGHTCODE:
                updateUris.add(Task.URI);
				t = new Task(values);
				sql = t.getSQLMoveItemRight(values);
				if (sql != null) {
					stmt = db.compileStatement(sql);
					result += stmt.executeUpdateDelete();
				}
				break;
			case Task.BASEITEMCODE:
                updateUris.add(Task.URI);
                updateUris.add(Task.URI_SECTIONED_BY_DATE);
                updateUris.add(Task.URI_TASK_HISTORY);
                updateUris.add(TaskList.URI);
                updateUris.add(TaskList.URI_WITH_COUNT);
				// regular update
				t = new Task(uri, values);
				if (t.getContent().size() > 0) {
					// Something changed in task

					result += db.update(Task.TABLE_NAME, t.getContent(),
							Task.whereIdIs(selection),
							Task.whereIdArg(t._id, selectionArgs));
				}
				break;
			case Task.BASEURICODE:
                updateUris.add(Task.URI);
                updateUris.add(TaskList.URI);
                updateUris.add(TaskList.URI_WITH_COUNT);
				// Batch. No checks made
				result += db.update(Task.TABLE_NAME, values, selection,
						selectionArgs);
				break;
			case Notification.BASEITEMCODE:
			case Notification.WITHTASKQUERYITEMCODE:
                updateUris.add(Notification.URI);
                updateUris.add(Notification.URI_WITH_TASK_PATH);
				// final Notification n = new Notification(uri, values);
				result += db.update(Notification.TABLE_NAME, values,
						Notification.whereIdIs(selection), Notification
								.whereIdArg(Long.parseLong(uri
										.getLastPathSegment()), selectionArgs));
				break;
			case Notification.BASEURICODE:
                updateUris.add(Notification.URI);
                updateUris.add(Notification.URI_WITH_TASK_PATH);
				// No checks
				result += db.update(Notification.TABLE_NAME, values, selection,
						selectionArgs);
				break;
			case RemoteTaskList.BASEITEMCODE:
                updateUris.add(RemoteTaskList.URI);
				result += db.update(RemoteTaskList.TABLE_NAME, values,
                        RemoteTaskList.whereIdIs(selection),
                        RemoteTaskList.whereIdArg(Long.parseLong(uri
                                .getLastPathSegment()), selectionArgs)
                );
                break;
                case RemoteTask.BASEITEMCODE:
                    updateUris.add(RemoteTask.URI);
                    result += db.update(RemoteTask.TABLE_NAME, values,
                            RemoteTask.whereIdIs(selection),
                            RemoteTask.whereIdArg(Long.parseLong(uri
                                    .getLastPathSegment()), selectionArgs)
                    );
                    break;
                default:
				throw new IllegalArgumentException("Faulty URI provided: "
						+ uri.toString());
			}

			if (result >= 0) {
				db.setTransactionSuccessful();
			}
		}
		finally {
			db.endTransaction();
		}

		if (result >= 0) {
            for (Uri u: updateUris) {
                DAO.notifyProviderOnChange(getContext(), u);
            }
			UpdateNotifier.updateWidgets(getContext());
            UpdateNotifier.notifyChangeList(getContext());
		}

		return result;
	}

	synchronized private int safeDeleteItem(final SQLiteDatabase db,
			final String tableName, final Uri uri, final String selection,
			final String[] selectionArgs) {
		db.beginTransaction();
		int result = 0;
		try {
			result += db.delete(
					tableName,
					DAO.whereIdIs(selection),
					DAO.joinArrays(selectionArgs,
							new String[] { uri.getLastPathSegment() }));
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
		}

		return result;
	}

	@Override
	synchronized public int delete(Uri uri, String selection,
			String[] selectionArgs) {
		final SQLiteDatabase db = DatabaseHandler.getInstance(getContext())
				.getWritableDatabase();
		int result = 0;
		// Do not add legacy URIs
		switch (sURIMatcher.match(uri)) {
		case TaskList.BASEITEMCODE:
			result += safeDeleteItem(db, TaskList.TABLE_NAME, uri, selection,
					selectionArgs);
			break;
		case TaskList.BASEURICODE:
			result += db.delete(TaskList.TABLE_NAME, selection, selectionArgs);
			break;
		case Task.BASEITEMCODE:
			result += safeDeleteItem(db, Task.TABLE_NAME, uri, selection,
					selectionArgs);
			break;
		case Task.BASEURICODE:
			result += db.delete(Task.TABLE_NAME, selection, selectionArgs);
			break;
		case Notification.BASEURICODE:
			result += db.delete(Notification.TABLE_NAME, selection,
					selectionArgs);
			break;
		case Notification.BASEITEMCODE:
		case Notification.WITHTASKQUERYITEMCODE:
			result += safeDeleteItem(db, Notification.TABLE_NAME, uri,
					selection, selectionArgs);
			break;
		case RemoteTaskList.BASEURICODE:
			result += db.delete(RemoteTaskList.TABLE_NAME, selection,
					selectionArgs);
			break;
		case RemoteTaskList.BASEITEMCODE:
			result += safeDeleteItem(db, RemoteTaskList.TABLE_NAME, uri,
					selection, selectionArgs);
			break;
		case RemoteTask.BASEURICODE:
			result += db
					.delete(RemoteTask.TABLE_NAME, selection, selectionArgs);
			break;
		case RemoteTask.BASEITEMCODE:
			result += safeDeleteItem(db, RemoteTask.TABLE_NAME, uri, selection,
					selectionArgs);
			break;
		case Task.DELETEDQUERYCODE:
			result += db.delete(Task.DELETE_TABLE_NAME, selection,
					selectionArgs);
			break;
		case Task.DELETEDITEMCODE:
			result += safeDeleteItem(db, Task.DELETE_TABLE_NAME, uri,
					selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Faulty delete-URI provided: "
					+ uri.toString());
		}

		if (result > 0) {
			DAO.notifyProviderOnChange(getContext(), uri);
			DAO.notifyProviderOnChange(getContext(), TaskList.URI_WITH_COUNT);
			UpdateNotifier.updateWidgets(getContext());
            UpdateNotifier.notifyChangeList(getContext());
		}
		return result;
	}

	@Override
	synchronized public Cursor query(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		Cursor result = null;
		final long id;
		// if (selection != null) Log.d("nononsenseapps", selection);
		// if (selectionArgs != null)
		// Log.d("nononsenseapps", DAO.arrayToCommaString(selectionArgs));
		switch (sURIMatcher.match(uri)) {
		case TaskList.BASEURICODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(TaskList.TABLE_NAME, projection, selection,
							selectionArgs, null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(),
					TaskList.URI);
			break;
		case TaskList.BASEITEMCODE:
			id = Long.parseLong(uri.getLastPathSegment());
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(TaskList.TABLE_NAME,
							projection,
							TaskList.whereIdIs(selection),
							TaskList.joinArrays(selectionArgs,
									new String[] { String.valueOf(id) }), null,
							null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case TaskList.VIEWCOUNTCODE:
			// Create view if not exists
			DatabaseHandler.getInstance(getContext()).getWritableDatabase()
					.execSQL(TaskList.CREATE_COUNT_VIEW);

			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(TaskList.VIEWCOUNT_NAME, projection, selection,
							selectionArgs, null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case Task.DELETEDQUERYCODE:
			final String[] query = sanitize(selectionArgs);
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.DELETE_TABLE_NAME,
							Task.Columns.DELETEFIELDS,
							Task.Columns._ID
									+ " IN (SELECT "
									+ Task.Columns._ID
									+ " FROM "
									+ Task.FTS3_DELETE_TABLE_NAME
									+ ((query[0].isEmpty() || query[0]
											.equals("'*'")) ? ")"
											: (" WHERE "
													+ Task.FTS3_DELETE_TABLE_NAME + " MATCH ?)")),
							(query[0].isEmpty() || query[0].equals("'*'")) ? null
									: query, null, null, sortOrder);

			result.setNotificationUri(getContext().getContentResolver(),
					Task.URI_DELETED_QUERY);
			break;
		case Task.BASEURICODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.TABLE_NAME, projection, selection,
							selectionArgs, null, null, sortOrder);

			result.setNotificationUri(getContext().getContentResolver(),
					Task.URI);
			break;
		case Task.BASEITEMCODE:
			id = Long.parseLong(uri.getLastPathSegment());
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.TABLE_NAME,
							projection,
							Task.whereIdIs(selection),
							Task.joinArrays(selectionArgs,
									new String[] { String.valueOf(id) }), null,
							null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case Task.SECTIONEDDATEQUERYCODE:
			// Add list null because that's what the headers will have
			final String listId;
			if (selectionArgs == null || selectionArgs.length == 0) {
				listId = null;
				// throw new SQLException(
				// "Need a listid as first arg at the moment for this view!");
			}
			else {
				listId = selectionArgs[0];
			}
			// Create view if not exists
			DatabaseHandler.getInstance(getContext()).getWritableDatabase()
					.execSQL(Task.CREATE_SECTIONED_DATE_VIEW(listId));

			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.getSECTION_DATE_VIEW_NAME(listId),
							projection,
							selection,
							selectionArgs,
							null,
							null,
							Task.SECRET_TYPEID + "," + Task.Columns.DUE + ","
									+ Task.SECRET_TYPEID2);

			result.setNotificationUri(getContext().getContentResolver(),
					Task.URI);
			break;
		case Task.HISTORYQUERYCODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.HISTORY_TABLE_NAME, projection, selection,
							selectionArgs, null, null,
							Task.Columns.UPDATED + " ASC");
			// SQLite timestamp in updated column.

			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case Notification.BASEITEMCODE:
			id = Long.parseLong(uri.getLastPathSegment());
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Notification.TABLE_NAME,
							projection,
							Notification.whereIdIs(selection),
							Notification.joinArrays(selectionArgs,
									new String[] { String.valueOf(id) }), null,
							null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case Notification.WITHTASKQUERYITEMCODE:
			// Create view if not exists
			DatabaseHandler.getInstance(getContext()).getWritableDatabase()
					.execSQL(Notification.CREATE_JOINED_VIEW);
			id = Long.parseLong(uri.getLastPathSegment());
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Notification.WITH_TASK_VIEW_NAME,
							projection,
							Notification.whereIdIs(selection),
							Notification.joinArrays(selectionArgs,
									new String[] { String.valueOf(id) }), null,
							null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case Notification.BASEURICODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Notification.TABLE_NAME, projection, selection,
							selectionArgs, null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case Notification.WITHTASKQUERYCODE:
			// Create view if not exists
			DatabaseHandler.getInstance(getContext()).getWritableDatabase()
					.execSQL(Notification.CREATE_JOINED_VIEW);
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Notification.WITH_TASK_VIEW_NAME, projection,
							selection, selectionArgs, null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case RemoteTaskList.BASEURICODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(RemoteTaskList.TABLE_NAME, projection, selection,
							selectionArgs, null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case RemoteTask.BASEURICODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(RemoteTask.TABLE_NAME, projection, selection,
							selectionArgs, null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case Task.SEARCHCODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.TABLE_NAME,
							Task.Columns.FIELDS,
							Task.Columns._ID + " IN (SELECT "
									+ Task.Columns._ID + " FROM "
									+ Task.FTS3_TABLE_NAME + " WHERE "
									+ Task.FTS3_TABLE_NAME + " MATCH ?)",
							sanitize(selectionArgs), null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(),
					Task.URI_SEARCH);
			break;

		case TaskList.LEGACYBASEURICODE:
		case TaskList.LEGACYVISIBLEURICODE:
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(TaskList.TABLE_NAME,
							LegacyDBHelper.convertLegacyColumns(projection),
							null, null, null, null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(),
					TaskList.URI);
			break;
		case TaskList.LEGACYBASEITEMCODE:
		case TaskList.LEGACYVISIBLEITEMCODE:
			id = Long.parseLong(uri.getLastPathSegment());
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(TaskList.TABLE_NAME,
							LegacyDBHelper.convertLegacyColumns(projection),
							TaskList.whereIdIs(selection),
							TaskList.joinArrays(selectionArgs,
									new String[] { String.valueOf(id) }), null,
							null, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(),
					TaskList.getUri(id));
			break;
		case Task.LEGACYBASEURICODE:
		case Task.LEGACYVISIBLEURICODE:
			final Cursor c = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.TABLE_NAME,
							LegacyDBHelper.convertLegacyColumns(projection),
							null, null, null, null, Task.Columns.DUE);
			result = new MatrixCursor(projection);
			while (c.moveToNext()) {
				((MatrixCursor) result).addRow(LegacyDBHelper
						.convertLegacyTaskValues(c));
			}
			c.close();

			result.setNotificationUri(getContext().getContentResolver(),
					Task.URI);
			break;
		case Task.SEARCHSUGGESTIONSCODE:
			final String limit = uri
					.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
			result = DatabaseHandler
					.getInstance(getContext())
					.getReadableDatabase()
					.query(Task.FTS3_TABLE_NAME,
							new String[] {
									Task.Columns._ID,
									Task.Columns._ID
											+ " AS "
											+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
									Task.Columns.TITLE
											+ " AS "
											+ SearchManager.SUGGEST_COLUMN_TEXT_1,
									Task.Columns.NOTE
											+ " AS "
											+ SearchManager.SUGGEST_COLUMN_TEXT_2 },
							Task.FTS3_TABLE_NAME + " MATCH ?",
							sanitize(selectionArgs), null, null,
							SearchManager.SUGGEST_COLUMN_TEXT_1, limit);
			result.setNotificationUri(getContext().getContentResolver(),
					Task.URI_SEARCH);
			break;
		// These legacy URIs will not be supported
		case Task.LEGACYBASEITEMCODE:
		case Task.LEGACYVISIBLEITEMCODE:
		default:

			Log.d("nononsenseapps db",
					"Faulty queryURI provided: " + uri.toString());
			return null;

			// throw new IllegalArgumentException("Faulty queryURI provided: "
			// + uri.toString());
		}

		return result;
	}

	private String[] sanitize(final String... args) {
		if (args.length == 0) return new String[] { "" };

		final StringBuilder result = new StringBuilder();
		for (String query : args) {
			// for (String part : query.split("\\s")) {
			if (result.length() > 0) result.append(" AND ");
			// Wrap each word in quotes and add star to the end
			result.append("'" + query + "*'");
			// }
		}

		return new String[] { result.toString() };
	}

}
