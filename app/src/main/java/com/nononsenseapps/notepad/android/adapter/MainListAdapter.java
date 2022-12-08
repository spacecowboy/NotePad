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

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.providercontract.ProviderContract;

/**
 * Adapter to handle the display of notes/tasks.
 */
public class MainListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private static final int VIEWTYPE_ITEM = 0;
    private static final int VIEWTYPE_FOLDER = 1;

    private final OnItemClickHandler mOnItemClickHandler;
    private Cursor mCursor = null;

    public MainListAdapter(OnItemClickHandler onItemClickHandler) {
        mOnItemClickHandler = onItemClickHandler;
    }

    public void setData(@Nullable Cursor cursor) {
        mCursor = cursor;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        switch (viewType) {
            case VIEWTYPE_FOLDER:
                return new FolderViewHolder(LayoutInflater.from(viewGroup.getContext())
                        .inflate(android.R.layout.simple_list_item_1, viewGroup, false), mOnItemClickHandler);
            case VIEWTYPE_ITEM:
            default:
                return new ItemViewHolder(LayoutInflater.from(viewGroup.getContext())
                        .inflate(android.R.layout.simple_list_item_1, viewGroup, false), mOnItemClickHandler);

        }
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder vh, final int position) {
        mCursor.moveToPosition(position);
        // Set uri for item
        vh.setPath(getString(mCursor, ProviderContract.COLUMN_PATH));

        vh.textView.setText(getString(mCursor, ProviderContract.COLUMN_TITLE));
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }

    @Override
    public int getItemViewType(final int position) {
        mCursor.moveToPosition(position);
        final long typemask = getLong(mCursor, ProviderContract.COLUMN_TYPEMASK);
        if (ProviderContract.isType(typemask, ProviderContract.TYPE_FOLDER)) {
            return VIEWTYPE_FOLDER;
        } else {
            // TODO check that data is checked, if not it is an error
            return VIEWTYPE_ITEM;
        }
    }

    @Override
    public long getItemId(final int position) {
        return super.getItemId(position);
    }

    private String getString(@NonNull Cursor cursor, @NonNull @ProviderContract.ColumnName String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    private long getLong(@NonNull Cursor cursor, @NonNull @ProviderContract.ColumnName String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    public interface OnItemClickHandler {
        void onItemClick(ItemViewHolder viewHolder);

        boolean onItemLongClick(ItemViewHolder viewHolder);
    }
}
