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

package com.nononsenseapps.notepad.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.fragment.MainListFragment;
import com.nononsenseapps.notepad.fragment.NavigationDrawerFragment;
import com.nononsenseapps.notepad.provider.ProviderManager;

/**
 * This is the main activity. It is the one that is started by users when they press the icon.
 */
public class MainListActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private Toolbar mToolbar;
    private Fragment mFragment;
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Set support toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                mToolbar);

        // Load main fragment
        if (savedInstanceState == null) {

            // TODO just load first provider we find, change this later
            //ProviderManager pm = ProviderManager.getInstance(this);
            //ProviderManager.Provider provider = pm.getAvailableProviders().get(0);

            //mNavigationDrawerFragment.selectProvider(provider);

            //mFragment = MainListFragment.newInstance(provider.uriBase);
            //getSupportFragmentManager().beginTransaction()
             //       .replace(R.id.listframe, mFragment, "single_pane").commit();
        } else {
            mFragment = getSupportFragmentManager().findFragmentByTag("single_pane");
        }
    }

    @Override
    public void switchProvider(ProviderManager.Provider provider) {
        // Called by navigation drawer
        setTitle(provider.label);
        mFragment = MainListFragment.newInstance(provider.uriBase);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.listframe, mFragment, "single_pane").commit();
    }
}
