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

package com.nononsenseapps.notepad.fragments;

import static com.nononsenseapps.util.ArrayHelper.toArray;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.util.SharedPreferencesHelper;
import com.nononsenseapps.util.SyncGtaskHelper;

import java.util.ArrayList;

/**
 * This fragment is the view which is displayed in the left drawer.
 * Any activity containing it must implement NavigationDrawerCallbacks.
 */
abstract class NavigationDrawerFragment_USELESS extends Fragment {

	// TODO useless ? you may want to delete this

	static final int VIEWTYPE_ITEM = 0;

	static final int VIEWTYPE_TOPLEVEL = 5;



	/**
	 * Per the design guidelines, you should show the drawer on launch until the user manually
	 * expands it. This shared preference tracks this.
	 */
	private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
	ArrayList<ArrayList<Object>> mExtraData;
	/**
	 * Helper component that ties the action bar to the navigation drawer.
	 */
	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;
	private boolean mFromSavedInstanceState;
	private boolean mUserLearnedDrawer;


	private boolean mIsThemeLight;


	/**
	 * Users of this fragment must call this method to set up the navigation drawer interactions.
	 *
	 * @param drawerLayout The DrawerLayout containing this fragment's UI.
	 */
	public void setUp(DrawerLayout drawerLayout, Toolbar toolbar) {
		mDrawerLayout = drawerLayout;

		// set a custom shadow that overlays the main content when the drawer opens
		//mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// Fix the color of the status bar

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the navigation drawer and the action bar app icon.
		mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, toolbar, R.string
				.navigation_drawer_open, R.string.navigation_drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				if (!isAdded()) {
					return;
				}

				if (!mUserLearnedDrawer) {
					// The user manually opened the drawer; store this flag to prevent auto-showing
					// the navigation drawer automatically in the future.
					mUserLearnedDrawer = true;
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences
							(getActivity());
					sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
				}
			}
		};

		// If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
		// per the navigation drawer design guidelines.
		if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
			mDrawerLayout.openDrawer(GravityCompat.START);
			//mDrawerLayout.openDrawer(fragmentContainerView);
		}

		// Defer code dependent on restoration of previous instance state.
		mDrawerLayout.post(() -> mDrawerToggle.syncState());

		mDrawerLayout.setDrawerListener(mDrawerToggle);
	}




	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savInstState) {
		View rootView = inflater.inflate(0, container, false);
		RecyclerView list = null;//(RecyclerView) rootView.findViewById(R.id.leftDrawer);

		// TODO add edit lists item?

		list.setHasFixedSize(true);

		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
		list.setLayoutManager(layoutManager);

		return rootView;
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Forward the new configuration the drawer toggle component.
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (android.R.id.home == item.getItemId()) {
			mDrawerLayout.openDrawer(GravityCompat.START);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}





	class TopLevelItem {

		public String getAvatarName() {
			// Try google account first
			String result = "";
			if (SyncGtaskHelper.isGTasksConfigured(getActivity())) {
				result = SharedPreferencesHelper.getGoogleAccount(getActivity());
			}


			if (result.isEmpty() && SharedPreferencesHelper.isSdSyncEnabled(getActivity())) {
				// Then try folder
				result = null;// SharedPreferencesHelper.getSdDirUri(getActivity());
			}

			return result;
		}


	}

}
