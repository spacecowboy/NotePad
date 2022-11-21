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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.nononsenseapps.notepad.ActivityMain;

import android.content.Context;
import android.database.Cursor;

/**
 * It's for something in the drawer in {@link ActivityMain}
 */
public class ExtraTypesCursorAdapter extends ExtrasCursorAdapter {

	protected final int[] extraTypes;
	protected final int[] extraLayouts;
	protected ArrayList<ArrayList<Object>> extraData = null;

	private final int typeCount;

	/**
	 * Extra types should be numbered from 1-length-1. Use 0 if you want the standard layout.
	 *
	 * Extra layouts should correspond to type, e.g. index 0 = type 1, index 1 = type 2.
	 */
	public ExtraTypesCursorAdapter(Context context, int layout, Cursor c,
								   String[] from, int[] to, int[] extraIds, int[] extraLabels, int[] extraTypes, int[] extraLayouts) {
		super(context, layout, c, from, to, extraIds, extraLabels, layout);
		this.extraTypes = extraTypes;
		this.extraLayouts = extraLayouts;

		typeCount = countTypes();
	}

	private int countTypes() {
		HashSet<Integer> types = new HashSet<>();
		for (int type : extraTypes) {
			types.add(type);
		}
		// Default layout
		types.add(0);
		return types.size();
	}

	@Override
	public int getViewTypeCount() {
		return typeCount;
	}

	@Override
	public int getItemViewType(final int position) {
		if (position < extraIds.length) {
			return extraTypes[position];
		} else {
			return 0;
		}
	}

	@Override
	protected int getItemLayout(final int position) {
		final int type = getItemViewType(position);
		if (position < extraIds.length && type > 0) {
			return extraLayouts[type - 1];
		} else {
			return layout;
		}
	}

	@Override
	protected void setExtraText(final ViewHolder viewHolder, final int position) {
		if (extraData == null || extraData.isEmpty()) {
			super.setExtraText(viewHolder, position);
		}
		// set all fields
		final List<Object> dataRow = extraData.get(position);
		Object col;
		int i;
		for (i = 0; i < viewHolder.texts.length && i < dataRow.size(); i++) {
			col = dataRow.get(i);
			if (col instanceof Integer) {
				viewHolder.texts[i].setText(context.getText((Integer) col));
			} else {
				viewHolder.texts[i].setText(col.toString());
			}
		}
	}

	public void setExtraData(ArrayList<ArrayList<Object>> extras) {
		this.extraData = extras;
	}
}
