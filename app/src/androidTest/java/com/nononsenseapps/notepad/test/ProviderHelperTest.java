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

package com.nononsenseapps.notepad.test;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.nononsenseapps.notepad.android.provider.ProviderHelperKt;

public class ProviderHelperTest {

    @Test
    public void testGetRelativePath() throws Exception {
        assertEquals("/foo/bar",
                ProviderHelperKt.getRelativePath("/ACTION/foo/bar"));
        assertEquals("/foo/bar",
                ProviderHelperKt.getRelativePath("ACTION/foo/bar"));
        assertEquals("/",
                ProviderHelperKt.getRelativePath("/Action"));
        assertEquals("/",
                ProviderHelperKt.getRelativePath("Action"));
    }

    @Test
    public void testSplit() throws Exception {
        String[] expected = new String[] {"foo", "bar", "baz"};

        assertArrayEquals(expected, ProviderHelperKt.split("/foo/bar/baz"));
        assertArrayEquals(expected, ProviderHelperKt.split("foo/bar/baz"));
        assertArrayEquals(expected, ProviderHelperKt.split("foo/bar/baz/"));
        assertArrayEquals(expected, ProviderHelperKt.split("/foo/bar/baz/"));

        assertArrayEquals(expected, ProviderHelperKt.split("//foo///bar////baz/////"));

        assertArrayEquals(new String[]{}, ProviderHelperKt.split("/"));
    }

    @Test
    public void testFirstPart() throws Exception {
        assertEquals("", ProviderHelperKt.firstPart(""));
        assertEquals("foo", ProviderHelperKt.firstPart("foo"));
        assertEquals("foo", ProviderHelperKt.firstPart("/foo"));
        assertEquals("foo", ProviderHelperKt.firstPart("/foo/bar"));
        assertEquals("foo", ProviderHelperKt.firstPart("foo/bar"));
    }

    @Test
    public void testRestPart() throws Exception {
        assertEquals("", ProviderHelperKt.restPart("foo"));
        assertEquals("", ProviderHelperKt.restPart("/foo"));
        assertEquals("", ProviderHelperKt.restPart("/foo/"));
        assertEquals("bar/baz", ProviderHelperKt.restPart("/foo/bar/baz"));
        assertEquals("bar/baz", ProviderHelperKt.restPart("foo/bar/baz"));
        assertEquals("bar/baz", ProviderHelperKt.restPart("/foo/bar/baz"));
    }

    @Test
    public void testMatchPath() throws Exception {
        // Root cases
        assertEquals(ProviderHelperKt.URI_ROOT,
                ProviderHelperKt.matchPath(null));
        assertEquals(ProviderHelperKt.URI_ROOT,
                ProviderHelperKt.matchPath(""));
        assertEquals(ProviderHelperKt.URI_ROOT,
                ProviderHelperKt.matchPath("/"));

        // List
        assertEquals(ProviderHelperKt.URI_LIST,
                ProviderHelperKt.matchPath("/list"));
        assertEquals(ProviderHelperKt.URI_LIST,
                ProviderHelperKt.matchPath("/list/"));
        assertEquals(ProviderHelperKt.URI_LIST,
                ProviderHelperKt.matchPath("list/"));
        assertEquals(ProviderHelperKt.URI_LIST,
                ProviderHelperKt.matchPath("/list/"));

        assertEquals(ProviderHelperKt.URI_LIST,
                ProviderHelperKt.matchPath("/list/foo"));
        assertEquals(ProviderHelperKt.URI_LIST,
                ProviderHelperKt.matchPath("list/foo/bar/"));

        // Details
        assertEquals(ProviderHelperKt.URI_DETAILS,
                ProviderHelperKt.matchPath("/details/foo"));
        assertEquals(ProviderHelperKt.URI_DETAILS,
                ProviderHelperKt.matchPath("details/foo/bar/"));

        // These uris are invalid
        assertEquals(ProviderHelperKt.URI_NOMATCH,
                ProviderHelperKt.matchPath("details"));

        assertEquals(ProviderHelperKt.URI_NOMATCH,
                ProviderHelperKt.matchPath("details/"));

        assertEquals(ProviderHelperKt.URI_NOMATCH,
                ProviderHelperKt.matchPath("unknownpredicate/foo/bar"));
    }

    @Test
    public void testJoin() throws Exception {
        assertEquals("/foo/bar", ProviderHelperKt.join("/foo", "bar"));
        assertEquals("/foo/bar", ProviderHelperKt.join("/foo", "/bar"));
        assertEquals("/foo/bar", ProviderHelperKt.join("/foo/", "/bar"));
        assertEquals("/foo/bar", ProviderHelperKt.join("/foo/", "bar"));
        assertEquals("/", ProviderHelperKt.join("/", "/"));
        assertEquals("/", ProviderHelperKt.join("/", ""));
        assertEquals("/", ProviderHelperKt.join("", "/"));
    }
}