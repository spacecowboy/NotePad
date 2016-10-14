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

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

/**
 * Fix from http://stackoverflow.com/questions/15632215/preference-items-being-automatically-re-set
 *
 * due to platform bug: https://code.google.com/p/android/issues/detail?id=26194
 */
public class FixedSwitchPreference extends SwitchPreference {
    public FixedSwitchPreference(Context context) {
        super(context);
    }

    public FixedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
