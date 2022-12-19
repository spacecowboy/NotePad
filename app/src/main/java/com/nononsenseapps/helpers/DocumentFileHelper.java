package com.nononsenseapps.helpers;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

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
	 */
	public static boolean write(String content, DocumentFile destination, Context context) {
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

}
