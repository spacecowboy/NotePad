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

package com.nononsenseapps.notepad.provider;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Helper functions related to provider operations.
 */
public class ProviderHelper {
    /**
     * Returns a list uri given a base and relativePath
     * @param base such as content://my.provider.authority
     * @param relativePath /foo/bar
     * @return the list uri: content://my.provider.authority/list/foo/bar
     */
    public static Uri getListUri(@NonNull Uri base,@NonNull  String relativePath) {
        return Uri.withAppendedPath(Uri.withAppendedPath(base, "list"),
                relativePath);
    }

    /**
     * Returns a details uri given a base and relativePath
     * @param base such as content://my.provider.authority
     * @param relativePath /foo/bar
     * @return the details uri: content://my.provider.authority/details/foo/bar
     */
    public static Uri getDetailsUri(@NonNull Uri base,@NonNull  String relativePath) {
        return Uri.withAppendedPath(Uri.withAppendedPath(base, "details"),
                relativePath);
    }

    /**
     * Returns only the scheme and authority parts.
     *
     * @param uri like content://my.provider.authority/details/foo/bar
     * @return uri with only scheme and authority: content://my.provider.authority
     */
    public static Uri getBase(@NonNull Uri uri) {
        return Uri.parse(uri.getScheme() + "://" + uri.getAuthority());
    }

    /**
     * Note that /ACTION will return "/".
     *
     * @param uri like content://my.provider.authority/ACTION/foo/bar
     * @return relative path without action part like /foo/bar
     */
    public static String getRelativePath(@NonNull Uri uri) {
        return getRelativePath(uri.getPath());
    }

    static String getRelativePath(@NonNull String path) {
        int i = path.indexOf("/");
        if (i == 0) {
            return getRelativePath(path.substring(1));
        } else if (i < 0) {
            return "/";
        } else {
            return path.substring(i);
        }
    }

    /**
     * Split a path on "/", taking initial slashes into account.
     * Multiple slashes are interpreted as single slashes, just as on the file system.
     *
     * @param path like /foo/bar/baz
     * @return array like [foo, bar, baz]
     */
    public static String[] split(@NonNull String path) {
        path = path.replaceAll("/+", "/");

        if (path.startsWith("/")) {
            return split(path.substring(1));
        } else if (path.isEmpty()) {
            return new String[] {};
        }
        return path.split("/");
    }

    /**
     *
     * @param path like /foo/bar
     * @return first part of path like foo
     */
    public static String firstPart(@NonNull String path) {
        int i = path.indexOf("/");
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
     * @return the bit after first like bar/baz (without starting slash)
     */
    public static String restPart(@NonNull String path) {
        int i = path.indexOf("/");
        if (i == 0) {
            return restPart(path.substring(1));
        } else if (i > 0) {
            return path.substring(i + 1);
        } else {
            return "";
        }
    }

    public static int matchUri(@NonNull Uri uri) {
        return matchPath(uri.getPath());
    }

    public static final int URI_NOMATCH = -1;
    public static final int URI_ROOT = 101;
    public static final int URI_LIST = 102;
    public static final int URI_DETAILS = 103;
    public static final String URI_LIST_PREFIX = "list";
    public static final String URI_DETAILS_PREFIX = "details";

    /**
     * Since UriMatcher isn't as good as it should be, this implements the matching I want.
     *
     * @param path like /foo/bar/baz
     * @return type of the path
     */
    static int matchPath(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return URI_ROOT;
        } else if (path.startsWith("/")) {
            return matchPath(path.substring(1));
        } else {
            String fp = firstPart(path).toLowerCase();
            switch (fp) {
                case URI_LIST_PREFIX:
                    return URI_LIST;
                case URI_DETAILS_PREFIX:
                    // Details must specify an item
                    if (restPart(path).isEmpty()) {
                        return URI_NOMATCH;
                    } else {
                        return URI_DETAILS;
                    }
                default:
                    return URI_NOMATCH;
            }
        }
    }

    /**
     * Join two pieces together, separated by a /
     * @param path1 like /foo
     * @param path2 like bar
     * @return /foo/bar
     */
    public static String join(@NonNull String path1, @NonNull String path2) {
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
