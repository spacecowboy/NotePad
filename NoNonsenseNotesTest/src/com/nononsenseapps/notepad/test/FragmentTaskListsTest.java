package com.nononsenseapps.notepad.test;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.ListView;

import com.nononsenseapps.notepad.R;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;
import com.squareup.spoon.Spoon;

public class FragmentTaskListsTest extends
		ActivityInstrumentationTestCase2<ActivityMain_> {

	private Instrumentation mInstrumentation;

	public FragmentTaskListsTest() {
		super(ActivityMain_.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mInstrumentation = getInstrumentation();

		setActivityInitialTouchMode(false);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@SmallTest
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
		assertNotNull("Fragment1-holder should always be present",
				getActivity().findViewById(R.id.fragment1));
	}

	private void testFragmentLoaded() throws InterruptedException {
		mInstrumentation.waitForIdleSync();
		
		Spoon.screenshot(getActivity(), "List_loaded");
		Fragment listPagerFragment = getActivity().getSupportFragmentManager()
					.findFragmentByTag(
							com.nononsenseapps.notepad.ActivityMain.LISTPAGERTAG);

		assertNotNull("List pager fragment should not be null",
				listPagerFragment);
		assertTrue("List pager fragment should be visible",
				listPagerFragment.isAdded() && listPagerFragment.isVisible());

		ListView taskList = (ListView) listPagerFragment.getView()
				.findViewById(android.R.id.list);

		assertNotNull("Could not find the list!", taskList);
	}

	private void recreate() {
		mInstrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				getActivity().recreate();
			}
		});
	}

	@SmallTest
	public void testFragmentLoadedPortraitLandscape()
			throws InterruptedException {
		getActivity();
		mInstrumentation.waitForIdleSync();
		Spoon.screenshot(getActivity(), "List_loaded_initial");
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// Load activity again
		//recreate();
		mInstrumentation.waitForIdleSync();
		Thread.sleep(100);
		testFragmentLoaded();
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		// Load activity again
		//recreate();
		mInstrumentation.waitForIdleSync();
		Thread.sleep(100);
		testFragmentLoaded();
		// And back again
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// Load activity again
		//recreate();
		mInstrumentation.waitForIdleSync();
		Thread.sleep(100);
		testFragmentLoaded();
	}
}
