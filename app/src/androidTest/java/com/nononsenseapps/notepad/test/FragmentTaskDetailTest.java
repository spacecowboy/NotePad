package com.nononsenseapps.notepad.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.espresso_tests.BaseTestClass;

import org.junit.Before;
import org.junit.Test;


public class FragmentTaskDetailTest extends BaseTestClass {

	@Before
	public void setUp() {
		// Set activity Intent
		// Intent should be task id
		Intent i = new Intent();
		i.setAction(Intent.ACTION_EDIT).setData(Task.getUri(1L));

		mActRule.launchActivity(i);
	}

	@Test
	public void testFragmentLoaded() {

		Fragment fragment = mActRule
				.getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(com.nononsenseapps.notepad.ActivityMain.DETAILTAG);
		assertNotNull("Editor should NOT be null", fragment);
		assertTrue("Editor should be visible",
				fragment.isAdded() && fragment.isVisible());

		Helper.takeScreenshot("Editor_loaded");

		// putting this here avoids crashes
		mActRule.finishActivity();
	}
}
