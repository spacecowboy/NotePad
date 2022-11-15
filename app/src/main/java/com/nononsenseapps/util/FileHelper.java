package com.nononsenseapps.util;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.FileDescriptor;

/**
 * Methods to help navigate through Google's mess regarding file access
 * in android 10 and higher. Functions that start with "document" are
 * related to {@link DocumentFile} objects
 */
public final class FileHelper {

	public static boolean documentIsWritableFolder(DocumentFile docDir) {
		return docDir != null && docDir.exists() && docDir.isDirectory() && docDir.canWrite();
	}

	/**
	 * @return a {@link FileDescriptor} for the File at the given {@link Uri}, or NULL if it
	 * could not find one
	 */
	public static FileDescriptor getFileDescriptor(@NonNull Uri docUri,
												   @NonNull Context context) {
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
}
