/*
 * Copyright (c) 2015. Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.test;

import android.app.Instrumentation;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.ListView;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.activities.ActivityList;
import com.squareup.spoon.Spoon;

public class FragmentTaskListsTest extends
		ActivityInstrumentationTestCase2<ActivityList> {

	private Instrumentation mInstrumentation;

	public FragmentTaskListsTest() {
		super(ActivityList.class);
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

	@SmallTest
	public void testFragmentLoaded() throws Exception {
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
}
