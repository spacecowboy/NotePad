package com.nononsenseapps.ui;

import java.security.InvalidParameterException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.database.Cursor;
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
			headers = new ArrayAdapter<String>(context, R.layout.list_header);
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

	public void addSection(String section, SimpleCursorAdapter adapter) {
		if (headers == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		this.headers.add(section);
		this.sections.put(section, adapter);
		notifyDataSetChanged();
	}

	public void removeSection(String section) {
		if (headers == null) {
			throw new InvalidParameterException(ERRORMSG);
		}
		this.headers.remove(section);
		this.sections.remove(section);
		notifyDataSetChanged();
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
			for (Object section : this.sections.keySet()) {
				Adapter adapter = sections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section
				if (position == 0)
					return section;
				if (position < size)
					return adapter.getItem(position - 1);

				// otherwise jump into next section
				position -= size;
			}
			return null;
		}
	}

	public long getSubItemId(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getItemId(position);
		} else {
			for (Adapter adapter : this.sections.values()) {
				int size = adapter.getCount() + 1;

				// check if position inside this section
				if (position == 0)
					return -1;
				if (position < size)
					return adapter.getItemId(position - 1);

				// otherwise jump into next section
				position -= size;
			}
			return -1;
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
			// assume that headers count as one, then total all sections
			int total = 1;
			for (Adapter adapter : this.sections.values())
				total += adapter.getViewTypeCount();
			return total;
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getItemViewType(position);
		} else {
			int type = 1;
			for (Object section : this.sections.keySet()) {
				Adapter adapter = sections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section
				if (position == 0)
					return TYPE_SECTION_HEADER;
				if (position < size)
					return type + adapter.getItemViewType(position - 1);

				// otherwise jump into next section
				position -= size;
				type += adapter.getViewTypeCount();
			}
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
			int sectionnum = 0;
			for (Object section : this.sections.keySet()) {
				Adapter adapter = sections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section
				if (position == 0)
					return headers.getView(sectionnum, convertView, parent);
				if (position < size)
					return adapter.getView(position - 1, convertView, parent);

				// otherwise jump into next section
				position -= size;
				sectionnum++;
			}
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		if (wrappedAdapter != null) {
			return wrappedAdapter.getItemId(position);
		} else
			return position;
	}

}
