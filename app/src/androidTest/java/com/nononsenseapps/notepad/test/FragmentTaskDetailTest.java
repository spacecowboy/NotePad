package com.nononsenseapps.notepad.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.fragment.app.Fragment;

import com.nononsenseapps.notepad.espresso_tests.BaseTestClass;
import com.nononsenseapps.notepad.espresso_tests.EspressoHelper;

import org.junit.Test;


public class FragmentTaskDetailTest extends BaseTestClass {

	@Test
	public void testFragmentLoaded() {

		EspressoHelper.hideShowCaseViewIfShown();
		EspressoHelper.createNoteWithName("test note content");

		Fragment fragment = mActRule
				.getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(com.nononsenseapps.notepad.ActivityMain.DETAILTAG);
		assertNotNull("Editor should NOT be null", fragment);
		assertTrue("Editor should be visible",
				fragment.isAdded() && fragment.isVisible());

		Helper.takeScreenshot("Editor_loaded");

	}
}
