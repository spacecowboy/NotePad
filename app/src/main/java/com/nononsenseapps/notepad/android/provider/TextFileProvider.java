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

package com.nononsenseapps.notepad.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.nononsenseapps.notepad.providercontract.ProviderContract;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TextFileProvider extends ContentProvider {

	// TODO is this whole com.nononsenseapps.notepad.android.provider namespace useless ?

	public static final String AUTHORITY = "com.nononsenseapps.notepad.TESTPROVIDER";
	private static final String TAG = "TextFileProvider";

	// This urimatcher converts incoming URIs to corresponding uricodes
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int URI_ROOT = 101;
	private static final int URI_LIST = 102;
	private static final int URI_DETAILS = 103;

	private static final String TYPE_NONONSENSENOTES_ITEM = "vnd.android.cursor.item/vnd.nononsensenotes.item";

	// Add uris to match (initial slash supported from JELLY_BEAN_MR2)
	static {
		// No item is specified, corresponds to listing all top-level items
		sUriMatcher.addURI(AUTHORITY, "/list", URI_ROOT);
		// List all items which are children of the URI (but not the URI-item itself)
		sUriMatcher.addURI(AUTHORITY, "/list/*", URI_LIST);
		// Return the single item at the specified URI
		sUriMatcher.addURI(AUTHORITY, "/details/*", URI_DETAILS);
	}


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

		String relativePath;
		switch (sUriMatcher.match(uri)) {
			case URI_ROOT:
				relativePath = "/";
				break;
			case URI_LIST:
				relativePath = ProviderHelper.getRelativePath(uri);
				break;
			default:
				throw new IllegalArgumentException("Unknown path: " + uri);
		}

		final File filePath = new File(ProviderHelper.join(mRootPath, relativePath));
		File[] files = filePath.listFiles(mFileFilter);
		Log.d(TAG, "Listing: " + filePath.getPath() + ", files: " + (files == null ? 0 : files.length));

		if (files == null) {
			return null;
		}

		// Sort by name and path
		List<File> fileList = Arrays.asList(files);
		Collections.sort(fileList);

		// Projection is ProviderContract.sMainListProjection
		MatrixCursor mc = new MatrixCursor(projection, fileList.size());

		for (File file : fileList) {
			mc.addRow(new Object[] { ProviderHelper.join(relativePath, file.getName()),
					ProviderContract.getTypeMask(file.isDirectory() ? ProviderContract.TYPE_FOLDER : ProviderContract.TYPE_DATA,
							ProviderContract.TYPE_DESCRIPTION),
					file.getName(), null, null, null });
		}

		return mc;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		// TODO: Implement this to handle requests to update one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
