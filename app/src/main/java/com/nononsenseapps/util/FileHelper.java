package com.nononsenseapps.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.prefs.SyncPrefs;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Methods to help navigate through Google's mess regarding file access in android 10
 * and higher.
 *
 * Functions that start with "document" are related to {@link DocumentFile}
 * objects, which Google recommends, but which we can still avoid, for now.
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
		File ourDir = new File(baseDir, "NoNonsenseNotes");

		// must ensure that it exists
		ourDir.mkdirs();
		return ourDir;
	}

	/**
	 * @return the path of the directory where ORG files are saved, or NULL if
	 * it could get one. It can be one of these: <br/>
	 * /storage/emulated/0/Android/data/packagename/files/orgfiles/ <br/>
	 * /storage/emulated/0/Documents/NoNonsenseNotes/
	 */
	public static String getUserSelectedOrgDir(@NonNull Context ctx) {
		// see if the user requested the Documents directory
		var sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		boolean useDocDir = sharedPrefs.getBoolean(SyncPrefs.KEY_SD_USE_DOC_DIR,false);

		// eventually you should use a DocumentFile from this:
		// Uri dir = Uri.parse( sharedPrefs.getString(SyncPrefs.KEY_SD_DIR_URI,null));

		File dir;
		if (useDocDir) {
			// the user requested the Documents directory => save org files in:
			// /storage/emulated/0/Documents/NoNonsenseNotes/
			dir = getAppExternalStorageFolder();
		} else {
			// we are going to use the default directory:
			// /storage/emulated/0/Android/data/packagename/files/orgfiles/
			dir = ctx.getExternalFilesDir("orgfiles");
		}

		boolean ok = dir != null && dir.exists() && dir.isDirectory() && dir.canWrite();
		if (ok) return dir.getAbsolutePath();
		else return null;
	}




}
