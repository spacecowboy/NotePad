package com.nononsenseapps.notepad.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public abstract class DAO {

	public static final String whereIdIs = "" + BaseColumns._ID + " IS ?";

	public String[] whereIdArg() {
		return new String[] { Long.toString(_id) };
	}

	public Uri getUri() {
		return Uri.withAppendedPath(getBaseUri(), Long.toString(_id));
	}

	public Uri getBaseUri() {
		return Uri.withAppendedPath(
				Uri.parse(MyContentProvider.SCHEME
						+ MyContentProvider.AUTHORITY), getTableName());
	}

	public long _id = -1;

	public synchronized boolean update(final Context context,
			final SQLiteDatabase db) {
		int result = 0;
		db.beginTransaction();

		try {

			if (_id > 0) {
				result += db.update(getTableName(), getContent(),
						whereIdIs,
						whereIdArg());
			}

			if (result > 0) {
				db.setTransactionSuccessful();
			}

		} catch (SQLException e) {
			throw e;
		} finally {
			db.endTransaction();
		}

		if (result > 0) {
			notifyProviderOnChange(context);
		}

		return result > 0;
	}

	public synchronized Uri insert(final Context context,
			final SQLiteDatabase db) {
		Uri retval = null;
		db.beginTransaction();
		try {
			beforeInsert(context, db);

			final long id = db.insert(getTableName(), null, getContent());

			if (id == -1) {
				throw new SQLException("Insert failed in " + getTableName());
			} else {
				_id = id;
				afterInsert(context, db);
				db.setTransactionSuccessful();
				retval = getUri();
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			db.endTransaction();
		}

		if (retval != null) {
			notifyProviderOnChange(context);
		}
		return retval;
	}

	public synchronized int remove(final Context context,
			final SQLiteDatabase db) {
		final int result = db.delete(getTableName(), BaseColumns._ID + " IS ?",
				new String[] { Long.toString(_id) });

		if (result > 1) {
			notifyProviderOnChange(context);
		}

		return result;
	}

	public static void notifyProviderOnChange(final Context context,
			final Uri uri) {
		try {
			context.getContentResolver().notifyChange(uri, null, false);
		} catch (UnsupportedOperationException e) {
			// Catch this for test suite. Mock provider cant notify
		}
	}

	protected void notifyProviderOnChange(final Context context) {
		notifyProviderOnChange(context, getUri());
	}

	public void setId(final Uri uri) {
		_id = Long.parseLong(uri.getLastPathSegment());
	}
	
	protected static String arrayToCommaString(final String pfx, final String[] items, final String sfx) {
		String result = "";
		for (final String item: items) {
			result += "," + pfx + item + sfx;
		}
		
		// Ignore first comma
		return result.substring(1);
	}

	protected void beforeInsert(final Context context, final SQLiteDatabase db) {

	}

	protected void afterInsert(final Context context, final SQLiteDatabase db) {

	}

	protected void beforeUpdate(final Context context, final SQLiteDatabase db) {

	}

	protected void afterUpdate(final Context context, final SQLiteDatabase db) {

	}

	protected void beforeRemove(final Context context, final SQLiteDatabase db) {

	}

	protected void afterRemove(final Context context, final SQLiteDatabase db) {

	}

	protected DAO(final Cursor c) {
	}

	protected DAO(final ContentValues values) {

	}

	protected DAO() {

	}

	public abstract ContentValues getContent();

	protected abstract String getTableName();
	
	public abstract String getContentType();
}
