package com.nononsenseapps.helpers;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Methods to help navigate through Google's mess regarding file access in
 * Android 10 and higher.
 */
public final class FileHelper {

	/**
	 * Writes the given {@link String} to the given {@link File}, using a method appropriate
	 * for every android API version
	 *
	 * @return TRUE if it worked, FALSE otherwise
	 */
	@Deprecated
	public static boolean writeStringToFile(String content, File target, Context context) {
		if (content == null || target == null) return false;
		if (target.isDirectory() || target.getParentFile() == null) return false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// bothersome requirements. Thanks Google
			String relPath = getRelativePathOrNull(target);
			if (relPath == null)
				return FileHelper.writeStringToFileSimple(content, target);
			else
				// will create & overwrite the file as needed
				return MediaStoreHelper.saveTextToFile(context, content, target, relPath);
		} else {
			return FileHelper.writeStringToFileSimple(content, target);
		}
	}

	/**
	 * @return the relative path to use with {@link MediaStore} in
	 * {@link MediaStoreHelper}, including the "No Nonsense Notes" subdirectory,
	 * or null if the simple {@link File} API should be used instead
	 */
	@Nullable
	@Deprecated
	private static String getRelativePathOrNull(File target) {
		// these directories require the write permission
		String dirDownload = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				.getAbsolutePath();
		boolean isInDownloadDir = target.getAbsolutePath().contains(dirDownload);

		String dirDocs = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
				.getAbsolutePath();
		boolean isInDocsDir = target.getAbsolutePath().contains(dirDocs);

		boolean hasSubDir = target.getAbsolutePath().contains("No Nonsense Notes");

		if (isInDownloadDir) {
			return hasSubDir
					? Environment.DIRECTORY_DOWNLOADS + "/No Nonsense Notes/"
					: Environment.DIRECTORY_DOWNLOADS;
		} else if (isInDocsDir) {
			return hasSubDir
					? Environment.DIRECTORY_DOCUMENTS + "/No Nonsense Notes/"
					: Environment.DIRECTORY_DOCUMENTS;
		} else {
			// file is in /Android/data/ so we can still use the simple function
			return null;
		}
	}

	/**
	 * Easy, but doesn't work in android API 29 and newer.
	 * Will create or overwrite the file automatically
	 *
	 * @return TRUE if it managed to write "content" to file "target"
	 */
	private static boolean writeStringToFileSimple(@NotNull String content, @NotNull File target) {
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
		// useless & does not work after API 28
		//		if (!tryDeleteFile(target, context)) return false;
		//		try {
		//			if (!target.createNewFile()) return false;
		//		} catch (IOException e) {
		//			// you just can't write to that folder
		//			return false;
		//		}

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
	 * @return the path of the directory where ORG files are saved, or NULL if
	 * it could not get one. This path is now hardcoded to something like Android/data/packagename
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
	 * @return a representation of the JSON file used for backups, located in the
	 * folder chosen by the user in the Backup preferences page. NOT guaranteed
	 * to be writable
	 */
	@NonNull
	public static File getBackupJsonFile(@NonNull Context ctx) {
		var sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String chosenPath = null; //sharedPrefs.getString(BackupPrefs.KEY_BACKUP_LOCATION, null);
		// TODO useless function. remove.
		if (chosenPath == null) {
			// the user did not choose a path yet => use a safe fallback path
			chosenPath = ctx.getExternalFilesDir(null).getAbsolutePath();
		}

		String fName = "NoNonsenseNotes_Backup.json";
		File fJson = new File(chosenPath, fName);

		// checks like .mkdirs() and .canWrite() are up to the caller.
		// The code already took care of those, anyway
		return fJson;
	}

	/**
	 * When you delete a file in android, additional attention is required.
	 * This function takes care of that
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
