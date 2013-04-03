package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.database.Task;
import com.squareup.spoon.Spoon;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.TextView;

//import static org.fest.assertions.api.ANDROID.assertThat;

public class FragmentTaskDetailTest extends
		ActivityInstrumentationTestCase2<ActivityMain_> {

	protected Instrumentation mInstrumentation;

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
		i.setAction(Intent.ACTION_EDIT).setData(Task.getUri(1L));

		setActivityIntent(i);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@SmallTest
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
	}

	public void testFragmentLoaded() {
		Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(com.nononsenseapps.notepad.ActivityMain.DETAILTAG);
		Spoon.screenshot(getActivity(), "Editor_loaded");
		assertNotNull("Editor should NOT be null", fragment);
		assertTrue("Editor should be visible", fragment.isAdded() && fragment.isVisible());
		//assertThat(fragment).isUserVisible();
		//assertNotNull("Could not find the editor!", taskText);
	}
}
