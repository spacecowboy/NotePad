package com.nononsenseapps.notepad.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.test.rule.ActivityTestRule;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.database.Task;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class FragmentTaskDetailTest {

	/**
	 * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
	 * for ActivityInstrumentationTestCase2.
	 *
	 * Rules are interceptors which are executed for each test method and will run before
	 * any of your setup code in the @Before method.
	 *
	 * {@link ActivityTestRule} will create and launch of the activity for you and also expose
	 * the activity under test. To get a reference to the activity you can use
	 * the {@link ActivityTestRule#getActivity()} method.
	 */
	// the replacement, ActivityScenarioRule does not work
	@SuppressWarnings("deprecation")
	@Rule
	public ActivityTestRule<ActivityMain_> mActivityRule
			= new ActivityTestRule<>(ActivityMain_.class, false);

	@Before
	public void setUp() {
		// Set activity Intent
		// Intent should be task id
		Intent i = new Intent();
		i.setAction(Intent.ACTION_EDIT).setData(Task.getUri(1L));

		mActivityRule.launchActivity(i);
	}

	@Test
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
	}

	@Test
	public void testFragmentLoaded() {
		Fragment fragment = mActivityRule
				.getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(com.nononsenseapps.notepad.ActivityMain.DETAILTAG);
		assertNotNull("Editor should NOT be null", fragment);
		assertTrue("Editor should be visible",
				fragment.isAdded() && fragment.isVisible());

		Helper.takeScreenshot("Editor_loaded");
	}
}
