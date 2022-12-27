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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nononsenseapps.notepad.providercontract.ProviderContract;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainListFragment extends Fragment  {

	// Fragment arguments
	private static final String ARG_URI = "arg_uri";

	private RecyclerView mRecyclerView;

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		int id = -1; // R.layout.fragment_main_list // a recyclerview + FAB
		View root = inflater.inflate(id, container, false);

		mRecyclerView = root.findViewById(android.R.id.list);
		// improve performance if you know that changes in content
		// do not change the size of the RecyclerView
		mRecyclerView.setHasFixedSize(true);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));



		View fab = null; // root.findViewById(R.id.fab_add);
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

	@NonNull

	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return new CursorLoader(getContext(), mUri,
				ProviderContract.sMainListProjection, null, null, null);
	}



}
