package com.nononsenseapps.notepad.sync.orgsync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nononsenseapps.helpers.FileHelper;
import com.nononsenseapps.helpers.PreferencesHelper;

import org.cowboyprogrammer.org.OrgFile;
import org.cowboyprogrammer.org.OrgNode;
import org.cowboyprogrammer.org.parser.OrgParser;
import org.cowboyprogrammer.org.parser.RegexParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;

public class OrgProvider extends ContentProvider {

	public static final String KEY_TITLE = "title";
	public static final String AUTHORITY = "com.nononsenseapps.notepad.orgprovider";
	public static final String SCHEME = "content://";
	public static final Uri BASE_URI = Uri.parse(SCHEME + AUTHORITY + "/file");
	private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int CODE_FILE = 101;
	private static final int CODE_FILE_ID = 102;

	static {
		uriMatcher.addURI(AUTHORITY, "file", CODE_FILE);
		uriMatcher.addURI(AUTHORITY, "file/*", CODE_FILE_ID);
	}

	private static final OrgParser orgParser = new RegexParser();

	public OrgProvider() {}

	private int deleteItem(Uri uri) {
		final String title = uri.getLastPathSegment();
		final String filename = title + ".org";
		final File file = new File(getDir(), filename);

		final String itemid = getFragment(uri);

		try {
			OrgFile orgFile = OrgFile.createFromFile(orgParser, file);

			OrgNode toDelete = null;
			for (OrgNode node : orgFile.getSubNodes()) {
				String nodeId = OrgConverter.getNodeId(node);

				if (nodeId != null && nodeId.equalsIgnoreCase(itemid)) {
					toDelete = node;
					break;
				}
			}

			if (toDelete != null) {
				orgFile.getSubNodes().remove(toDelete);
				writeToFile(file, orgFile);
				return 1;
			}
		} catch (IOException | ParseException e) {
			throw new RuntimeException(e);
		}

		return -1;
	}

	private static void writeToFile(File file, OrgFile orgFile) throws IOException {
		final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(orgFile.treeToString());
		bw.close();
	}

	/**
	 * @return 1 = the number of items affected
	 */
	private int deleteFile(Uri uri) {
		final String title = uri.getLastPathSegment();
		final String filename = title + ".org";
		final File file = new File(getDir(), filename);

		if (file.isFile() && file.exists()) {
			if (file.delete()) {
				return 1;
			}
		}
		throw new RuntimeException("Failed to delete " + file);
	}

	@Nullable
	private String getFragment(@NonNull Uri uri) {
		String fragment = uri.getFragment();
		if (fragment != null && fragment.isEmpty()) {
			throw new UnsupportedOperationException("Empty URI fragments are now allowed");
		}
		return fragment;
	}

	private Uri insertItem(Uri uri, ContentValues values) {
		// todo
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private Uri insertFile(Uri uri, ContentValues values) {
		final String title = values.getAsString(KEY_TITLE);
		if (title.contains("/")) {
			throw new IllegalArgumentException("Filenames cannot contain slashes");
		}
		if (title.isEmpty()) {
			throw new IllegalArgumentException("Filenames cannot be empty");
		}
		final String filename = title + ".org";
		final File file = new File(getDir(), filename);

		try {
			if (!file.createNewFile()) {
				throw new IllegalArgumentException("Failed to create file: " + filename);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return Uri.withAppendedPath(BASE_URI, title);
	}

	private String getDir() {
		return FileHelper.getUserSelectedOrgDir(getContext());
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
						String[] selectionArgs, String sortOrder) {
		if (!PreferencesHelper.isSdSyncEnabled(getContext())) {
			return null;
		}
		// TODO: Implement this to handle query requests from clients.

		// Nullable
		String fragment = getFragment(uri);

		switch (uriMatcher.match(uri)) {
			case CODE_FILE -> {
				return queryFiles(uri, projection, selection, selectionArgs, sortOrder);
			}
			case CODE_FILE_ID -> {
				return fragment == null ?
						queryFile(uri, projection, selection, selectionArgs, sortOrder) :
						queryItem(uri, projection, selection, selectionArgs, sortOrder);
			}
		}

		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String getType(@NonNull Uri uri) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		if (!PreferencesHelper.isSdSyncEnabled(getContext())) {
			return null;
		}
		// TODO: Implement this to handle requests to insert a new row.

		switch (uriMatcher.match(uri)) {
			case CODE_FILE -> {
				return insertFile(uri, values);
			}
			case CODE_FILE_ID -> {
				return insertItem(uri, values);
			}
		}

		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		if (!PreferencesHelper.isSdSyncEnabled(getContext())) {
			return -1;
		}
		// Implement this to handle requests to delete one or more rows.
		String fragment = getFragment(uri);

		if (uriMatcher.match(uri) == CODE_FILE_ID) {
			return fragment == null ? deleteFile(uri) : deleteItem(uri);
		}

		throw new UnsupportedOperationException("Delete not supported for " + uri);
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		if (!PreferencesHelper.isSdSyncEnabled(getContext())) {
			return -1;
		}
		// TODO: Implement this to handle requests to update one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private Cursor queryItem(Uri uri, String[] projection, String selection,
							 String[] selectionArgs, String sortOrder) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private Cursor queryFile(Uri uri, String[] projection, String selection,
							 String[] selectionArgs, String sortOrder) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private Cursor queryFiles(Uri uri, String[] projection, String selection,
							  String[] selectionArgs, String sortOrder) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}