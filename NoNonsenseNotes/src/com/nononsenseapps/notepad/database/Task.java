package com.nononsenseapps.notepad.database;

import java.security.InvalidParameterException;
import java.util.Calendar;

import com.googlecode.androidannotations.annotations.rest.Post;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.text.format.Time;
import android.util.Log;

/**
 * An object that represents the task information contained in the database.
 * Provides convenience methods for moving and indenting items.
 */
public class Task extends DAO {

	// SQL convention says Table name should be "singular"
	public static final String TABLE_NAME = "task";
	public static final String DELETE_TABLE_NAME = "deleted_task";

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.note";

	public static final Uri URI = Uri.withAppendedPath(
			Uri.parse(MyContentProvider.SCHEME + MyContentProvider.AUTHORITY),
			TABLE_NAME);

	public static Uri getUri(final long id) {
		return Uri.withAppendedPath(URI, Long.toString(id));
	}

	public static final int BASEURICODE = 201;
	public static final int BASEITEMCODE = 202;
	public static final int INDENTEDQUERYCODE = 203;
	public static final int MOVESUBTREECODE = 204;
	public static final int INDENTCODE = 205;
	public static final int INDENTITEMCODE = 206;
	public static final int UNINDENTCODE = 207;
	public static final int UNINDENTITEMCODE = 208;
	public static final int DELETEDQUERYCODE = 209;
	public static final int DELETEDITEMCODE = 210;
	// Legacy support, these also need to use legacy projections
	public static final int LEGACYBASEURICODE = 221;
	public static final int LEGACYBASEITEMCODE = 222;
	public static final int LEGACYVISIBLEURICODE = 223;
	public static final int LEGACYVISIBLEITEMCODE = 224;

	public static void addMatcherUris(UriMatcher sURIMatcher) {
		sURIMatcher
				.addURI(MyContentProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/#",
				BASEITEMCODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ INDENTEDQUERY, INDENTEDQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ MOVESUBTREE + "/#", MOVESUBTREECODE);

		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ INDENT, INDENTCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ INDENT + "/#", INDENTITEMCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ UNINDENT, UNINDENTCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ UNINDENT + "/#", UNINDENTITEMCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ DELETEDQUERY, DELETEDQUERYCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY, TABLE_NAME + "/"
				+ DELETEDQUERY + "/#", DELETEDITEMCODE);

