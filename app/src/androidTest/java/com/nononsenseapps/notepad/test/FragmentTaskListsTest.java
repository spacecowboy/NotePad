package com.nononsenseapps.notepad.test;

import static org.junit.Assert.*;

import android.Manifest;
import android.graphics.Bitmap;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.R;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FragmentTaskListsTest {

	@Rule
	public ActivityTestRule<ActivityMain_> mActivityRule
			= new ActivityTestRule<>(ActivityMain_.class, false);

	@Rule
	public GrantPermissionRule mRuntimePermissionRule
			= GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

	@Test
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
		assertNotNull("Fragment1-holder should always be present",
				mActivityRule.getActivity().findViewById(R.id.fragment1));
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

	@Test
	public void testFragmentLoaded() {
		InstrumentationRegistry.getInstrumentation().waitForIdleSync();
		assertNotNull(mActivityRule.getActivity());

		Fragment listPagerFragment = mActivityRule
				.getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(com.nononsenseapps.notepad.ActivityMain.LISTPAGERTAG);

		assertNotNull("List pager fragment should not be null", listPagerFragment);
		assertTrue("List pager fragment should be visible",
				listPagerFragment.isAdded() && listPagerFragment.isVisible());

		ListView taskList = (ListView) listPagerFragment
				.getView()
				.findViewById(android.R.id.list);

		assertNotNull("Could not find the list!", taskList);

		takeScreenshot("List_loaded");
	}
}
