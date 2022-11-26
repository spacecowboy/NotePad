package com.nononsenseapps.notepad.test;

import androidx.test.platform.app.InstrumentationRegistry;

import com.nononsenseapps.helpers.FileHelper;

import junit.framework.TestCase;

import java.io.File;

/**
 * Various tests related to storage, filesystem, ...
 */
public class StorageTest extends TestCase {

	public void testIfExternalStorageIsAvailable() {
		var context = InstrumentationRegistry
				.getInstrumentation()
				.getTargetContext();
		File dir = context.getExternalFilesDir("example");
		assertNotNull("External storage is not available!", dir);
		String dir2 = FileHelper.getUserSelectedOrgDir(context);
		assertNotNull("Can't determine org directory!", dir2);
	}

}
