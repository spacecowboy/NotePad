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

package com.nononsenseapps.notepad.android.adapter;

import android.graphics.Color;
import android.view.View;

/**
 * ViewHolder class for folder-items which go in the main list.
 */
public class FolderViewHolder extends ItemViewHolder {

	public FolderViewHolder(View itemView, MainListAdapter.OnItemClickHandler onItemClickHandler) {
		super(itemView, onItemClickHandler);
		// TODO remove this, just for dev
		textView.setTextColor(Color.RED);
	}

	public boolean isFolder() {
		return true;
	}
}