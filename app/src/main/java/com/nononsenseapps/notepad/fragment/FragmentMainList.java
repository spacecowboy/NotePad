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

package com.nononsenseapps.notepad.fragment;


import android.database.Cursor;
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
import android.widget.TextView;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.provider.ProviderManager;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentMainList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentMainList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mRecyclerView;
    private TestAdapter mAdapter;

    public FragmentMainList() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FragmentMainList.
     */
    public static FragmentMainList newInstance() {
        FragmentMainList fragment = new FragmentMainList();
        fragment.setArguments(Bundle.EMPTY);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mAdapter = new TestAdapter();
        mRecyclerView.setAdapter(mAdapter);

        /*TextView tv = (TextView) root.findViewById(android.R.id.text1);
        StringBuilder sb = new StringBuilder("Providers:\n");
        ProviderManager pm = ProviderManager.getInstance(getActivity());
        for (ProviderManager.Provider provider: pm.getAvailableProviders()) {
            sb.append(provider.label).append(" - ").append(provider.authority).append("\n");
        }
        tv.setText(sb.toString());
*/


        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        ProviderManager pm = ProviderManager.getInstance(getContext());
        ProviderManager.Provider provider = pm.getAvailableProviders().get(0);

        return new CursorLoader(getContext(), provider.uriBase, null, null, null, null);
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


    public static class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private Cursor mCursor = null;

        public TestAdapter() {

        }

        public void setData(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            return new TestViewHolder(LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.listitem_main_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(TestViewHolder vh, int position) {
            mCursor.moveToPosition(position);
            vh.textView.setText(mCursor.getString(0) + " - " + mCursor.getString(1));
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
    }

    public static class TestViewHolder extends RecyclerView.ViewHolder {

        public final TextView textView;

        public TestViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
