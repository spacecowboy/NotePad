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

package com.nononsenseapps.notepad.android.activity;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.providercontract.ProviderContract;

/**
 * This activity displays the content of a folder-item (an item which contains sub-items).
 * It not the root and thus will have an "up-arrow" to navigate back.
 */
public class FolderListActivity extends AppCompatActivity {

	// TODO the whole "com.nononsenseapps.notepad.android.*" namespace is useless.
	//  take a look at these classes, choose what to keep, delete the rest

	private Toolbar mToolbar;
	private Fragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_folder);

		// Set support toolbar
		mToolbar = findViewById(R.id.toolbar);
		if (mToolbar != null) {
			setSupportActionBar(mToolbar);
		}

		// Set up-navigation
		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			ab.setDisplayHomeAsUpEnabled(true);
			ab.setHomeButtonEnabled(true);
			//ab.setDisplayShowTitleEnabled(false);
		}

		// Setup tabs
		ViewPager viewPager = findViewById(R.id.viewpager);


		TabLayout tabLayout = findViewById(R.id.tabs);
		tabLayout.setupWithViewPager(viewPager);

        /*NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.leftDrawer);
        // Set up the drawer.
        if (navigationDrawerFragment != null) {
            navigationDrawerFragment.setUp(
                    R.id.leftDrawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout),
                    mToolbar);
        }*/

		if (getIntent().hasExtra(ProviderContract.COLUMN_TITLE)) {
			setTitle(getIntent().getStringExtra(ProviderContract.COLUMN_TITLE));
		}
	}




}
