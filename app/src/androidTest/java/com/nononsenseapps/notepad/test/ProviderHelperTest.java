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

import com.nononsenseapps.notepad.android.provider.ProviderHelper;

public class ProviderHelperTest {

	@Test
	public void testGetRelativePath() throws Exception {
		assertEquals("/foo/bar",
				ProviderHelper.getRelativePath("/ACTION/foo/bar"));
		assertEquals("/foo/bar",
				ProviderHelper.getRelativePath("ACTION/foo/bar"));
		assertEquals("/",
				ProviderHelper.getRelativePath("/Action"));
		assertEquals("/",
				ProviderHelper.getRelativePath("Action"));
	}

	@Test
	public void testFirstPart() throws Exception {
		assertEquals("", ProviderHelper.firstPart(""));
		assertEquals("foo", ProviderHelper.firstPart("foo"));
		assertEquals("foo", ProviderHelper.firstPart("/foo"));
		assertEquals("foo", ProviderHelper.firstPart("/foo/bar"));
		assertEquals("foo", ProviderHelper.firstPart("foo/bar"));
	}

	@Test
	public void testRestPart() throws Exception {
		assertEquals("", ProviderHelper.restPart("foo"));
		assertEquals("", ProviderHelper.restPart("/foo"));
		assertEquals("", ProviderHelper.restPart("/foo/"));
		assertEquals("bar/baz", ProviderHelper.restPart("/foo/bar/baz"));
		assertEquals("bar/baz", ProviderHelper.restPart("foo/bar/baz"));
		assertEquals("bar/baz", ProviderHelper.restPart("/foo/bar/baz"));
	}

	@Test
	public void testMatchPath() throws Exception {
		// Root cases
		assertEquals(ProviderHelper.URI_ROOT,
				ProviderHelper.matchPath(null));
		assertEquals(ProviderHelper.URI_ROOT,
				ProviderHelper.matchPath(""));
		assertEquals(ProviderHelper.URI_ROOT,
				ProviderHelper.matchPath("/"));

		// List
		assertEquals(ProviderHelper.URI_LIST,
				ProviderHelper.matchPath("/list"));
		assertEquals(ProviderHelper.URI_LIST,
				ProviderHelper.matchPath("/list/"));
		assertEquals(ProviderHelper.URI_LIST,
				ProviderHelper.matchPath("list/"));
		assertEquals(ProviderHelper.URI_LIST,
				ProviderHelper.matchPath("/list/"));

		assertEquals(ProviderHelper.URI_LIST,
				ProviderHelper.matchPath("/list/foo"));
		assertEquals(ProviderHelper.URI_LIST,
				ProviderHelper.matchPath("list/foo/bar/"));

		// Details
		assertEquals(ProviderHelper.URI_DETAILS,
				ProviderHelper.matchPath("/details/foo"));
		assertEquals(ProviderHelper.URI_DETAILS,
				ProviderHelper.matchPath("details/foo/bar/"));

		// These uris are invalid
		assertEquals(ProviderHelper.URI_NOMATCH,
				ProviderHelper.matchPath("details"));

		assertEquals(ProviderHelper.URI_NOMATCH,
				ProviderHelper.matchPath("details/"));

		assertEquals(ProviderHelper.URI_NOMATCH,
				ProviderHelper.matchPath("unknownpredicate/foo/bar"));
	}

	@Test
	public void testJoin() throws Exception {
		assertEquals("/foo/bar", ProviderHelper.join("/foo", "bar"));
		assertEquals("/foo/bar", ProviderHelper.join("/foo", "/bar"));
		assertEquals("/foo/bar", ProviderHelper.join("/foo/", "/bar"));
		assertEquals("/foo/bar", ProviderHelper.join("/foo/", "bar"));
		assertEquals("/", ProviderHelper.join("/", "/"));
		assertEquals("/", ProviderHelper.join("/", ""));
		assertEquals("/", ProviderHelper.join("", "/"));
	}
}