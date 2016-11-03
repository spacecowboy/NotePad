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

package com.nononsenseapps.notepad.ui.base;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.util.ActivityHelper;

/**
 * A base activity which sets the user's configured language and theme.
 */
public abstract class ActivityBase extends AppCompatActivity {

    private boolean shouldRestart = false;
    private final static String TAG = "RICKSMESSAGE";
    private final SharedPreferences.OnSharedPreferenceChangeListener mThemeLocaleChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.const_preference_theme_key)) || key.equals
                    (getString(R.string.const_preference_locale_key))) {
                onThemeOrLocaleChange();
                Log.i(TAG, "ln#44, ActivityBase.onSharedPreferenceChanged calls onThemeOrLocaleChange() in if()");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        useConfiguration();
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mThemeLocaleChangeListener);
        Log.i(TAG, "ln#56, ActivityBase.onCreate calls useConfiguration();\n" + "// super.onCreate(savedInstanceState) " +
                " // PreferenceManager.getDefaultSharedPreferences(this)\n" +
                ".registerOnSharedPreferenceChangeListener(mThemeLocaleChangeListener) where mThemeLocaleChangeListener is: " + mThemeLocaleChangeListener);
    }

    protected void useConfiguration() {
        ActivityHelper.useUserConfigurationFullscreen(this);
        Log.i(TAG, "ln#61, ActivityBase.useConfiguration ActivityHelper.useUserConfigurationFullscreen(this)");
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(mThemeLocaleChangeListener);
        Log.i(TAG, "ln#68, ActivityBase.onDestroy calls PreferenceManager.getDefaultSharedPreferences(this)\n" +
                ".unregisterOnSharedPreferenceChangeListener(mThemeLocaleChangeListener) and" +
                "mThemeLocaleChangeListener is: " + mThemeLocaleChangeListener);
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        if (shouldRestart) {
            shouldRestart = false;
            restartActivity();
            Log.i(TAG, "ln#80, ActivityBase.onResume in if(shouldRestart) sets shouldRestart to false" +
                    "and calls restartActivity()");
        }
        Log.i(TAG, "ln#83, ActivityBase.onResume calls super.onResume()");
        super.onResume();

    }

    /**
     * Restarts the activity using the same intent that started it. Disables animations to get a
     * seamless restart.
     */
    protected void restartActivity() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        Log.i(TAG, "ln#96, ActivityBase.restartActivity Restarts the activity using the same intent" +
                "that started it. Disables animations to get a seamless restart.");
    }

    /**
     * Default implementation is to restart activity on onResume. Override if that is not desired.
     */
    protected void onThemeOrLocaleChange() {
        shouldRestart = true;
        Log.i(TAG, "ln#104, ActivityBase.onThemeOrLocaleChange sets shouldRestart to true on a change when called");
    }
}
