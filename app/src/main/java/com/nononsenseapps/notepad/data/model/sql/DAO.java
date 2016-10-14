/*
 * Copyright (c) 2015. Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.data.model.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.nononsenseapps.notepad.data.local.sql.MyContentProvider;

import java.util.ArrayList;

public abstract class DAO {

	private static final String whereIdIs = "" + BaseColumns._ID + " IS ?";

	/**
	 * Append where is id ? to string
	 */
	public static String whereIdIs(final String orgWhere) {
		final StringBuilder sb = new StringBuilder();
		if (orgWhere != null) {
			sb.append("(");
			sb.append(orgWhere);
			sb.append(") AND ");
		}
		sb.append(BaseColumns._ID).append(" IS ?");
		return sb.toString();
	}

	public String[] whereIdArg() {
		return new String[] { Long.toString(_id) };
	}

	public static String[] whereIdArg(final long _id) {
		return new String[] { Long.toString(_id) };
	}

	/**
	 * Append the id argument to array
	 */
	public static String[] whereIdArg(final long _id,
			final String[] orgWhereArgs) {
		if (orgWhereArgs == null) {
			return whereIdArg(_id);
		}
		else {
			return joinArrays(orgWhereArgs, whereIdArg(_id));
		}
	}

	public static String[] prefixArray(final String prefix, final String[] array) {
		final String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = "" + prefix + array[i];
		}
		return result;
	}

	public static String[] joinArrays(final String[]... arrays) {
		final ArrayList<String> list = new ArrayList<String>();
		for (final String[] array : arrays) {
			if (array != null) {
				for (final String txt : array) {
					list.add(txt);
				}
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Example: [] -> "" [a] -> "a" [a, b] -> "a,b"
	 */
	// public static String arrayToCommaString(final String[] array) {
	// return arrayToCommaString("", array);
	// }

	public static String arrayToCommaString(final long... array) {
		StringBuilder result = new StringBuilder();
		for (final long val : array) {
			final String txt = Long.toString(val);
			if (result.length() > 0) result.append(",");
			result.append(txt);
		}
		return result.toString();
	}

	public static String arrayToCommaString(final String... array) {
		return arrayToCommaString("", array);
	}

	/**
	 * Example (prefix=t.): [] -> "" [a] -> "t.a" [a, b] -> "t.a,t.b"
	 */
	public static String arrayToCommaString(final String prefix,
			final String[] array) {
		return arrayToCommaString(prefix, array, "");
	}

	/**
	 * Example (prefix=t., suffix=.45): [] -> "" [a] -> "t.a.45" [a, b] ->
	 * "t.a.45,t.b.45"
	 * 
	 * In addition, the txt itself can be referenced using %1$s in either prefix
	 * or suffix. The prefix can be referenced as %2$s in suffix, and
	 * vice-versa.
	 * 
	 * So the following is valid:
	 * 
	 * (prefix='t.', suffix=' AS %2$s%1$s')
	 * 
	 * [listId] -> t.listId AS t.listId
	 */
	protected static String arrayToCommaString(final String pfx,
			final String[] array, final String sfx) {
		StringBuilder result = new StringBuilder();
		for (final String txt : array) {
			if (result.length() > 0) result.append(",");
			result.append(String.format(pfx, txt, sfx));
			result.append(txt);
			result.append(String.format(sfx, txt, pfx));
		}
		return result.toString();
	}

	/**
	 * Second and Third value is wrapped in '' ticks, NOT the first.
	 */
	protected static String asEmptyCommaStringExcept(final String[] asColumns,
			final String exceptCol1, final String asValue1,
			final String exceptCol2, final String asValue2,
			final String exceptCol3, final String asValue3) {
		StringBuilder result = new StringBuilder();
		for (final String colName : asColumns) {
			if (result.length() > 0) result.append(",");

			if (colName.equals(exceptCol2)) {
				result.append("'").append(asValue2).append("'");
			}
			else if (colName.equals(exceptCol3)) {
				result.append("'").append(asValue3).append("'");
			}
			else if (colName.equals(exceptCol1)) {
				result.append(asValue1);
			}
			else {
				result.append("null");
			}
		}
		return result.toString();
	}

	/**
	 * Third and Fourth value is wrapped in '' ticks, NOT the first and second.
	 */
	protected static String asEmptyCommaStringExcept(final String[] asColumns,
			final String exceptCol1, final String asValue1,
			final String exceptCol2, final String asValue2,
			final String exceptCol3, final String asValue3,
			final String exceptCol4, final String asValue4) {
		StringBuilder result = new StringBuilder();
		for (final String colName : asColumns) {
			if (result.length() > 0) result.append(",");

			if (colName.equals(exceptCol3)) {
				result.append("'").append(asValue3).append("'");
			}
			else if (colName.equals(exceptCol4)) {
				result.append("'").append(asValue4).append("'");
			}
			else if (colName.equals(exceptCol2)) {
				result.append(asValue2);
			}
			else if (colName.equals(exceptCol1)) {
				result.append(asValue1);
			}
			else {
				result.append("null");
			}
		}
		return result.toString();
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
				result += db.update(getTableName(), getContent(), whereIdIs,
						whereIdArg());
			}

			if (result > 0) {
				db.setTransactionSuccessful();
			}

		}
		catch (SQLException e) {
			throw e;
		}
		finally {
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
			}
			else {
				_id = id;
				afterInsert(context, db);
				db.setTransactionSuccessful();
				retval = getUri();
			}
		}
		catch (SQLException e) {
			throw e;
		}
		finally {
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
		}
		catch (UnsupportedOperationException e) {
			// Catch this for test suite. Mock provider cant notify
		}
	}

	protected void notifyProviderOnChange(final Context context) {
		notifyProviderOnChange(context, getUri());
	}

	public void setId(final Uri uri) {
		_id = Long.parseLong(uri.getLastPathSegment());
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

	/**
	 * Convenience method for normal operations. Updates "updated" field.
	 * Returns number of db-rows affected. Fail if < 1
	 */
	public abstract int save(final Context context);

	/**
	 * Delete object from database
	 */
	public int delete(final Context context) {
		if (_id > 0) {
            return context.getContentResolver().delete(getUri(), null, null);
        } else {
            return 0;
        }
	}
}
