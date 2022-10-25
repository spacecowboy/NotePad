/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.prefs;

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
