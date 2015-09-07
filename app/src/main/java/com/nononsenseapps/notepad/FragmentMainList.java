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

package com.nononsenseapps.notepad;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentMainList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentMainList extends Fragment {

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

    public FragmentMainList() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_main_list, container, false);

        TextView tv = (TextView) root.findViewById(android.R.id.text1);

        StringBuilder sb = new StringBuilder("Providers:\n");
        ProviderManager pm = ProviderManager.getInstance(getActivity());
        for (ProviderManager.Provider provider: pm.getAvailableProviders()) {
            sb.append(provider.name).append(" - ").append(provider.authority).append("\n");
        }
        tv.setText(sb.toString());

        return root;
    }


}
