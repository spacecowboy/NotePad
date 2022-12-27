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

package com.nononsenseapps.notepad.android.provider;

import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Objects;

import kotlin.collections.CollectionsKt;
import kotlin.text.Regex;
import kotlin.text.StringsKt;

/**
 * Helper functions related to provider operations.
 */
public final class ProviderHelperKt {

	public static final int URI_NOMATCH = -1;
	public static final int URI_ROOT = 101;
	public static final int URI_LIST = 102;
	public static final int URI_DETAILS = 103;
	public static final String URI_LIST_PREFIX = "list";
	public static final String URI_DETAILS_PREFIX = "details";

	/**
	 * Returns a list uri given a base and relativePath
	 *
	 * @param base         such as content://my.provider.authority
	 *                     *
	 * @param relativePath /foo/bar
	 *                     *
	 * @return the list uri: content://my.provider.authority/list/foo/bar
	 */
	public static Uri getListUri(@NotNull Uri base, @NotNull String relativePath) {
		return Uri.withAppendedPath(Uri.withAppendedPath(base, "list"),
				relativePath);
	}

	/**
	 * Returns a details uri given a base and relativePath
	 *
	 * @param base         such as content://my.provider.authority
	 *                     *
	 * @param relativePath /foo/bar
	 *                     *
	 * @return the details uri: content://my.provider.authority/details/foo/bar
	 */
	@NotNull
	public static Uri getDetailsUri(@NotNull Uri base, @NotNull String relativePath) {
		return Uri.withAppendedPath(Uri.withAppendedPath(base, "details"),
				relativePath);
	}

	/**
	 * Returns only the scheme and authority parts.
	 *
	 * @param uri like content://my.provider.authority/details/foo/bar
	 *            *
	 * @return uri with only scheme and authority: content://my.provider.authority
	 */
	public static Uri getBase(@NotNull Uri uri) {
		return Uri.parse(uri.getScheme() + "://" + uri.getAuthority());
	}

	/**
	 * Note that /ACTION will return "/".
	 *
	 * @param uri like content://my.provider.authority/ACTION/foo/bar
	 *            *
	 * @return relative path without action part like /foo/bar
	 */
	@NotNull
	public static String getRelativePath(@NotNull Uri uri) {
		return getRelativePath(uri.getPath());
	}

	@NotNull
	public static String getRelativePath(@NotNull String path) {
		var i = path.indexOf("/");
		if (i == 0) {
			return getRelativePath(path.substring(1));
		} else if (i < 0) {
			return "/";
		} else {
			return path.substring(i);
		}
	}

	/**
	 * @param path like /foo/bar
	 *             *
	 * @return first part of path like foo
	 */
	@NotNull
	public static String firstPart(@NotNull String path) {
		var i = path.indexOf("/");
		if (i == 0) {
			return firstPart(path.substring(1));
		} else if (i > 0) {
			return path.substring(0, i);
		} else {
			// No slashes
			return path;
		}
	}


	/**
	 * If nothing remains, returns the empty string.
	 *
	 * @param path like /foo/bar/baz
	 *             *
	 * @return the bit after first like bar/baz (without starting slash)
	 */
	@NotNull
	public static String restPart(@NotNull String path) {
		var i = path.indexOf("/");
		if (i == 0) {
			return restPart(path.substring(1));
		} else if (i > 0) {
			return path.substring(i + 1);
		} else {
			return "";
		}
	}

	public static int matchUri(@NotNull Uri uri) {
		return matchPath(uri.getPath());
	}


	/**
	 * Since UriMatcher isn't as good as it should be, this implements the matching I want.
	 *
	 * @param path like /foo/bar/baz
	 *             *
	 * @return type of the path
	 */
	public static int matchPath(@Nullable String path) {
		while (true) {
			if (path != null && ((CharSequence) path).length() != 0) {
				String var10000;
				if (StringsKt.startsWith(path, "/", false)) {
					var10000 = path.substring(1);

					path = var10000;
					continue;
				}

				var10000 = firstPart(path).toLowerCase(Locale.ROOT);

				String fp = var10000;
				if (Objects.equals(fp, "list")) {
					return 102;
				}

				if (Objects.equals(fp, "details")) {
					if (((CharSequence) restPart(path)).length() == 0) {
						return -1;
					}

					return 103;
				}

				return -1;
			}

			return 101;
		}
	}

	/**
	 * Join two pieces together, separated by a /
	 *
	 * @param path1 like /foo
	 *              *
	 * @param path2 like bar
	 *              *
	 * @return /foo/bar
	 */
	@NotNull
	public static String join(@NotNull String path1, @NotNull String path2) {
		if (path1.endsWith("/")) {
			if (path2.startsWith("/")) {
				return path1 + path2.substring(1);
			} else {
				return path1 + path2;
			}
		} else {
			if (path2.startsWith("/")) {
				return path1 + path2;
			} else {
				return path1 + "/" + path2;
			}
		}
	}
}
