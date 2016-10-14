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
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.ui.editor.ActivityEditor;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.squareup.spoon.Spoon;

public class FragmentTaskDetailTest extends ActivityInstrumentationTestCase2<ActivityEditor> {

    protected Instrumentation mInstrumentation;

    public FragmentTaskDetailTest() {
        super(ActivityEditor.class);
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

    @SmallTest
    public void testFragmentLoaded() {
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id
                .fragment);
        Spoon.screenshot(getActivity(), "Editor_loaded");
        assertNotNull("Editor should NOT be null", fragment);
        assertTrue("Editor should be visible", fragment.isAdded() && fragment.isVisible());
    }
}
