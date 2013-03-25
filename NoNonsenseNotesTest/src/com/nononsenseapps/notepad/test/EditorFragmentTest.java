package com.nononsenseapps.notepad.test;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.notepad.MainActivity;

public class EditorFragmentTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private Instrumentation mInstrumentation;
	private MainActivity mActivity;

	public EditorFragmentTest() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
	      super.setUp();
	      mInstrumentation = getInstrumentation();
	      
	      setActivityInitialTouchMode(false);
	      
	      // Set activity Intent

	      mActivity = getActivity(); // get a references to the app under test

	      /*
	       * Get a reference to the main widget of the app under test, a Spinner
	       */
	      //mSpinner = (Spinner) mActivity.findViewById(com.android.demo.myactivity.R.id.Spinner01)
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@SmallTest
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
	}
}
