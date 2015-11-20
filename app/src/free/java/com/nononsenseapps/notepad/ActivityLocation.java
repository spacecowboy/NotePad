/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
