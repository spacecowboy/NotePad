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

import static org.junit.Assert.assertNotNull;

import android.Manifest;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.nononsenseapps.notepad.dashclock.DashclockPrefActivity;

import org.junit.Rule;
import org.junit.Test;

/**
 * Verify that the activity opens OK on any screensize.
 */
public class DashClockSettingsTest {

	// the replacement, ActivityScenarioRule does not work
	@SuppressWarnings("deprecation")
	@Rule
	public ActivityTestRule<DashclockPrefActivity> mActivityRule
			= new ActivityTestRule<>(DashclockPrefActivity.class, false);

	@Rule
	public GrantPermissionRule writePermission
			= GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

	@Test
	public void testLoadOK() {
		assertNotNull(mActivityRule.getActivity());
		Helper.takeScreenshot("Activity_loaded");
	}
}
