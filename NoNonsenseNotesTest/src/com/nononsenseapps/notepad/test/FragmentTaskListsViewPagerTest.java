package com.nononsenseapps.notepad.test;

import android.app.Instrumentation;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.R;

public class FragmentTaskListsViewPagerTest extends
		ActivityInstrumentationTestCase2<ActivityMain_> {

	private Instrumentation mInstrumentation;
	private ActivityMain_ mActivity;
	private Fragment viewPagerFragment;

	public FragmentTaskListsViewPagerTest() {
		super(ActivityMain_.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mInstrumentation = getInstrumentation();

		setActivityInitialTouchMode(false);

		mActivity = getActivity(); // get a references to the app under test

		// List fragment will be here
		viewPagerFragment = mActivity.getSupportFragmentManager().findFragmentById(
				R.id.fragment1);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@SmallTest
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
		assertNotNull("Fragment1-holder should always be present",
				mActivity.findViewById(R.id.fragment1));
	}

	public void testFragmentLoaded() {
		assertNotNull("ListFragment should not be null", viewPagerFragment);
		// View taskList = listFragment.findViewById(android.R.id.list);
		// assertNotNull("Could not the list!", taskList);
	}

}
