package com.nononsenseapps.notepad.test;

import static org.junit.Assert.*;

import android.Manifest;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.R;

import org.junit.Rule;
import org.junit.Test;

public class FragmentTaskListsTest {

	@Rule
	public ActivityTestRule<ActivityMain_> mActivityRule
			= new ActivityTestRule<>(ActivityMain_.class, false);

	@Rule
	public GrantPermissionRule mRuntimePermissionRule
			= GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

	@Test
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
		assertNotNull("Fragment1-holder should always be present",
				mActivityRule.getActivity().findViewById(R.id.fragment1));
	}

	@Test
	public void testFragmentLoaded() {
		InstrumentationRegistry.getInstrumentation().waitForIdleSync();
		assertNotNull(mActivityRule.getActivity());

		Fragment listPagerFragment = mActivityRule
				.getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(com.nononsenseapps.notepad.ActivityMain.LISTPAGERTAG);

		assertNotNull("List pager fragment should not be null", listPagerFragment);
		assertTrue("List pager fragment should be visible",
				listPagerFragment.isAdded() && listPagerFragment.isVisible());

		ListView taskList = (ListView) listPagerFragment
				.getView()
				.findViewById(android.R.id.list);

		assertNotNull("Could not find the list!", taskList);

		Helper.takeScreenshot("List_loaded");
	}
}
