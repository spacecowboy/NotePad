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

package com.nononsenseapps.notepad.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;

public class TextFileProvider extends ContentProvider {
    private static final String TAG = "TextFileProvider";
    private String mRootPath;
    private FileFilter mFileFilter;

    public TextFileProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        mRootPath = Environment.getExternalStorageDirectory().getPath();
        mFileFilter = new FileFilter() {

            /**
             * Indicating whether a specific file should be included in a pathname list.
             *
             * @param pathname the abstract file to check.
             * @return {@code true} if the file should be included, {@code false}
             * otherwise.
             */
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                } else {
                    return true;
                    //return pathname.getName().toLowerCase().endsWith(".txt");
                }
            }
        };
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Log.d(TAG, "Uri: " + uri.getAuthority() + ", " + uri.getPath() + ", " + uri.getQuery());

        // If path is null, then display root
        String relativePath = uri.getPath();
        if (relativePath == null || relativePath.isEmpty()) {
            relativePath = "/";
        }

        final File filePath = new File(join(mRootPath, relativePath));
        File[] files = filePath.listFiles(mFileFilter);

        if (files == null) {
            return null;
        }

        // Projection is ProviderContract.sMainListProjection
        MatrixCursor mc = new MatrixCursor(projection, files.length);

        for (File file : files) {
            mc.addRow(new Object[]{join(relativePath, file.getName()),
                    ProviderContract.getTypeMask(file.isDirectory() ? ProviderContract.TYPE_FOLDER : ProviderContract.TYPE_DATA,
                            ProviderContract.TYPE_DESCRIPTION),
                    file.getName(), null, null, null});
        }

        return mc;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String join(@NonNull String path1, @NonNull String path2) {
        if (path1.endsWith("/")) {
            if (path2.startsWith("/")) {
                return path1 + path2.substring(1);
            } else {
                return path1 + path2;
            }
        } else {
            if (path2.startsWith("/")) {
                return path1 + path2;
            } else {
                return path1 + "/" + path2;
            }
        }
    }
}
