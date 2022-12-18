package com.nononsenseapps.helpers;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.prefs.BackupPrefs;
import com.nononsenseapps.notepad.prefs.SyncPrefs;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Methods to help navigate through Google's mess regarding file access in
 * Android 10 and higher.
 *
 * Functions that start with "document" are related to {@link DocumentFile}
 * objects, which Google recommends, but which we can still avoid, for now.
 */
public final class FileHelper {

	public static boolean documentIsWritableFolder(DocumentFile docDir) {
		return docDir != null && docDir.exists() && docDir.isDirectory() && docDir.canWrite();
	}

	/**
	 * Get a {@link FileDescriptor} for the File at the given {@link Uri} and
	 * run the code in the {@link Function}
	 *
	 * @return TRUE if it finished, FALSE if there was an error
	 */
	private static boolean doWithFileDescriptorFor(@NonNull Uri docUri, @NonNull Context context,
												   Function<FileDescriptor, Void> function) {

		// TODO this is here for the poor soul that will try to migrate from File to DocumentFile,
		//  but as of now this code is useless
		var docFile = DocumentFile.fromTreeUri(context, docUri);
		if (docFile == null || docFile.isDirectory()) return false;

		try {
			ParcelFileDescriptor pfd = context
					.getContentResolver()
					.openFileDescriptor(docUri, "r");
			FileDescriptor fileDescriptor = pfd.getFileDescriptor();

			boolean ok = fileDescriptor.valid();
			if (!ok) return false;

			function.apply(fileDescriptor);
			pfd.close();
			return true;

		} catch (Exception ex) {
			NnnLogger.exception(ex);
			return false;
		}
	}

	/**
	 * Writes the given {@link String} to the given {@link File}, using a method appropriate
	 * for every android API version
	 *
	 * @return TRUE if it worked, FALSE otherwise
	 */
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
	 * @return the relative path to use with {@link MediaStore} in {@link MediaStoreHelper},
	 * or null if the simple {@link File} API should be used instead
	 */
	@Nullable
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

		if (isInDownloadDir) {
			return Environment.DIRECTORY_DOWNLOADS;
		} else if (isInDocsDir) {
			return Environment.DIRECTORY_DOCUMENTS;
		} else {
			// file is in /Android/data/ so we can still use the simple function
			return null;
		}
	}

	/**
	 * Easy, but doesn't work in android API 29 and newer
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
		// TODO useless & does not work
		// .
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
	 * it could not get one. The user can choose among a few selected folders, see
	 * {@link FileHelper#getPathsOfPossibleFolders(Context)}
	 */
	public static String getUserSelectedOrgDir(@NonNull Context ctx) {
		// see if the user requested a specific directory
		var sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String favDirPath = sharedPrefs.getString(SyncPrefs.KEY_SD_DIR, null);

		// eventually you should use a DocumentFile from this:
		// Uri dir = Uri.parse( sharedPrefs.getString(SyncPrefs.KEY_SD_DIR_URI, null));

		File dir;
		if (favDirPath != null) {
			// the user requested a specific directory => save org files there
			dir = new File(favDirPath);
		} else {
			// nothing was specified => we are going to use the default directory:
			// /storage/emulated/0/Android/data/packagename/files/orgfiles/
			dir = ctx.getExternalFilesDir("orgfiles");
		}

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
		String chosenPath = sharedPrefs.getString(BackupPrefs.KEY_BACKUP_LOCATION, null);
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
	 * Android 10 and newer don't allow us to make a folder in the "root" of the external
	 * storage. The workaround is to make the directory in the "Documents" folder, for example.
	 *
	 * @return an array of folder paths where files can be saved using the simple
	 * {@link File} API, without bothering with filepickers and the Storage Access
	 * Framework. The list consists of:<br/>
	 * the app's folder in <i>/Android/data/</i> <br/>
	 * the <i>Download</i> directory <br/>
	 * the <i>Documents</i> directory <br/>
	 * and subdirectories of those. <br/>
	 * Every other folder is either dedicated to audio files, therefore useless for us, or
	 * impossible to access in Android API >= 30 without using the DocumentFile API.
	 * @implNote for some of these you have to ask for write permissions
	 */
	public static String[] getPathsOfPossibleFolders(@NonNull Context context) {
		File dirDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		// (don't use DCIM, you can't write to it anymore with the File API)
		File dirDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
		String subDirName = "No Nonsense Notes/";

		var dirs = new File[] {
				// the safest, recommended option
				context.getExternalFilesDir(null),
				// The directories themselves
				dirDownload,
				dirDocs,
				// a subfolder that identifies the app
				new File(dirDownload, subDirName),
				new File(dirDocs, subDirName),
		};

		String[] paths = Arrays.stream(dirs)
				.map(File::getAbsolutePath)
				.toArray(String[]::new);
		return paths;
	}

	/**
	 * When you delete a file in android, additional attention is required.
	 * This function takes care of that
	 */
	public static boolean tryDeleteFile(@NonNull File toDelete, @NonNull Context context) {
		// TODO test on API 30+ I think this should be replaced by MediaStore
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

	/**
	 * Tested and works on API 32 & API 23 devices.
	 * Returns FALSE with folders that are unreachable for us, like /data/
	 *
	 * @return TRUE only if the given {@link File} is a folder that CAN be written to
	 * using the {@link File} API, but only if the user gives the
	 * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} permission.
	 */
	public static boolean folderNeedsAndroidWritePermission(@NonNull File folder) {
		if (!folder.isDirectory()) return false;

		String dirDownload = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				.getAbsolutePath();
		String dirDocs = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
				.getAbsolutePath();

		// these directories require the write permission
		if (folder.getAbsolutePath().contains(dirDownload)) return true;
		if (folder.getAbsolutePath().contains(dirDocs)) return true;

		// anything else: it's either writable without permissions, or unreachable at all
		return false;
	}

}
