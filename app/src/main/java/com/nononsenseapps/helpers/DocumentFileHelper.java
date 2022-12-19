package com.nononsenseapps.helpers;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.nononsenseapps.notepad.prefs.BackupPrefs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.util.function.Function;

/**
 * Functions to work with {@link DocumentFile}
 */
public final class DocumentFileHelper {

	public static boolean isWritableFolder(DocumentFile docDir) {
		return docDir != null && docDir.exists() && docDir.isDirectory() && docDir.canWrite();
	}

	/**
	 * Get a {@link FileDescriptor} for the file at the given {@link Uri} and
	 * run the code in the {@link Function}
	 *
	 * @return TRUE if it finished, FALSE if there was an error
	 */
	private static boolean doWithFileDescriptorFor(@NotNull DocumentFile target,
												   @NotNull Context context,
												   Function<FileDescriptor, Boolean> function) {
		try {
			ParcelFileDescriptor pfd = context
					.getContentResolver()
					.openFileDescriptor(target.getUri(), "rw");
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
	 * Write "content" in "destination" using the {@link DocumentFile} API
	 *
	 * @return TRUE if it succeeded, FALSE otherwise
	 */
	public static boolean write(String content, DocumentFile destination, Context context) {
		if (content == null || destination == null || context == null) return false;
		return doWithFileDescriptorFor(destination, context, fd -> {
			try {
				var fileOutputStream = new FileOutputStream(fd);
				fileOutputStream.write(content.getBytes());
				// Let the document provider know you're done by closing the stream.
				fileOutputStream.close();
			} catch (Exception e) {
				return false;
			}
			return true;
		});
	}

	/**
	 * Hardcoded filename of the backup file. The user chooses where to save this
	 */
	private static final String backupJsonFileName = "NoNonsenseNotes_Backup.json";


	/**
	 * Delete the existing Json file and create a new one, for the backup
	 *
	 * @return the newly created {@link DocumentFile}, or null if it wasn't possible to create one
	 */
	public static DocumentFile createBackupJsonFile(Context context) {
		Uri dirUri = BackupPrefs.getSelectedBackupDirUri(context);
		if (dirUri == null) return null;

		var docDir = DocumentFile.fromTreeUri(context, dirUri);
		if (docDir == null) return null;
		var oldDocFile = docDir.findFile(backupJsonFileName);
		if (oldDocFile != null && oldDocFile.exists()) {
			// already exists => delete it before creating a new one
			oldDocFile.delete();
		}

		// android doesn't care about the mimetype anyway, having the extension
		// in displayName is enough
		String mt = MimeTypeMap.getSingleton().getMimeTypeFromExtension("json");
		return docDir.createFile(mt, backupJsonFileName);
	}

	/**
	 * @return the {@link DocumentFile} representing the json file that will be read to restore
	 * the backup, or NULL if it could not find the file. It's in the user-selected backup
	 * directory. See {@link BackupPrefs}
	 */
	@Nullable
	public static DocumentFile getSelectedBackupJsonFile(Context context) {
		var dirUri = BackupPrefs.getSelectedBackupDirUri(context);
		// user didn't choose a folder
		if (dirUri == null) return null;

		var dirDoc = DocumentFile.fromTreeUri(context, dirUri);
		if (dirDoc == null) return null;
		return dirDoc.findFile("NoNonsenseNotes_Backup.json");
	}

}
