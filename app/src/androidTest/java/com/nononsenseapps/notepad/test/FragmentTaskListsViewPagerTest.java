package com.nononsenseapps.notepad.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.rule.ActivityTestRule;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.R;

import org.junit.Rule;
import org.junit.Test;

public class FragmentTaskListsViewPagerTest {

	// the replacement, ActivityScenarioRule does not work
	@SuppressWarnings("deprecation")
	@Rule
	public final ActivityTestRule<ActivityMain_> mActivityRule
			= new ActivityTestRule<>(ActivityMain_.class, false);

	@Test
	public void testSanity() {
		assertEquals("This should succeed", 1, 1);
		assertNotNull("Error in the activityrule", mActivityRule.getActivity());
		assertNotNull("Fragment1-holder should always be present",
				mActivityRule.getActivity().findViewById(R.id.fragment1));
	}
}
