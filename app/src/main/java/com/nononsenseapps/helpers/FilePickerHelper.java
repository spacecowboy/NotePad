package com.nononsenseapps.helpers;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;

/**
 * Methods to use android's built-in file picker.
 * More of a reference, rather than something useful
 */
public class FilePickerHelper {

	/**
	 * For onActivityResult
	 */
	public static final int REQ_CODE = 123321;

	/**
	 * Shows the system's default filepicker, to  let the user choose a directory. See:
	 * https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
	 *
	 * @param prefFragComp The settings page that launched this file picker
	 * @param initialDir   the starting directory to show, or NULL if you don't care
	 */
	public static void showFolderPickerActivity(PreferenceFragmentCompat prefFragComp,
												@Nullable Uri initialDir) {
		var i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

		// don't add this: it stops working on some devices, like the emulator with API 25!
		// i.setType(DocumentsContract.Document.MIME_TYPE_DIR);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			// get the previously selected Uri, if available
			if (initialDir != null) i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialDir);
			// else the filepicker will just open in its default state. whatever.
		}
		try {
			// Start the built-in filepicker
			prefFragComp.startActivityForResult(i, REQ_CODE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(prefFragComp.getContext(), R.string.file_picker_not_available,
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Called when the user picks a "directory" with the system's filepicker
	 *
	 * @param fromActivityResult an {@link Intent} from onActivityResult
	 * @param keyOfPrefToUpdate key of the preference where "uri" will be saved in
	 */
	public static void onUriPicked(Intent fromActivityResult, Context context,
								   String keyOfPrefToUpdate) {
		Uri uri = fromActivityResult.getData();
		// represents the directory that the user just picked
		// Use this instead of the "File" class
		DocumentFile docDir = DocumentFile.fromTreeUri(context, uri);

		// to maintain permission when the device restarts
		context.getContentResolver().takePersistableUriPermission(uri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

		if (DocumentFileHelper.isWritableFolder(docDir)) {
			// save the uri in the preferences, with the given key
			PreferenceManager
					.getDefaultSharedPreferences(context)
					.edit()
					.putString(keyOfPrefToUpdate, uri.toString())
					.apply();
		} else {
			Toast.makeText(context, R.string.cannot_write_to_directory, Toast.LENGTH_SHORT).show();
		}
	}

}
