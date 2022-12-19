package com.nononsenseapps.helpers;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.nononsenseapps.notepad.prefs.BackupPrefs;

import org.jetbrains.annotations.NotNull;

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
	 * Delete the existing Json file and create a new one, for the backup
	 *
	 * @return the newly created {@link DocumentFile}, or null if it wasn't possible to create one
	 */
	public static DocumentFile createBackupJsonFile(Context context) {
		String displayName = "NoNonsenseNotes_Backup.json";
		Uri dirUri = BackupPrefs.getSelectedBackupDirUri(context);
		if (dirUri == null) return null;

		var docDir = DocumentFile.fromTreeUri(context, dirUri);
		if (docDir == null) return null;
		var oldDocFile = docDir.findFile(displayName);
		if (oldDocFile != null && oldDocFile.exists()) {
			// already exists => delete it before creating a new one
			oldDocFile.delete();
		}

		// android doesn't care about the mimetype anyway, having the extension
		// in displayName is enough
		String mt = MimeTypeMap.getSingleton().getMimeTypeFromExtension("json");
		return docDir.createFile(mt, displayName);
	}

}
