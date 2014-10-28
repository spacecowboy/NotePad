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

package com.nononsenseapps.filepicker;

import android.annotation.SuppressLint;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.net.Uri;
import android.widget.Toast;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.nononsenseapps.notepad.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressLint("ValidFragment")
public class DropboxFilePickerFragment extends
        AbstractFilePickerFragment<DbxPath> {

    private final DbxFileSystem fs;

    @SuppressLint("ValidFragment")
    public DropboxFilePickerFragment(final DbxFileSystem fs) {
        super();
        if (fs == null) {
            throw new NullPointerException("FileSystem may not be null");
        }
        this.fs = fs;
    }

    @Override
    protected boolean isDir(final DbxPath dbxPath) {
        try {
            return fs.isFolder(dbxPath);
        } catch (DbxException e) {
            return false;
        }
    }

    @Override
    protected DbxPath getParent(final DbxPath dbxPath) {
        DbxPath parent = dbxPath.getParent();
        if (parent == null) {
            parent = dbxPath;
        }

        return parent;
    }

    @Override
    protected DbxPath getPath(final String s) {
        return new DbxPath(s);
    }

    @Override
    protected String getFullPath(final DbxPath dbxPath) {
        return dbxPath.toString();
    }

    @Override
    protected String getName(final DbxPath dbxPath) {
        return dbxPath.getName();
    }

    @Override
    protected DbxPath getRoot() {
        return new DbxPath("/");
    }

    @Override
    protected Uri toUri(final DbxPath dbxPath) {
        return new Uri.Builder().scheme("dropbox")
                .path(dbxPath.toString()).build();
    }

    @Override
    protected Comparator<DbxPath> getComparator() {
        return new Comparator<DbxPath>() {
            @Override
            public int compare(final DbxPath lhs, final DbxPath rhs) {
                if (isDir(lhs) && !isDir(rhs)) {
                    return -1;
                } else if (isDir(rhs) && !isDir(lhs)) {
                    return 1;
                } else {
                    return lhs.getName().toLowerCase().compareTo(rhs.getName()
                            .toLowerCase());
                }
            }
        };
    }

    @Override
    protected Loader<List<DbxPath>> getLoader() {
        return new AsyncTaskLoader<List<DbxPath>>(getActivity()) {
            DbxPath listenPath;
            public DbxFileSystem.PathListener pathListener;

            @Override
            public List<DbxPath> loadInBackground() {
                ArrayList<DbxPath> files = new ArrayList<DbxPath>();
                try {
                    for (DbxFileInfo fileInfo : fs.listFolder(currentPath)) {
                        if ((mode == MODE_FILE || mode == MODE_FILE_AND_DIR)
                                || fileInfo.isFolder) {
                            files.add(fileInfo.path);
                        }
                    }
                } catch (DbxException e) {
                    // e.printStackTrace();
                }
                return files;
            }

            /**
             * Handles a request to start the Loader.
             */
            @Override
            protected void onStartLoading() {
                super.onStartLoading();

                if (pathListener == null) {
                    pathListener = new DbxFileSystem.PathListener() {
                        @Override
                        public void onPathChange(final DbxFileSystem dbxFileSystem, final DbxPath dbxPath, final Mode mode) {
                            // Reload
                            onContentChanged();
                        }
                    };
                }

                // Make sure it's valid
                try {
                    if (!fs.exists(currentPath)) {
                        currentPath = getRoot();
                    }
                } catch (DbxException e) {
                    currentPath = getRoot();
                }

                // Start watching for changes
                listenPath = currentPath;
                fs.addPathListener(pathListener, listenPath,
                        DbxFileSystem.PathListener.Mode.PATH_OR_CHILD);

                forceLoad();
            }

            /**
             * Handles a request to completely reset the Loader.
             */
            @Override
            protected void onReset() {
                super.onReset();

                // Stop watching
                if (listenPath != null) {
                    fs.removePathListener(pathListener, listenPath,
                            DbxFileSystem.PathListener.Mode.PATH_OR_CHILD);
                    listenPath = null;
                }
            }
        };
    }

    @Override
    public void onNewFolder(final String s) {
        DbxPath path = new DbxPath(s);

        try {
            fs.createFolder(path);
            currentPath = path;
            refresh();
        } catch (DbxException e) {
            Toast.makeText(getActivity(), R.string.create_folder_error,
                    Toast.LENGTH_SHORT).show();
        }
    }
}