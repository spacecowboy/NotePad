package com.nononsenseapps.notepad.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Helper {

	public static Task getATask(final Context context) {
		Cursor c = context.getContentResolver().query(Task.URI, Task.Columns.FIELDS, null, null, null);
		Task result = null;
		if (c.moveToFirst())
			result = new Task(c);
		return result;
	}

	public static TaskList getATaskList(final Context context) {
		Cursor c = context.getContentResolver().query(TaskList.URI, TaskList.Columns.FIELDS, null, null, null);
		TaskList result = null;
		if (c.moveToFirst())
			result = new TaskList(c);
		return result;
	}

	/**
	 * Takes a screenshots and saves it as
	 * /storage/emulated/0/Android/data/com.nononsenseapps.notepad/files/screenshots/fileName.png
	 * This is mandatory in new android versions, since it's the only folder we can easily write to
	 *
	 * @param fileName
	 */
	public static void takeScreenshot(String fileName) {
		// wait a second for the activity to load.
		try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

		var tool = InstrumentationRegistry.getInstrumentation();
		Bitmap bmp = tool.getUiAutomation().takeScreenshot();

		File dir = tool.getTargetContext().getExternalFilesDir("screenshots");
		if (!dir.exists()) {
			assertTrue("Could not create directory", dir.mkdirs());
		}

		// the png file
		Path filePath = Paths.get(dir.getAbsolutePath(), fileName + ".png");

		try (var out = new FileOutputStream(filePath.toString())) {
			bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
		} catch (IOException e) {
			fail("Could not save png screenshot");
		}
	}
}
