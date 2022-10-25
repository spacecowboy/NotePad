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

package com.nononsenseapps.notepad;

/**
 * Dummy implementation. See Play build flavor for actual class.
 */
public class ActivityLocation {
    public static final String EXTRA_LATITUDE = "DUMMY";
    public static final String EXTRA_LONGITUDE = "DUMMY";
    public static final String EXTRA_RADIUS = "DUMMY";
    public static final String EXTRA_LOCATION_NAME = "DUMMY";
    public static final String EXTRA_ID = "DUMMY";

    /**
     *
     * @return the class defined in the manifest.
     */
    public static Class getAnnotatedActivityClass() {
        return ActivityLocation.class;
    }
}
