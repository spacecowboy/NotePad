package com.nononsenseapps.helpers;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Methods to help navigate through Google's mess regarding file access.
 * These use the {@link File} API, so they work well only in
 * {@link Context#getExternalFilesDir}. Avoid these for Android 10 and higher,
 * prefer {@link FilePickerHelper} instead
 */
public final class FileHelper {

	/**
	 * Writes the given {@link String} to the given {@link File}. Does not work outside
	 * of the external files directory, due to bad design by Google
	 *
	 * @return TRUE if it worked, FALSE otherwise
	 */
	@Deprecated
	private static boolean writeStringToFile(String content, File target) {
		if (content == null || target == null) return false;
		if (target.isDirectory() || target.getParentFile() == null) return false;

		NnnLogger.debug(FileHelper.class,
				"Writing, with PrintStream, to file " + target.getAbsolutePath());
		try {
			target.getParentFile().mkdirs();
		} catch (SecurityException se) {
			NnnLogger.error(FileHelper.class, "Can't create: " + target.getParentFile());
			NnnLogger.exception(se);
			return false;
		}

		try (PrintStream out = new PrintStream(new FileOutputStream(target))) {
			out.print(content);
			out.close();
			return true;
		} catch (Exception e) {
			NnnLogger.exception(e);
			return false;
		}
	}

	/**
	 * @return the path of the directory where ORG files are saved,
	 * or NULL if it could not get one. This path is now hardcoded
	 * to something like Android/data/packagename
	 */
	public static String getUserSelectedOrgDir(@NonNull Context ctx) {
		// we are going to use the default directory:
		// /storage/emulated/0/Android/data/packagename/files/orgfiles/
		File dir = ctx.getExternalFilesDir("orgfiles");

		// most likely, the shared storage is not available in this device/emulator
		if (dir == null) return null;

		// must ensure that it exists
		if (!dir.exists()) dir.mkdirs();

		boolean ok = dir.exists() && dir.isDirectory() && dir.canWrite();
		if (ok) return dir.getAbsolutePath();
		else return null;
	}

	/**
	 * When you delete a file in android, additional attention is required.
	 * This function takes care of that. Does not work above API 29
	 *
	 * @return TRUE if it succeeded, FALSE otherwise
	 */
	public static boolean tryDeleteFile(@NonNull File toDelete, @NonNull Context context) {
		boolean contains = toDelete
				.getAbsolutePath()
				.contains(getUserSelectedOrgDir(context));
		// the File API only works in that directory
		if (!contains) return false;

		if (toDelete.exists()) {
			try {
				if (!toDelete.delete()) return false;
			} catch (SecurityException e) {
				return false;
			}
		}

		// once you successfully deleted it, you have to update the media scanner to
		// let android know that the file was deleted, ELSE IT WILL CRASH!
		MediaScannerConnection.scanFile(context, new String[] { toDelete.getAbsolutePath() },
				null, null);

		// wait a bit for the mediascanner to do its work
		// 2 seconds should be enough
		SystemClock.sleep(1900);

		return true;
	}

}
