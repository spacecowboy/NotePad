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

import android.os.AsyncTask;
import android.support.annotation.NonNull;

/**
 * Helper methods related to running things in the background.
 */
public class AsyncTaskHelper {

    /**
     * Schedule something to be done on a background thread.
     * This method must be called from the UI thread.
     */
    public static void background(final @NonNull Job job) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                job.doInBackground();
                return null;
            }
        };

        asyncTask.execute();
    }

    public interface Job {
        void doInBackground();
    }
}
