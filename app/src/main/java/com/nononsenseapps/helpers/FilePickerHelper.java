package com.nononsenseapps.helpers;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.SyncPrefs;

/**
 * Methods to use android's built-in file picker.
 * More of a reference, rather than something useful
 */
public class FilePickerHelper {

	static void setPrefOnClickAnswer(Preference prefDirUri, PreferenceFragmentCompat pfc) {
		// when the user clicks on the settings entry to choose the directory, do this
		prefDirUri.setOnPreferenceClickListener(preference -> {
			boolean ok = PermissionsHelper
					.hasPermissions(pfc.getContext(), PermissionsHelper.FOR_SDCARD);
			if (ok) {
				// we CAN read the filesystem => show the filepicker
				showFolderPickerActivity(pfc);
			} else {
				pfc.requestPermissions(
						PermissionsHelper.FOR_SDCARD,
						PermissionsHelper.REQCODE_WRITE_SD);
				// continues in onRequestPermissionsResult()
			}
			// tell android to update the preference value
			return true;
		});
	}

	/**
	 * Shows the system's default filepicker, to  let the user choose a directory. See:
	 * https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
	 */
	static void showFolderPickerActivity(PreferenceFragmentCompat pfc) {
		var i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

		// don't add this: it stops working on some devices, like the emulator with API 25!
		// i.setType(DocumentsContract.Document.MIME_TYPE_DIR);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			// specify a URI for the directory that should be opened in
			// the system file picker when it loads.
			var sharPrefs = PreferenceManager
					.getDefaultSharedPreferences(pfc.getContext());

			// get the previously selected Uri, if available
			String oldSetting = null; //sharPrefs.getString(KEY_SD_DIR_URI, null);
			if (oldSetting != null) {
				Uri uriToLoad = Uri.parse(oldSetting);
				i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
			}
			// else the filepicker will just open in its default state. whatever.
		}
		try {
			// Start the built-in filepicker
			pfc.startActivityForResult(i, SyncPrefs.PICK_SD_DIR_CODE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(pfc.getContext(),
					R.string.file_picker_not_available, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Called when the user picks a "directory" with the system's filepicker
	 *
	 * @param uri points to the chosen "directory"
	 */
	public static void onSdDirUriPicked(Uri uri, Context context) {

		// represents the directory that the user just picked
		// Use this instead of the "File" class
		var docDir = DocumentFile.fromTreeUri(context, uri);

		if (FileHelper.documentIsWritableFolder(docDir)) {
			// save it
			PreferenceManager
					.getDefaultSharedPreferences(context)
					.edit()
					.putString("KEY_SD_DIR_URI", uri.toString()) // replace with your pref. key
					.apply();
			Toast.makeText(context, R.string.feature_is_WIP, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(context, R.string.cannot_write_to_directory, Toast.LENGTH_SHORT).show();
		}
	}

}
