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

package com.nononsenseapps.notepad.android.fragment;


import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.android.activity.FolderListActivity;
import com.nononsenseapps.notepad.android.adapter.ItemViewHolder;
import com.nononsenseapps.notepad.android.adapter.MainListAdapter;
import com.nononsenseapps.notepad.android.provider.ProviderContract;
import com.nononsenseapps.notepad.android.provider.ProviderHelper;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, MainListAdapter.OnItemClickHandler {

    // Fragment arguments
    private static final String ARG_URI = "arg_uri";

    private RecyclerView mRecyclerView;
    private MainListAdapter mAdapter;
    private Uri mUri;

    public MainListFragment() {
        // Required empty public constructor
    }

    /**
     * @param uri to list items in
     * @return a new instance of this fragment
     */
    public static MainListFragment newInstance(Uri uri) {
        MainListFragment fragment = new MainListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URI, uri.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUri = Uri.parse(getArguments().getString(ARG_URI));

        // Load data
        getLoaderManager().restartLoader(0, Bundle.EMPTY, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_main_list, container, false);

        mRecyclerView = (RecyclerView) root.findViewById(android.R.id.list);
        // improve performance if you know that changes in content
        // do not change the size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new MainListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        View fab = root.findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO do something more interesting
                ContentValues values = new ContentValues();
                values.put(ProviderContract.COLUMN_TITLE, "A random title");
                getContext().getContentResolver().insert(mUri, values);
            }
        });

        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getContext(), mUri,
                ProviderContract.sMainListProjection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.setData(cursor);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.setData(null);
    }

    @Override
    public void onItemClick(ItemViewHolder viewHolder) {
        if (viewHolder.isFolder()) {
            Intent i = new Intent(getContext(), FolderListActivity.class);
            i.putExtra(ProviderContract.COLUMN_TITLE, viewHolder.textView.getText().toString());
            i.setData(ProviderHelper.getListUri(ProviderHelper.getBase(mUri),
                    viewHolder.getPath()));
            startActivity(i);
        }
    }

    @Override
    public boolean onItemLongClick(ItemViewHolder viewHolder) {
        return false;
    }
}
