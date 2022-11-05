/*
 * Copyright (c) 2015. Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.ui;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ThemedSpinnerAdapter;

import com.nononsenseapps.notepad.R;

/**
 * Adapter which holds the sorting options.
 */
public class SortingSpinnerAdapter implements ThemedSpinnerAdapter {
    public static final int VIEWTYPE_COUNT = 1;
    public static final int VIEWTYPE_SORT_OPTION = 1;
    private final Helper mHelper;
    private final Context mContext;
    private final String[] mKeys;
    private final String[] mValues;
    private final int mLength;

    // TODO useless ? you may want to delete this

    public SortingSpinnerAdapter(@NonNull Context context) {
        this.mContext = context;
        this.mHelper = new Helper(context);
        this.mKeys = context.getResources().getStringArray(R.array.sorting_preference);
        this.mValues = context.getResources().getStringArray(R.array.sortingvalues_preference);

        // Just a safeguard against non-aligned lengths
        this.mLength = Math.min(mKeys.length, mValues.length);
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme) {
        mHelper.setDropDownViewTheme(theme);
    }

    @Nullable
    @Override
    public Resources.Theme getDropDownViewTheme() {
        return mHelper.getDropDownViewTheme();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflateView(position, parent);
        }
        bindView(position, convertView);
        return convertView;
    }

    private void bindView(int position, View convertView) {
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getItem(position));
    }

    private View inflateView(int position, ViewGroup parent) {
        return mHelper.getDropDownViewInflater().inflate(android.R.layout
                .simple_spinner_dropdown_item, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public int getCount() {
        return mLength;
    }

    @Override
    public String getItem(int position) {
        return mValues[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEWTYPE_SORT_OPTION;
    }

    @Override
    public int getViewTypeCount() {
        return VIEWTYPE_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return mLength == 0;
    }
}
