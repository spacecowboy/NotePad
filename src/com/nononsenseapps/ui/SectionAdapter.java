package com.nononsenseapps.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import com.nononsenseapps.notepad.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SimpleCursorAdapter;

public class SectionAdapter extends BaseAdapter {

	public final Map<String,SimpleCursorAdapter> sections = new LinkedHashMap<String,SimpleCursorAdapter>();
	public final ArrayAdapter<String> headers;
	public final static int TYPE_SECTION_HEADER = 0;

	public SectionAdapter(Context context) {
		headers = new ArrayAdapter<String>(context, R.layout.list_header);
	}

	public void addSection(String section, SimpleCursorAdapter adapter) {
		this.headers.add(section);
		this.sections.put(section, adapter);
	}
	
	public void removeSection(String section) {
		this.headers.remove(section);
		this.sections.remove(section);
	}

	@Override
	public Object getItem(int position) {
		for(Object section : this.sections.keySet()) {
			Adapter adapter = sections.get(section);
			int size = adapter.getCount() + 1;

			// check if position inside this section
			if(position == 0) return section;
			if(position < size) return adapter.getItem(position - 1);

			// otherwise jump into next section
			position -= size;
		}
		return null;
	}
	
	public long getSubItemId(int position) {
		for(Adapter adapter : this.sections.values()) {
			int size = adapter.getCount() + 1;

			// check if position inside this section
			if(position == 0) return -1;
			if(position < size) return adapter.getItemId(position - 1);

			// otherwise jump into next section
			position -= size;
		}
		return -1;
	}

	@Override
	public int getCount() {
		// total together all sections, plus one for each section header
		int total = 0;
		for(Adapter adapter : this.sections.values())
			total += adapter.getCount() + 1;
		return total;
	}

	@Override
	public int getViewTypeCount() {
		// assume that headers count as one, then total all sections
		int total = 1;
		for(Adapter adapter : this.sections.values())
			total += adapter.getViewTypeCount();
		return total;
	}

	@Override
	public int getItemViewType(int position) {
		int type = 1;
		for(Object section : this.sections.keySet()) {
			Adapter adapter = sections.get(section);
			int size = adapter.getCount() + 1;

			// check if position inside this section
			if(position == 0) return TYPE_SECTION_HEADER;
			if(position < size) return type + adapter.getItemViewType(position - 1);

			// otherwise jump into next section
			position -= size;
			type += adapter.getViewTypeCount();
		}
		return -1;
	}

	public boolean areAllItemsSelectable() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return (getItemViewType(position) != TYPE_SECTION_HEADER);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int sectionnum = 0;
		for(Object section : this.sections.keySet()) {
			Adapter adapter = sections.get(section);
			int size = adapter.getCount() + 1;

			// check if position inside this section
			if(position == 0) return headers.getView(sectionnum, convertView, parent);
			if(position < size) return adapter.getView(position - 1, convertView, parent);

			// otherwise jump into next section
			position -= size;
			sectionnum++;
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
