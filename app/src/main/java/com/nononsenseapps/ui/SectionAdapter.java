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

package com.nononsenseapps.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SimpleCursorAdapter;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.R;

import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class SectionAdapter_USELESS extends BaseAdapter {

	// TODO useless, delete

	private final static String ERRORMSG = "This adapter is in the wrong state for that method to be used!";

	public final Map<String, SimpleCursorAdapter> sections = new LinkedHashMap<>();
	public final ArrayAdapter<String> headers = null;
	public final Map<String, Long> sectionIds = new HashMap<>();
	private final SimpleCursorAdapter wrappedAdapter = null;
	public final static int TYPE_SECTION_HEADER = -39567;
	public final static int TYPE_ITEM = -892746;
	public final static int TYPE_COUNT = 2;

	private final DataSetObserver subObserver = null;

	private String state = "";


	/**
	 * Get the section a position is contained in.
	 */
	public String getSection(int position) {
		if (headers == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		// Top is always a section
		// Sorting matters!
		for (int headerPos = 0; headerPos < headers.getCount(); headerPos++) {
			Adapter adapter = sections.get(headers.getItem(headerPos));
			// Ignore headers that are empty
			if (adapter.getCount() > 0) {
				if (position == 0)
					return headers.getItem(headerPos);

				position -= 1;

				if (position < adapter.getCount())
					return headers.getItem(headerPos);

				position -= adapter.getCount();
			}
		}
		return null;
	}

	/**
	 * Add a section to the list with a corresponding ID which can be retrieved
	 * using getSectionId()
	 */
	public void addSection(long sectionId, String section,
						   SimpleCursorAdapter adapter, Comparator<String> comp) {
		if (headers == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		if (section != null)
			sectionIds.put(section, sectionId);
		addSection(section, adapter, comp);
	}

	/**
	 * Add a section to the list with corresponding adapter and defined sorting
	 */
	public void addSection(String section, SimpleCursorAdapter adapter,
						   Comparator<String> comp) {
		if (headers == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		if (adapter == null) {
			throw new NullPointerException();
		}
		this.headers.add(section);
		SimpleCursorAdapter prev = this.sections.put(section, adapter);
		if (prev != null) {
			NnnLogger.debug(SectionAdapter_USELESS.class, "killing previous adapter");
			prev.unregisterDataSetObserver(subObserver);
			prev.swapCursor(null);
		}
		if (adapter != null) {
			adapter.registerDataSetObserver(subObserver);
		}
		// Need to sort the headers each time it changes
		if (comp != null) {
			headers.sort(comp);
		}
	}



	public void swapCursor(Cursor data) {
		if (wrappedAdapter == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		this.wrappedAdapter.swapCursor(data);
		// notifyDataSetChanged();
	}

	@Override
	public Object getItem(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getItem(position);
		} else {
			// Top is always a section
			// Sorting matters!
			for (int headerPos = 0; headerPos < headers.getCount(); headerPos++) {
				Adapter adapter = sections.get(headers.getItem(headerPos));
				// Ignore headers that are empty
				if (adapter.getCount() > 0) {
					if (position == 0)
						return headers.getItem(headerPos);

					position -= 1;

					if (position < adapter.getCount())
						return adapter.getItem(position);

					position -= adapter.getCount();
				}
			}
			return null;
		}
	}

	@Override
	public int getCount() {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getCount();
		} else {
			// total together all sections, plus one for each section header
			int total = 0;
			for (Adapter adapter : this.sections.values()) {
				// Ignore headers that are empty
				if (adapter.getCount() > 0) {
					total += adapter.getCount() + 1;
				}
			}
			return total;
		}
	}

	@Override
	public int getViewTypeCount() {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getViewTypeCount();
		} else {
			return TYPE_COUNT;
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getItemViewType(position);
		} else {
			// Top is always a section
			// Sorting matters!
			for (int i = 0; i < headers.getCount(); i++) {
				Adapter adapter = sections.get(headers.getItem(i));
				// Ignore headers that are empty
				if (adapter.getCount() > 0) {
					if (position == 0)
						return TYPE_SECTION_HEADER;

					position -= 1;

					if (position < adapter.getCount())
						return TYPE_ITEM;

					position -= adapter.getCount();
				}
			}
			// Could not be found
			return -1;
		}
	}

	@Override
	public boolean isEnabled(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.isEnabled(position);
		} else {
			return (getItemViewType(position) != TYPE_SECTION_HEADER);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getView(position, convertView, parent);
		} else {
			// Top is always a section
			// Sorting matters!
			for (int headerPos = 0; headerPos < headers.getCount(); headerPos++) {
				Adapter adapter = sections.get(headers.getItem(headerPos));
				// Ignore headers that are empty
				if (adapter.getCount() > 0) {
					if (position == 0) {
						return headers.getView(headerPos, convertView, parent);
					}

					position -= 1;

					if (position < adapter.getCount())
						return adapter.getView(position, convertView, parent);

					position -= adapter.getCount();
				}
			}

			// None could be found
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getItemId(position);
		} else {
			// Top is always a section
			// Sorting matters!
			for (int headerPos = 0; headerPos < headers.getCount(); headerPos++) {
				Adapter adapter = sections.get(headers.getItem(headerPos));
				// Ignore headers that are empty
				if (adapter.getCount() > 0) {
					if (position == 0)
						return headers.getItemId(headerPos);

					position -= 1;

					if (position < adapter.getCount())
						return adapter.getItemId(position);

					position -= adapter.getCount();
				}
			}
			return -1;
		}
	}

	/**
	 * Guaranteed to be non null
	 *
	 * @return
	 */
	public String getState() {
		if (state == null)
			state = "";
		return state;
	}


}
