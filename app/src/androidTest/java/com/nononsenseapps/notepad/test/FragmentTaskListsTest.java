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
import android.view.View;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.ui.list.ActivityList;
import com.squareup.spoon.Spoon;

public class FragmentTaskListsTest extends ActivityInstrumentationTestCase2<ActivityList> {

    static final int LISTFRAGMENT_CONTAINER = R.id.listfragment_container;
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
        assertNotNull("Fragment1-holder should always be present", getActivity().findViewById
                (LISTFRAGMENT_CONTAINER));
    }

    @SmallTest
    public void testFragmentLoaded() throws Exception {
        mInstrumentation.waitForIdleSync();

        Spoon.screenshot(getActivity(), "List_loaded");
        Fragment listFragment = getActivity().getSupportFragmentManager().findFragmentById
                (LISTFRAGMENT_CONTAINER);

        assertNotNull("List fragment should not be null", listFragment);
        assertTrue("List fragment should be visible", listFragment.isAdded() && listFragment
                .isVisible());

        View taskList = listFragment.getView().findViewById(android.R.id.list);

        assertNotNull("Could not find the list!", taskList);
    }
}
