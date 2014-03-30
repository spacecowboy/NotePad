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

package com.nononsenseapps.utils.filepicker;

import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilePickerFragment extends AbstractFilePickerFragment<File> {

    /**
     * Return true if the path is a directory and not a file.
     *
     * @param path
     */
    @Override
    protected boolean isDir(final File path) {
        return path.isDirectory();
    }

    /**
     * Return the path to the parent directory. Should return the root if
     * from is root.
     *
     * @param from
     */
    @Override
    protected File goUp(final File from) {
        if (from.getParentFile() != null) {
            return from.getParentFile();
        } else {
            return from;
        }
    }

    /**
     * Convert the path to the type used.
     *
     * @param path
     */
    @Override
    protected File getPath(final String path) {
        return new File(path);
    }

    /**
     * Get the root path (lowest allowed).
     */
    @Override
    protected File getRoot() {
        return Environment.getExternalStorageDirectory();
    }

    /**
     * Try to create a designated directory.
     *
     * @param path Path to directory to create
     * @return true on success. false if failed.
     */
    @Override
    protected boolean createDir(final File path) {
        if (path.isDirectory()) {
            return true;
        } else if (path.exists()) {
            return false;
        } else {
            return path.mkdirs();
        }
    }

    /**
     * Get a loader that lists the files in the current path,
     * and monitors changes.
     */
    @Override
    protected Loader<List<File>> getLoader() {
        return new AsyncTaskLoader<List<File>>(getActivity()) {

            FileObserver fileObserver;

            @Override
            public List<File> loadInBackground() {
                Log.d("JONAS", "loadInBackground");
                ArrayList<File> files = new ArrayList<File>();
                for (File f : currentPath.listFiles()) {
                    files.add(f);
                }
                return files;
            }

            /**
             * Handles a request to start the Loader.
             */
            @Override
            protected void onStartLoading() {
                Log.d("JONAS", "startLoading");
                super.onStartLoading();

                // Start watching for changes
                fileObserver = new FileObserver(currentPath.getPath(),
                        FileObserver.CREATE |
                                FileObserver.DELETE
                                | FileObserver.MOVED_FROM | FileObserver.MOVED_TO
                ) {

                    @Override
                    public void onEvent(int event, String path) {
                        // Reload
                        onContentChanged();
                    }
                };
                fileObserver.startWatching();

                forceLoad();
            }

            /**
             * Handles a request to completely reset the Loader.
             */
            @Override
            protected void onReset() {
                Log.d("JONAS", "reset");
                super.onReset();

                // Stop watching
                if (fileObserver != null) {
                    fileObserver.stopWatching();
                    fileObserver = null;
                }
            }
        };
    }
}
