package com.nononsenseapps.ui;

import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SimpleCursorAdapter;

public class SectionAdapter extends BaseAdapter {
	private final static String ERRORMSG = "This adapter is in the wrong state for that method to be used!";

	public final Map<String, SimpleCursorAdapter> sections = new LinkedHashMap<String, SimpleCursorAdapter>();
	public final ArrayAdapter<String> headers;
	private final SimpleCursorAdapter wrappedAdapter;
	public final static int TYPE_SECTION_HEADER = 0;
	public final static int TYPE_ITEM = 1;
	public final static int TYPE_COUNT = TYPE_ITEM + 1;

	private String state = "";

	/**
	 * A section adapter works in two ways. First, like a normal adapter. In
	 * that case you give the constructor a regular adapter and the
	 * sectionadapter works merely as a wrapper. Second, like a section adapter.
	 * In that case you must give it a null parameter during construction. If
	 * you at any time try to access methods that are special to either case
	 * while being in the other case, and exception will be thrown! This should
	 * only be an issue with fetching CursorLoaders.
	 * 
	 * One exception is the "GetSubItemId" method which will return the
	 * appropriate Id in both cases.
	 * 
	 * @param context
	 */
	public SectionAdapter(Context context, SimpleCursorAdapter wrappedAdapter) {
		if (wrappedAdapter == null) {
			headers = new ArrayAdapter<String>(context, R.layout.list_header,
					R.id.list_header_title);
			this.wrappedAdapter = null;
		} else {
			headers = null;
			this.wrappedAdapter = wrappedAdapter;
		}
	}

	/**
	 * @return True if this adapter is in a sectioned state, False otherwise
	 */
	public boolean isSectioned() {
		return headers != null;
	}

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
			Log.d("listproto", "killing previous adapter");
			prev.swapCursor(null);
		}
		// Need to sort the headers each time it changes
		if (comp != null) {
			headers.sort(comp);
		}
		notifyDataSetChanged();
	}

	public void removeSection(String section, Comparator<String> comp) {
		if (headers == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		this.headers.remove(section);
		SimpleCursorAdapter prev = this.sections.remove(section);
		if (prev != null) {
			Log.d("listproto", "killing previous adapter");
			prev.swapCursor(null);
		}
		// Need to sort the headers each time it changes
		if (comp != null) {
			headers.sort(comp);
		}
		// dont notify, this is only called during resets
		// notifyDataSetChanged();
	}

	public void swapCursor(Cursor data) {
		if (wrappedAdapter == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		this.wrappedAdapter.swapCursor(data);
		notifyDataSetChanged();
	}

	@Override
	public Object getItem(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getItem(position);
		} else {
			// Top is always a section
			int headerPos = 0;
			// Sorting matters!
			for (int i = 0; i < headers.getCount(); i++) {
				Adapter adapter = sections.get(headers.getItem(i));
				if (position == 0)
					return headers.getItem(headerPos);

				position -= 1;

				if (position < adapter.getCount())
					return adapter.getItem(position);

				position -= adapter.getCount();
				headerPos += 1;
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
			for (Adapter adapter : this.sections.values())
				total += adapter.getCount() + 1;
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
				if (position == 0)
					return TYPE_SECTION_HEADER;

				position -= 1;

				if (position < adapter.getCount())
					return TYPE_ITEM;

				position -= adapter.getCount();
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
				if (position == 0)
					return headers.getView(headerPos, convertView, parent);

				position -= 1;

				if (position < adapter.getCount())
					return adapter.getView(position, convertView, parent);

				position -= adapter.getCount();
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
			int headerPos = 0;
			// Sorting matters!
			for (int i = 0; i < headers.getCount(); i++) {
				Adapter adapter = sections.get(headers.getItem(i));
				if (position == 0)
					return headers.getItemId(headerPos);

				position -= 1;

				if (position < adapter.getCount())
					return adapter.getItemId(position);

				position -= adapter.getCount();
				headerPos += 1;
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

	/**
	 * Will also clear existing adapters etc
	 * 
	 * @param state
	 */
	public void changeState(String state) {
		if (headers == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		if (!getState().equals(state)) {
			for (String header : sections.keySet().toArray(new String[sections.size()])) {
				removeSection(header, null);
			}
			this.state = state;
		}
	}

}
