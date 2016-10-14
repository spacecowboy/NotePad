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

package com.nononsenseapps.notepad.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.nononsenseapps.notepad.R;

import java.util.Locale;

/**
 * This class contains some helper methods for activities.
 */
public class ActivityHelper {
    /**
     * Set up the activity according to the user's configuration.
     * Sets the fullscreen theme and language of the activity.
     *
     * @param context
     */
    public static void useUserConfigurationFullscreen(@NonNull Context context) {
        setSelectedTheme(context, true);
        setSelectedLanguage(context);
    }

    /**
     * Set up the activity according to the user's configuration.
     * Sets the dialogWhenLarge theme and language of the activity.
     *
     * @param context
     */
    public static void useUserConfigurationDialogWhenLarge(@NonNull Context context) {
        setSelectedTheme(context, false);
        setSelectedLanguage(context);
    }

    /**
     * Set the configured theme on the current context
     */
    public static void setSelectedTheme(@NonNull Context context, boolean fullscreen) {
        final String selectedTheme = SharedPreferencesHelper.getCurrentTheme(context, "");
        final String dark = context.getString(R.string.const_preference_theme_dark);
        final String black = context.getString(R.string.const_preference_theme_black);

        if (dark.equals(selectedTheme)) {
            context.setTheme(fullscreen ? R.style.DarkTheme : R.style.DarkTheme_DialogWhenLarge);
        } else if (black.equals(selectedTheme)) {
            context.setTheme(fullscreen ? R.style.BlackTheme : R.style.BlackTheme_DialogWhenLarge);
        } else {
            // light
            context.setTheme(fullscreen ? R.style.LightTheme : R.style.LightTheme_DialogWhenLarge);
        }
    }

    /**
     * Set configured locale on current context
     */
    public static void setSelectedLanguage(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Configuration config = context.getResources().getConfiguration();

        String lang = prefs.getString(context.getString(R.string.const_preference_locale_key), "");
        if (!config.locale.toString().equals(lang)) {
            Locale locale;
            if ("".equals(lang))
                locale = Locale.getDefault();
            else if (lang.length() == 5) {
                locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
            } else {
                locale = new Locale(lang.substring(0, 2));
            }
            // Locale.setDefault(locale);
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources()
                    .getDisplayMetrics());
        }
    }
}
