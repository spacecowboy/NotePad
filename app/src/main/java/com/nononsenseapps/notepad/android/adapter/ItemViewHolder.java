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

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * ViewHolder class for items which go in the main list.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder {

    public final TextView textView;
    private String mPath = null;

    public ItemViewHolder(final View itemView, final MainListAdapter.OnItemClickHandler onItemClickHandler) {
        super(itemView);
        textView = (TextView) itemView.findViewById(android.R.id.text1);

        if (onItemClickHandler != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickHandler.onItemClick(ItemViewHolder.this);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return onItemClickHandler.onItemLongClick(ItemViewHolder.this);
                }
            });
        }
    }

    public boolean isFolder() {
        return false;
    }
    /**
     * Set the uri for this item. Should only be called in onBindViewHolder methods
     * @param path for the item.
     */
    public void setPath(String path) {
        mPath = path;
    }

    /**
     *
     * @return path for item backing this viewholder.
     */
    public String getPath() {
        return mPath;
    }
}