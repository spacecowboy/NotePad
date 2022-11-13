package com.nononsenseapps.notepad.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.ActivityTestRule;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.espresso_tests.BaseTestClass;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class FragmentTaskDetailTest {

	/**
	 * Set activity Intent. Intent should be task id
	 */
	Intent iLaunch = new Intent().setAction(Intent.ACTION_EDIT).setData(Task.getUri(1L));

	@Rule
	public ActivityScenarioRule<ActivityMain_> mAsRule = new ActivityScenarioRule<>(iLaunch);

	@Test
	public void testFragmentLoaded() {
		mAsRule.getScenario().onActivity(a -> {
			Fragment fragment = a.getSupportFragmentManager()
					.findFragmentByTag(com.nononsenseapps.notepad.ActivityMain.DETAILTAG);
			assertNotNull("Editor should NOT be null", fragment);
			assertTrue("Editor should be visible",
					fragment.isAdded() && fragment.isVisible());
		});

		Helper.takeScreenshot("Editor_loaded");
	}
}
