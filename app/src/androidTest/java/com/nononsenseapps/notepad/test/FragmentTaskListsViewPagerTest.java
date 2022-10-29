package com.nononsenseapps.notepad.test;

import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import androidx.fragment.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.R;
import com.squareup.spoon.Spoon;

public class FragmentTaskListsViewPagerTest extends
		ActivityInstrumentationTestCase2<ActivityMain_> {

	private Instrumentation mInstrumentation;

	public FragmentTaskListsViewPagerTest() {
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
}
