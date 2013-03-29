package com.nononsenseapps.notepad.test;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.ListView;

import com.nononsenseapps.notepad.R;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;

public class FragmentTaskListsTest extends
		ActivityInstrumentationTestCase2<ActivityMain_> {

	private Instrumentation mInstrumentation;
	private ActivityMain_ mActivity;
	private Fragment listFragment;
	private TaskListViewPagerFragment listPagerFragment;
	private ListView taskList;

	public FragmentTaskListsTest() {
		super(ActivityMain_.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mInstrumentation = getInstrumentation();

		setActivityInitialTouchMode(false);

		mActivity = getActivity(); // get a references to the app under test

		// List fragment will be here
		listPagerFragment = (TaskListViewPagerFragment) mActivity.getSupportFragmentManager()
				.findFragmentById(R.id.fragment1);
		
		if (listPagerFragment != null) {
			taskList = (ListView) listPagerFragment.getView().findViewById(android.R.id.list);
		}
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
		assertNotNull("ListPagerFragment should not be null", listPagerFragment);
		assertNotNull("Could not find the list!", taskList);
	}

}
