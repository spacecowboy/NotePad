package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.database.Task;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.TextView;


public class FragmentTaskDetailTest extends
		ActivityInstrumentationTestCase2<ActivityMain_> {

	private Instrumentation mInstrumentation;
	private ActivityMain_ mActivity;

	public FragmentTaskDetailTest() {
		super(ActivityMain_.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mInstrumentation = getInstrumentation();

		setActivityInitialTouchMode(false);

		// Set activity Intent
		// Intent should be task id
		Intent i = new Intent();
		i.setAction(Intent.ACTION_EDIT).setData(
				Task.getUri(1L));
		
		setActivityIntent(i);
		mActivity = getActivity(); // get a references to the app under test

		/*
		 * Get a reference to the main widget of the app under test, a Spinner
		 */
		// mSpinner = (Spinner)
		// mActivity.findViewById(com.android.demo.myactivity.R.id.Spinner01)
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@SmallTest
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
	}
	
	public void testFragmentLoaded() {
		View taskText = mActivity.findViewById(com.nononsenseapps.notepad.R.id.taskText);
		assertNotNull("Could not find the editor!", taskText);
	}
}
