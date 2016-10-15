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

package com.nononsenseapps.notepad.util;

import java.util.Collection;
import java.util.Iterator;

public class ArrayHelper {

    public static <T> T[] toArray(T... items) {
        return items;
    }

    public static long[] toArray(Collection<Long> longs) {
        Iterator<Long> it = longs.iterator();
        long[] result = new long[longs.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }
}