		// Legacy URIs
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.PATH_NOTES, LEGACYBASEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.PATH_NOTES + "/#",
				LEGACYBASEITEMCODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.PATH_VISIBLE_NOTES,
				LEGACYVISIBLEURICODE);
		sURIMatcher.addURI(MyContentProvider.AUTHORITY,
				LegacyDBHelper.NotePad.Notes.PATH_VISIBLE_NOTES + "/#",
				LEGACYVISIBLEITEMCODE);
	}

	// Used in indented query
	public static final String INDENT = "indent";
	public static final String UNINDENT = "unindent";
	public static final String TARGETLEFT = "targetleft";
	public static final String TARGETRIGHT = "targetright";

	private static final String INDENTEDQUERY = "indentedquery";
	private static final String MOVESUBTREE = "movesubtree";
	private static final String DELETEDQUERY = "deletedquery";

	// Special URI to look at backup table
	public static final Uri URI_DELETED_QUERY = Uri.withAppendedPath(URI,
			DELETEDQUERY);

	// Special URI where the last column will be a count
	public static final Uri URI_INDENTED_QUERY = Uri.withAppendedPath(URI,
			INDENTEDQUERY);

	// Special URI to use when a move is requested
	public static final Uri URI_WRITE_MOVESUBTREE = Uri.withAppendedPath(URI,
			MOVESUBTREE);

	public Uri getMoveSubTreeUri() {
		if (_id < 1) {
			throw new InvalidParameterException(
					"_ID of this object is not valid");
		}
		return Uri.withAppendedPath(URI_WRITE_MOVESUBTREE, Long.toString(_id));
	}

	// Special URI to use when an indent action is requested
	public static final Uri URI_WRITE_INDENT = Uri
			.withAppendedPath(URI, INDENT);

	public Uri getIndentUri() {
		if (_id < 1) {
			throw new InvalidParameterException(
					"_ID of this object is not valid");
		}
		return Uri.withAppendedPath(URI_WRITE_INDENT, Long.toString(_id));
	}

	// Special URI to use when an unindent action is requested
	public static final Uri URI_WRITE_UNINDENT = Uri.withAppendedPath(URI,
			UNINDENT);

	public Uri getUnIndentUri() {
		if (_id < 1) {
			throw new InvalidParameterException(
					"_ID of this object is not valid");
		}
		return Uri.withAppendedPath(URI_WRITE_UNINDENT, Long.toString(_id));
	}

	public static class Columns implements BaseColumns {

		private Columns() {
		}

		public static final String TITLE = "title";
		public static final String NOTE = "note";
		public static final String DBLIST = "dblist";
		public static final String COMPLETED = "completed";
		public static final String DUE = "due";
		public static final String UPDATED = "updated";

		public static final String LEFT = "lft";
		public static final String RIGHT = "rgt";

		public static final String GTASKID = "gtaskid";
		public static final String GTASKACCOUNT = "gtaskaccount";

		public static final String DROPBOXID = "dropboxid";
		public static final String DROPBOXACCOUNT = "dropboxaccount";

		public static final String[] FIELDS = { _ID, TITLE, NOTE, COMPLETED,
				DUE, UPDATED, LEFT, RIGHT, DBLIST, GTASKACCOUNT, GTASKID,
				DROPBOXACCOUNT, DROPBOXID };
		public static final String[] DELETEFIELDS = { _ID, TITLE, NOTE,
				COMPLETED, DUE, DBLIST };
		// Same but no ID
		private static final String[] DELETEFIELDS_TRIGGER = { TITLE, NOTE,
				COMPLETED, DUE, DBLIST };

	}

	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ "(" + Columns._ID + " INTEGER PRIMARY KEY," + Columns.TITLE
			+ " TEXT NOT NULL DEFAULT ''," + Columns.NOTE
			+ " TEXT NOT NULL DEFAULT '',"
			// These are all msec times
			+ Columns.COMPLETED
			+ " INTEGER DEFAULT NULL,"
			+ Columns.UPDATED
			+ " INTEGER DEFAULT NULL,"
			+ Columns.DUE
			+ " INTEGER DEFAULT NULL,"

			// sync stuff
			+ Columns.GTASKACCOUNT + " TEXT," + Columns.GTASKID
			+ " TEXT,"
			+ Columns.DROPBOXACCOUNT
			+ " TEXT,"
			+ Columns.DROPBOXID
			+ " TEXT,"

			// position stuff
			+ Columns.LEFT + " INTEGER NOT NULL DEFAULT 1,"
			+ Columns.RIGHT
			+ " INTEGER NOT NULL DEFAULT 2,"
			+ Columns.DBLIST
			+ " INTEGER NOT NULL,"

			// Positions must be positive and ordered!
			+ " CHECK(" + Columns.LEFT + " > 0), " + " CHECK("
			+ Columns.RIGHT
			+ " > 0), "
			// Each side's value should be unique in it's list
			// Handled in trigger
			//+ " UNIQUE(" + Columns.LEFT + ", " + Columns.DBLIST + ")"
			//+ " UNIQUE(" + Columns.RIGHT + ", " + Columns.DBLIST + ")"

			// Foreign key for list
			+ "FOREIGN KEY(" + Columns.DBLIST + ") REFERENCES "
			+ TaskList.TABLE_NAME + "(" + TaskList.Columns._ID
			+ ") ON DELETE CASCADE" + ")";

	// Delete table has no constraints. In fact, list values and positions
	// should not even be thought of as valid.
	public static final String CREATE_DELETE_TABLE = "CREATE TABLE "
			+ DELETE_TABLE_NAME + "(" + Columns._ID + " INTEGER PRIMARY KEY,"
			+ Columns.TITLE + " TEXT NOT NULL DEFAULT ''," + Columns.NOTE
			+ " TEXT NOT NULL DEFAULT ''," + Columns.COMPLETED + " TEXT,"
			+ Columns.DUE + " TEXT," + Columns.DBLIST + " INTEGER)";

	public String title = null;
	public String note = null;
	// All milliseconds since 1970-01-01 UTC
	public Long completed = null;
	public Long due = null;
	public Long updated = null;
	// Sync stuff
	public String gtaskaccount = null;
	public String gtaskid = null;
	public String dropboxaccount = null;
	public String dropboxid = null;

	// position stuff
	public Long left = null;
	public Long right = null;
	public Long dblist = null;

	// A calculated value. Must use indented uri
	// for this to be accurate
	// It is read-only
	public int level = 0;

	public Task() {

	}

	/**
	 * Set task as completed. Returns the time stamp that is set.
	 */
	public Long setAsCompleted() {

		final Time time = new Time(Time.TIMEZONE_UTC);
		time.setToNow();
		completed = new Time().toMillis(false);
		return completed;
	}

	/**
	 * Set first line as title, rest as note.
	 * 
	 * @param text
	 */
	public void setText(final String text) {
		int titleEnd = text.indexOf("\n");

		if (titleEnd < 0) {
			titleEnd = text.length();
		}

		title = text.substring(0, titleEnd);
		if (titleEnd + 1 < text.length()) {
			note = text.substring(titleEnd + 1, text.length());
		}
		else {
			note = "";
		}
	}

	/**
	 * Returns a text where first line is title, rest is note
	 */
	public String getText() {
		String result = "";
		if (title != null) {
			result += title;
		}
		if (note != null && !note.isEmpty()) {
			if (result.length() > 0) {
				result += "\n";
			}
			result += note;
		}
		return result;
	}

	public Task(final Cursor c) {
		this._id = c.getLong(0);
		this.title = c.getString(1);
		note = c.getString(2);
		// msec times which can be null
		if (!c.isNull(3))
			completed = c.getLong(3);
		if (!c.isNull(4))
			due = c.getLong(4);
		if (!c.isNull(5))
			updated = c.getLong(5);
		
		// enforced not to be null
		left = c.getLong(6);
		right = c.getLong(7);
		dblist = c.getLong(8);

		gtaskaccount = c.getString(9);
		gtaskid = c.getString(10);
		dropboxaccount = c.getString(11);
		dropboxid = c.getString(12);

		if (c.getColumnCount() > Columns.FIELDS.length) {
			level = c.getInt(Columns.FIELDS.length);
		}
	}

	public Task(final long id, final ContentValues values) {
		this(values);
		this._id = id;
	}

	public Task(final Uri uri, final ContentValues values) {
		this(Long.parseLong(uri.getLastPathSegment()), values);
	}

	public Task(final ContentValues values) {
		if (values != null) {
			this.title = values.getAsString(Columns.TITLE);
			this.note = values.getAsString(Columns.NOTE);
			this.completed = values.getAsLong(Columns.COMPLETED);
			this.due = values.getAsLong(Columns.DUE);
			this.updated = values.getAsLong(Columns.UPDATED);

			gtaskaccount = values.getAsString(Columns.GTASKACCOUNT);
			gtaskid = values.getAsString(Columns.GTASKID);
			dropboxaccount = values.getAsString(Columns.DROPBOXACCOUNT);
			dropboxid = values.getAsString(Columns.DROPBOXID);

			this.dblist = values.getAsLong(Columns.DBLIST);
			this.left = values.getAsLong(Columns.LEFT);
			this.right = values.getAsLong(Columns.RIGHT);
		}
	}

	/**
	 * A move operation should be performed alone. No other information should
	 * accompany such an update.
	 */
	public ContentValues getMoveValues(final long targetLeft,
			final long targetRight) {
		final ContentValues values = new ContentValues();
		values.put(TARGETLEFT, targetLeft);
		values.put(TARGETRIGHT, targetRight);
		values.put(Columns.LEFT, left);
		values.put(Columns.RIGHT, right);
		values.put(Columns.DBLIST, dblist);
		return values;
	}

	/**
	 * An indent operation should be performed alone. No other information
	 * should accompany such an update.
	 */
	public ContentValues getIndentValues() {
		final ContentValues values = new ContentValues();
		// values.put(INDENT, true);
		values.put(Columns.LEFT, left);
		values.put(Columns.RIGHT, right);
		values.put(Columns.DBLIST, dblist);
		return values;
	}

	/**
	 * Use this for regular updates of the task.
	 */
	@Override
	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		// Note that ID is NOT included here
		if (title != null) values.put(Columns.TITLE, title);
		if (note != null) values.put(Columns.NOTE, note);

		if (dblist != null) values.put(Columns.DBLIST, dblist);
		if (left != null) values.put(Columns.LEFT, left);
		if (right != null) values.put(Columns.RIGHT, right);

		values.put(Columns.UPDATED, updated);
		values.put(Columns.DUE, due);
		values.put(Columns.COMPLETED, completed);

		values.put(Columns.GTASKACCOUNT, gtaskaccount);
		values.put(Columns.GTASKID, gtaskid);
		values.put(Columns.DROPBOXACCOUNT, dropboxaccount);
		values.put(Columns.DROPBOXID, dropboxid);

		return values;
	}
	
	/**
	 * Compares this task to another and returns true if their contents are the same.
	 * Content is defined as: title, note, duedate, completed != null
	 * Returns false if title or note are null.
	 * 
	 * The intended usage is the editor where content and not id's or position are of
	 * importance.
	 */
	@Override
	public boolean equals(Object o) {
		boolean result = false;
		
		if (o instanceof Task) {
			final Task other = (Task) o;
			result = true;
			
			result &= (title != null && title.equals(other.title));
			result &= (note != null && note.equals(other.note));
			result &= (due == other.due);
			result &= ((completed != null) == (other.completed != null));
			
		} else {
			result = super.equals(o);
		}
		
		return result;
	}
	
	/**
	 * Convenience method for normal operations. Updates "updated" field.
	 * Returns number of db-rows affected. Fail if < 1
	 */
	public int save(final Context context) {
		int result = 0;
		updated = Calendar.getInstance().getTimeInMillis();
		if (_id < 1) {
			final Uri uri = context.getContentResolver().insert(
				getBaseUri(),getContent());
			if (uri != null) {
				_id = Long.parseLong(uri.getLastPathSegment());
				result++;
			}
		}
		else {
			result += context.getContentResolver().update(
					getUri(),getContent(), null, null);
		}
		return result;
	}
	
	/**
	 * Convenience method to complete tasks in list view for example.
	 * Starts an asynctask to do the operation in the background.
	 */
	public static void setCompleted(final Context context, final boolean completed, final Long... ids) {
		// TODO
		if (ids.length > 0) {
		final AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {
			@Override
			protected Void doInBackground(final Long... ids) {
				final ContentValues values = new ContentValues();
				values.put(Columns.COMPLETED, 
						completed ? Calendar.getInstance().getTimeInMillis() : null);
				String idStrings = "(";
				for (Long id: ids) {
					idStrings += id + ",";
				}
				idStrings = idStrings.substring(0, idStrings.length() - 1);
				idStrings += ")";
				Log.d("JONAS", "where: " + Columns._ID + " IN " + idStrings);
				context.getContentResolver().update(URI, values, Columns._ID + " IN " + idStrings, null);
				return null;
			}
		};
		task.execute(ids);
		}
	}

	@Override
	protected String getTableName() {
		return TABLE_NAME;
	}
	
	// TODO trigger pre-update, make room if list changes
		// TODO trigger post-update, get rid of space if list changed
		
		/**
		 * Can't use unique constraint on positions because SQLite checks constraints
		 * after every row is updated an not after each statement like it should.
		 * So have to do the check in a trigger instead.
		 */
		//TODO
		static final String countVals(final String col, final String ver) {
			return String.format("SELECT COUNT(DISTINCT %2$s)" +
				" AS ColCount FROM %1$s WHERE %3$s=%4$s.%3$s", TABLE_NAME, col, Columns.DBLIST, ver);
		}

		// verify that left are unique
		// count number of id and compare to number of left and right
		static final String posUniqueConstraint(final String ver, final String msg) { 
			return String.format(
				" SELECT CASE WHEN ((%1$s) != (%2$s) OR (%1$s) != (%3$s)) THEN "
			    + " RAISE (ABORT, '" + msg + "')"
			    + " END;", countVals(Columns._ID, ver), countVals(Columns.LEFT, ver),
				countVals(Columns.RIGHT, ver));
		};
		
		//public static final String TRIGGER_POST_UPDATE = String.format(
		//		"CREATE TRIGGER task_post_update AFTER UPDATE ON %1$s BEGIN "
		//		+ posUniqueConstraint("new", "pos not unique post update")
		//		+ posUniqueConstraint("old", "pos not unique post update")
		//		+ " END;", TABLE_NAME);

	// Makes a gap in the list where the task is being inserted
	private static final String BUMP_TO_RIGHT = " UPDATE %1$s SET %2$s = %2$s + 2, %3$s = %3$s + 2 WHERE %3$s >= new.%3$s AND %4$s IS new.%4$s;";
	public static final String TRIGGER_PRE_INSERT = String.format(
			"CREATE TRIGGER task_pre_insert BEFORE INSERT ON %s BEGIN ",
			TABLE_NAME)
			+ String.format(BUMP_TO_RIGHT, TABLE_NAME, Columns.RIGHT,
					Columns.LEFT, Columns.DBLIST) 
					+ " END;";
	
	public static final String TRIGGER_POST_INSERT = String.format(
			"CREATE TRIGGER task_post_insert AFTER INSERT ON %s BEGIN ",
			TABLE_NAME)
					// Enforce integrity
					+ posUniqueConstraint("new", "pos not unique post insert")
					
					+ " END;";

	// Upgrades children and closes the gap made from the delete
	private static final String BUMP_TO_LEFT = " UPDATE %1$s SET %2$s = %2$s - 2 WHERE %2$s > old.%3$s AND %4$s IS old.%4$s;";
	private static final String UPGRADE_CHILDREN = " UPDATE %1$s SET %2$s = %2$s - 1, %3$s = %3$s - 1 WHERE %4$s IS old.%4$s AND %2$s BETWEEN old.%2$s AND old.%3$s;";
	public static final String TRIGGER_POST_DELETE = String.format(
			"CREATE TRIGGER task_post_delete AFTER DELETE ON %s BEGIN ",
			TABLE_NAME)
			+ String.format(UPGRADE_CHILDREN, TABLE_NAME, Columns.LEFT,
					Columns.RIGHT, Columns.DBLIST)
			+ String.format(BUMP_TO_LEFT, TABLE_NAME, Columns.RIGHT,
					Columns.RIGHT, Columns.DBLIST)
			+ String.format(BUMP_TO_LEFT, TABLE_NAME, Columns.LEFT,
					Columns.RIGHT, Columns.DBLIST) 
					
				// Enforce integrity
					+ posUniqueConstraint("old", "pos not unique post delete")
					
					+ " END;";

	public static final String TRIGGER_PRE_DELETE = String.format(
			"CREATE TRIGGER task_pre_delete BEFORE DELETE ON %1$s BEGIN "
					+ " INSERT INTO %2$s ("
					+ arrayToCommaString("", Columns.DELETEFIELDS_TRIGGER, "")
					+ ") "
					+ " VALUES("
					+ arrayToCommaString("old.", Columns.DELETEFIELDS_TRIGGER,
							"") + "); "

					+ " END;", TABLE_NAME, DELETE_TABLE_NAME);

	public static String getSQLIndentedQuery(final String[] fields) {
		return String.format("SELECT " + arrayToCommaString("T2.", fields, "")
				+ ", COUNT(T1.%4$s) AS %5$s " + " FROM %1$s AS T1, %1$s AS T2 "
				+ " WHERE T2.%2$s BETWEEN T1.%2$s AND T1.%3$s " +
				// Limit to list
				" AND T2.%6$s IS ? AND T1.%6$s IS ? "
				// Count requires group
				+ " GROUP BY T2.%4$s " +
				// Sort on left
				" ORDER BY T2.%2$s;",

		TABLE_NAME, Columns.LEFT, Columns.RIGHT, Columns._ID, INDENT,
				Columns.DBLIST);
	}

	public boolean shouldMove(final ContentValues values) {
		return values.containsKey(TARGETLEFT)
				&& values.containsKey(TARGETRIGHT) && left != null
				&& right != null && dblist != null
				&& values.getAsLong(TARGETLEFT) > 0
				&& values.getAsLong(TARGETRIGHT) > values.getAsLong(TARGETLEFT);
	}

	public int move(final ContentResolver resolver, final long targetLeft,
			final long targetRight) {
		return resolver.update(getMoveSubTreeUri(),
				getMoveValues(targetLeft, targetRight), null, null);
	}

	public boolean shouldIndent() {
		return left != null && right != null && dblist != null;
	}

	public int indent(final ContentResolver resolver) {
		return resolver.update(getIndentUri(), getIndentValues(), null, null);
	}

	public static int indentMany(final ContentResolver resolver,
			final long[] idList) {
		if (idList.length < 1) {
			throw new InvalidParameterException("Must give some ids to indent");
		}
		// Ex: WHERE _id IN (15,27,2,94)
		String whereClause = Columns._ID + " IN (";
		for (long id : idList) {
			whereClause += Long.toString(id) + ",";
		}
		whereClause = whereClause.substring(0, whereClause.length() - 1) + ")";
		return resolver.update(URI_WRITE_INDENT, new ContentValues(),
				whereClause, null);
	}

	public int unindent(final ContentResolver resolver) {
		return resolver.update(getUnIndentUri(), getIndentValues(), null, null);
	}

	public static int unIndentMany(final ContentResolver resolver,
			final long[] idList) {
		if (idList.length < 1) {
			throw new InvalidParameterException("Must give some ids to indent");
		}
		// Ex: WHERE _id IN (15,27,2,94)
		String whereClause = Columns._ID + " IN (";
		for (long id : idList) {
			whereClause += Long.toString(id) + ",";
		}
		whereClause = whereClause.substring(0, whereClause.length() - 1) + ")";
		return resolver.update(URI_WRITE_UNINDENT, new ContentValues(),
				whereClause, null);
	}

	@SuppressLint("DefaultLocale")
	public String getSQLUnIndentItem(final long parentRight) {
		// final String GETPARENTRIGHT =
		// "SELECT %3$s FROM %1$s WHERE %2$s < %4$s AND %3$s > %5$s AND %6$s IS %7$d ORDER BY (%3$s - %2$s) ASC LIMIT 1";
		if (shouldIndent())
			// First left value
			return String.format("UPDATE %1$s SET %2$s = " +

			" CASE " +
			// Must exist a parent to be able to unindent
			// Narrowest enclosing item
			// " WHEN EXISTS(" + GETPARENTRIGHT + ") " +
			// " THEN CASE " +
			// PARENT left does not change. right gets my previous left
			// Subtree takes one step right
			// Root's left takes one step right
					" WHEN %2$s >= %4$d AND %3$s <= %5$d " + " THEN %2$s + 1 " +
					// Dont change others
					// " ELSE %2$s END " +
					// If no parent, no change
					" ELSE %2$s END, " +

					// Right value
					" %3$s =  " +

					" CASE " +
					// Must exist a parent to be able to unindent
					// Narrowest enclosing item
					// " WHEN EXISTS(" + GETPARENTRIGHT + ") " +
					// " THEN CASE " +
					// PARENT left does not change. right gets my previous left
					" WHEN %3$s IS (" + parentRight + ") " + " THEN %4$d " +
					// Subtree takes one step right
					" WHEN %2$s > %4$d AND %3$s < %5$d " + " THEN %3$s + 1 " +

					// Root's right gets parent right
					" WHEN %3$s IS %5$d " + " THEN (" + parentRight + ") " +

					// Dont change others
					" ELSE %3$s END " +
					// If no parent, no change
					// " ELSE %3$s END " +

					// Restrict to list
					" WHERE %6$s IS %7$d; "
					
					//Enforce integrity
					+ posUniqueConstraint("new", "pos not unique unindent")
					
					, TABLE_NAME, Columns.LEFT,
					Columns.RIGHT, left, right, Columns.DBLIST, dblist);

		else
			return null;
	}

	@SuppressLint("DefaultLocale")
	public String getSQLIndentItem() {
		if (shouldIndent())
			// First left value
			return String
					.format("UPDATE %1$s SET %2$s = "
							+

							" CASE "
							+
							// Must exist a sibling to the left to indent at all
							" WHEN EXISTS(SELECT 1 FROM %1$s WHERE %3$s IS (%4$s - 1) AND %6$s IS %7$d LIMIT 1) "
							+ " THEN CASE "
							+
							// I move one step left, and leave my children
							" WHEN %2$s IS %4$s "
							+ " THEN (%4$s - 1) "
							+
							// Others are left
							" ELSE %2$s END "
							+
							// If no sibling, can't indent
							" ELSE %2$s END, "
							+
							// Right value
							" %3$s =  "
							+

							" CASE "
							+
							// Must exist a sibling to the left to indent at all
							" WHEN EXISTS(SELECT 1 FROM %1$s WHERE %3$s IS (%4$s - 1) AND %6$s IS %7$d LIMIT 1) "
							+ " THEN CASE " +
							// sibling gets my right
							" WHEN %3$s IS (%4$s - 1) " + " THEN %5$s " +
							// I move one step left, new width is one
							" WHEN %2$s IS %4$s " + " THEN %4$s " +
							// Others are left
							" ELSE %3$s END " +
							// If no sibling, can't indent
							" ELSE %3$s END " +

							// Restrict to list
							" WHERE %6$s IS %7$d; "
							
							//Enforce integrity
							+ posUniqueConstraint("new", "pos not unique indent item")
							
							, TABLE_NAME, Columns.LEFT,
							Columns.RIGHT, left, right, Columns.DBLIST, dblist);

		else
			return null;
	}

	@SuppressLint("DefaultLocale")
	public String getSQLMoveSubTree(final ContentValues values) {
		if (shouldMove(values)) {
			return String.format("UPDATE %1$s SET %2$s = %2$s + "
					+

					" CASE "
					+
					// Tasks are moving left
					" WHEN (%4$d < %6$d) "
					+

					" THEN CASE "
					+ " WHEN %2$s BETWEEN %4$d AND (%6$d - 1) "
					+
					// Then they must flow [width] to the right
					" THEN %7$d - %6$d + 1 "
					+ " WHEN %2$s BETWEEN %6$d AND %7$d "
					+
					// Tasks in subtree jump to the left
					// targetleft - left
					" THEN %4$d - %6$d "
					+
					// Do nothing otherwise
					" ELSE 0 END "
					+
					// Tasks are moving right
					" WHEN (%4$d > %6$d) "
					+ " THEN CASE "
					+ " WHEN %2$s BETWEEN (%7$d + 1) AND %4$d "
					+
					// Then move them [width] to the left
					" THEN %6$d - %7$d - 1"
					+ " WHEN %2$s BETWEEN %6$d AND %7$d "
					+
					// Tasks in subtree jump to the right
					// targetleft - left

					// Depends on if we are moving inside a task or
					// moving an entire one
					" THEN CASE WHEN %5$d > (%4$d + 1) "
					+ " THEN %4$d - %7$d "
					+ " ELSE %4$d - %7$d + 1 END "
					+
					// Do nothing otherwise
					" ELSE 0 END "
					+
					// No move actually performed. comma to do right next
					" ELSE 0 END, "
					+

					" %3$s = %3$s + "
					+ " CASE "
					+
					// Tasks are moving left
					" WHEN (%4$d < %6$d) "
					+

					" THEN CASE "
					+
					// but only if right is left of originleft
					" WHEN %3$s BETWEEN %4$d AND (%6$d - 1)"
					+
					// Then they must flow [width] to the right
					" THEN %7$d - %6$d + 1"
					+ " WHEN %2$s BETWEEN %6$d AND %7$d "
					+
					// Tasks in subtree jump to the left
					// targetleft - left
					" THEN %4$d - %6$d "
					+
					// Do nothing otherwise
					" ELSE 0 END "
					+
					// Tasks are moving right
					" WHEN (%4$d > %6$d) "
					+ " THEN CASE "
					+
					// when right is between myright + 1 and targetleft + 1
					" WHEN %3$s BETWEEN (%7$d + 1) AND (%4$d + 1) "
					+
					// Then move them [width] to the left
					" THEN %6$d - %7$d - 1"
					+ " WHEN %2$s BETWEEN %6$d AND %7$d "
					+
					// targetleft - left
					// Depends on if we are moving inside a task or
					// moving an entire one
					" THEN CASE WHEN %5$d > (%4$d + 1) "
					+ " THEN %4$d - %7$d  " + " ELSE %4$d - %7$d + 1 END " +
					// Do nothing otherwise
					" ELSE 0 END " +
					// No move actually performed. End update with semicolon
					" ELSE 0 END " + " WHERE %8$s IS %9$d; "
					
					//Enforce integrity
					+ posUniqueConstraint("new", "pos not unique move sub tree")
					,

			TABLE_NAME, Columns.LEFT, Columns.RIGHT,
					values.getAsLong(TARGETLEFT),
					values.getAsLong(TARGETRIGHT), left, right, Columns.DBLIST,
					dblist

			);

		}
		else
			return null;
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}
}
