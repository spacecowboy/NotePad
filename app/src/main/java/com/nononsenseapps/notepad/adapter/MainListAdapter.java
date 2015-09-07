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

package com.nononsenseapps.notepad.adapter;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.provider.ProviderContract;

/**
 * Adapter to handle the display of notes/tasks.
 */
public class MainListAdapter extends RecyclerView.Adapter<MainListViewHolder> {

    private Cursor mCursor = null;

    public MainListAdapter() {

    }

    public void setData(@Nullable Cursor cursor) {
        mCursor = cursor;
    }

    @Override
    public MainListViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new MainListViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.listitem_main_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(MainListViewHolder vh, int position) {
        mCursor.moveToPosition(position);

        vh.textView.setText(getString(mCursor, ProviderContract.COLUMN_ID)
                + " - " + getString(mCursor, ProviderContract.COLUMN_TITLE));
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public long getItemId(final int position) {
        return super.getItemId(position);
    }

    private String getString(@NonNull Cursor cursor, @NonNull @ProviderContract.ColumnName String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    private int getInt(@NonNull Cursor cursor, @NonNull @ProviderContract.ColumnName String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }
}