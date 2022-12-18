package com.nononsenseapps.helpers;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

/**
 * Contains helper methods to deal with {@link MediaStore}, which is the only way
 * to save files in Documents/ and Download/ in Android API 29 and greater
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
public final class MediaStoreHelper {

	// forbid instances: it's a static class
	private MediaStoreHelper() {}

	/**
	 * @return the content of the file at the given {@link Uri}
	 */
	@Nullable
	private static String readTextFromUri(Context context, Uri source) {
		try {
			InputStream is = context.getContentResolver().openInputStream(source);
			Scanner s = new Scanner(is).useDelimiter("\\A");
			return s.hasNext() ? s.next() : null;
		} catch (FileNotFoundException ignored) {
			return null;
		}
	}

	/**
	 * @param relativePath either {@link Environment#DIRECTORY_DOCUMENTS}
	 *                     or {@link Environment#DIRECTORY_DOWNLOADS}
	 * @return the {@link Uri} pointing to "target" if it already exists,
	 * or NULL if no such file exists
	 */
	@Nullable
	private static Uri getExistingFileUri(Context context, String relativePath, File target) {
		// represents /storage/emulated/0 but it's not a path, it's like content://
		Uri dirUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

		// SQL arguments for the contentprovider. The RELATIVE_PATH may be saved
		// as "Documents/", so we have to use "like %?%" and not "=?"
		String selection = MediaStore.MediaColumns.RELATIVE_PATH + " like ?";
		String[] selectionArgs = new String[] { "%" + relativePath + "%" };

		Cursor cursor = context
				.getContentResolver()
				.query(dirUri, null, selection, selectionArgs, null);
		if (cursor.getCount() == 0) return null;

		while (cursor.moveToNext()) {
			int ci1 = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
			String fileName = cursor.getString(ci1);

			// "target.getName()" must include the extension. For the Json backup, we're good
			if (fileName.equals(target.getName())) {
				int ci2 = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
				long id = cursor.getLong(ci2);
				Uri uri = ContentUris.withAppendedId(dirUri, id);
				return uri;
			}
		}
		NnnLogger.debug(MediaStoreHelper.class, "No match found for: " + target.getName());
		return null;
	}

	/**
	 * Saves "content" to a file in "target", whose path includes "relativePath".
	 * Made only for {@link FileHelper}
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	public static boolean saveTextToFile(Context context, String content, File target,
								   String relativePath) {
		// check if the file already exists
		Uri uri = getExistingFileUri(context, relativePath, target);
		if (uri == null) {
			// file not found => create new one
			return writeStringToNewFile(context, target, content, relativePath);
		}
		// else, the file already exists => overwrite it
		try {
			OutputStream outputStream = context
					.getContentResolver()
					.openOutputStream(uri, "rwt"); // overwrite mode
			outputStream.write(content.getBytes());
			outputStream.close();
			// File written successfully
			return true;
		} catch (IOException e) {
			NnnLogger.exception(e);
			return false;
		}
	}

	// TODO test and use this to save files. note that it can't overwrite files,
	//  so the restore functionality should have a file picker to chose which json to use
	//  see https://stackoverflow.com/a/62879112/6307322

	/**
	 * Saves "content" in "target", located in "relativePath", using {@link MediaStore}.
	 * Creates a new file.
	 *
	 * @return TRUE if it succeeded, FALSE otherwise
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	private static boolean writeStringToNewFile(Context context, File target, String content,
												String relativePath) {
		NnnLogger.debug(MediaStoreHelper.class,
				"Writing, with MediaStore, to new file " + target.getAbsolutePath());

		// use mediastore to bypass filesystem writing permissions
		ContentValues values = new ContentValues();
		values.put(MediaStore.MediaColumns.DISPLAY_NAME, target.getName());
		// superfluous, because we already give the extension to MediaColumns.DISPLAY_NAME
		// values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
		values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

		Uri dirUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
		// useless alternatives:
		// Uri u1 = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
		// Uri u4 = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

		Uri fileUri = context.getContentResolver().insert(dirUri, values);
		if (fileUri == null) return false;
		try {
			OutputStream outputStream = context
					.getContentResolver()
					.openOutputStream(fileUri);
			byte[] bytes = content.getBytes();
			outputStream.write(bytes);
			outputStream.close();
			return true;
		} catch (IOException e) {
			NnnLogger.exception(e);
			return false;
		}
		// TODO TEST! on API 23, in all folders, with old & existing file, with a file from outside the phone, ...
	}
}
