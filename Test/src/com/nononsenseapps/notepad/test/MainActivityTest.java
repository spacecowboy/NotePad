package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.MainActivity;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private MainActivity mActivity;
	private Instrumentation mInstrumentation;

	public MainActivityTest() {
		super(MainActivity.class);
	}
	
	protected void setUp() throws Exception {
	      super.setUp();
	      mInstrumentation = getInstrumentation();
	      
	      setActivityInitialTouchMode(false);

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
