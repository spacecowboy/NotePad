/*
 * Copyright (c) 2015. Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.util;

import android.os.Handler;
import android.support.annotation.NonNull;

/**
 * Some utility methods useful for Fragments
 */
public class FragmentHelper {

    /**
     * Run an action using a Handler. Useful to commit fragment changes during OnLoadFinished
     * for example, where it is typically not allowed.
     *
     * @param runnable to run in handler
     */
    static public void handle(@NonNull Runnable runnable) {
        Handler handler = new Handler();
        handler.post(runnable);
    }
}
