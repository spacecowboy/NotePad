package com.nononsenseapps.notepad.widget;

import java.util.InputMismatchException;
import java.util.TreeMap;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad_donate.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.util.TimeHelper;

import android.content.Context;
import android.database.Cursor;

public class HeaderCursor {
	public static final int itemType = 0;
	public static final int headerType = 1;

	private TreeMap<Integer, String> headers = null;
	private Cursor cursor = null;
	private String sortType = null;
	// private String sortOrder = null;
	private int position = -1;
	private int cursorPos = -1;
	private int totalCount = 0;
	private int offset = 0;
	
	private boolean none = false;
	private boolean overdue = false;
	private boolean yesterday = false;
	private boolean today = false;
	private boolean tomorrow = false;
	private boolean sevendays = false;
	private boolean later = false;
	private boolean earlier = false;

	public HeaderCursor(Context context, Cursor c, String stype, String sorder) {
		this.cursor = c;
		this.sortType = stype;
		// this.sortOrder = sorder;

		// setup shit
		int so = NotePad.Notes.ASCENDING_SORT_ORDERING.equals(sorder) ? 1 : -1;
		this.headers = new TreeMap<Integer, String>();

		offset = 0;
		if (MainPrefs.DUEDATESORT.equals(sortType)) {
			final int dateIndex = cursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
			String date;

			for (int pos = 0; pos < cursor.getCount(); pos++) {
				cursor.moveToPosition(pos);
				date = cursor.getString(dateIndex);

				if (date == null || date.isEmpty()) {
					if (!none) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.date_header_none));
						offset++;
						none = true;
					}
				} else if (TimeHelper.dateBefore(date, TimeHelper.dateToday())) {
					if (!overdue) {

						headers.put(pos + offset, context.getResources()
								.getString(R.string.date_header_overdue));
						offset++;
						overdue = true;
					}
				} else if (TimeHelper.dateBefore(date,
						TimeHelper.dateTomorrow())) {
					if (!today) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.date_header_today));
						offset++;
						today = true;
					}
				} else if (TimeHelper.dateIs(date, TimeHelper.dateTomorrow())) {
					if (!tomorrow) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.date_header_tomorrow));
						offset++;
						tomorrow = true;
					}
				} else if (TimeHelper.dateBefore(date,
						TimeHelper.dateEightDay())) {
					if (!sevendays) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.date_header_7days));
						offset++;
						sevendays = true;
					}
				} else {
					if (!later) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.date_header_future));
						offset++;
						later = true;
					}
				}

			}

		} else if (MainPrefs.MODIFIEDSORT.equals(sortType)) {
			final int modIndex = cursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
			long modtime;

			for (int pos = 0; pos < cursor.getCount(); pos++) {
				cursor.moveToPosition(pos);
				modtime = cursor.getLong(modIndex);
				if (modtime > TimeHelper.milliTodayStartLong()) {
					if (!today) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.mod_header_today));
						offset++;
						today = true;
					}
				} else if (modtime > TimeHelper.milliYesterdayStartLong()) {
					if (!yesterday) {

						headers.put(pos + offset, context.getResources()
								.getString(R.string.mod_header_yesterday));
						offset++;
						yesterday = true;
					}
				} else if (modtime > TimeHelper.milli7DaysAgoLong()) {
					if (!sevendays) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.mod_header_thisweek));
						offset++;
						sevendays = true;
					}
				} else {
					if (!earlier) {
						headers.put(pos + offset, context.getResources()
								.getString(R.string.mod_header_earlier));
						offset++;
						earlier = true;
					}
				}
			}

		} else {

		}

		totalCount = cursor.getCount() + headers.size();
	}

	public Cursor getCursor() {
		return cursor;
	}

	public int getCount() {
		return totalCount;
	}

	/**
	 * Wraps cursor method to handle returning headers
	 * 
	 * @param pos
	 */
	public boolean moveToPosition(int pos) {
		position = pos;
		if (headerType == getViewType(pos)) {
			return true;
		} else {
			// Correct for offset
			cursorPos = position - headers.subMap(0, pos + 1).size();
			return cursor.moveToPosition(cursorPos);
		}
	}

	/**
	 * 
	 * @return 0 for normal content from cursor, 1 for header
	 */
	public int getViewType() {
		return getViewType(position);
	}

	/**
	 * 
	 * @return 0 for normal content from cursor, 1 for header
	 */
	public int getViewType(int pos) {
		if (headers.containsKey(pos))
			return headerType;
		else
			return itemType;
	}

	/**
	 * 
	 * @return The header text for the current position.
	 */
	public String getHeaderText() throws InputMismatchException {
		if (headerType == getViewType()) {
			return headers.get(position);
		} else {
			throw new InputMismatchException("This item is not a header...");
		}
	}
}
