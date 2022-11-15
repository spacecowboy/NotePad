package com.nononsenseapps.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.nononsenseapps.helpers.NnnLogger;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Methods to help navigate through Google's mess regarding file access in android 10
 * and higher.
 *
 * Functions that start with "document" are related to {@link DocumentFile}
 * objects, which Google recommends, but which we can still avoid, for now.
 *
 *
 */
public final class FileHelper {

	public static boolean documentIsWritableFolder(DocumentFile docDir) {
		return docDir != null && docDir.exists() && docDir.isDirectory() && docDir.canWrite();
	}

	/**
	 * @return a {@link FileDescriptor} for the File at the given {@link Uri}, or NULL if it
	 * could not find one
	 */
	private static FileDescriptor getFileDescriptor(@NonNull Uri docUri,
												   @NonNull Context context) {
		// TODO this is here for the poor soul that will try to migrate from File to DocumentFile,
		//  but as of now this code is useless
		var docFile = DocumentFile.fromTreeUri(context, docUri);
		if (docFile == null || docFile.isDirectory()) return null;

		try {
			ParcelFileDescriptor parcelFileDescriptor = context
					.getContentResolver()
					.openFileDescriptor(docUri, "r");
			FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

			boolean ok = fileDescriptor.valid();

			return fileDescriptor;

		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Writes the given {@link String} to the given {@link File}
	 *
	 * @return TRUE if it worked, FALSE otherwise
	 */
	public static boolean writeStringToFile(String content, File target) {
		if (content == null || target == null) return false;

		try (PrintStream out = new PrintStream(new FileOutputStream(target.getAbsolutePath()))) {
			out.print(content);
			return true;
		} catch (Exception e) {
			NnnLogger.exception(e);
			return false;
		}
	}

	/**
	 * Returns the folder used by the app to save files with the normal {@link File} objects.
	 * It is a subdirectory of {@link Environment#DIRECTORY_DOCUMENTS}
	 */
	public static File getAppExternalStorageFolder() {
		// android 10 and newer don't allow us to make a folder in the "root" of the external
		// storage. The workaround is to make the directory in the "Documents" folder
		File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
		File ourDir = new File(baseDir,"No Nonsense Notes");

		// must ensure that it exists
		ourDir.mkdirs();
		return ourDir;
	}
}
