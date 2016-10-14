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

package com.nononsenseapps.notepad.ui.settings;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.ui.base.ActivityBase;
import com.nononsenseapps.notepad.util.SyncGtaskHelper;

import java.io.IOException;

/**
 * Main Settings activity. Loads the suitable fragment.
 */
public class ActivitySettings extends ActivityBase implements AccountManagerCallback<Bundle> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        getFragmentManager().beginTransaction().replace(R.id.container, new FragmentSettings())
                .commit();
    }

    /**
     * Restart prefs immediately since the change is made here
     */
    protected void onThemeOrLocaleChange() {
        restartActivity();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            NavUtils.navigateUpFromSameTask(this);
            //finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when the user has selected a Google account when pressing the enable Gtask switch.
     */
    @SuppressLint("CommitPrefEdits")
    @Override
    public void run(AccountManagerFuture<Bundle> future) {
        try {
            // If the user has authorized
            // your application to use the
            // tasks API
            // a token is available.
            String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
            // Now we are authorized by the user.

            if (token != null && !token.isEmpty()) {
                // Also mark enabled as true, as the dialog was shown from enable button
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(this);
                sharedPreferences.edit().putBoolean(getString(R.string
                        .const_preference_gtask_enabled_key), true).commit();

                // Set it syncable
                SyncGtaskHelper.toggleSync(this, sharedPreferences);
                // And schedule an immediate sync
                SyncGtaskHelper.requestSyncIf(this, SyncGtaskHelper.MANUAL);
            }
        } catch (OperationCanceledException | AuthenticatorException | IOException ignored) {
            // if the request was canceled for any reason, or something went wrong
            SyncGtaskHelper.disableSync(this);
        }
    }
}
