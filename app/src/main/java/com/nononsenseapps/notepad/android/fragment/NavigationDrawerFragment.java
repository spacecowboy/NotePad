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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.core.view.GravityCompat;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.android.provider.ProviderManager;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

	/**
	 * Remember the position of the selected item.
	 */
	private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

	/**
	 * Per the design guidelines, you should show the drawer on launch until the user manually
	 * expands it. This shared preference tracks this.
	 */
	private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

	/**
	 * Helper component that ties the action bar to the navigation drawer.
	 */
	private ActionBarDrawerToggle mDrawerToggle;

	private DrawerLayout mDrawerLayout;
	private View mFragmentContainerView;

	private int mCurrentSelectedPosition = 0;
	private boolean mFromSavedInstanceState;
	private boolean mUserLearnedDrawer;
	private LinearLayout mNavContainer;
	private ImageView mMainAvatar;
	private NavigationDrawerCallbacks mCallbacks;

	public NavigationDrawerFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Read in the flag indicating whether or not the user has demonstrated awareness of the
		// drawer. See PREF_USER_LEARNED_DRAWER for details.
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

		if (savedInstanceState != null) {
			mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
			mFromSavedInstanceState = true;
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(
				R.layout.drawer_layout, container, false);

		mNavContainer = null; // rootView.findViewById(R.id.nav_container);
		mMainAvatar = null; // rootView.findViewById(R.id.main_avatar);

		addProviders();

		addFooterItems();

		return rootView;
	}

	/**
	 * Add items which are always visible, such as "settings" and "add provider".
	 */
	private void addFooterItems() {
		LayoutInflater inflater = LayoutInflater.from(getContext());

		// Add provider
		View additem = inflater.inflate(R.layout.listitem_avatar, mNavContainer, false);
		((ImageView) additem.findViewById(android.R.id.icon1)).setImageResource(R.drawable.ic_add_24dp);
		((TextView) additem.findViewById(android.R.id.text1)).setText("Add source");
		additem.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Add provider was clicked
				// TODO
			}
		});
		mNavContainer.addView(additem);

		// Manage providers
		View manage = inflater.inflate(R.layout.listitem_avatar, mNavContainer, false);
		((ImageView) manage.findViewById(android.R.id.icon1)).setImageResource(R.drawable.ic_settings_24dp);
		((TextView) manage.findViewById(android.R.id.text1)).setText("Manage sources");
		manage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Settings was clicked
				// TODO
			}
		});
		mNavContainer.addView(manage);
	}

	/**
	 * Populate the container with available & configured providers
	 */
	private void addProviders() {
		LayoutInflater inflater = LayoutInflater.from(getContext());

		// TODO take configuration into account
		ProviderManager pm = new ProviderManager(getContext());
		for (final ProviderManager.Provider provider : pm.getConfiguredProviders()) {
			View view = inflater.inflate(R.layout.listitem_avatar, mNavContainer, false);
			// provider might have icon
			ImageView im = (ImageView) view.findViewById(android.R.id.icon1);
			if (provider.getIcon() > 0) {
				// An icon is specifed by the provider
				im.setImageResource(provider.getIcon());
				// Don't clip icons
				im.setClipToOutline(false);
			} else {
				// No icon specified, use first letter as icon
				im.setImageDrawable(TextDrawable.builder()
						.buildRect(provider.getLabel().substring(0, 1),
								ColorGenerator.MATERIAL.getColor(provider.getLabel())));
			}
			((TextView) view.findViewById(android.R.id.text1)).setText(provider.getLabel());
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Provider clicked
					selectProvider(provider);
				}
			});
			mNavContainer.addView(view);
		}
	}

	/**
	 * Change to the specified provider
	 *
	 * @param provider to switch to
	 */
	public void selectProvider(ProviderManager.Provider provider) {
		// Set main avatar
		if (provider.getIcon() > 0) {
			// An icon is specifed by the provider
			mMainAvatar.setImageResource(provider.getIcon());
			// Don't clip icons
			mMainAvatar.setClipToOutline(false);
		} else {
			// No icon specified, use first letter as icon
			mMainAvatar.setImageDrawable(TextDrawable.builder()
					.buildRect(provider.getLabel().substring(0, 1),
							ColorGenerator.MATERIAL.getColor(provider.getLabel())));
		}
		// TODO set labels

		// Load new provider
		mCallbacks.switchProvider(provider);

		// Close drawer
		if (isDrawerOpen() && mUserLearnedDrawer) {
			mDrawerLayout.closeDrawers();
		}
	}

	public boolean isDrawerOpen() {
		return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
	}

	/**
	 * Users of this fragment must call this method to set up the navigation drawer interactions.
	 *
	 * @param fragmentId   The android:id of this fragment in its activity's layout.
	 * @param drawerLayout The DrawerLayout containing this fragment's UI.
	 */
	public void setUp(int fragmentId, DrawerLayout drawerLayout, Toolbar toolbar) {
		mFragmentContainerView = getActivity().findViewById(fragmentId);
		mDrawerLayout = drawerLayout;

		// set a custom shadow that overlays the main content when the drawer opens
		// mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// Fix the color of the status bar

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the navigation drawer and the action bar app icon.
		mDrawerToggle = new ActionBarDrawerToggle(
				getActivity(),                    /* host Activity */
				mDrawerLayout,                    /* DrawerLayout object */
				toolbar,             /* nav drawer image to replace 'Up' caret */
				R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
				R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
		) {
			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				if (!isAdded()) {
					return;
				}

				getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
			}

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
					SharedPreferences sp = PreferenceManager
							.getDefaultSharedPreferences(getActivity());
					sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
				}

				getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
			}
		};

		// If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
		// per the navigation drawer design guidelines.
		if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
			mDrawerLayout.openDrawer(mFragmentContainerView);
		}

		// Defer code dependent on restoration of previous instance state.
		mDrawerLayout.post(new Runnable() {
			@Override
			public void run() {
				mDrawerToggle.syncState();
			}
		});

		mDrawerLayout.setDrawerListener(mDrawerToggle);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			mCallbacks = (NavigationDrawerCallbacks) getActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Forward the new configuration the drawer toggle component.
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	private ActionBar getActionBar() {
		return ((AppCompatActivity) getActivity()).getSupportActionBar();
	}

	public interface NavigationDrawerCallbacks {
		void switchProvider(ProviderManager.Provider provider);
	}
}
