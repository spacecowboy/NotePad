/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.test;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.notepad.ui.dashclock.TasksSettings;
import com.squareup.spoon.Spoon;

/**
 * Verify that the activity opens OK on any screensize.
 */
public class DashClockSettingsTest extends
        ActivityInstrumentationTestCase2<TasksSettings> {

    private Instrumentation mInstrumentation;

    public DashClockSettingsTest() {
        super(TasksSettings.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mInstrumentation = getInstrumentation();

        setActivityInitialTouchMode(false);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @SmallTest
    public void testLoadOK() {
        assertNotNull(getActivity());
        Spoon.screenshot(getActivity(), "Activity_loaded");
    }
}
